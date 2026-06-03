"""
face_detection_test_realtime.py

목적
----
original-set/faces/ 안의 원본 이미지들을 기준으로,
test-set/ 안에 같은 이름의 노이즈 판본이 있는지 찾고,
여러 얼굴 감지 방법으로 노이즈 판본의 얼굴 검출 성공/실패를 CSV로 기록한다.

Windows + Anaconda 환경에서 최대한 안정적으로 실행하기 위해,
기본 실행에서는 모든 detector를 시도하되 초기화 또는 smoke test에 실패한
detector는 CSV 열에서 제외한다.

기본 detector (--detectors all)
-------------------------------
- opencv_haar
- insightface
- mediapipe
- mtcnn
- retinaface
- yolo_face
- face_detection

주의
----
- face_detection 패키지는 DSFD 모델 자동 다운로드 URL이 HTTP 410 Gone으로
  실패할 수 있으므로 기본 backend를 RetinaNetMobileNetV1로 둔다.
- yolo_face는 얼굴 검출 전용 모델 파일이 필요하다. 일반 yolov8n.pt는
  얼굴 전용 모델이 아니므로 실험 결과가 의미 없을 수 있다.
- mtcnn/retinaface는 TensorFlow/Keras 버전 조합에 따라 detect 중
  KerasTensor 에러가 날 수 있다. 그래서 본 실행 전에 smoke test를 한다.

CSV 결과값
----------
N/A  = 노이즈 판본 없음 또는 노이즈 판본을 읽을 수 없음
null = 원본에서 해당 detector가 얼굴을 못 잡음
1    = 원본에서는 얼굴을 잡고, 노이즈 판본에서는 얼굴을 못 잡음
0    = 원본에서도 얼굴을 잡고, 노이즈 판본에서도 얼굴을 잡음

실시간 기록
-----------
이미지를 한 장 처리할 때마다 콘솔에 출력하고 CSV에 즉시 writerow + flush한다.
Ctrl+C로 중간 종료해도 이미 처리한 행은 CSV에 남도록 한다.

터미널 성공률
-------------
터미널에는 detector별 누적 성공률을 함께 출력한다.
성공률 계산에서는 1만 성공으로 세고, 0과 N/A는 실패로 분모에 포함한다.
null은 원본부터 detector가 얼굴을 못 잡은 경우라 분모에서 제외한다.
"""

# ============================================================
# 0. 로그 줄이기
# ============================================================

import os

os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["GLOG_minloglevel"] = "3"
os.environ.setdefault(
    "YOLO_CONFIG_DIR",
    os.path.abspath("models"),
)


# ============================================================
# 1. 기본 import
# ============================================================

from pathlib import Path
import argparse
import csv
import logging
import math
import traceback
import warnings

import cv2
import numpy as np

warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning, module="tensorflow")
warnings.filterwarnings("ignore", category=UserWarning, module="keras")
warnings.filterwarnings("ignore", category=UserWarning, module="insightface")
logging.getLogger("tensorflow").setLevel(logging.ERROR)
logging.getLogger("absl").setLevel(logging.ERROR)


# ============================================================
# 2. 기본 설정
# ============================================================

DEFAULT_ORIGINAL_DIR = Path("original-set") / "faces"
DEFAULT_TEST_DIR = Path("test-set")
DEFAULT_OUTPUT_CSV = Path("face_detection_result.csv")
ALL_DETECTOR_NAMES = [
    "opencv_haar",
    "insightface",
    "mediapipe",
    "mtcnn",
    "retinaface",
    "yolo_face",
    "face_detection",
    "face_detection_tflite",
]
DEFAULT_DETECTORS = "all"
DEFAULT_YOLO_MODEL = Path("models") / "yolov8n-face.pt"
DEFAULT_MEDIAPIPE_TASKS_MODEL = Path("models") / "face_detector.tflite"
DEFAULT_TORCH_HUB_DIR = Path("models") / "torch"
DEFAULT_FACE_DETECTION_BACKEND = "RetinaNetMobileNetV1"
DEFAULT_FACE_DETECTION_TFLITE_MODEL = "FRONT_CAMERA"
FACE_DETECTION_BACKENDS = [
    "DSFDDetector",
    "RetinaNetMobileNetV1",
    "RetinaNetResNet50",
]
FACE_DETECTION_CHECKPOINTS = {
    "DSFDDetector": "WIDERFace_DSFD_RES152.pth",
    "RetinaNetMobileNetV1": "RetinaFace_mobilenet025.pth",
    "RetinaNetResNet50": "RetinaFace_ResNet50.pth",
}

IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".webp",
    ".tif",
    ".tiff",
}

VALUE_NO_NOISY_IMAGE = "N/A"
VALUE_ORIGINAL_FACE_NOT_FOUND = "null"


# ============================================================
# 3. 출력 / 옵션 도우미
# ============================================================

def configure_quiet_logging():
    """
    라이브러리 로그를 최대한 줄인다.
    실제 초기화/detect 에러는 숨기지 않고 [SKIP]/[WARN] 메시지로 요약한다.
    """

    os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
    os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
    os.environ["GLOG_minloglevel"] = "3"

    logging.basicConfig(level=logging.ERROR)

    for logger_name in [
        "tensorflow",
        "absl",
        "mediapipe",
        "insightface",
        "onnxruntime",
        "ultralytics",
    ]:
        logging.getLogger(logger_name).setLevel(logging.ERROR)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Run face detector robustness test and write CSV row by row.",
    )

    parser.add_argument(
        "--original-dir",
        type=Path,
        default=DEFAULT_ORIGINAL_DIR,
        help=f"original image directory (default: {DEFAULT_ORIGINAL_DIR})",
    )
    parser.add_argument(
        "--test-dir",
        type=Path,
        default=DEFAULT_TEST_DIR,
        help=f"noisy image directory (default: {DEFAULT_TEST_DIR})",
    )
    parser.add_argument(
        "--output-csv",
        type=Path,
        default=DEFAULT_OUTPUT_CSV,
        help=f"output CSV path (default: {DEFAULT_OUTPUT_CSV})",
    )
    parser.add_argument(
        "--detectors",
        default=DEFAULT_DETECTORS,
        help=(
            "comma-separated detector names or 'all' "
            f"(default: {DEFAULT_DETECTORS})"
        ),
    )
    parser.add_argument(
        "--yolo-model",
        type=Path,
        default=DEFAULT_YOLO_MODEL,
        help=f"YOLO face model path (default: {DEFAULT_YOLO_MODEL})",
    )
    parser.add_argument(
        "--mediapipe-model",
        type=Path,
        default=DEFAULT_MEDIAPIPE_TASKS_MODEL,
        help=(
            "MediaPipe Tasks Face Detector .tflite model path "
            f"(default: {DEFAULT_MEDIAPIPE_TASKS_MODEL})"
        ),
    )
    parser.add_argument(
        "--face-detection-backend",
        choices=FACE_DETECTION_BACKENDS,
        default=DEFAULT_FACE_DETECTION_BACKEND,
        help=(
            "face_detection package backend "
            f"(default: {DEFAULT_FACE_DETECTION_BACKEND})"
        ),
    )
    parser.add_argument(
        "--face-detection-checkpoint",
        type=Path,
        default=None,
        help=(
            "local checkpoint path for the face_detection package. "
            "If omitted, models/<checkpoint-name> is used first."
        ),
    )
    parser.add_argument(
        "--face-detection-tflite-model",
        choices=["FRONT_CAMERA", "BACK_CAMERA", "SHORT", "FULL", "FULL_SPARSE"],
        default=DEFAULT_FACE_DETECTION_TFLITE_MODEL,
        help=(
            "model variant for face-detection-tflite/fdlite "
            f"(default: {DEFAULT_FACE_DETECTION_TFLITE_MODEL})"
        ),
    )
    parser.add_argument(
        "--torch-hub-dir",
        type=Path,
        default=DEFAULT_TORCH_HUB_DIR,
        help=(
            "PyTorch hub cache directory for face_detection checkpoints "
            f"(default: {DEFAULT_TORCH_HUB_DIR})"
        ),
    )
    parser.add_argument(
        "--no-traceback",
        action="store_true",
        help="do not print traceback when detect fails",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="reduce third-party library logs as much as possible",
    )

    return parser.parse_args()


def parse_detector_names(detectors_text: str) -> list[str]:
    if detectors_text.strip().lower() == "all":
        return list(ALL_DETECTOR_NAMES)

    names = []

    for raw_name in detectors_text.split(","):
        name = raw_name.strip()

        if name and name not in names:
            names.append(name)

    return names


def format_error(error: Exception) -> str:
    return f"{type(error).__name__}: {error}"


