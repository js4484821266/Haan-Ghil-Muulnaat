# 한 길 물낯 / Haan Ghil Muulnaat

<img src="./icon.png" width="333">

**한 길 물낯**은 로컬 인물 사진 perturbation 실험과 방어 평가를 위한 Android 앱입니다. 선택한 이미지에 보호용 변형을 적용하고, 복원 시도 이후에도 얼굴 감지 방해 효과가 유지되는지 기기 안에서 점검합니다.

다른 언어의 표시명은 **Haan Ghil Muulnaat**입니다.

## 이름의 뜻

이름은 속담 "열 길 물 속은 알아도 한 길 사람 속은 모른다"에서 출발했습니다. 여기서 "물낯"은 물의 표면을 가리키며, 물에 파동이 일면 그 위에 비친 상이 일그러지는 장면을 떠올립니다. 이 이미지는 사진 위에 작게 일으킨 변화가 인식 모델의 시야를 흔드는 perturbation 개념과 맞닿아 있습니다.

## 주요 기능

- Android 갤러리 또는 공유 시트에서 인물 사진을 불러옵니다.
- 선택한 보호 농도로 로컬 이미지 perturbation을 적용합니다.
- 방어 평가 결과를 기준으로 최소 유효 농도를 탐색합니다.
- 보호 후 복원 시도에 가까운 방어 평가를 실행합니다.
- 결과를 `PASS`, `HELD`, `BROKEN` 상태로 보여줍니다.
- 보호된 이미지를 갤러리에 저장합니다.
- 공유 시트에서 여러 이미지를 받아 백그라운드 자동 저장 작업으로 처리합니다.

## 평가 범위와 한계

이 프로젝트는 실용적인 진단 도구이며, 보편적인 보안 보증이 아닙니다.

- `PASS`: 기본 보호 이미지에서 얼굴 인식 신호가 억제됐습니다.
- `HELD`: denoising + 2x upscaling + sharpening 복원 시도 이후에도 눈, 눈썹, 코, 입 관련 얼굴 특징이 감지되지 않았습니다.
- `BROKEN`: 복원 시도 뒤 얼굴 특징 신호가 하나라도 회복되어 더 강한 조정이나 다른 방법이 필요합니다.

결과는 기기, 이미지, 모델 동작, 공격 가정에 따라 달라집니다. 로컬 환경에서 `HELD`가 나왔다고 해서 모든 분류기나 복원 파이프라인에 대한 보호를 증명하는 것은 아닙니다.

## 실험 KPI

현재 로컬 얼굴 감지 실험에서 관찰한 값은 다음과 같습니다.

| 항목 | 결과 |
| --- | --- |
| CSV 평가 대상 | 실제 노이즈 이미지 13,178장 + 보수적 padding 244장 |
| CSV 목표 총량 | 13,422장 |

이번 얼굴 감지 CSV는 원본 이미지와의 쌍 비교가 아니라 `test-set` 안의 노이즈 적용 이미지 자체를 기준으로 집계했습니다. detector가 얼굴을 감지하지 못하면 성공 `1`, 얼굴을 감지하거나 보수적 실패로 처리하면 `0`으로 기록했습니다.

실제 처리된 이미지는 13,178장이며, 최대 농도에서도 얼굴 검출을 통과해 별도로 제외된 사례를 보수적으로 실패로 반영하기 위해 244개의 padding 행을 추가했습니다. padding 행은 실제 이미지 파일을 만들지 않고 CSV 집계 보정을 위해 `random_missing_...png` 이름과 모든 detector 값 `0`으로만 기록했습니다.

| 검출기 | 성공 | 성공률 |
| --- | ---: | ---: |
| OpenCV Haar | 2,659 / 13,422 | 19.81% |
| InsightFace | 11,368 / 13,422 | 84.70% |
| MTCNN | 11,066 / 13,422 | 82.45% |
| YOLO Face | 11,810 / 13,422 | 87.99% |
| face_detection | 3,472 / 13,422 | 25.87% |
| face_detection_tflite | 7,298 / 13,422 | 54.37% |

이 수치는 "원본에서 확실히 얼굴이 잡힌 이미지에 대한 방어율"이 아니라, 대규모 노이즈 결과물에 대한 detector별 얼굴 미검출률입니다. detector별 성능 차이는 모델 민감도와 구현 특성의 영향을 크게 받으므로, 단일 보안 보증이 아니라 앱의 방어 경향을 이해하기 위한 참고 지표로만 사용해야 합니다.

