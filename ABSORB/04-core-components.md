# 핵심 구성요소

## 이번 문서의 학습 목표

이미지 입력, 얼굴 영역 보호, 평가, 저장을 담당하는 핵심 구성요소를 연결해서 본다.

## 앞 문서와의 연결

이 문서는 [03-execution-flow.md](03-execution-flow.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 5. 핵심 기능별 구조

### 이미지 입력과 로드

* 목적: 갤러리 또는 공유 시트에서 들어온 이미지 URI를 앱 내부 `Bitmap`으로 변환한다.
* 관련 파일: [`MainActivity.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivity.kt), [`MainActivityImageFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityIntents.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityIntents.kt), [`ShareForwardingActivity.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingActivity.kt), [`ShareIntentUris.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareIntentUris.kt), [`ImageStoreLoad.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreLoad.kt), [`ImageStoreOrientation.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreOrientation.kt), [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)
* 주요 함수 또는 클래스: [`pickImageLauncher`](../app/src/main/java/com/haanghil/muulnaat/MainActivity.kt), [`processSingleImageUri`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`sharedImageUris`](../app/src/main/java/com/haanghil/muulnaat/ShareIntentUris.kt), [`ImageStore.loadBitmapFromUri`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreLoad.kt)
* 입력: Android Photo Picker 결과 URI, `Intent.EXTRA_STREAM`, `ClipData`
* 처리: URI 목록 추출, 권한 부여 flag/clipData 전달, bitmap bounds 확인, `inSampleSize` downsample, `ARGB_8888` decode, EXIF 방향 보정
* 출력: [`originalBitmap`](../app/src/main/java/com/haanghil/muulnaat/MainActivity.kt), [`binding.originalImage`](../app/src/main/java/com/haanghil/muulnaat/MainBindingAliases.kt)에 표시된 원본 이미지
* 예외 또는 주의점: 이미지 읽기 실패 시 `null`을 반환하고 UI에는 load failed 문구를 표시한다. 최대 변 1280px보다 큰 이미지는 downsample된다.
* 내가 면접에서 설명해야 할 핵심: “입력은 파일 경로가 아니라 Android URI로 받고, ContentResolver 기반으로 안전하게 읽으며 EXIF 회전까지 보정했다.”

### 보호 이미지 생성

* 목적: 원본 이미지에서 얼굴 bbox와 가능한 외곽 contour를 찾고, contour 또는 타원 근사 마스크 영역에만 선택한 은닉 방법을 적용해 보호 이미지를 만든다.
* 관련 파일: [`FaceRegionDetector.kt`](../app/src/main/java/com/haanghil/muulnaat/FaceRegionDetector.kt), [`NoiseRegionMask.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseRegionMask.kt), [`NoiseEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt), [`BlurEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/BlurEngine.kt), [`HidingMethod.kt`](../app/src/main/java/com/haanghil/muulnaat/HidingMethod.kt), [`PipelineContracts.kt`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt), [`MainActivityProtectionFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt)
* 주요 함수 또는 클래스: [`FaceRegionDetector.detectRegions`](../app/src/main/java/com/haanghil/muulnaat/FaceRegionDetector.kt), [`NoiseRegionMask.build`](../app/src/main/java/com/haanghil/muulnaat/NoiseRegionMask.kt), [`NoiseEngine.applyProtection`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt), [`PerturbationModule`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)
* 입력: `Bitmap source`, `Int strength`, optional 얼굴 `FaceProtectionRegion` 목록
* 처리: ML Kit face detection으로 bounding box와 가능한 contour를 얻고, contour가 있으면 polygon 마스크를, 없으면 bbox 안의 타원 마스크를 만든다. Noising은 edge-aware 채널별 노이즈를, Blurring은 마스크 내부 box blur를, Solid Fill은 선택 RGB 단색 채우기를 적용한다.
* 출력: 새 `Bitmap` 보호 이미지
* 예외 또는 주의점: 모든 픽셀을 순회하므로 이미지 크기에 비례해 비용이 든다. 원본 alpha는 보존하지 않고 출력 alpha를 `0xFF`로 설정한다.
* 내가 면접에서 설명해야 할 핵심: “사진 전체를 훼손하지 않고, 기기 안에서 감지한 얼굴 윤곽 또는 타원 근사 마스크에만 선택한 은닉 방법을 적용하도록 구현했다.”

### 최소 보호 강도 탐색

* 목적: 복원 후에도 방어 상태가 유지되는 최소 perturbation 농도를 찾는다.
* 관련 파일: [`NoiseSearcher.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt), [`StrengthAdvisor.kt`](../app/src/main/java/com/haanghil/muulnaat/StrengthAdvisor.kt), [`MainActivityImageFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityBatchSearch.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityBatchSearch.kt), [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt)
* 주요 함수 또는 클래스: [`NoiseSearcher.findMinimumStrength`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt), [`StrengthAdvisor.findRecommendedStrength`](../app/src/main/java/com/haanghil/muulnaat/StrengthAdvisor.kt), [`NoiseSearcher.SearchStep`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt)
* 입력: 원본 bitmap, perturbation module, defense evaluator, optional `onStep`, optional `shouldCancel`
* 처리: Noising과 Blurring은 `0, 20, 40, 60, 80` 후보를 먼저 이분 탐색하고, 여기서 통과 후보가 없으면 `81..100` 모든 정수를 이분 탐색한다. Noising은 복원 공격 후 HELD 기준, Blurring은 보호 이미지의 얼굴 특징 0개 기준이다. Solid Fill은 강도 탐색 대상이 아니다.
* 출력: 최소 통과 농도 또는 `null`
* 예외 또는 주의점: 후보 기반 탐색이므로 실제 연속값 최소가 아니라 후보 집합 안의 최소값이다.
* 내가 면접에서 설명해야 할 핵심: “낮은 농도는 20단위로 빠르게 확인하고, 방어가 어려운 80 이상 구간은 모든 정수 후보를 확인해 최대 강도 근처의 빈틈을 줄였다.”

### 방법별 평가

* 목적: 선택한 은닉 방법에 맞게 얼굴 특징 억제 여부를 판단한다. Noising은 denoising + 2x upscaling + sharpening 복원 시뮬레이션 뒤까지 보고, Blurring은 보호 이미지 자체에서 얼굴 특징이 잡히는지만 보며, Solid Fill은 평가하지 않는다.
* 관련 파일: [`RestorationAttackProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt), [`RedTeamEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt), [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) 계열 파일, [`ImageMetrics.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt), [`PipelineContracts.kt`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)
* 주요 함수 또는 클래스: [`RestorationAttackProbe.evaluateAfterAttack`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt), [`RedTeamEngine.simulateAttack`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt), [`ModelProbe.evaluate`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt), [`ImageMetrics.evaluate`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt)
* 입력: 원본 bitmap, 보호 bitmap
* 처리: Noising은 [`RedTeamEngine`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt)으로 복원 시뮬레이션 bitmap을 만든 뒤 [`ModelProbe`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)와 [`ImageMetrics`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt)로 평가한다. Blurring은 복원 시뮬레이션 없이 보호 bitmap을 바로 `ModelProbe`에 넣고 얼굴 특징 수가 0인지 확인한다.
* 출력: [`DefenseEvaluationReport`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)와 [`ProtectionStatus.HELD`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt) 또는 [`ProtectionStatus.BROKEN`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)
* 예외 또는 주의점: [`ModelProbe.evaluate()`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)는 `Tasks.await()`를 사용하므로 UI thread에서 직접 호출하면 안 된다. [`MainActivityImageFlow`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityProtectionFlow`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), [`AutoSaveWorker`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt)는 thread 안에서 호출한다.
* 내가 면접에서 설명해야 할 핵심: “방어 평가는 보호 직후가 아니라 복원 시뮬레이션 이후의 얼굴 특징/라벨 신호를 기준으로 한다.”

### 결과 표시와 기술 세부 지표

* 목적: 보호 상태, 얼굴 수 변화, 얼굴 특징 수 변화, 라벨 변화량, anti-detection score, 품질 지표를 UI에 표시한다.
* 관련 파일: [`MainActivityUi.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityUi.kt), [`MainActivityDefenseRender.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityDefenseRender.kt), [`MainActivityImageRender.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageRender.kt), [`MainActivityMetricMath.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityMetricMath.kt), [`main_image_panels.xml`](../app/src/main/res/layout/main_image_panels.xml), [`main_status_card.xml`](../app/src/main/res/layout/main_status_card.xml), [`main_model_metrics_card.xml`](../app/src/main/res/layout/main_model_metrics_card.xml), [`main_quality_metrics_card.xml`](../app/src/main/res/layout/main_quality_metrics_card.xml)
* 주요 함수 또는 클래스: [`renderDefenseResult`](../app/src/main/java/com/haanghil/muulnaat/MainActivityDefenseRender.kt), [`renderProtectedImage`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageRender.kt), [`renderRecoveredImage`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageRender.kt), [`setTechnicalDetailsVisible`](../app/src/main/java/com/haanghil/muulnaat/MainActivityUi.kt)
* 입력: [`DefenseEvaluationReport`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt), 원본/보호/복원 bitmap
* 처리: 이미지 3패널을 갱신하고, PSNR을 8..50 dB 범위로 0..100% 표시값으로 변환한다.
* 출력: 화면의 status card, model metrics, quality metrics, perturbation magnitude
* 예외 또는 주의점: 기술 세부 지표 컨테이너는 기본 숨김 상태다.
* 내가 면접에서 설명해야 할 핵심: “사용자에게는 PASS/HELD/BROKEN을 보여주고, 필요하면 세부 점수와 품질 지표를 열어볼 수 있게 했다.”

### 갤러리 저장

* 목적: 보호된 이미지를 Android 갤러리에 PNG로 저장한다.
* 관련 파일: [`ImageStore.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt), [`ImageStoreSave.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreSave.kt), [`MainActivityStorage.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityStorage.kt), [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt), [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)
* 주요 함수 또는 클래스: [`ImageStore.saveImageToGallery`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreSave.kt), [`GallerySaveResult`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt), [`runWithStoragePermissionIfNeeded`](../app/src/main/java/com/haanghil/muulnaat/MainActivityStorage.kt)
* 입력: 보호 bitmap
* 처리: 파일명 `haan_ghil_muulnaat_{timestamp}.png` 생성, MediaStore entry 생성, PNG 압축 저장, Android Q 이상에서 `IS_PENDING` 처리
* 출력: 갤러리에 저장된 PNG와 저장 결과 UI/알림
* 예외 또는 주의점: Android P 이하에서는 `WRITE_EXTERNAL_STORAGE` 권한이 필요하다. entry 생성 실패, write 실패, exception을 구분한다.
* 내가 면접에서 설명해야 할 핵심: “저장은 raw file path가 아니라 MediaStore API로 처리해 Android 버전별 저장 정책을 고려했다.”

### 공유 시트 자동 저장

* 목적: 다른 앱에서 보낸 이미지를 앱 화면 진입 없이 자동 보호/저장한다.
* 관련 파일: [`ShareForwardingActivity.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingActivity.kt), [`ShareEntrypoints.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareEntrypoints.kt), [`ShareIntentUris.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareIntentUris.kt), [`ShareForwardingPermissions.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingPermissions.kt), [`ShareForwardingIntents.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingIntents.kt), [`ShareContract.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareContract.kt), [`AutoSaveProtectionService.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveProtectionService.kt), [`AutoSaveStart.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveStart.kt), [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt), [`AutoSaveWorkerProgress.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorkerProgress.kt), [`AutoSaveNotifications.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveNotifications.kt), [`AutoSaveStatusStore.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveStatusStore.kt), [`MainActivityAutoSaveStatus.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityAutoSaveStatus.kt), [`AutoSaveFinish.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveFinish.kt), [`AutoSaveIntentParsing.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveIntentParsing.kt), [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)
* 주요 함수 또는 클래스: [`ShareReadyToSaveActivity`](../app/src/main/java/com/haanghil/muulnaat/ShareEntrypoints.kt), `ShareAutoSaveNoiseActivity`, `ShareAutoSaveBlurActivity`, `ShareAutoSaveSolidFillActivity`, [`AutoSaveProtectionService.start`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveStart.kt), [`startWorker`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt), [`notifyItemSaved`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorkerProgress.kt), [`updateProgressNotification`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveNotifications.kt), [`AutoSaveStatusStore.publish`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveStatusStore.kt)
* 입력: `ACTION_SEND`, `ACTION_SEND_MULTIPLE`, `ClipData`, URI list
* 처리: 공유 URI 수집, 권한 요청, foreground service 시작, 큐 처리, 보호 농도 탐색, 보호 적용, 저장, notification progress 업데이트, 열린 앱 화면 진행 상태 동기화
* 출력: 저장된 PNG, 진행/완료/취소 알림, MainActivity 진행 문구
* 예외 또는 주의점: 서비스는 `START_NOT_STICKY`이며, cancel 요청 시 남은 큐를 skipped로 처리한다.
* 내가 면접에서 설명해야 할 핵심: “일괄 처리 경로도 메인 보호 로직을 재사용하고, Android foreground service와 알림으로 긴 작업을 처리했다.”

### 로컬 실험 CSV 생성 스크립트

* 목적: `test-set` 안의 perturbation 적용 이미지를 얼굴 감지 기준과 얼굴 특징 기준으로 나눠 검사한다.
* 관련 파일: [`ipynbbbbb/face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py), [`ipynbbbbb/face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py)
* 주요 함수 또는 클래스: [`initialize_detectors`](../ipynbbbbb/face_detection_test_realtime.py), [`calculate_result_value`](../ipynbbbbb/face_detection_test_realtime.py), [`simulate_restoration_attack`](../ipynbbbbb/face_feature_test_realtime.py), [`initialize_probes`](../ipynbbbbb/face_feature_test_realtime.py), `main`, 각 detector/probe class
* 입력: `test-set`
* 처리: `face_detection_test_realtime.py`는 노이즈 이미지를 여러 얼굴 감지기로 검사하고, `face_feature_test_realtime.py`는 denoise + 2x upscale + sharpen 이후 MediaPipe FaceMesh/InsightFace keypoints가 얼굴 특징을 잡는지 검사한다.
* 출력: `face_detection_result.csv`, `face_feature_result.csv`
* 예외 또는 주의점: 기존 얼굴 감지 CSV 값은 얼굴 미검출 `1`, 얼굴 검출 또는 보수적 실패 `0`이다. 얼굴 특징 CSV 값은 복원 공격 후 특징 미검출 `1`, 특징 검출 또는 보수적 실패 `0`이다. 얼굴 특징 CSV는 기본 target count 1000을 기준으로 부족분을 `random_missing_<uuid>.png`와 모든 probe 값 `0`인 padding 행으로 채운다. MediaPipe가 `mp.solutions`를 제공하지 않는 환경에서는 로컬 `ipynbbbbb/face_landmarker.task` 또는 `--mediapipe-landmarker-model` 경로가 필요하며, 스크립트는 모델을 자동 다운로드하지 않는다. 두 CSV의 목적과 컬럼은 섞지 않는다.
* 내가 면접에서 설명해야 할 핵심: “앱 외부 검증은 얼굴 감지만 보는 기존 KPI와 복원 공격 후 얼굴 특징이 남는지 보는 강화 KPI를 분리했다.”

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [05-data-flow.md](05-data-flow.md)