def print_detect_error(detector_name: str, image_path: Path, error: Exception, no_traceback: bool):
    print(
        f"[WARN] {detector_name} detect failed on {image_path.name}: {format_error(error)}",
        flush=True,
    )

    if not no_traceback:
        traceback.print_exc(limit=1)


# ============================================================
# 4. 이미지 파일 관련 도우미 함수
# ============================================================

def is_image_file(path: Path) -> bool:
    return path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS


def read_image_bgr(path: Path):
    """
    이미지를 OpenCV BGR 형식으로 읽는다.

    Windows에서 한글 경로나 특수문자가 들어간 경로를 cv2.imread가 못 읽는
    경우가 있으므로 np.fromfile + cv2.imdecode를 사용한다.
    """

    try:
        file_bytes = np.fromfile(str(path), dtype=np.uint8)
        return cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)

    except Exception:
        return None


def collect_images_by_stem(folder: Path) -> dict[str, Path]:
    result = {}

    if not folder.exists():
        return result

    image_paths = sorted(path for path in folder.iterdir() if is_image_file(path))

    for path in image_paths:
        if path.stem not in result:
            result[path.stem] = path

    return result


def collect_original_images(folder: Path) -> list[Path]:
    if not folder.exists():
        raise FileNotFoundError(f"원본 폴더가 없습니다: {folder}")

    return sorted(path for path in folder.iterdir() if is_image_file(path))


def format_console_value(value) -> str:
    return str(value)


# ============================================================
# 5. 얼굴 감지기 공통 규칙
# ============================================================

class BaseDetector:
    """
    모든 얼굴 감지기가 따라야 하는 최소 인터페이스.

    detect(image_bgr)의 반환값:
        True  = 얼굴이 하나 이상 있다.
        False = 얼굴이 없다.
    """

    name = "base"

    def detect(self, image_bgr) -> bool:
        raise NotImplementedError

    def close(self):
        pass


# ============================================================
# 6. OpenCV Haar Cascade
# ============================================================

class OpenCVHaarDetector(BaseDetector):
    name = "opencv_haar"

    def __init__(self):
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        self.detector = cv2.CascadeClassifier(cascade_path)

        if self.detector.empty():
            raise RuntimeError("Haar Cascade XML 파일을 불러오지 못했습니다.")

    def detect(self, image_bgr) -> bool:
        gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)

        faces = self.detector.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(30, 30),
        )

        return len(faces) > 0


# ============================================================
# 7. MediaPipe Face Detection
# ============================================================

class MediaPipeDetector(BaseDetector):
    """
    MediaPipe 얼굴 감지기.

    먼저 기존 mp.solutions.face_detection을 시도한다.
    mp.solutions가 없는 환경이면 MediaPipe Tasks API를 시도한다.
    Tasks API는 metadata가 포함된 Face Detector .tflite 모델 파일이 필요하다.
    """

    name = "mediapipe"

    def __init__(
        self,
        model_path: Path = DEFAULT_MEDIAPIPE_TASKS_MODEL,
        confidence_threshold: float = 0.5,
    ):
        import mediapipe as mp

        self.model_path = Path(model_path)
        self.mode = None
        self.face_detection = None
        self.task_detector = None

        solutions = getattr(mp, "solutions", None)

        if solutions is not None and hasattr(solutions, "face_detection"):
            self.face_detection = solutions.face_detection.FaceDetection(
                model_selection=1,
                min_detection_confidence=confidence_threshold,
            )
            self.mode = "solutions"
            return

        self._init_tasks_api(mp, confidence_threshold)

    def _init_tasks_api(self, mp, confidence_threshold: float):
        if not self.model_path.exists():
            raise RuntimeError(
                "MediaPipe Tasks model file not found: "
                f"{self.model_path}. Put a Google AI Edge Face Detector "
                "BlazeFace .tflite model at this path or pass --mediapipe-model."
            )

        try:
            base_options = mp.tasks.BaseOptions(model_asset_path=str(self.model_path))
            vision = mp.tasks.vision
            options = vision.FaceDetectorOptions(
                base_options=base_options,
                min_detection_confidence=confidence_threshold,
            )
            self.task_detector = vision.FaceDetector.create_from_options(options)
            self.mode = "tasks"

        except AttributeError as error:
            raise RuntimeError("MediaPipe Tasks API is not available") from error

    def detect(self, image_bgr) -> bool:
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)

        if self.mode == "solutions":
            result = self.face_detection.process(rgb)
            detections = result.detections or []
            return len(detections) > 0

        if self.mode == "tasks":
            import mediapipe as mp

            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            result = self.task_detector.detect(mp_image)
            detections = result.detections or []
            return len(detections) > 0

        raise RuntimeError("MediaPipe detector was not initialized")

    def close(self):
        for resource in [self.face_detection, self.task_detector]:
            if resource is None:
                continue

            close = getattr(resource, "close", None)

            if close is not None:
                close()