얼굴 감지만으로는 딥페이크 방지 목적을 충분히 검증하기 어렵습니다. 얼굴 전체 박스가 사라져도 denoising + 2x upscaling + sharpening 이후 눈, 눈썹, 코, 입 같은 특징점이 하나라도 복구되면 합성 파이프라인의 단서가 남을 수 있기 때문입니다. 그래서 기존 `face_detection_result.csv`는 얼굴 미검출 참고 지표로 유지하고, 더 엄격한 검증은 별도 얼굴 특징 실험으로 분리했습니다.

[`ipynbbbbb/face_feature_test_realtime.py`](ipynbbbbb/face_feature_test_realtime.py)는 보호 이미지에 Python 기준 denoising + 2x upscaling + sharpening을 적용한 뒤 MediaPipe FaceMesh 또는 InsightFace keypoints가 얼굴 특징을 다시 잡는지 확인합니다. 기본 출력 `face_feature_result.csv`는 `1=특징 미검출`, `0=특징 검출 또는 보수적 실패`로 기록하며, 기본 목표 행 수 1000장에 맞추기 위해 실제 이미지가 부족하면 `random_missing_...png` padding 행을 `0`으로 추가합니다.

현재 `face_feature_result.csv` 집계는 다음과 같습니다.

| 항목 | 결과 |
| --- | ---: |
| CSV 평가 대상 | 실제 보호 이미지 994장 + 보수적 padding 6장 |
| CSV 목표 총량 | 1,000장 |
| 엄격 통과 기준 | MediaPipe와 InsightFace 모두 특징 미검출 |
| 엄격 통과 | 910 / 1,000 |
| 엄격 통과율 | 91.00% |
| 엄격 실패 | 90 / 1,000 |

probe별 결과는 다음과 같습니다. 단일 probe 성공률은 참고용이며, 실제 성공 판정은 한쪽이라도 특징을 잡으면 실패로 보는 엄격 기준을 우선합니다.

| 얼굴 특징 probe | 특징 미검출 | 특징 미검출률 |
| --- | ---: | ---: |
| MediaPipe FaceMesh / FaceLandmarker | 965 / 1,000 | 96.50% |
| InsightFace keypoints | 925 / 1,000 | 92.50% |

MediaPipe가 `mp.solutions`를 제공하지 않는 환경에서는 로컬 `ipynbbbbb/face_landmarker.task` 또는 `--mediapipe-landmarker-model` 경로가 필요합니다. 이 얼굴 특징 실험은 기존 `face_detection_result.csv`와 컬럼/의미를 섞지 않습니다.

## 개인정보 모델

앱은 기기 내 처리를 기본 전제로 설계되어 있습니다. 앱 자체에는 이미지 업로드 경로나 원격 분석 파이프라인이 없으며, 선택한 이미지는 로컬에서 처리됩니다.

이미지 처리, Android 권한, Google Play Services ML Kit 모델 동작에 대한 자세한 내용은 [PRIVACY.md](PRIVACY.md)를 참고하세요.

## 빌드 및 설치

요구 사항:

- Android Studio 또는 Android SDK
- JDK 17 이상
- Windows PowerShell helper script 또는 Gradle을 직접 실행할 수 있는 환경

단위 테스트 실행:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

디버그 APK 빌드:

```powershell
.\gradlew.bat assembleDebug
```

로컬 release APK 빌드:

```powershell
.\build-android.ps1
```

연결된 Android 기기에 release APK 설치:

```powershell
.\install-apk.ps1
```

서명 환경 변수가 없으면 `build-android.ps1`은 로컬 테스트용 debug signing release APK를 만듭니다.

## Release APK

APK 바이너리는 저장소에 커밋하지 않습니다. 배포나 공유가 필요할 때는 로컬에서 release APK를 빌드한 뒤 직접 관리합니다.

기본 출력 파일:

```text
app/build/outputs/apk/release/app-release.apk
```

정식 배포용 서명이 필요하면 `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` 환경 변수를 설정한 뒤 `.\build-android.ps1`을 실행합니다.

## 저장소 메모

이전 정적 프로젝트 페이지는 보존용으로 [docs/site](docs/site)에 남아 있습니다. 이 저장소는 GitHub Pages 중심으로 구성되어 있지 않습니다.

## License

[GNU LGPL v3.0](LICENSE).
