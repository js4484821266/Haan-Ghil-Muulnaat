"""
face_feature_test_realtime.py

목적
----
test-set/ 보호 이미지에 denoise + 2x upscale + sharpen 복원 공격을 적용한 뒤,
MediaPipe FaceMesh 또는 InsightFace keypoints가 눈/코/입 같은 얼굴 특징을
다시 잡아내는지 CSV로 기록한다.

CSV 결과값
----------
1 = 복원 공격 후 얼굴 특징을 못 잡음
0 = 얼굴 특징을 잡았거나, 이미지 읽기/probe 실행에 실패해 보수적으로 실패 처리

실제 이미지 수가 기본 target-count 1000보다 작으면 random_missing_<uuid>.png
이름과 모든 probe 값 0으로 padding 행을 추가한다.

기존 face_detection_result.csv와 값 규칙을 섞지 않기 위해 기본 출력은
face_feature_result.csv로 분리한다.
"""

from pathlib import Path
import argparse
import csv
import os
import traceback
import uuid

os.environ.setdefault("NO_ALBUMENTATIONS_UPDATE", "1")
os.environ.setdefault("TF_ENABLE_ONEDNN_OPTS", "0")
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "3")
os.environ.setdefault("GLOG_minloglevel", "3")
os.environ.setdefault("DISABLE_TELEMETRY", "1")

DEFAULT_TEST_DIR = Path("test-set")
DEFAULT_OUTPUT_CSV = Path("face_feature_result.csv")
DEFAULT_INSIGHTFACE_ROOT = Path.home() / ".insightface"
DEFAULT_MEDIAPIPE_LANDMARKER_MODEL = Path("ipynbbbbb") / "face_landmarker.task"
DEFAULT_TARGET_COUNT = 1000
ALL_PROBE_NAMES = ["mediapipe_facemesh", "insightface_keypoints"]
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp", ".tif", ".tiff"}


def parse_args():
    parser = argparse.ArgumentParser(
        description="Run facial-feature robustness test after restoration attack.",
    )
    parser.add_argument("--test-dir", type=Path, default=DEFAULT_TEST_DIR)
    parser.add_argument("--output-csv", type=Path, default=DEFAULT_OUTPUT_CSV)
    parser.add_argument(
        "--probes",
        default="all",
        help="comma-separated probe names or 'all'",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="allow replacing an existing output CSV",
    )
    parser.add_argument(
        "--traceback",
        action="store_true",
        help="print a short traceback when a probe fails",
    )
    parser.add_argument(
        "--insightface-root",
        type=Path,
        default=DEFAULT_INSIGHTFACE_ROOT,
        help="local InsightFace root containing models/buffalo_l/det_10g.onnx",
    )
    parser.add_argument(
        "--mediapipe-landmarker-model",
        type=Path,
        default=DEFAULT_MEDIAPIPE_LANDMARKER_MODEL,
        help=(
            "local MediaPipe FaceLandmarker .task model path "
            f"(default: {DEFAULT_MEDIAPIPE_LANDMARKER_MODEL})"
        ),
    )
    parser.add_argument(
        "--target-count",
        type=int,
        default=DEFAULT_TARGET_COUNT,
        help=f"pad CSV rows up to this count (default: {DEFAULT_TARGET_COUNT})",
    )
    return parser.parse_args()


def parse_probe_names(text: str) -> list[str]:
    if text.strip().lower() == "all":
        return list(ALL_PROBE_NAMES)

    names = []
    for raw_name in text.split(","):
        name = raw_name.strip()
        if name and name not in names:
            names.append(name)
    return names


def is_image_file(path: Path) -> bool:
    return path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS


def collect_test_images(folder: Path) -> list[Path]:
    if not folder.exists():
        raise FileNotFoundError(f"테스트 폴더가 없습니다: {folder}")
    return sorted(path for path in folder.rglob("*") if is_image_file(path))