# ============================================================
# 8. MTCNN
# ============================================================

class MTCNNDetector(BaseDetector):
    name = "mtcnn"

    def __init__(self):
        from mtcnn import MTCNN

        self.detector = MTCNN()

    def detect(self, image_bgr) -> bool:
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        faces = self.detector.detect_faces(rgb)
        return len(faces) > 0


# ============================================================
# 9. RetinaFace
# ============================================================

class RetinaFaceDetector(BaseDetector):
    name = "retinaface"

    def __init__(self):
        from retinaface import RetinaFace

        self.retinaface = RetinaFace

    def detect(self, image_bgr) -> bool:
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        faces = self.retinaface.detect_faces(rgb)

        if isinstance(faces, dict):
            return len(faces) > 0

        return False


# ============================================================
# 10. InsightFace
# ============================================================

class InsightFaceDetector(BaseDetector):
    """
    InsightFace 얼굴 감지기.

    얼굴 검출만 필요하므로 allowed_modules=["detection"]으로 제한한다.
    recognition, genderage, landmark 모델은 사용하지 않는다.
    """

    name = "insightface"

    def __init__(self):
        from insightface.app import FaceAnalysis

        self.app = FaceAnalysis(
            allowed_modules=["detection"],
            providers=["CPUExecutionProvider"],
        )

        self.app.prepare(
            ctx_id=0,
            det_size=(640, 640),
        )

    def detect(self, image_bgr) -> bool:
        faces = self.app.get(image_bgr)
        return len(faces) > 0


# ============================================================
# 11. YOLO Face
# ============================================================

class YOLOFaceDetector(BaseDetector):
    """
    YOLO 얼굴 감지기.

    얼굴 검출 전용 YOLO 모델 파일이 필요하다.
    일반 yolov8n.pt는 얼굴 전용 모델이 아니므로 이 실험에 쓰면 안 된다.
    """

    name = "yolo_face"

    def __init__(
        self,
        model_path: str = DEFAULT_YOLO_MODEL,
        confidence_threshold: float = 0.5,
    ):
        model_path = Path(model_path)

        if not model_path.exists():
            raise RuntimeError(
                "face-specific YOLO model file not found: "
                f"{model_path}. Put yolov8n-face.pt there or pass --yolo-model. "
                "Do not use plain yolov8n.pt because it is not a face detector."
            )

        from ultralytics import YOLO

        self.model = YOLO(str(model_path))
        self.confidence_threshold = confidence_threshold

    def detect(self, image_bgr) -> bool:
        results = self.model(image_bgr, verbose=False)

        for result in results:
            if result.boxes is None:
                continue

            for box in result.boxes:
                confidence = float(box.conf[0])

                if confidence >= self.confidence_threshold:
                    return True

        return False


# ============================================================
# 12. face_detection 패키지
# ============================================================

