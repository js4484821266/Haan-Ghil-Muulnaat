# 한 길 물낯 / Haan Ghil Muulnaat

<img src="./icon.png" width="333">

**한 길 물낯**은 로컬 인물 사진 perturbation 실험과 방어 평가를 위한 Android 앱입니다. 선택한 이미지에 보호용 변형을 적용하고, 복원 시도 이후에도 얼굴 감지 방해 효과가 유지되는지 기기 안에서 점검합니다.

다른 언어의 표시명은 **Haan Ghil Muulnaat**입니다.

## 이름의 뜻

이름은 속담 "열 길 물 속은 알아도 한 길 사람 속은 모른다"에서 출발했습니다. 여기서 "물낯"은 물의 표면을 가리키며, 물에 파동이 일면 그 위에 비친 상이 일그러지는 장면을 떠올립니다. 이 이미지는 사진 위에 작게 일으킨 변화가 인식 모델의 시야를 흔드는 perturbation 개념과 맞닿아 있습니다.

## 주요 기능

- Android 갤러리 또는 공유 시트에서 인물 사진을 불러옵니다.
- 선택한 보호 강도로 로컬 이미지 perturbation을 적용합니다.
- 방어 평가 결과를 기준으로 최소 유효 강도를 탐색합니다.
- 보호 후 복원 시도에 가까운 방어 평가를 실행합니다.
- 결과를 `PASS`, `HELD`, `BROKEN` 상태로 보여줍니다.
- 보호된 이미지를 갤러리에 저장합니다.
- 공유 시트에서 여러 이미지를 받아 백그라운드 자동 저장 작업으로 처리합니다.

## 평가 범위와 한계

이 프로젝트는 실용적인 진단 도구이며, 보편적인 보안 보증이 아닙니다.

- `PASS`: 기본 보호 이미지에서 얼굴 인식이 억제됐습니다.
- `HELD`: 복원 시도 이후에도 방해 효과가 유지됐습니다.
- `BROKEN`: 보호 이미지에 로컬 denoising 및 upscaling을 적용한 뒤 얼굴 인식 신호가 회복되어 더 강한 조정이나 다른 방법이 필요합니다.

결과는 기기, 이미지, 모델 동작, 공격 가정에 따라 달라집니다. 로컬 환경에서 `HELD`가 나왔다고 해서 모든 분류기나 복원 파이프라인에 대한 보호를 증명하는 것은 아닙니다.

## 실험 KPI

현재 로컬 실험에서 관찰한 값은 다음과 같습니다.

| 항목 | 결과 |
| --- | --- |
| 강도 탐색 공간 | 101개 후보에서 10개 후보로 축소 |
| 100장 처리 시간 | 24분에서 800초로 감소 |
| 처리 시간 개선 | 약 44.4% 감소 |
| 188장 실험 중 최대 강도에서도 방어 실패 | 30장 |

여기서 방어 실패는 보호 이미지를 로컬에서 denoising 및 upscaling한 뒤 얼굴이 다시 인식된 경우를 뜻합니다. 최대 강도에서도 방어되지 않은 사례를 실패로 포함한 보수적 집계 기준의 방어 성공률은 다음과 같습니다.

| 검출기 | 성공 | 성공률 |
| --- | ---: | ---: |
| MTCNN | 142 / 180 | 78.9% |
| InsightFace | 122 / 188 | 64.9% |

OpenCV Haar와 RetinaFace는 원본 단계의 얼굴 감지 성립성이 낮아 대표 KPI에서 제외했습니다.

이 수치는 특정 로컬 데이터셋과 실험 조건에서 얻은 관찰값입니다. 앱의 방어 성능을 이해하기 위한 참고 지표로만 사용해야 합니다.

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