def relative_image_name(path: Path, root: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.name


def read_image_bgr(path: Path):
    import cv2
    import numpy as np

    try:
        file_bytes = np.fromfile(str(path), dtype=np.uint8)
        return cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
    except Exception:
        return None


def simulate_restoration_attack(image_bgr):
    import cv2

    """
    앱의 RedTeamEngine과 같은 의도로 구성한 Python 기준 공격이다.
    외부 모델을 다운로드하지 않고 OpenCV 필터만 사용한다.
    """

    median = cv2.medianBlur(image_bgr, 3)
    mean = cv2.blur(image_bgr, (3, 3))
    denoised = cv2.addWeighted(median, 0.7, mean, 0.3, 0)
    upscaled = cv2.resize(
        denoised,
        None,
        fx=2.0,
        fy=2.0,
        interpolation=cv2.INTER_CUBIC,
    )
    blurred = cv2.GaussianBlur(upscaled, (0, 0), 1.0)
    return cv2.addWeighted(upscaled, 1.5, blurred, -0.5, 0)


class BaseProbe:
    name = "base"

    def has_feature(self, image_bgr) -> bool:
        raise NotImplementedError

    def close(self):
        pass


class MediaPipeFaceMeshProbe(BaseProbe):
    name = "mediapipe_facemesh"

    def __init__(self, model_path: Path = DEFAULT_MEDIAPIPE_LANDMARKER_MODEL):
        import mediapipe as mp

        self.mp = mp
        self.mode = None
        self.mesh = None
        self.landmarker = None

        solutions = getattr(mp, "solutions", None)
        if solutions is not None and hasattr(solutions, "face_mesh"):
            self.mesh = solutions.face_mesh.FaceMesh(
                static_image_mode=True,
                max_num_faces=5,
                refine_landmarks=True,
                min_detection_confidence=0.5,
            )
            self.mode = "solutions"
            return

        self._init_tasks_landmarker(mp, Path(model_path))

    def _init_tasks_landmarker(self, mp, model_path: Path):
        if not model_path.exists():
            raise RuntimeError(
                "MediaPipe mp.solutions.face_mesh is not available in this install, "
                "and local FaceLandmarker model was not found: "
                f"{model_path}. This script does not download models. "
                "Put a MediaPipe face_landmarker.task file there or pass "
                "--mediapipe-landmarker-model PATH."
            )

        base_options = mp.tasks.BaseOptions(model_asset_path=str(model_path))
        vision = mp.tasks.vision
        options = vision.FaceLandmarkerOptions(
            base_options=base_options,
            num_faces=5,
            min_face_detection_confidence=0.5,
        )
        self.landmarker = vision.FaceLandmarker.create_from_options(options)
        self.mode = "tasks"

    def has_feature(self, image_bgr) -> bool:
        import cv2

        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        if self.mode == "solutions":
            result = self.mesh.process(rgb)
            faces = result.multi_face_landmarks or []
            return any(face.landmark for face in faces)

        if self.mode == "tasks":
            mp_image = self.mp.Image(image_format=self.mp.ImageFormat.SRGB, data=rgb)
            result = self.landmarker.detect(mp_image)
            return any(landmarks for landmarks in result.face_landmarks)

        raise RuntimeError("MediaPipe probe was not initialized")

    def close(self):
        for resource in [self.mesh, self.landmarker]:
            if resource is not None:
                resource.close()


class InsightFaceKeypointProbe(BaseProbe):
    name = "insightface_keypoints"

    def __init__(self, root: Path = DEFAULT_INSIGHTFACE_ROOT):
        from insightface.app import FaceAnalysis

        root = Path(root)
        detector_model = root / "models" / "buffalo_l" / "det_10g.onnx"
        if not detector_model.exists():
            raise RuntimeError(
                "local InsightFace detector model not found: "
                f"{detector_model}. This script does not download models."
            )

        self.app = FaceAnalysis(
            name="buffalo_l",
            root=str(root),
            allowed_modules=["detection"],
            providers=["CPUExecutionProvider"],
        )
        self.app.prepare(ctx_id=0, det_size=(640, 640))

    def has_feature(self, image_bgr) -> bool:
        faces = self.app.get(image_bgr)
        for face in faces:
            keypoints = getattr(face, "kps", None)
            if keypoints is not None and len(keypoints) > 0:
                return True
        return False


PROBE_REGISTRY = {
    "mediapipe_facemesh": MediaPipeFaceMeshProbe,
    "insightface_keypoints": InsightFaceKeypointProbe,
}


def initialize_probes(selected_names: list[str], args) -> tuple[list[BaseProbe], dict[str, str]]:
    probes = []
    init_errors = {}

    for name in selected_names:
        probe_class = PROBE_REGISTRY.get(name)
        if probe_class is None:
            init_errors[name] = "unknown probe"
            print(f"[SKIP] unknown probe: {name}", flush=True)
            continue

        try:
            if name == "insightface_keypoints":
                probe = probe_class(root=args.insightface_root)
            elif name == "mediapipe_facemesh":
                probe = probe_class(model_path=args.mediapipe_landmarker_model)
            else:
                probe = probe_class()
            probes.append(probe)
            print(f"[OK] {name}", flush=True)
        except Exception as error:
            init_errors[name] = f"{type(error).__name__}: {error}"
            print(f"[SKIP] {name} initialization failed: {init_errors[name]}", flush=True)

    return probes, init_errors


def close_probes(probes: list[BaseProbe]):
    for probe in probes:
        try:
            probe.close()
        except Exception as error:
            print(f"[WARN] {probe.name} close failed: {type(error).__name__}: {error}", flush=True)


def result_value(probe: BaseProbe, attacked_bgr, show_traceback: bool) -> int:
    try:
        return 0 if probe.has_feature(attacked_bgr) else 1
    except Exception as error:
        print(f"[WARN] {probe.name} failed: {type(error).__name__}: {error}", flush=True)
        if show_traceback:
            traceback.print_exc(limit=1)
        return 0


def create_padding_row(probe_names: list[str]) -> dict:
    row = {
        "image_filename": f"random_missing_{uuid.uuid4().hex}.png",
    }
    for probe_name in probe_names:
        row[probe_name] = 0
    return row


def write_padding_rows(writer, file, probe_names: list[str], processed_count: int, target_count: int) -> int:
    missing_count = max(0, target_count - processed_count)
    if missing_count == 0:
        return 0

    print(
        f"[INFO] padding rows 추가: {missing_count} "
        f"(processed={processed_count}, target={target_count})",
        flush=True,
    )

    for index in range(1, missing_count + 1):
        row = create_padding_row(probe_names)
        writer.writerow(row)
        file.flush()

        if index == 1 or index == missing_count or index % 100 == 0:
            print(f"[PAD] {index}/{missing_count} {row['image_filename']}", flush=True)

    return missing_count


def main() -> int:
    args = parse_args()

    if args.output_csv.exists() and not args.overwrite:
        print(f"[ERROR] 출력 CSV가 이미 있습니다: {args.output_csv}", flush=True)
        print("[ERROR] 덮어쓰려면 --overwrite를 명시하세요.", flush=True)
        return 1

    try:
        image_paths = collect_test_images(args.test_dir)
    except FileNotFoundError as error:
        print(f"[ERROR] {error}", flush=True)
        return 1

    if not image_paths:
        print(f"[ERROR] 테스트 이미지가 없습니다: {args.test_dir}", flush=True)
        return 1

    selected_names = parse_probe_names(args.probes)
    probes, init_errors = initialize_probes(selected_names, args)
    if not probes:
        print("[ERROR] 사용 가능한 얼굴 특징 probe가 없습니다.", flush=True)
        for name, message in init_errors.items():
            print(f"- {name}: {message}", flush=True)
        return 1

    fieldnames = ["image_filename"] + [probe.name for probe in probes]

    try:
        with args.output_csv.open("w", newline="", encoding="utf-8-sig") as file:
            writer = csv.DictWriter(file, fieldnames=fieldnames)
            writer.writeheader()
            file.flush()

            processed_count = 0
            for index, image_path in enumerate(image_paths, start=1):
                image_name = relative_image_name(image_path, args.test_dir)
                image_bgr = read_image_bgr(image_path)
                row = {"image_filename": image_name}

                if image_bgr is None:
                    print(f"[WARN] image read failed: {image_path}", flush=True)
                    for probe in probes:
                        row[probe.name] = 0
                else:
                    attacked_bgr = simulate_restoration_attack(image_bgr)
                    for probe in probes:
                        row[probe.name] = result_value(probe, attacked_bgr, args.traceback)

                writer.writerow(row)
                file.flush()
                processed_count = index
                values = ", ".join(f"{name}={row[name]}" for name in fieldnames[1:])
                print(f"[{index}/{len(image_paths)}] {image_name}: {values}", flush=True)

            write_padding_rows(
                writer=writer,
                file=file,
                probe_names=[probe.name for probe in probes],
                processed_count=processed_count,
                target_count=args.target_count,
            )
    finally:
        close_probes(probes)

    print(f"[DONE] CSV 저장 완료: {args.output_csv.resolve()}", flush=True)
    return 0


if __name__ == "__main__":
    main()