class FaceDetectionPackageDetector(BaseDetector):
    """
    face_detection 패키지 기반 얼굴 감지기.

    DSFD 모델 자동 다운로드 URL이 HTTP 410 Gone으로 실패할 수 있으므로
    기본 backend는 RetinaNetMobileNetV1을 사용한다.
    PyTorch hub checkpoint는 repo 내부 models/torch/checkpoints에 직접 둔다.
    """

    name = "face_detection"

    def __init__(
        self,
        backend: str = DEFAULT_FACE_DETECTION_BACKEND,
        torch_hub_dir: Path = DEFAULT_TORCH_HUB_DIR,
        checkpoint_path: Path | None = None,
    ):
        if backend not in FACE_DETECTION_BACKENDS:
            raise RuntimeError(
                f"unsupported face_detection backend: {backend}. "
                f"Choose one of: {', '.join(FACE_DETECTION_BACKENDS)}"
            )

        checkpoint_name = FACE_DETECTION_CHECKPOINTS[backend]
        torch_hub_dir = Path(torch_hub_dir)
        candidate_paths = []

        if checkpoint_path is not None:
            candidate_paths.append(Path(checkpoint_path))

        candidate_paths.extend(
            [
                Path("models") / checkpoint_name,
                torch_hub_dir / "checkpoints" / checkpoint_name,
            ]
        )

        checkpoint_path = next(
            (path for path in candidate_paths if path.exists()),
            candidate_paths[0],
        )

        if not checkpoint_path.exists():
            raise RuntimeError(
                "face_detection checkpoint file not found: "
                f"{checkpoint_path}. Put {checkpoint_name} at models\\{checkpoint_name} "
                "or pass --face-detection-checkpoint. The script does not "
                "auto-download this file."
            )

        import torch
        import face_detection

        self._patch_face_detection_checkpoint_loader(
            backend=backend,
            torch_module=torch,
            checkpoint_path=checkpoint_path,
        )
        torch.hub.set_dir(str(torch_hub_dir))
        self.backend = backend
        self.detector = face_detection.build_detector(
            backend,
            confidence_threshold=0.5,
            nms_iou_threshold=0.3,
        )

    def _patch_face_detection_checkpoint_loader(self, backend: str, torch_module, checkpoint_path: Path):
        def load_local_state_dict(*args, **kwargs):
            map_location = kwargs.get("map_location", None)
            return torch_module.load(
                str(checkpoint_path),
                map_location=map_location,
                weights_only=False,
            )

        if backend == "DSFDDetector":
            import face_detection.dsfd.detect as detect_module

        else:
            import face_detection.retinaface.detect as detect_module
            self._patch_face_detection_retinaface_prior_box(
                detect_module=detect_module,
                torch_module=torch_module,
            )

        detect_module.load_state_dict_from_url = load_local_state_dict

    def _patch_face_detection_retinaface_prior_box(self, detect_module, torch_module):
        import face_detection.retinaface.prior_box as prior_box_module

        def generate_prior_box_fixed(feature_maps, image_size, steps, min_sizes):
            feature_maps = np.asarray(feature_maps, dtype=np.int64)
            image_size = np.asarray(image_size, dtype=np.int64)
            steps = np.asarray(steps, dtype=np.int64)

            n_anchors = 0

            for index, feature_map in enumerate(feature_maps):
                n_anchors += int(feature_map[0]) * int(feature_map[1]) * len(min_sizes[index])

            anchors = np.empty((n_anchors * 4), dtype=np.float32)
            idx_anchor = 0

            for k, feature_map in enumerate(feature_maps):
                for i in range(int(feature_map[0])):
                    for j in range(int(feature_map[1])):
                        for min_size in min_sizes[k]:
                            s_kx = float(min_size) / float(image_size[1])
                            s_ky = float(min_size) / float(image_size[0])
                            cx = (j + 0.5) * float(steps[k]) / float(image_size[1])
                            cy = (i + 0.5) * float(steps[k]) / float(image_size[0])
                            anchors[idx_anchor:idx_anchor + 4] = [cx, cy, s_kx, s_ky]
                            idx_anchor += 4

            return anchors

        class PriorBoxFixed:
            def __init__(self, cfg, image_size=None, phase="train"):
                self.min_sizes = cfg["min_sizes"]
                self.steps = np.asarray(cfg["steps"], dtype=np.int64)
                self.clip = cfg["clip"]
                self.image_size = np.asarray(image_size, dtype=np.int64)
                self.feature_maps = np.asarray(
                    [
                        [
                            math.ceil(int(self.image_size[0]) / int(step)),
                            math.ceil(int(self.image_size[1]) / int(step)),
                        ]
                        for step in self.steps
                    ],
                    dtype=np.int64,
                )
                self.name = "s"

            def forward(self):
                anchors = generate_prior_box_fixed(
                    self.feature_maps,
                    self.image_size,
                    self.steps,
                    self.min_sizes,
                )
                output = torch_module.from_numpy(anchors).view(-1, 4)

                if self.clip:
                    output.clamp_(max=1, min=0)

                return output

        prior_box_module.PriorBox = PriorBoxFixed
        prior_box_module.generate_prior_box = generate_prior_box_fixed
        detect_module.PriorBox = PriorBoxFixed

    def detect(self, image_bgr) -> bool:
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        detections = self.detector.detect(rgb)

        if detections is None:
            return False

        return len(detections) > 0


