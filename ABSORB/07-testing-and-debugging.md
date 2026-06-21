# 테스트와 디버깅

## 이번 문서의 학습 목표

프로젝트를 실행하고 검증할 때 확인해야 하는 명령과 관찰 지점을 익힌다.

## 앞 문서와의 연결

이 문서는 [06-implementation-details.md](06-implementation-details.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 10. 실행 방법

현재 저장소에서 코드와 스크립트로 확인한 실행 방법은 다음과 같다.

### 단위 테스트

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

이 문서 작성 중 `.\gradlew.bat :app:testDebugUnitTest --no-daemon`를 실행했고 성공했다. 단, sandbox 내부 첫 실행은 사용자 Gradle cache lock 접근 권한 때문에 실패했으며, 승인 권한으로 재실행해 통과했다.

### 디버그 APK 빌드

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

이 명령은 현재 저장소에서 실행되어 성공한 이력이 있다.

### 로컬 release APK 빌드

```powershell
.\build-android.ps1
```

[`build-android.ps1`](../build-android.ps1)는 `JAVA_HOME`이 없으면 Android Studio JBR 경로를 시도하고, signing 환경변수가 없으면 debug signing release APK를 만든다.

### 기기 설치

`install-apk.ps1`는 기존 교재에 설치 helper로 언급되어 있지만, 현재 repo에서는 파일이 확인되지 않았다. 기기 설치가 필요하면 먼저 helper 파일 존재 여부나 `adb install` 경로를 확인해야 한다.

### Python 실험 스크립트

```powershell
python ipynbbbbb\face_detection_test_realtime.py
```

스크립트 파일은 추적 대상이다. 필요한 Python 패키지 설치 상태와 `test-set/` 전체 내용은 환경마다 확인이 필요하지만, 현재 로컬 `face_detection_result.csv`는 13,178개 실제 이미지 행과 244개 padding 행으로 구성된 13,422행 결과로 확인했다.

얼굴 특징 기준 실험은 별도 파일과 별도 CSV를 사용한다.

```powershell
python ipynbbbbb\face_feature_test_realtime.py --test-dir test-set
```

`face_feature_test_realtime.py`는 `face_feature_result.csv`가 이미 있으면 `--overwrite` 없이는 덮어쓰지 않는다. 이 스크립트는 MediaPipe FaceMesh 또는 InsightFace keypoints 중 현재 환경에서 초기화 가능한 probe만 CSV 열로 사용한다. MediaPipe `mp.solutions.face_mesh`가 없으면 Tasks API의 FaceLandmarker를 시도하고, 이때 `ipynbbbbb/face_landmarker.task` 또는 `--mediapipe-landmarker-model`로 지정한 로컬 모델이 필요하다. 실제 이미지 수가 기본 1000장보다 적으면 부족분은 보수적 실패 `0` padding 행으로 채운다.

## 11. 테스트와 검증 방법

### 코드에서 확인 가능한 자동 테스트

| 테스트 파일 | 검증 대상 | 확인 내용 |
| --- | --- | --- |
| [`NoiseSearcherTest.kt`](../app/src/test/java/com/haanghil/muulnaat/NoiseSearcherTest.kt) | [`NoiseSearcher.findMinimumStrength`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt) | 통과 후보 없음, lo 통과, 최고 후보 통과, 20단위 후보 보정, `81..100` 정수 후보, progress step |
| [`ModelProbeTest.kt`](../app/src/test/java/com/haanghil/muulnaat/ModelProbeTest.kt) | [`ModelProbe`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) 보조 계산 | face suppression, feature suppression, label shift, weighted score, feature-zero 통과 조건 |

자동 테스트에서 실제 ML Kit 호출, Android UI, MediaStore 저장, foreground service 동작은 검증하지 않는다.

### 수동 검증 포인트

- Photo Picker에서 이미지를 선택하고 원본 이미지가 표시되는지 확인한다.
- 자동 농도 탐색이 진행 메시지를 표시하는지 확인한다.
- 보호 이미지가 표시되고 perturbation magnitude가 갱신되는지 확인한다.
- `Run Defense Evaluation` 실행 후 복원 이미지와 metrics가 표시되는지 확인한다.
- Android P 이하에서 저장 권한 요청이 정상 동작하는지 확인한다.
- Android 13 이상에서 자동 저장 공유 대상이 알림 권한을 요청하는지 확인한다.
- 공유 시트에서 단일 이미지와 다중 이미지 모두 정상 처리되는지 확인한다.
- 자동 저장 알림에서 cancel action이 큐를 중단하는지 확인한다.

### CSV, 로그, 성공률, 처리 시간

- [`ipynbbbbb/face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py)는 `face_detection_result.csv`를 생성하도록 작성되어 있다.
- 이 스크립트는 `test-set`의 각 이미지 상대 경로와 사용 가능한 얼굴 감지기별 1/0 결과를 CSV에 즉시 기록하고 flush한다.
- CSV 값은 얼굴 미검출 `1`, 얼굴 검출 또는 보수적 실패 처리 `0`이다. 이번 CSV는 원본 이미지와의 쌍 비교 없이 노이즈 적용 이미지 자체를 기준으로 집계했다.
- 현재 CSV 집계는 총 13,422행이며, 실제 이미지 13,178장과 padding 244장으로 구성된다.
- detector별 얼굴 미검출률은 OpenCV Haar 19.81%, InsightFace 84.70%, MTCNN 82.45%, YOLO Face 87.99%, face_detection 25.87%, face_detection_tflite 54.37%다.
- [`ipynbbbbb/face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py)는 `face_feature_result.csv`를 생성한다. 값은 복원 공격 후 얼굴 특징 미검출 `1`, 특징 검출 또는 보수적 실패 `0`이다. 기본 목표 행 수는 1000이고 부족분은 padding 행으로 추가한다.

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [08-extension-and-maintenance.md](08-extension-and-maintenance.md)




