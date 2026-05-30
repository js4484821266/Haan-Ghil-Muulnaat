"""
face_detection_test_realtime.py

목적
----
original-set/faces/ 안의 원본 이미지들을 기준으로,
test-set/ 안에 같은 이름의 노이즈 판본이 있는지 찾고,
여러 얼굴 감지 방법으로 노이즈 판본의 얼굴 검출 성공/실패를 CSV로 기록한다.

이번 버전의 추가 사항
--------------------
1. face_detection 패키지 기반 감지기를 추가했다.
2. 이미지를 하나 처리할 때마다 명령줄에 결과를 출력한다.
3. 이미지를 하나 처리할 때마다 CSV에도 즉시 기록한다.
4. CSV 파일은 매 행마다 flush하므로, 중간에 프로그램이 죽어도 이미 처리된 결과는 남는다.

폴더 구조
---------
./
├─ original-set/
│  └─ faces/
│     ├─ 001.jpg
│     ├─ 002.png
│     └─ ...
│
├─ test-set/
│  ├─ 001.png
│  ├─ 002.jpg
│  └─ ...
│
└─ face_detection_test_realtime.py

중요 규칙
---------
1. CSV의 제1열은 원본 파일 이름이다.
2. 각 얼굴 감지 방법마다 1개 열을 만든다.
3. 원본과 노이즈 판본은 파일 이름의 stem이 같다.
   예:
       원본:  abc.jpg
       노이즈: abc.png
       => 같은 이미지로 본다.
4. 대응되는 노이즈 판본이 없으면 N/A
5. 대응되는 원본이 있지만, 원본에서 얼굴 감지가 안 되면 null
6. 원본에서 얼굴 감지가 됐고,
   노이즈 판본에서 얼굴 감지가 안 되면 1
   노이즈 판본에서 얼굴 감지가 되면 0

해석
----
1    = 공격 성공 / 얼굴 검출 무력화 성공
0    = 공격 실패 / 얼굴이 여전히 검출됨
N/A  = 노이즈 판본 없음 또는 노이즈 판본을 읽을 수 없음
null = 원본부터 얼굴 검출 실패라서 실험 대상 제외
"""

# ============================================================
# 0. 로그 줄이기
# ============================================================
#
# TensorFlow 계열 라이브러리는 import될 때 로그를 많이 뿌린다.
# 이 환경변수들은 TensorFlow import 전에 설정되어야 효과가 있다.

import os

os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"


# ============================================================
# 1. 기본 import
# ============================================================

from pathlib import Path
import csv
import traceback

import cv2
import numpy as np


# ============================================================
# 2. 기본 설정
# ============================================================

# 원본 이미지들이 있는 폴더
ORIGINAL_DIR = Path("original-set") / "faces"

# 노이즈가 씌워진 이미지들이 있는 폴더
TEST_DIR = Path("test-set")

# 결과 CSV 파일 이름
OUTPUT_CSV = Path("face_detection_result.csv")

# 이미지로 인정할 확장자 목록
IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".webp",
    ".tif",
    ".tiff",
}

# CSV에 쓸 특수 값
VALUE_NO_NOISY_IMAGE = "N/A"
VALUE_ORIGINAL_FACE_NOT_FOUND = "null"


# ============================================================
# 3. 이미지 파일 관련 도우미 함수
# ============================================================

def is_image_file(path: Path) -> bool:
    """
    이 파일이 이미지 파일인지 확인한다.

    True가 되는 조건:
        1. 실제 파일이다.
        2. 확장자가 IMAGE_EXTENSIONS 안에 있다.
    """

    return path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS


def read_image_bgr(path: Path):
    """
    이미지를 OpenCV BGR 형식으로 읽는다.

    왜 cv2.imread(str(path))를 바로 쓰지 않는가?
        Windows에서 한글 경로나 특수문자가 들어간 경로를
        cv2.imread가 못 읽는 경우가 있다.

    그래서:
        1. np.fromfile로 파일 바이트를 읽고
        2. cv2.imdecode로 이미지로 디코딩한다.

    반환:
        성공: OpenCV BGR 이미지 배열
        실패: None
    """

    try:
        file_bytes = np.fromfile(str(path), dtype=np.uint8)
        image = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
        return image

    except Exception:
        return None