class FaceDetectionTFLiteDetector(BaseDetector):
    """
    face-detection-tflite(fdlite) 기반 얼굴 감지기.

    NumPy 2.x에서 fdlite가 np.math를 참조하는 문제가 있어 import 전에
    np.math를 표준 math 모듈로 보강한다.
    """

    name = "face_detection_tflite"

    def __init__(self, model_name: str = DEFAULT_FACE_DETECTION_TFLITE_MODEL):
        if not hasattr(np, "math"):
            np.math = math

        from fdlite import FaceDetection, FaceDetectionModel

        model_type = getattr(FaceDetectionModel, model_name)
        self.detector = FaceDetection(model_type=model_type)

    def detect(self, image_bgr) -> bool:
        from PIL import Image

        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        image = Image.fromarray(rgb)
        detections = self.detector(image)

        return len(detections) > 0


# ============================================================
# 13. detector registry / 초기화 / smoke test
# ============================================================

DETECTOR_REGISTRY = {
    "opencv_haar": OpenCVHaarDetector,
    "mediapipe": MediaPipeDetector,
    "mtcnn": MTCNNDetector,
    "retinaface": RetinaFaceDetector,
    "insightface": InsightFaceDetector,
    "yolo_face": YOLOFaceDetector,
    "face_detection": FaceDetectionPackageDetector,
    "face_detection_tflite": FaceDetectionTFLiteDetector,
}


def create_detector(name: str, args) -> BaseDetector:
    detector_class = DETECTOR_REGISTRY[name]

    if name == "mediapipe":
        return detector_class(model_path=args.mediapipe_model)

    if name == "yolo_face":
        return detector_class(model_path=args.yolo_model)

    if name == "face_detection":
        return detector_class(
            backend=args.face_detection_backend,
            torch_hub_dir=args.torch_hub_dir,
            checkpoint_path=args.face_detection_checkpoint,
        )

    if name == "face_detection_tflite":
        return detector_class(model_name=args.face_detection_tflite_model)

    return detector_class()


def initialize_detectors(selected_names: list[str], args) -> tuple[list[BaseDetector], dict[str, str]]:
    detectors = []
    init_errors = {}

    for name in selected_names:
        if name not in DETECTOR_REGISTRY:
            message = "unknown detector"
            init_errors[name] = message
            print(f"[SKIP] unknown detector: {name}", flush=True)
            continue

        try:
            detector = create_detector(name, args)
            detectors.append(detector)
            print(f"[OK] {name}", flush=True)

        except Exception as error:
            message = format_error(error)
            init_errors[name] = message
            print(f"[SKIP] {name} initialization failed: {message}", flush=True)
            print_detector_recovery_hint(name, args)

    return detectors, init_errors


def print_detector_recovery_hint(name: str, args):
    if name == "yolo_face":
        print(
            f"[HINT] yolo_face needs a face-specific model file. "
            f"Set it with --yolo-model PATH. Current: {args.yolo_model}",
            flush=True,
        )

    elif name == "mediapipe":
        print(
            "[HINT] mediapipe needs a Google AI Edge Face Detector "
            "BlazeFace .tflite model with metadata. "
            f"Put it at {args.mediapipe_model} or pass --mediapipe-model.",
            flush=True,
        )

    elif name == "face_detection":
        checkpoint_name = FACE_DETECTION_CHECKPOINTS[args.face_detection_backend]
        checkpoint_path = args.face_detection_checkpoint or Path("models") / checkpoint_name
        print(
            "[HINT] face_detection uses a local PyTorch hub checkpoint. "
            f"For backend {args.face_detection_backend}, put {checkpoint_name} "
            f"at {checkpoint_path}. This script patches the package loader "
            "to avoid the broken auto-download URL.",
            flush=True,
        )

    elif name == "face_detection_tflite":
        print(
            "[HINT] face_detection_tflite requires package face-detection-tflite. "
            "If import fails with NumPy np.math, this script applies a compatibility patch.",
            flush=True,
        )


def smoke_test_detectors(
    detectors: list[BaseDetector],
    sample_image_bgr,
) -> tuple[list[BaseDetector], dict[str, str]]:
    available_detectors = []
    smoke_errors = {}

    for detector in detectors:
        try:
            detector.detect(sample_image_bgr)
            available_detectors.append(detector)

        except Exception as error:
            message = format_error(error)
            smoke_errors[detector.name] = message
            print(
                f"[SKIP] {detector.name} smoke test failed: {message}",
                flush=True,
            )
            print_smoke_test_recovery_hint(detector.name)

            try:
                detector.close()
            except Exception:
                pass

    return available_detectors, smoke_errors


