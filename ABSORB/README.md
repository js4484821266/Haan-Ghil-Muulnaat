# ABSORB 학습교재

## 교재의 목적

이 교재는 Haan Ghil Muulnaat의 완성된 Android 앱 구조를 순서대로 흡수하기 위한 문서다. 단일 `ABSORB.md`에 있던 내용을 보존하되, 새 `AGENTS.md` 규칙에 맞춰 `ABSORB/README.md`에서 시작하는 단계별 학습 흐름으로 나누었다.

## 학습 대상과 필요한 선수 지식

- Kotlin과 Android Activity, Service, ViewBinding의 기본 개념을 아는 학습자
- 이미지 비트맵 처리, ML Kit face detection, Android MediaStore 저장 흐름을 프로젝트 코드 안에서 배우려는 학습자
- Gradle 기반 Android 프로젝트의 파일 구조를 읽을 수 있는 학습자

## 완성된 프로젝트 요약

Haan Ghil Muulnaat는 Android에서 인물 사진을 불러와 얼굴 윤곽 또는 타원형 근사 마스크 영역에 noising, blurring, solid fill을 적용하고, 로컬 복원 시뮬레이션 이후에도 얼굴 특징 억제가 유지되는지 평가한 뒤 보호 이미지를 갤러리에 저장하는 앱이다. 공유 시트로 받은 이미지도 foreground service를 통해 자동 보호하고 저장할 수 있다.

## 전체 학습 순서

1. [프로젝트 개요](01-project-overview.md)
2. [아키텍처](02-architecture.md)
3. [실행 흐름](03-execution-flow.md)
4. [핵심 구성요소](04-core-components.md)
5. [데이터 흐름](05-data-flow.md)
6. [구현 세부](06-implementation-details.md)
7. [테스트와 디버깅](07-testing-and-debugging.md)
8. [확장과 유지보수](08-extension-and-maintenance.md)
9. [면접과 포트폴리오](09-interview-and-portfolio.md)

## 각 문서에서 배우는 내용

| 문서 | 배우는 내용 |
| --- | --- |
| [01-project-overview.md](01-project-overview.md) | 프로젝트가 해결하는 문제와 최종 결과 |
| [02-architecture.md](02-architecture.md) | 주요 파일 목록, 모듈 책임, 파일 간 연결 관계 |
| [03-execution-flow.md](03-execution-flow.md) | 메인 화면 수동 흐름과 공유 시트 자동 저장 흐름 |
| [04-core-components.md](04-core-components.md) | 이미지 입력, 보호 이미지 생성, 평가, 저장의 핵심 구성요소 |
| [05-data-flow.md](05-data-flow.md) | 앱 내부 이미지 데이터, 평가 데이터, CSV 실험 데이터의 이동 |
| [06-implementation-details.md](06-implementation-details.md) | 알고리즘, 처리 로직, 설정값과 파라미터 |
| [07-testing-and-debugging.md](07-testing-and-debugging.md) | 실행 명령, 단위 테스트, 수동 검증 포인트 |
| [08-extension-and-maintenance.md](08-extension-and-maintenance.md) | 현재 구조의 약점, 다음 개선 과제, 유지보수 기준 |
| [09-interview-and-portfolio.md](09-interview-and-portfolio.md) | 면접과 포트폴리오에서 설명할 핵심 내용 |

## 프로젝트 실행에 필요한 환경

- Android Studio 또는 Gradle wrapper를 실행할 수 있는 JDK 환경
- Android SDK 경로가 담긴 [../local.properties](../local.properties)
- Android 앱 빌드 설정인 [../app/build.gradle.kts](../app/build.gradle.kts)
- 단위 테스트 실행용 Gradle wrapper [../gradlew.bat](../gradlew.bat)

## 원본 코드와 주요 산출물

- 앱 핵심 코드: [../app/src/main/java/com/haanghil/muulnaat](../app/src/main/java/com/haanghil/muulnaat)
- 단위 테스트: [../app/src/test/java/com/haanghil/muulnaat](../app/src/test/java/com/haanghil/muulnaat)
- 앱 리소스: [../app/src/main/res](../app/src/main/res)
- 프로젝트 소개 문서: [../README.md](../README.md)
- 개인정보 문서: [../PRIVACY.md](../PRIVACY.md)
- 기존 단일 교재 원본: [../ABSORB.md](../ABSORB.md)

## 권장 학습 방법

`README.md`에서 시작해 번호가 붙은 문서를 순서대로 읽는다. 각 문서에서 링크된 실제 코드 파일을 함께 열고, [테스트와 디버깅](07-testing-and-debugging.md)의 명령으로 현재 코드가 설명과 일치하는지 확인한다. 코드가 바뀌면 관련 문서를 함께 갱신해 교재와 구현이 어긋나지 않게 유지한다.