def collect_images_by_stem(folder: Path) -> dict:
    """
    특정 폴더 안의 이미지들을 모아서
    파일 이름 stem을 key로 하는 dict를 만든다.

    stem이란?
        확장자를 뺀 파일 이름이다.

    예:
        abc.jpg -> abc
        abc.png -> abc

    원본과 노이즈 판본은 확장자가 다를 수 있다고 했으므로,
    stem 기준으로 서로 대응시킨다.

    반환 예:
        {
            "abc": Path("test-set/abc.jpg"),
            "def": Path("test-set/def.png"),
        }
    """

    result = {}

    if not folder.exists():
        return result

    image_paths = sorted(
        path for path in folder.iterdir()
        if is_image_file(path)
    )

    for path in image_paths:
        stem = path.stem

        # 같은 stem이 이미 있으면 덮어쓰지 않는다.
        # 즉, 정렬했을 때 먼저 나온 파일 하나만 사용한다.
        if stem not in result:
            result[stem] = path

    return result


def collect_original_images(folder: Path) -> list[Path]:
    """
    원본 폴더 안의 원본 이미지 목록을 만든다.

    지금 조건:
        하위 폴더는 모두 없앴다.

    따라서 rglob가 아니라 iterdir만 쓴다.
    """

    if not folder.exists():
        raise FileNotFoundError(f"원본 폴더가 없습니다: {folder}")

    image_paths = sorted(
        path for path in folder.iterdir()
        if is_image_file(path)
    )

    return image_paths


def format_console_value(value) -> str:
    """
    명령줄 출력용으로 값을 문자열로 바꾼다.

    CSV에는 int 1/0 또는 문자열 N/A/null이 들어가지만,
    콘솔에서는 모두 문자열로 보여주면 읽기 쉽다.
    """

    return str(value)


# ============================================================
# 4. 얼굴 감지기 공통 규칙
# ============================================================

class BaseDetector:
    """
    모든 얼굴 감지기가 따라야 하는 최소 인터페이스.

    각 감지기는 detect(image_bgr)를 가진다.

    detect(image_bgr)의 반환값:
        True  = 얼굴이 하나 이상 있다.
        False = 얼굴이 없다.
    """

    name = "base"

    def detect(self, image_bgr) -> bool:
        raise NotImplementedError


# ============================================================
# 5. 방법 1: OpenCV Haar Cascade
# ============================================================

class OpenCVHaarDetector(BaseDetector):
    """
    OpenCV 기본 Haar Cascade 얼굴 감지기.

    장점:
        설치가 쉽다.
        빠르다.
        opencv-python만 있으면 된다.

    단점:
        정면 얼굴 위주다.
        최신 딥러닝 방식보다 정확도가 낮다.
    """

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
# 6. 방법 2: MediaPipe Face Detection
# ============================================================

class MediaPipeDetector(BaseDetector):
    """
    MediaPipe 얼굴 감지기.

    설치:
        pip install mediapipe

    주의:
        어떤 환경에서는 mediapipe에 solutions 속성이 없을 수 있다.
        그 경우 이 감지기는 초기화 실패로 처리된다.
    """

    name = "mediapipe"

    def __init__(self, confidence_threshold: float = 0.5):
        import mediapipe as mp

        if not hasattr(mp, "solutions"):
            raise AttributeError("module 'mediapipe' has no attribute 'solutions'")

        self.face_detection = mp.solutions.face_detection.FaceDetection(
            model_selection=1,
            min_detection_confidence=confidence_threshold,
        )

    def detect(self, image_bgr) -> bool:
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)

        result = self.face_detection.process(rgb)

        detections = result.detections or []

        return len(detections) > 0


# ============================================================
# 7. 방법 3: MTCNN
# ============================================================

class MTCNNDetector(BaseDetector):
    """
    MTCNN 얼굴 감지기.

    설치:
        pip install mtcnn tensorflow

    주의:
        TensorFlow가 필요해서 무겁다.
    """

    name = "mtcnn"

    def __init__(self):
        from mtcnn import MTCNN

        self.detector = MTCNN()

    def detect(self, image_bgr) -> bool:
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)

        faces = self.detector.detect_faces(rgb)

        return len(faces) > 0


# ============================================================
# 8. 방법 4: RetinaFace
# ============================================================

class RetinaFaceDetector(BaseDetector):
    """
    RetinaFace 얼굴 감지기.

    설치:
        pip install retina-face

    주의:
        TensorFlow / Keras 버전 문제로 초기화 실패할 수 있다.
    """

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
# 9. 방법 5: InsightFace
# ============================================================