def print_smoke_test_recovery_hint(name: str):
    if name in {"mtcnn", "retinaface"}:
        print(
            "[HINT] This may be a TensorFlow/Keras compatibility issue. "
            "Current Anaconda environments with TensorFlow 2.21 + Keras 3.x "
            "can trigger KerasTensor errors. For these detectors, use a "
            "separate conda env with Python 3.10/3.11 and TensorFlow/Keras 2.x.",
            flush=True,
        )


def close_detectors(detectors: list[BaseDetector]):
    for detector in detectors:
        try:
            detector.close()

        except Exception as error:
            print(
                f"[WARN] {detector.name} close failed: {format_error(error)}",
                flush=True,
            )


# ============================================================
# 14. 얼굴 감지 실행
# ============================================================

def original_has_face(
    detector: BaseDetector,
    original_path: Path,
    no_traceback: bool,
) -> bool | None:
    image_bgr = read_image_bgr(original_path)

    if image_bgr is None:
        return None

    try:
        return detector.detect(image_bgr)

    except Exception as error:
        print_detect_error(detector.name, original_path, error, no_traceback)
        return None


def noisy_has_face(
    detector: BaseDetector,
    noisy_path: Path,
    no_traceback: bool,
) -> bool | None:
    image_bgr = read_image_bgr(noisy_path)

    if image_bgr is None:
        return None

    try:
        return detector.detect(image_bgr)

    except Exception as error:
        print_detect_error(detector.name, noisy_path, error, no_traceback)
        return None


def calculate_result_value(
    detector: BaseDetector,
    original_path: Path,
    noisy_path: Path | None,
    no_traceback: bool,
) -> str | int:
    if noisy_path is None:
        return VALUE_NO_NOISY_IMAGE

    original_result = original_has_face(detector, original_path, no_traceback)

    if original_result is not True:
        return VALUE_ORIGINAL_FACE_NOT_FOUND

    noisy_result = noisy_has_face(detector, noisy_path, no_traceback)

    if noisy_result is None:
        return VALUE_NO_NOISY_IMAGE

    if noisy_result is False:
        return 1

    return 0


def create_rate_stats(detector_names: list[str]) -> dict[str, dict[str, int]]:
    return {
        name: {
            "success_count": 0,
            "eligible_count": 0,
        }
        for name in detector_names
    }


def update_rate_stats(rate_stats: dict[str, dict[str, int]], row: dict, detector_names: list[str]):
    """
    터미널 성공률용 누적치를 갱신한다.

    1은 성공, 0과 N/A는 실패로 분모에 포함한다.
    null은 원본부터 얼굴을 못 잡은 경우라 분모에서 제외한다.
    CSV 값은 그대로 유지한다.
    """

    for name in detector_names:
        value = row[name]

        if value == VALUE_ORIGINAL_FACE_NOT_FOUND:
            continue

        rate_stats[name]["eligible_count"] += 1

        if value == 1:
            rate_stats[name]["success_count"] += 1


def format_rate_stats(rate_stats: dict[str, dict[str, int]], detector_names: list[str]) -> str:
    rate_parts = []

    for name in detector_names:
        success_count = rate_stats[name]["success_count"]
        eligible_count = rate_stats[name]["eligible_count"]

        if eligible_count == 0:
            rate_parts.append(f"{name}=N/A (0/0)")
            continue

        success_rate = success_count / eligible_count * 100
        rate_parts.append(f"{name}={success_rate:.2f}% ({success_count}/{eligible_count})")

    return ", ".join(rate_parts)


def print_rate_result(rate_stats: dict[str, dict[str, int]], detector_names: list[str]):
    print(f"  rate: {format_rate_stats(rate_stats, detector_names)}", flush=True)


def print_final_rate_summary(rate_stats: dict[str, dict[str, int]], detector_names: list[str]):
    print()
    print("[RATE] 최종 성공률 (N/A는 0으로 간주, null은 제외)", flush=True)
    print(format_rate_stats(rate_stats, detector_names), flush=True)


# ============================================================
# 15. 명령줄 출력
# ============================================================

def print_row_result(
    index: int,
    total: int,
    original_path: Path,
    noisy_path: Path | None,
    row: dict,
    detector_names: list[str],
):
    if noisy_path is None:
        noisy_text = "노이즈 없음"
    else:
        noisy_text = noisy_path.name

    result_parts = []

    for name in detector_names:
        result_parts.append(f"{name}={format_console_value(row[name])}")

    result_text = ", ".join(result_parts)

    print(f"[{index}/{total}] {original_path.name} -> {noisy_text}", flush=True)
    print(f"  {result_text}", flush=True)


