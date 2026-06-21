# 프로젝트 개요

## 이번 문서의 학습 목표

프로젝트가 어떤 문제를 해결하고, 최종 결과가 무엇인지 먼저 잡는다.

## 앞 문서와의 연결

이 문서는 [README.md](README.md)에서 이어진다. 순서대로 읽으면 이전 문서에서 잡은 맥락을 바탕으로 이번 문서의 세부 내용을 이해할 수 있다.

## 1. 프로젝트 한 문장 요약

Haan Ghil Muulnaat는 Android에서 인물 사진을 불러와 얼굴 윤곽 또는 타원형 근사 마스크 영역에 edge-aware perturbation을 적용하고, 로컬 denoising + 2x upscaling 복원 시뮬레이션 뒤에도 얼굴 특징 억제가 유지되는지 확인한 다음 보호 이미지를 갤러리에 저장하는 앱이다.

## 2. 프로젝트가 해결하려는 문제

인물 사진은 공유되거나 저장된 뒤 얼굴 감지 모델, 이미지 라벨링 모델, 복원/업스케일링 처리에 다시 노출될 수 있다. 이 프로젝트는 사용자가 기기 안에서 이미지에 보호용 perturbation을 적용하고, 복원 시도 이후 얼굴 인식 신호가 얼마나 억제되는지 확인하도록 돕는다.

코드에서 확인되는 문제 정의는 다음과 같다.

- [`FaceRegionDetector`](../app/src/main/java/com/haanghil/muulnaat/FaceRegionDetector.kt)는 기기 안에서 ML Kit로 얼굴 bounding box와 가능한 얼굴 외곽 contour를 찾는다.
- [`NoiseEngine`](../app/src/main/java/com/haanghil/muulnaat/NoiseEngine.kt)은 선택된 은닉 방법에 따라 noising, blurring, solid fill을 얼굴 contour 또는 bbox 타원 근사 마스크 영역에 적용한다.
- [`RedTeamEngine`](../app/src/main/java/com/haanghil/muulnaat/RedTeamEngine.kt)은 보호 이미지에 denoising, 2x upscaling, sharpening 성격의 복원 시뮬레이션을 적용한다.
- [`ModelProbe`](../app/src/main/java/com/haanghil/muulnaat/ModelProbe.kt)는 ML Kit face detection landmark/contour와 image labeling 결과를 이용해 얼굴 특징 억제와 라벨 변화량을 점수화한다.
- [`RestorationAttackProbe`](../app/src/main/java/com/haanghil/muulnaat/RestorationAttackProbe.kt)는 복원 시뮬레이션 뒤 평가 결과를 [`HELD`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt) 또는 [`BROKEN`](../app/src/main/java/com/haanghil/muulnaat/PipelineContracts.kt)으로 변환한다.

보안 보증이나 모든 모델에 대한 방어를 증명하는 코드는 확인되지 않았다. 그런 주장은 확인 필요다.

## 이번 문서에서 반드시 이해해야 할 요점

- 이 문서의 설명은 현재 repo의 완성된 코드와 산출물 기준이다.
- 링크된 실제 파일을 함께 열어 보면 책임 분리와 실행 흐름을 더 정확히 확인할 수 있다.
- 코드가 바뀌면 이 문서의 설명도 함께 갱신해야 한다.

## 다음 문서

다음 문서: [02-architecture.md](02-architecture.md)