class InsightFaceDetector(BaseDetector):
    """
    InsightFace 얼굴 감지기.

    설치:
        pip install insightface onnxruntime

    이번 코드에서는 얼굴 검출만 필요하다.
    그래서 allowed_modules=["detection"]으로 제한한다.

    장점:
        불필요한 성별/나이/랜드마크/인식 모델을 덜 쓴다.
        얼굴 검출 실험에는 이쪽이 더 깔끔하다.
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
# 10. 방법 6: YOLO Face
# ============================================================

class YOLOFaceDetector(BaseDetector):
    """
    YOLO 얼굴 감지기.

    설치:
        pip install ultralytics

    추가 필요:
        얼굴 검출용 YOLO 모델 파일이 필요하다.

    기본값:
        yolov8n-face.pt

    주의:
        일반 yolov8n.pt는 얼굴 전용 모델이 아니다.
        얼굴 검출용으로 학습된 pt 파일을 써야 한다.
    """

    name = "yolo_face"

    def __init__(
        self,
        model_path: str = "yolov8n-face.pt",
        confidence_threshold: float = 0.5,
    ):
        from ultralytics import YOLO

        self.model = YOLO(model_path)
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
# 11. 방법 7: face_detection 패키지
# ============================================================

class FaceDetectionPackageDetector(BaseDetector):
    """
    face_detection 패키지 기반 얼굴 감지기.

    설치 예:
        pip install face-detection

    보통 사용 방식:
        import face_detection
        detector = face_detection.build_detector(...)
        detections = detector.detect(image)

    이 패키지는 환경에 따라 PyTorch/CUDA 관련 의존성이 필요할 수 있다.
    설치가 안 되어 있거나 모델 초기화가 안 되면
    이 감지기는 자동으로 [SKIP] 처리된다.

    반환값 처리:
        detector.detect(...)가 빈 배열/list를 반환하면 얼굴 없음.
        하나 이상 반환하면 얼굴 있음.
    """

    name = "face_detection"

    def __init__(self):
        import face_detection

        # DSFDDetector는 얼굴 검출용으로 자주 쓰이는 선택지다.
        # CPU 환경에서는 느릴 수 있다.
        self.detector = face_detection.build_detector(
            "DSFDDetector",
            confidence_threshold=0.5,
            nms_iou_threshold=0.3,
        )

    def detect(self, image_bgr) -> bool:
        # face_detection 계열 예제는 RGB 이미지를 쓰는 경우가 많다.
        rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)

        detections = self.detector.detect(rgb)

        if detections is None:
            return False

        return len(detections) > 0


# ============================================================
# 12. 감지기 초기화
# ============================================================

DETECTOR_CLASSES = [
    OpenCVHaarDetector,
    MediaPipeDetector,
    MTCNNDetector,
    RetinaFaceDetector,
    InsightFaceDetector,
    YOLOFaceDetector,
    FaceDetectionPackageDetector,
]


def initialize_detectors() -> tuple[list[BaseDetector], dict]:
    """
    사용할 수 있는 얼굴 감지기들을 초기화한다.

    중요한 점:
        어떤 라이브러리가 설치되어 있지 않아도
        전체 프로그램이 죽지 않게 한다.

    반환:
        detectors:
            실제로 사용 가능한 감지기 목록

        init_errors:
            초기화 실패한 감지기들의 에러 메시지
    """

    detectors = []
    init_errors = {}

    for detector_class in DETECTOR_CLASSES:
        name = detector_class.name

        try:
            detector = detector_class()
            detectors.append(detector)
            print(f"[OK] {name}", flush=True)

        except Exception as error:
            message = f"{type(error).__name__}: {error}"
            init_errors[name] = message
            print(f"[SKIP] {name} 초기화 실패: {message}", flush=True)

    return detectors, init_errors


# ============================================================
# 13. 원본 이미지에서 얼굴이 있는지 검사
# ============================================================

def original_has_face(detector: BaseDetector, original_path: Path) -> bool | None:
    """
    원본 이미지에서 해당 감지기로 얼굴이 검출되는지 확인한다.

    반환:
        True:
            원본에서 얼굴이 검출됨

        False:
            원본에서 얼굴이 검출되지 않음

        None:
            이미지 읽기 실패 또는 감지 중 에러 발생

    왜 원본을 먼저 보는가?
        실험의 의미는
        '원래는 잡히던 얼굴이 노이즈 후에도 잡히는가?' 이다.

        원본부터 못 잡는 방법이면,
        그 방법으로는 노이즈의 성공/실패를 평가하면 안 된다.
    """

    image_bgr = read_image_bgr(original_path)

    if image_bgr is None:
        return None

    try:
        return detector.detect(image_bgr)

    except Exception:
        traceback.print_exc(limit=1)
        return None


# ============================================================
# 14. 노이즈 이미지에서 얼굴이 있는지 검사
# ============================================================

def noisy_has_face(detector: BaseDetector, noisy_path: Path) -> bool | None:
    """
    노이즈 판본 이미지에서 해당 감지기로 얼굴이 검출되는지 확인한다.

    반환:
        True:
            노이즈 판본에서도 얼굴이 검출됨

        False:
            노이즈 판본에서 얼굴이 검출되지 않음

        None:
            이미지 읽기 실패 또는 감지 중 에러 발생
    """

    image_bgr = read_image_bgr(noisy_path)

    if image_bgr is None:
        return None

    try:
        return detector.detect(image_bgr)

    except Exception:
        traceback.print_exc(limit=1)
        return None


# ============================================================
# 15. 실험 결과값 계산
# ============================================================

def calculate_result_value(
    detector: BaseDetector,
    original_path: Path,
    noisy_path: Path | None,
) -> str | int:
    """
    CSV에 들어갈 최종 값을 계산한다.

    규칙:
        대응되는 노이즈 판본이 없으면:
            N/A

        대응되는 원본이 있지만 원본에서 얼굴 감지가 안 되면:
            null

        원본에서 얼굴 감지가 됐고,
        노이즈 판본에서 얼굴 감지가 안 되면:
            1

        원본에서 얼굴 감지가 됐고,
        노이즈 판본에서 얼굴 감지가 되면:
            0
    """

    # 1. 노이즈 판본 자체가 없으면 실험할 수 없다.
    if noisy_path is None:
        return VALUE_NO_NOISY_IMAGE

    # 2. 원본에서 얼굴이 잡히는지 먼저 확인한다.
    original_result = original_has_face(detector, original_path)

    # 3. 원본에서 얼굴이 안 잡히면,
    #    노이즈가 얼굴 검출을 무력화했는지 판단할 수 없다.
    if original_result is not True:
        return VALUE_ORIGINAL_FACE_NOT_FOUND

    # 4. 원본에서 얼굴이 잡혔다면,
    #    이제 노이즈 판본에서 얼굴이 잡히는지 확인한다.
    noisy_result = noisy_has_face(detector, noisy_path)

    # 5. 노이즈 판본이 있는데 읽기 실패했거나,
    #    감지 중 에러가 났다면 실험 불능으로 본다.
    if noisy_result is None:
        return VALUE_NO_NOISY_IMAGE

    # 6. 노이즈 판본에서 얼굴이 안 잡혔다.
    #    즉, 노이즈가 얼굴 검출을 무력화했다.
    if noisy_result is False:
        return 1

    # 7. 노이즈 판본에서도 얼굴이 잡혔다.
    #    즉, 노이즈가 얼굴 검출을 무력화하지 못했다.
    return 0


# ============================================================
# 16. 명령줄에 한 행 결과 출력
# ============================================================

def print_row_result(
    index: int,
    total: int,
    original_path: Path,
    noisy_path: Path | None,
    row: dict,
    detector_names: list[str],
):
    """
    이미지 하나의 처리 결과를 명령줄에 보기 좋게 출력한다.

    예:
        [3/188] abc.jpg -> abc.png
          opencv_haar=1, mtcnn=0, insightface=1
    """

    if noisy_path is None:
        noisy_text = "노이즈 없음"
    else:
        noisy_text = noisy_path.name

    result_parts = []

    for name in detector_names:
        result_parts.append(f"{name}={format_console_value(row[name])}")

    result_text = ", ".join(result_parts)

    print(
        f"[{index}/{total}] {original_path.name} -> {noisy_text}",
        flush=True,
    )
    print(
        f"  {result_text}",
        flush=True,
    )


# ============================================================
# 17. 메인 실행 함수
# ============================================================

def main():
    """
    전체 실행 흐름.

    1. 원본 이미지 목록을 읽는다.
    2. 노이즈 이미지 목록을 stem 기준으로 읽는다.
    3. 얼굴 감지기들을 초기화한다.
    4. CSV 파일을 바로 연다.
    5. 원본 이미지 하나를 처리할 때마다:
        5-1. 명령줄에 결과를 출력한다.
        5-2. CSV에 한 행을 쓴다.
        5-3. CSV를 flush한다.
    """

    # ------------------------------------------------------------
    # 1. 원본 이미지 목록
    # ------------------------------------------------------------

    original_paths = collect_original_images(ORIGINAL_DIR)

    if not original_paths:
        raise RuntimeError(f"원본 이미지가 없습니다: {ORIGINAL_DIR}")

    print(f"[INFO] 원본 이미지 수: {len(original_paths)}", flush=True)

    # ------------------------------------------------------------
    # 2. 노이즈 이미지 목록
    # ------------------------------------------------------------

    noisy_images_by_stem = collect_images_by_stem(TEST_DIR)

    print(f"[INFO] 노이즈 이미지 수: {len(noisy_images_by_stem)}", flush=True)

    # ------------------------------------------------------------
    # 3. 얼굴 감지기 초기화
    # ------------------------------------------------------------

    detectors, init_errors = initialize_detectors()

    if not detectors:
        raise RuntimeError("사용 가능한 얼굴 감지기가 없습니다.")

    detector_names = [detector.name for detector in detectors]

    # ------------------------------------------------------------
    # 4. CSV 헤더 만들기
    # ------------------------------------------------------------

    # 제1열은 반드시 원본 파일 이름
    fieldnames = ["original_filename"]

    # 이후에는 사용 가능한 얼굴 감지 방법별로 1개 열
    #
    # 초기화 실패한 감지기는 CSV 열에서 제외한다.
    # 이유:
    #   CSV의 값 규칙은 N/A/null/1/0으로 유지해야 하기 때문이다.
    #   초기화 실패까지 N/A로 넣으면
    #   "노이즈 판본 없음"과 의미가 섞인다.
    fieldnames.extend(detector_names)

    # ------------------------------------------------------------
    # 5. CSV 파일을 먼저 열고, 한 줄씩 실시간 기록
    # ------------------------------------------------------------

    with OUTPUT_CSV.open("w", newline="", encoding="utf-8-sig") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)

        # 헤더도 즉시 기록한다.
        writer.writeheader()
        file.flush()

        # --------------------------------------------------------
        # 6. 원본 이미지마다 결과 행 만들기
        # --------------------------------------------------------

        total = len(original_paths)

        for index, original_path in enumerate(original_paths, start=1):
            row = {
                "original_filename": original_path.name,
            }

            # 원본 파일명에서 확장자를 뺀 부분
            #
            # 예:
            #   원본 파일: abc.jpg
            #   stem: abc
            #
            # test-set 안에서 abc.png, abc.jpg, abc.webp 등을 찾을 때 쓴다.
            stem = original_path.stem

            # 같은 stem을 가진 노이즈 판본 찾기
            noisy_path = noisy_images_by_stem.get(stem)

            # 각 감지기별로 결과값 계산
            for detector in detectors:
                value = calculate_result_value(
                    detector=detector,
                    original_path=original_path,
                    noisy_path=noisy_path,
                )

                row[detector.name] = value

            # 명령줄에 즉시 출력
            print_row_result(
                index=index,
                total=total,
                original_path=original_path,
                noisy_path=noisy_path,
                row=row,
                detector_names=detector_names,
            )

            # CSV에도 즉시 기록
            writer.writerow(row)

            # 파일 버퍼를 즉시 비운다.
            # 이렇게 하면 중간에 프로그램이 멈춰도
            # 이미 처리한 줄은 CSV에 남아 있을 가능성이 높다.
            file.flush()

    print()
    print(f"[DONE] CSV 저장 완료: {OUTPUT_CSV.resolve()}", flush=True)

    # ------------------------------------------------------------
    # 7. 초기화 실패한 감지기 안내
    # ------------------------------------------------------------

    if init_errors:
        print()
        print("[INFO] 아래 감지기는 초기화에 실패해서 CSV 열에서 제외되었습니다.", flush=True)

        for name, message in init_errors.items():
            print(f"- {name}: {message}", flush=True)


if __name__ == "__main__":
    main()
