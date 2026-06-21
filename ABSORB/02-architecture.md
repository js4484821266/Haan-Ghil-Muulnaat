# 아키텍처

## 이번 문서의 학습 목표

주요 파일과 모듈이 어떤 책임으로 나뉘는지 이해한다.

## 앞 문서와의 연결

이 문서는 [01-project-overview.md](01-project-overview.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 3. 전체 구조 요약

| 경로 | 역할 | 중요도 | 비고 |
| --- | --- | --- | --- |
| [`app/src/main/java/com/haanghil/muulnaat/MainActivity.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivity.kt), [`MainActivityActions.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityActions.kt), [`MainActivityImageFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityBatchFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityBatchFlow.kt), [`MainActivityBatchSearch.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityBatchSearch.kt), [`MainActivityBatchRender.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityBatchRender.kt), [`MainActivityProtectionFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), [`MainActivityStorage.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityStorage.kt), [`MainActivityUi.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityUi.kt), [`MainActivityAutoSaveStatus.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityAutoSaveStatus.kt) | 메인 UI, 이미지 선택, 보호 적용, 평가 실행, 저장 흐름 제어 | High | 실제 코드 100줄 이하 기준으로 흐름/탐색/렌더링을 분리 |
| [`MainActivityDefenseRender.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityDefenseRender.kt), [`MainActivityImageRender.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageRender.kt), [`MainActivityMetricMath.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityMetricMath.kt), [`MainActivityDialogs.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityDialogs.kt), [`MainActivityIntents.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityIntents.kt) | 메인 화면 보조 렌더링/계산/대화상자/intent 처리 | Medium | UI 파일이 한 덩어리로 비대해지지 않도록 분리 |
| [`MainBindingAliases.kt`](../app/src/main/java/com/haanghil/muulnaat/MainBindingAliases.kt), [`HeaderBindingAliases.kt`](../app/src/main/java/com/haanghil/muulnaat/HeaderBindingAliases.kt), [`TechnicalBindingAliases.kt`](../app/src/main/java/com/haanghil/muulnaat/TechnicalBindingAliases.kt) | 분리된 include layout의 ViewBinding 별칭 | Medium | Kotlin 호출부가 기존 `binding.*` 이름을 유지하도록 연결 |
| [`AutoSaveProtectionService.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveProtectionService.kt), [`AutoSaveStart.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveStart.kt), [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt), [`AutoSaveWorkerProgress.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorkerProgress.kt), [`AutoSaveNotifications.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveNotifications.kt), [`AutoSaveFinish.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveFinish.kt), [`AutoSaveNotificationChannel.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveNotificationChannel.kt), [`AutoSaveIntentParsing.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveIntentParsing.kt), [`AutoSaveStatusStore.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveStatusStore.kt) | 공유 시트 기반 백그라운드 자동 보호/저장 처리 | High | service shell, start helper, worker, progress, notification, finish, 상태 전달을 분리 |
| [`app/src/main/java/com/haanghil/muulnaat/FaceRegionDetector.kt`](../app/src/main/java/com/haanghil/muulnaat/FaceRegionDetector.kt), [`NoiseRegionMask.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseRegionMask.kt), [`NoiseEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt), [`BlurEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/BlurEngine.kt), [`HidingMethod.kt`](../app/src/main/java/com/haanghil/muulnaat/HidingMethod.kt) | 얼굴 영역 감지와 은닉 이미지 생성 | High | 같은 얼굴 마스크에 noising, blurring, solid fill 중 선택 적용 |
| [`app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt) | 보호 농도 후보에서 최소 통과 농도 탐색 | High | `0,20,40,60,80` coarse 탐색 후 `81..100` dense 탐색 |
| [`app/src/main/java/com/haanghil/muulnaat/StrengthAdvisor.kt`](../app/src/main/java/com/haanghil/muulnaat/StrengthAdvisor.kt) | 권장 보호 농도 탐색 래퍼와 UI 문구 생성 | Medium | [`NoiseSearcher`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt)를 감싼 얇은 계층 |
| [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt), [`ModelProbeEvaluator.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeEvaluator.kt), [`ModelProbeFeatures.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeFeatures.kt), [`ModelProbeScoring.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeScoring.kt), [`ModelProbeReportText.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeReportText.kt), [`ModelProbeResultFactory.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbeResultFactory.kt) | ML Kit 얼굴 특징/라벨링 결과를 점수화 | High | face suppression, facial feature suppression, label shift, anti-detection score |
| [`app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt) | 복원 시뮬레이션 후 평가 리포트 생성 | High | [`DefenseEvaluator`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt) 구현 |
| [`RedTeamEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt), [`RedTeamDenoise.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamDenoise.kt), [`RedTeamUpscale.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamUpscale.kt), [`RedTeamSharpen.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamSharpen.kt) | denoising + 2x upscaling + sharpening 복원 시뮬레이션 | High | 외부 모델 없이 필터와 Android scaled bitmap으로 공격 근사 |
| [`ImageStore.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt), [`ImageStoreLoad.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreLoad.kt), [`ImageStoreOrientation.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreOrientation.kt), [`ImageStoreSave.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreSave.kt) | URI 이미지 로드, EXIF 회전 보정, PNG 갤러리 저장 | High | API 모양은 유지하고 load/orientation/save 구현을 분리 |
| [`app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt) | 상태, 메트릭, 리포트, 인터페이스 정의 | High | 모듈 간 계약 |
| [`ShareForwardingActivity.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingActivity.kt), [`ShareEntrypoints.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareEntrypoints.kt), [`ShareIntentUris.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareIntentUris.kt), [`ShareForwardingPermissions.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingPermissions.kt), [`ShareForwardingIntents.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingIntents.kt) | Android share intent를 메인 화면 또는 자동 저장 서비스로 전달 | High | entrypoint, URI 정규화, 권한, handoff intent를 분리 |
| [`app/src/main/java/com/haanghil/muulnaat/ShareContract.kt`](../app/src/main/java/com/haanghil/muulnaat/ShareContract.kt) | 공유/서비스 intent action, extra, mode 상수 | Medium | MainActivity, 서비스, forwarding activity가 공유 |
| [`activity_main.xml`](../app/src/main/res/layout/activity_main.xml), [`main_header_controls.xml`](../app/src/main/res/layout/main_header_controls.xml), [`main_header_title.xml`](../app/src/main/res/layout/main_header_title.xml), [`main_header_pick_actions.xml`](../app/src/main/res/layout/main_header_pick_actions.xml), [`main_header_strength_controls.xml`](../app/src/main/res/layout/main_header_strength_controls.xml), [`main_header_processing_actions.xml`](../app/src/main/res/layout/main_header_processing_actions.xml), [`main_image_panels.xml`](../app/src/main/res/layout/main_image_panels.xml), [`main_status_card.xml`](../app/src/main/res/layout/main_status_card.xml), [`main_technical_details.xml`](../app/src/main/res/layout/main_technical_details.xml), [`main_model_metrics_card.xml`](../app/src/main/res/layout/main_model_metrics_card.xml), [`main_quality_metrics_card.xml`](../app/src/main/res/layout/main_quality_metrics_card.xml) | 메인 화면 레이아웃 | High | include layout도 실제 코드 100줄 이하 단위로 분리 |
| [`strings.xml`](../app/src/main/res/values/strings.xml), [`strings_notifications.xml`](../app/src/main/res/values/strings_notifications.xml), [`values-ko/strings.xml`](../app/src/main/res/values-ko/strings.xml), [`values-ko/strings_notifications.xml`](../app/src/main/res/values-ko/strings_notifications.xml) | 영어/한국어 앱 표시 문자열 | Medium | 기본/알림 문자열을 나눠 100줄 이하로 유지 |
| [`app/src/main/AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | 권한, activity, service, ML Kit dependency 선언 | High | 런처와 공유 진입점 정의 |
| [`app/src/test/java/com/haanghil/muulnaat/NoiseSearcherTest.kt`](../app/src/test/java/com/haanghil/muulnaat/NoiseSearcherTest.kt) | 보호 농도 탐색 로직 단위 테스트 | High | 후보 범위와 progress callback 검증 |
| [`app/src/test/java/com/haanghil/muulnaat/ModelProbeTest.kt`](../app/src/test/java/com/haanghil/muulnaat/ModelProbeTest.kt) | 점수 계산 보조 함수 단위 테스트 | High | ML Kit 호출 자체는 테스트하지 않음 |
| [`app/build.gradle.kts`](../app/build.gradle.kts) | Android 모듈 빌드 설정과 의존성 | High | ML Kit, TensorFlow Lite 의존성 포함 |
| [`build-android.ps1`](../build-android.ps1) | Windows release APK 빌드 helper | Medium | signing env var 없으면 debug signing release |
| `install-apk.ps1` | release APK를 연결 기기에 설치하는 helper | Medium | 현재 repo에서 파일이 확인되지 않으므로 사용 전 존재 여부 확인 필요 |
| [`ipynbbbbb/face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py) | 로컬 얼굴 감지 실험 CSV 생성 스크립트 | Medium | 기존 얼굴 미검출률 실험 |
| [`ipynbbbbb/face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py) | 복원 공격 후 얼굴 특징 실험 CSV 생성 스크립트 | Medium | `face_feature_result.csv`, 기존 얼굴 감지 CSV와 분리 |
| [`docs/site/index.html`](../docs/site/index.html), [`script.js`](../docs/site/script.js), [`styles.css`](../docs/site/styles.css), [`translations.js`](../docs/site/translations.js) | 보존용 정적 사이트 | Low | 앱 실행에는 직접 관여하지 않음 |
| [`README.md`](../README.md), [`PRIVACY.md`](../PRIVACY.md), [`LICENSE`](../LICENSE) | 공개 문서와 라이선스 | Medium | 구조 이해의 보조 자료 |

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [03-execution-flow.md](03-execution-flow.md)


