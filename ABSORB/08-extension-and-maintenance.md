# 확장과 유지보수

## 이번 문서의 학습 목표

현재 구조의 약점과 다음 개선 지점을 보되, 기존 설계 기준을 망가뜨리지 않는 방법을 정리한다.

## 앞 문서와의 연결

이 문서는 [07-testing-and-debugging.md](07-testing-and-debugging.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 14. 현재 구조의 약점

- 메인 화면 로직은 역할별 파일로 쪼갰지만, 여전히 [`MainActivity`](../app/src/main/java/com/haanghil/muulnaat/MainActivity.kt)의 extension 함수들이 화면 상태를 직접 공유한다.
- `thread {}`를 직접 사용해 Android lifecycle 취소, configuration change, structured concurrency 대응이 약하다.
- [`ModelProbe.evaluate()`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)가 `Tasks.await()`를 사용하므로 호출자가 background thread를 지켜야 한다.
- [`RedTeamEngine`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt)는 실제 생성형 복원 모델이 아니라 직접 구현한 필터 기반 시뮬레이션이다.
- [`ImageMetrics.evaluate()`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt)의 [`ComparisonResult.passed`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt)는 현재 최종 `ProtectionStatus` 결정에 쓰이지 않는다.
- 자동 저장 서비스가 bitmap recycle이나 메모리 압박 대응을 적극적으로 하지 않는다.
- ML Kit 모델 다운로드/업데이트는 Google Play Services 동작에 의존한다. 오프라인 최초 실행 상태는 확인 필요다.
- Python 실험 스크립트는 다양한 외부 라이브러리에 의존하지만 requirements 파일이 확인되지 않는다.
- 앱 UI 문자열과 내부 클래스명이 일부 연구/실험 용어를 유지한다. 포트폴리오 톤과 사용자용 톤을 더 분리할 수 있다.
- 실제 end-to-end instrumentation test는 확인되지 않는다.

## 15. 다음 개선 과제

| 난이도 | 개선 과제 | 기대 효과 | 관련 파일 |
| --- | --- | --- | --- |
| Easy | [`RedTeamEngine`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt) 주석을 실제 구현 기준으로 정리 | TFLite 사용 여부 오해 감소 | [`RedTeamEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt), [`app/build.gradle.kts`](../app/build.gradle.kts) |
| Easy | `ImageMetrics.evaluate().passed` 사용 여부 결정 | dead-ish field 정리 또는 평가 강화 | [`ImageMetrics.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageMetrics.kt), [`RestorationAttackProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt) |
| Easy | Python 실험 스크립트용 requirements 문서화 | 재현성 향상 | [`ipynbbbbb/face_detection_test_realtime.py`](../ipynbbbbb/face_detection_test_realtime.py) |
| Medium | 메인 화면 extension 함수에서 pipeline/use-case 계층 분리 | 유지보수성과 테스트 용이성 향상 | [`MainActivityImageFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityProtectionFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), 신규 use-case 파일 |
| Medium | `thread {}`를 coroutine/ViewModel 기반으로 전환 | lifecycle 대응과 취소 처리 개선 | [`MainActivityImageFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityImageFlow.kt), [`MainActivityProtectionFlow.kt`](../app/src/main/java/com/haanghil/muulnaat/MainActivityProtectionFlow.kt), [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt) |
| Medium | [`ModelProbe.evaluate()`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)를 suspend API로 감싸기 | 비동기 흐름 명확화 | [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt), [`RestorationAttackProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt) |
| Medium | 자동 저장 서비스 메모리 관리와 진행 상태 저장 강화 | 다중 이미지 처리 안정성 향상 | [`AutoSaveWorker.kt`](../app/src/main/java/com/haanghil/muulnaat/AutoSaveWorker.kt), [`ImageStore.kt`](../app/src/main/java/com/haanghil/muulnaat/ImageStore.kt) |
| Medium | instrumentation test 또는 Robolectric 도입 | Android URI/MediaStore/UI 흐름 검증 | `app/src/androidTest`, Gradle 설정 |
| Hard | 실제 복원 모델 기반 평가 옵션 추가 | 공격 가정 현실성 향상 | [`RedTeamEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt), assets/model, Gradle |
| Hard | perturbation 알고리즘 GPU/native 최적화 | 큰 이미지 처리 성능 개선 | [`NoiseEngine.kt`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt) |
| Hard | 평가 정책을 설정 가능하게 만들기 | 사용자/실험별 threshold 조정 가능 | [`ModelProbe.kt`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt), UI, persistence |

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [09-interview-and-portfolio.md](09-interview-and-portfolio.md)

