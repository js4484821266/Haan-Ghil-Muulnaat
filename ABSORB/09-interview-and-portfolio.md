# 면접과 포트폴리오

## 이번 문서의 학습 목표

프로젝트를 외부에 설명할 때 무엇을 핵심으로 말해야 하는지 정리한다.

## 앞 문서와의 연결

이 문서는 [08-extension-and-maintenance.md](08-extension-and-maintenance.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 12. 면접 대비 설명 스크립트

### 30초 요약

이 프로젝트는 Android에서 인물 사진에 로컬 perturbation을 적용하고, denoising + 2x upscaling 복원 시뮬레이션 뒤에도 눈/눈썹/코/입 얼굴 특징이 남지 않는지 확인하는 앱입니다. 핵심 구조는 [`NoiseEngine`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt)의 edge-aware perturbation, [`NoiseSearcher`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt)의 최소 보호 농도 탐색, [`RestorationAttackProbe`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt)의 복원 후 평가, 그리고 [`ImageStore`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt)의 Android MediaStore 저장입니다. 공유 시트에서 여러 이미지를 받아 백그라운드로 자동 보호/저장하는 foreground service도 구현했습니다.

### 1분 설명

Haan Ghil Muulnaat는 인물 사진을 기기 안에서 처리하는 Android 앱입니다. 사용자가 갤러리나 공유 시트로 이미지를 넣으면 [`ImageStore`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt)가 bitmap으로 로드하고 EXIF 회전을 보정합니다. 이후 [`NoiseSearcher`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt)가 후보 범위 안에서 최소 통과 농도를 찾습니다. 각 후보는 [`NoiseEngine`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt)으로 보호 이미지를 만들고, [`RestorationAttackProbe`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt)가 denoising + 2x upscaling + sharpening 복원 시뮬레이션을 거친 뒤 ML Kit 얼굴 landmark/contour와 이미지 라벨링 결과를 점수화합니다. 복원 후 얼굴 특징이 하나도 남지 않으면 [`HELD`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt), 하나라도 남으면 [`BROKEN`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)으로 보여주고, 결과 이미지는 MediaStore를 통해 갤러리에 PNG로 저장합니다. 단일 수동 처리뿐 아니라 공유 시트 기반 자동 저장 서비스도 같은 핵심 파이프라인을 재사용합니다.

### 기술 질문 대응용 설명

핵심 로직은 세 부분입니다. 첫째, [`NoiseEngine`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt)은 luma gradient로 edge map을 만들고 edge가 강한 영역에 더 큰 amplitude를 주는 방식으로 perturbation을 적용합니다. 둘째, [`NoiseSearcher`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt)는 `0,20,40,60,80`을 먼저 보고 실패하면 `81..100` 모든 정수 후보를 탐색합니다. 셋째, [`RestorationAttackProbe`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt)는 [`RedTeamEngine`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt)의 denoising + 2x upscaling + sharpening 결과를 [`ModelProbe`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)와 [`ImageMetrics`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt)로 평가합니다. [`ModelProbe`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)는 ML Kit accurate face detection에 landmark/contour를 켜고, face suppression 0.20, facial feature suppression 0.50, label shift 0.30 가중치로 anti-detection score를 계산합니다. 원본에서 얼굴/특징 기준선이 있고 복원 후 얼굴 특징이 0개일 때만 통과합니다. UI thread를 막지 않도록 [`MainActivityImageFlow`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityProtectionFlow`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), [`AutoSaveWorker`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt)는 무거운 처리를 background thread에서 실행합니다.

### AI 코딩 도구 사용 질문 대응

GitHub Copilot과 Codex를 활용해 구현 속도를 높였고, 구조 이해, 테스트, 디버깅, 결과 검증을 직접 수행했습니다. 특히 코드 제안은 그대로 받아들이지 않고, [`NoiseSearcherTest`](../app/src/test/java/com/haanghil/muulnaat/NoiseSearcherTest.kt)와 [`ModelProbeTest`](../app/src/test/java/com/haanghil/muulnaat/ModelProbeTest.kt) 같은 단위 테스트로 핵심 로직을 확인했으며, Android 권한, MediaStore 저장, 공유 시트, foreground service 같은 플랫폼 동작은 실제 코드 흐름을 따라가며 검증했습니다. AI 도구는 반복 구현과 문서화 보조에 사용했고, 최종 구조 판단과 실험 결과 해석은 제가 코드와 실행 결과를 확인해 결정했습니다.

## 13. 내가 반드시 이해해야 할 코드

| 우선순위 | 파일 또는 함수 | 왜 중요한가 | 읽을 때 확인할 것 |
| --- | --- | --- | --- |
| 1 | [`startOptimalStrengthFlow`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt) | 이미지 로드 후 자동 보호 농도 탐색과 보호 적용의 중심 | thread 사용, [`StrengthAdvisor`](../app/src/main/java/com/haanghil/muulnaat/StrengthAdvisor.kt), [`runProtectionFlow`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), autoSave callback |
| 2 | [`NoiseEngine.applyProtection`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) | 실제 perturbation 생성 로직 | edge map, strength clamp, xorshift seed, channel clamp |
| 3 | [`NoiseSearcher.findMinimumStrength`](../app/src/main/java/com/haanghil/muulnaat/NoiseSearcher.kt) | 최소 보호 농도 탐색 알고리즘 | 후보 범위, 이분 탐색, 경계 반환, `null` 조건 |
| 4 | [`RestorationAttackProbe.evaluateAfterAttack`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt) | 복원 후 평가를 앱 상태로 변환 | [`RedTeamEngine`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt), [`ModelProbe`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt), [`ImageMetrics`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt), HELD/BROKEN 매핑 |
| 5 | [`ModelProbe.evaluate`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt) | 얼굴 특징/라벨 기반 점수 계산 핵심 | ML Kit 호출, landmark/contour count, feature-zero 조건 |
| 6 | [`RedTeamEngine.simulateAttack`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt) | 복원 시뮬레이션 가정 | denoising, 2x upscaling, sharpening pass |
| 7 | [`ImageStore.loadBitmapFromUri`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreLoad.kt) | Android URI 입력 처리 | downsample, EXIF orientation, 실패 시 null |
| 8 | [`ImageStore.saveImageToGallery`](../app/src/main/java/com/haanghil/muulnaat/ImageStoreSave.kt) | 출력 저장 처리 | MediaStore, PNG compress, `IS_PENDING`, 실패 타입 |
| 9 | [`startWorker`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt) | 백그라운드 일괄 처리 | queue, cancel, notification, 저장/skip count |
| 10 | [`ShareForwardingActivity`](../app/src/main/java/com/haanghil/muulnaat/ShareForwardingActivity.kt)와 [`sharedImageUris`](../app/src/main/java/com/haanghil/muulnaat/ShareIntentUris.kt) | 공유 시트 입력 분기 | `ACTION_SEND_MULTIPLE`, permission request, mode 선택 |
| 11 | [`PipelineContracts.kt`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt) | 모듈 간 데이터 계약 | status enum, metrics data class, interfaces |
| 12 | [`NoiseSearcherTest.kt`](../app/src/test/java/com/haanghil/muulnaat/NoiseSearcherTest.kt) | 탐색 로직 기대 동작 | 후보 범위와 edge case |
| 13 | [`ModelProbeTest.kt`](../app/src/test/java/com/haanghil/muulnaat/ModelProbeTest.kt) | 점수 계산 기대 동작 | face/feature suppression, label shift, threshold logic |
| 14 | [`face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py) | 앱 외부 얼굴 감지 CSV 생성 방식 | detector 초기화 실패 처리, result value 규칙 |
| 15 | [`face_feature_test_realtime.py`](../ipynbbbbb/face_feature_test_realtime.py) | 앱 외부 얼굴 특징 CSV 생성 방식 | 복원 공격, probe 초기화 실패 처리, overwrite 방지 |

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [README.md](README.md)

