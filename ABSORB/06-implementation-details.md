# 구현 세부

## 이번 문서의 학습 목표

핵심 알고리즘, 처리 로직, 파라미터가 전체 결과에 어떤 영향을 주는지 읽는다.

## 앞 문서와의 연결

이 문서는 [05-data-flow.md](05-data-flow.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 8. 핵심 알고리즘 또는 처리 로직

### Edge-aware perturbation

* 무엇을 하는가: [`NoiseEngine.applyProtection()`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt)이 이미지 픽셀을 순회하며 edge 세기에 따라 노이즈 amplitude를 조절한다.
* 왜 필요한가: smooth 영역보다 고주파 영역에 perturbation을 더 집중해 시각적 부담과 보호 효과 사이의 균형을 잡으려는 구조다.
* 어떤 입력을 받는가: `Bitmap source`, 보호 농도 값
* 어떤 출력을 만드는가: `Bitmap.Config.ARGB_8888` 보호 이미지
* 시간 또는 성능상 주의할 점: edge map 생성 1회와 픽셀 변환 1회가 필요하므로 O(width * height)다. 큰 이미지는 [`ImageStore`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt)에서 최대 변 1280px 기준으로 downsample된다.
* 개선 가능성: Kotlin/CPU 픽셀 루프를 RenderScript 대체 API, GPU, native, 또는 coroutine 분할 처리로 개선할 수 있다. 현재 seed는 이미지 크기와 보호 농도로 결정되어 같은 입력에는 결정적이다.

### 보호 농도 후보 이분 탐색

* 무엇을 하는가: [`NoiseSearcher.findMinimumStrength()`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt)가 후보 범위 안에서 가장 작은 통과 농도를 찾는다.
* 왜 필요한가: 가능한 모든 농도를 평가하면 ML Kit 평가와 복원 시뮬레이션 비용이 커진다.
* 어떤 입력을 받는가: `lo`, `hi`, `test(strength)`, optional progress callback, optional cancel callback
* 어떤 출력을 만드는가: 통과 후보 농도 또는 `null`
* 시간 또는 성능상 주의할 점: 각 step마다 보호 이미지 생성과 복원 후 평가가 일어난다.
* 개선 가능성: 후보 granularity를 설정값으로 빼거나, 이미지 크기/이전 결과 기반 adaptive search를 도입할 수 있다.

### 복원 시뮬레이션

* 무엇을 하는가: [`RedTeamEngine.simulateAttack()`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt)이 3x3 median/mean 혼합 필터로 denoising하고, Android filtered scaling으로 2배 확대한 뒤 unsharp mask 성격의 sharpening을 적용한다.
* 왜 필요한가: 보호 이미지가 단순 노이즈 제거/복원 이후에도 유지되는지 확인하기 위한 로컬 공격 가정이다.
* 어떤 입력을 받는가: 보호 bitmap
* 어떤 출력을 만드는가: 복원 시도 후 bitmap
* 시간 또는 성능상 주의할 점: denoise는 원본 크기, sharpening은 2배 확대 후 크기에서 수행되므로 큰 이미지에서 메모리와 CPU 비용이 증가한다.
* 개선 가능성: 실제 모델 기반 복원 평가를 붙일지는 별도 확인이 필요하다.

### ML Kit 기반 점수화

* 무엇을 하는가: [`ModelProbe.evaluate()`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)가 원본/테스트 bitmap에 대해 얼굴 수, 눈/눈썹/코/입 landmark/contour 수, image label set을 얻고 face suppression, facial feature suppression, label shift를 계산한다. 실제 ML Kit 호출은 [`ModelProbeEvaluator.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeEvaluator.kt), 특징 카운트는 [`ModelProbeFeatures.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeFeatures.kt), 점수 계산은 [`ModelProbeScoring.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeScoring.kt)가 맡는다.
* 왜 필요한가: 단순 얼굴 박스 미검출이 아니라 딥페이크 정렬/복원에 쓸 얼굴 특징 신호가 남았는지 판단하기 위해서다.
* 어떤 입력을 받는가: 원본 bitmap, 보호 또는 복원 후 bitmap
* 어떤 출력을 만드는가: [`ModelProbeResult`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt). 원본에서 얼굴/특징 기준선이 있고, 복원 공격 후 특징 수가 0이며, weighted score가 threshold 이상일 때만 통과한다.
* 시간 또는 성능상 주의할 점: `Tasks.await()`로 ML Kit 비동기 작업을 동기 대기한다. 호출자는 background thread여야 한다.
* 개선 가능성: coroutine suspend wrapper로 바꾸면 취소/에러 처리와 UI 생명주기 대응이 더 좋아진다.

### 이미지 품질 평가

* 무엇을 하는가: [`ImageMetrics.evaluate()`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt)가 PSNR, mean absolute delta, edge energy delta를 계산한다.
* 왜 필요한가: perturbation이 너무 약하거나 너무 강한지 판단할 보조 지표를 제공한다.
* 어떤 입력을 받는가: 원본 bitmap, 테스트 bitmap
* 어떤 출력을 만드는가: [`ComparisonResult`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt) 또는 [`QualityMetrics`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)
* 시간 또는 성능상 주의할 점: 이미지 크기가 다르면 작은 크기로 scale한다. 픽셀 전체를 순회하므로 O(width * height)다.
* 개선 가능성: [`ComparisonResult.passed`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt) 기준은 현재 [`RestorationAttackProbe`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt)의 상태 결정에는 직접 사용되지 않는다. 이 값을 UI나 decision에 반영할지 확인 필요다.

### MediaStore 저장

* 무엇을 하는가: [`ImageStore.saveImageToGallery()`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreSave.kt)가 PNG를 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`에 기록한다.
* 왜 필요한가: Android 버전별 저장 정책을 따르면서 갤러리에 결과물을 노출하기 위해서다.
* 어떤 입력을 받는가: 보호 bitmap
* 어떤 출력을 만드는가: [`GallerySaveResult`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt)
* 시간 또는 성능상 주의할 점: PNG 압축은 CPU와 I/O 비용이 있으며 background thread에서 호출되는 경로가 있다.
* 개선 가능성: 저장 포맷/압축률 옵션, 실패 원인 로깅 강화, 저장된 URI 반환을 추가할 수 있다.

## 9. 설정값과 파라미터

| 이름 | 위치 | 의미 | 기본값 | 바꾸면 생기는 영향 |
| --- | --- | --- | --- | --- |
| `compileSdk` | [`app/build.gradle.kts`](../app/build.gradle.kts) | Android compile SDK | 34 | 최신 API 컴파일 가능 범위 변화 |
| `minSdk` | [`app/build.gradle.kts`](../app/build.gradle.kts) | 최소 지원 Android 버전 | 26 | 낮추면 권한/MediaStore/서비스 호환성 검토 필요 |
| `targetSdk` | [`app/build.gradle.kts`](../app/build.gradle.kts) | target SDK | 34 | 권한/백그라운드 동작 정책 영향 |
| `applicationId` | [`app/build.gradle.kts`](../app/build.gradle.kts) | 앱 패키지 ID | `com.haanghil.muulnaat` | 설치/업데이트 대상 변경 |
| `versionCode`, `versionName` | [`app/build.gradle.kts`](../app/build.gradle.kts) | 앱 버전 | `1`, `1.0` | 배포 업데이트 정책 영향 |
| `ANDROID_KEYSTORE_PATH` | [`app/build.gradle.kts`](../app/build.gradle.kts), [`build-android.ps1`](../build-android.ps1) | release signing keystore path | env var | 없으면 local release는 debug signing 사용 |
| `ANDROID_KEYSTORE_PASSWORD` | [`app/build.gradle.kts`](../app/build.gradle.kts), [`build-android.ps1`](../build-android.ps1) | keystore password | env var | release signing 필요 |
| `ANDROID_KEY_ALIAS` | [`app/build.gradle.kts`](../app/build.gradle.kts), [`build-android.ps1`](../build-android.ps1) | signing key alias | env var | release signing 필요 |
| `ANDROID_KEY_PASSWORD` | [`app/build.gradle.kts`](../app/build.gradle.kts), [`build-android.ps1`](../build-android.ps1) | signing key password | env var | release signing 필요 |
| [`NOISE_BASE`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) | [`NoiseEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) | 기본 노이즈 amplitude | `2f` | 전체 perturbation 최소 농도 변화 |
| [`NOISE_STRENGTH_MULTIPLIER`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) | [`NoiseEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) | 농도당 amplitude 증가량 | `2.5f` | 농도 변화 민감도 증가/감소 |
| [`safeStrength`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) range | [`NoiseEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) | 보호 농도 제한 범위 | `0..100` | UI/탐색 범위와 맞춰야 함 |
| [`candidateStrengths`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt) | [`NoiseSearcher.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt) | 자동 탐색 후보 | `0,20,40,60,80..100` | 탐색 정확도/시간 trade-off |
| [`FACE_SUPPRESSION_WEIGHT`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | 얼굴 수 억제 점수 가중치 | `0.20` | face count 변화의 영향도 변경 |
| [`FACIAL_FEATURE_SUPPRESSION_WEIGHT`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | 얼굴 특징 억제 점수 가중치 | `0.50` | 얼굴 특징 잔존 여부 영향도 변경 |
| [`LABEL_SHIFT_WEIGHT`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | 라벨 변화량 가중치 | `0.30` | image label 변화 영향도 변경 |
| [`PASS_THRESHOLD`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | 통과 점수 threshold | `0.50` | HELD/BROKEN 판정 민감도 변경 |
| [`targetMaxSide`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt) | [`ImageStore.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt) | 로드 시 최대 변 기준 | `1280` | 품질/성능/메모리 사용량 영향 |
| PNG filename prefix | [`ImageStore.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt) | 저장 파일명 접두사 | `haan_ghil_muulnaat_` | 갤러리 파일 식별 방식 변화 |
| 저장 폴더 | [`ImageStore.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt) | Android Q 이상 상대 경로 | `Pictures/Haan Ghil Muulnaat` | 갤러리 내 저장 위치 변경 |
| notification channel | [`AutoSaveNotifications.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveNotifications.kt) | 알림 채널 ID | `protection_jobs` | 기존 채널과 호환성 영향 |
| `POST_NOTIFICATIONS` | [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml), [`ShareForwardingActivity.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingActivity.kt) | Android 13+ 알림 권한 | 런타임 요청 | 자동 저장 알림 표시 가능 여부 |
| `WRITE_EXTERNAL_STORAGE` | [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml), [`MainActivityStorage.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityStorage.kt) | Android P 이하 저장 권한 | maxSdk 28 | 구버전 저장 가능 여부 |
| `ML Kit dependencies` | [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | 설치 시 모델 의존성 힌트 | `face,ica` | Play Services 모델 준비 방식 영향 |
| [`DEFAULT_TEST_DIR`](../ipynbbbbb/face_detection_test_realtime.py) | [`face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py) | 실험 보호 이미지 경로 | `test-set` | 하위 폴더까지 재귀 탐색 |
| [`DEFAULT_TARGET_COUNT`](../ipynbbbbb/face_detection_test_realtime.py) | [`face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py) | CSV 목표 행 수 | `13422` | 부족분은 랜덤 이름과 0 값으로 padding |
| [`OUTPUT_CSV`](../ipynbbbbb/face_detection_test_realtime.py) | [`face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py) | 실험 결과 CSV | `face_detection_result.csv` | 현재 로컬 CSV는 13,422행 집계 확인 |
| [`DEFAULT_OUTPUT_CSV`](../ipynbbbbb/face_feature_test_realtime.py) | [`face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py) | 얼굴 특징 실험 결과 CSV | `face_feature_result.csv` | 기존 얼굴 감지 CSV와 분리 |
| [`DEFAULT_TARGET_COUNT`](../ipynbbbbb/face_feature_test_realtime.py) | [`face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py) | 얼굴 특징 실험 목표 행 수 | `1000` | 부족분은 보수적 실패 `0` padding |
| [`DEFAULT_MEDIAPIPE_LANDMARKER_MODEL`](../ipynbbbbb/face_feature_test_realtime.py) | [`face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py) | MediaPipe Tasks FaceLandmarker 모델 경로 | `ipynbbbbb/face_landmarker.task` | `mp.solutions` 없는 MediaPipe 환경에서 필요 |

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [07-testing-and-debugging.md](07-testing-and-debugging.md)

