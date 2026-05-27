const TRANSLATIONS = {
  en: {
    meta: { lang: "en", dir: "ltr" },
    ui: {
      languageLabel: "Language",
      languageSelectAria: "Language selector",
      languageOptions: {
        en: "English",
        ko: "한국어"
      }
    },
    nav: {
      overview: "Overview",
      whatBuilt: "Features",
      evaluation: "Evaluation",
      results: "Scope",
      help: "Usage",
      buildProcess: "Build"
    },
    hero: {
      eyebrow: "Archived project overview",
      title: "Haan Ghil Muulnaat",
      subtitle: "An Android app for local portrait perturbation experiments and post-restoration defense evaluation.",
      honesty: "This page is kept as a repository-local project note. The public repository README is the source of truth for setup, releases, and licensing.",
      callout: "On-device processing, explicit evaluation, transparent limitations."
    },
    whatBuilt: {
      title: "Features",
      intro: "The app combines image perturbation, local evaluation, and a simple workflow for reviewing outcomes.",
      items: [
        "Applies perturbation to selected portrait images on the device.",
        "Searches for a minimum viable strength with binary search.",
        "Runs a restoration-style evaluation pass after protection.",
        "Reports HELD or BROKEN outcomes with supporting metrics.",
        "Supports gallery import, protected image export, and Android share targets."
      ]
    },
    approach: {
      title: "Approach",
      offlineTitle: "Offline-first app behavior",
      offlineItems: [
        "The app does not upload selected images.",
        "Processing runs locally in the Android app.",
        "Protected images are written back to the user's gallery only when requested."
      ],
      pipelineTitle: "Pipeline",
      pipelineItems: [
        "Pick a portrait image.",
        "Apply perturbation at a chosen or searched strength.",
        "Run defense evaluation with a simple restoration attack simulation.",
        "Review status, model metrics, and image-quality metrics."
      ]
    },
    evaluation: {
      title: "Evaluation framing",
      intro: "The evaluation is a practical diagnostic, not a universal security proof.",
      statusHeld: "HELD",
      statusBroken: "BROKEN",
      held: "Protection remains effective after the app's restoration-style evaluation pass.",
      broken: "The evaluation recovers enough signal that stronger tuning or a different method is needed."
    },
    results: {
      title: "Current Scope",
      intro: "These are project boundaries, not benchmark claims.",
      headers: {
        area: "Area",
        current: "Current behavior",
        note: "Note"
      },
      rows: [
        ["Processing", "Runs on device", "No app-level image upload path is implemented."],
        ["Evaluation", "Restoration-style diagnostic", "Designed to catch obvious post-processing weakness."],
        ["Security claim", "Limited", "No claim of universal protection against all models or attacks."],
        ["Distribution", "GitHub Releases", "APK binaries are not committed to the repository."]
      ],
      note: "Use the README and source code for the current build and release process."
    },
    help: {
      title: "Usage",
      intro: "Typical app flow:",
      steps: [
        "Pick a portrait image from the Android gallery.",
        "Run the optimal-strength scan or set a strength manually.",
        "Apply protection to create a protected image.",
        "Run defense evaluation and review HELD or BROKEN status.",
        "Save the protected image if the result is acceptable."
      ]
    },
    buildProcess: {
      title: "Build",
      body: "Builds are handled from the Android Gradle project. Release APKs are intended to be published as GitHub Release assets, not through GitHub Pages."
    },
    footer: {
      line: "Repository-local archived overview for Haan Ghil Muulnaat."
    }
  },
  ko: {
    meta: { lang: "ko", dir: "ltr" },
    ui: {
      languageLabel: "언어",
      languageSelectAria: "언어 선택기",
      languageOptions: {
        en: "English",
        ko: "한국어"
      }
    },
    nav: {
      overview: "개요",
      whatBuilt: "기능",
      evaluation: "평가",
      results: "범위",
      help: "사용",
      buildProcess: "빌드"
    },
    hero: {
      eyebrow: "보관용 프로젝트 개요",
      title: "한 길 물낯",
      subtitle: "인물 이미지 교란 실험과 복원 이후 방어 평가를 로컬에서 실행하는 Android 앱입니다.",
      honesty: "이 페이지는 repo 안에 보관된 프로젝트 노트입니다. 설정, 릴리스, 라이선스의 기준 문서는 공개 repo의 README입니다.",
      callout: "온디바이스 처리, 명시적 평가, 투명한 한계."
    },
    whatBuilt: {
      title: "기능",
      intro: "이미지 교란, 로컬 평가, 결과 검토 흐름을 하나의 앱으로 묶었습니다.",
      items: [
        "선택한 인물 이미지에 기기 내부에서 교란을 적용합니다.",
        "이진 탐색으로 최소 유효 강도를 찾습니다.",
        "보호 적용 후 복원 공격 형태의 평가 단계를 실행합니다.",
        "HELD 또는 BROKEN 상태와 보조 지표를 표시합니다.",
        "갤러리 가져오기, 보호 이미지 저장, Android 공유 대상을 지원합니다."
      ]
    },
    approach: {
      title: "접근 방식",
      offlineTitle: "오프라인 우선 동작",
      offlineItems: [
        "앱은 선택한 이미지를 업로드하지 않습니다.",
        "처리는 Android 앱 내부에서 로컬로 실행됩니다.",
        "보호 이미지는 사용자가 요청할 때만 갤러리에 저장됩니다."
      ],
      pipelineTitle: "파이프라인",
      pipelineItems: [
        "인물 이미지를 선택합니다.",
        "선택하거나 탐색한 강도로 교란을 적용합니다.",
        "간단한 복원 공격 시뮬레이션으로 방어 성능을 평가합니다.",
        "상태, 모델 지표, 이미지 품질 지표를 확인합니다."
      ]
    },
    evaluation: {
      title: "평가 기준",
      intro: "이 평가는 실용적 진단 도구이며 보편적 보안 증명이 아닙니다.",
      statusHeld: "HELD",
      statusBroken: "BROKEN",
      held: "앱의 복원형 평가 단계를 거친 후에도 보호가 유지된 상태입니다.",
      broken: "평가 과정에서 신호가 충분히 복구되어 더 강한 조정이나 다른 방법이 필요한 상태입니다."
    },
    results: {
      title: "현재 범위",
      intro: "아래 내용은 벤치마크 주장 대신 프로젝트 경계를 설명합니다.",
      headers: {
        area: "영역",
        current: "현재 동작",
        note: "비고"
      },
      rows: [
        ["처리", "기기 내부 실행", "앱 수준의 이미지 업로드 경로는 구현되어 있지 않습니다."],
        ["평가", "복원형 진단", "명백한 후처리 취약성을 확인하기 위한 단계입니다."],
        ["보안 주장", "제한적", "모든 모델과 공격에 대한 보편적 보호를 주장하지 않습니다."],
        ["배포", "GitHub Releases", "APK 바이너리는 저장소에 커밋하지 않습니다."]
      ],
      note: "현재 빌드와 릴리스 절차는 README와 소스 코드를 기준으로 확인하세요."
    },
    help: {
      title: "사용 흐름",
      intro: "일반적인 앱 사용 절차:",
      steps: [
        "Android 갤러리에서 인물 이미지를 선택합니다.",
        "최적 강도 탐색을 실행하거나 강도를 직접 지정합니다.",
        "보호 적용으로 보호 이미지를 생성합니다.",
        "방어 성능 평가를 실행하고 HELD 또는 BROKEN 상태를 확인합니다.",
        "결과가 적절하면 보호 이미지를 저장합니다."
      ]
    },
    buildProcess: {
      title: "빌드",
      body: "빌드는 Android Gradle 프로젝트에서 수행합니다. 릴리스 APK는 GitHub Pages가 아니라 GitHub Release asset으로 배포하는 것을 기준으로 합니다."
    },
    footer: {
      line: "한 길 물낯 repo 안에 보관된 프로젝트 개요입니다."
    }
  }
};