def print_skip_summary(title: str, errors: dict[str, str]):
    if not errors:
        return

    print()
    print(title, flush=True)

    for name, message in errors.items():
        print(f"- {name}: {message}", flush=True)


# ============================================================
# 16. 메인 실행 함수
# ============================================================

def main() -> int:
    args = parse_args()

    if args.quiet:
        configure_quiet_logging()

    selected_names = parse_detector_names(args.detectors)

    if not selected_names:
        print("[ERROR] 선택된 detector가 없습니다.", flush=True)
        return 1

    try:
        original_paths = collect_original_images(args.original_dir)

    except FileNotFoundError as error:
        print(f"[ERROR] {error}", flush=True)
        return 1

    if not original_paths:
        print(f"[ERROR] 원본 이미지가 없습니다: {args.original_dir}", flush=True)
        return 1

    print(f"[INFO] 원본 이미지 수: {len(original_paths)}", flush=True)

    noisy_images_by_stem = collect_images_by_stem(args.test_dir)

    print(f"[INFO] 노이즈 이미지 수: {len(noisy_images_by_stem)}", flush=True)
    print(f"[INFO] 요청 detector: {', '.join(selected_names)}", flush=True)

    detectors, init_errors = initialize_detectors(selected_names, args)

    sample_image_bgr = read_image_bgr(original_paths[0])

    if sample_image_bgr is None:
        close_detectors(detectors)
        print(
            f"[ERROR] smoke test용 원본 이미지를 읽을 수 없습니다: {original_paths[0]}",
            flush=True,
        )
        return 1

    detectors, smoke_errors = smoke_test_detectors(detectors, sample_image_bgr)

    if not detectors:
        print("[ERROR] 사용 가능한 얼굴 감지기가 없습니다.", flush=True)
        print_skip_summary(
            "[INFO] 아래 detector는 초기화에 실패해서 CSV 열에서 제외되었습니다.",
            init_errors,
        )
        print_skip_summary(
            "[INFO] 아래 detector는 smoke test에 실패해서 CSV 열에서 제외되었습니다.",
            smoke_errors,
        )
        return 1

    detector_names = [detector.name for detector in detectors]
    rate_stats = create_rate_stats(detector_names)

    fieldnames = ["original_filename"]
    fieldnames.extend(detector_names)

    interrupted = False
    processed_count = 0

    try:
        with args.output_csv.open("w", newline="", encoding="utf-8-sig") as file:
            writer = csv.DictWriter(file, fieldnames=fieldnames)

            writer.writeheader()
            file.flush()

            total = len(original_paths)

            for index, original_path in enumerate(original_paths, start=1):
                row = {
                    "original_filename": original_path.name,
                }

                noisy_path = noisy_images_by_stem.get(original_path.stem)

                for detector in detectors:
                    value = calculate_result_value(
                        detector=detector,
                        original_path=original_path,
                        noisy_path=noisy_path,
                        no_traceback=args.no_traceback,
                    )

                    row[detector.name] = value

                print_row_result(
                    index=index,
                    total=total,
                    original_path=original_path,
                    noisy_path=noisy_path,
                    row=row,
                    detector_names=detector_names,
                )

                update_rate_stats(rate_stats, row, detector_names)
                print_rate_result(rate_stats, detector_names)

                writer.writerow(row)
                file.flush()
                processed_count = index

    except KeyboardInterrupt:
        interrupted = True
        print()
        print("[STOP] Ctrl+C 입력으로 중간 종료합니다.", flush=True)
        print(
            f"[STOP] 처리 완료된 이미지 수: {processed_count}/{len(original_paths)}",
            flush=True,
        )
        print(f"[STOP] 현재까지 저장된 CSV: {args.output_csv.resolve()}", flush=True)
        print_final_rate_summary(rate_stats, detector_names)

    finally:
        close_detectors(detectors)

    if interrupted:
        return 130

    print()
    print(f"[DONE] CSV 저장 완료: {args.output_csv.resolve()}", flush=True)
    print_final_rate_summary(rate_stats, detector_names)

    print_skip_summary(
        "[INFO] 아래 detector는 초기화에 실패해서 CSV 열에서 제외되었습니다.",
        init_errors,
    )
    print_skip_summary(
        "[INFO] 아래 detector는 smoke test에 실패해서 CSV 열에서 제외되었습니다.",
        smoke_errors,
    )

    return 0


if __name__ == "__main__":
    main()
