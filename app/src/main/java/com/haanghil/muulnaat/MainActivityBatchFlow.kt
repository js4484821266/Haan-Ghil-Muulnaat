package com.haanghil.muulnaat

import android.net.Uri
import kotlin.concurrent.thread

/**
 * 화면에 진행 상태를 보여 주는 일괄 처리의 진입점입니다.
 *
 * 조용한 자동 저장은 포그라운드 서비스가 맡습니다. 이 경로는 각 항목을 메인 화면에
 * 비춰 주어 사용자가 처리 결과를 볼 수 있는 일괄 처리용입니다.
 */
internal fun MainActivity.processImageBatch(uris: List<Uri>) {
    if (uris.isEmpty()) return

    resetBatchUi(uris.size)
    // 일괄 모드는 같은 보호 파이프라인을 재사용하되 진행 상태를 메인 화면에 보여 줍니다.
    thread {
        var savedCount = 0
        var skippedCount = 0

        uris.forEachIndexed { index, uri ->
            val result = processBatchItem(index + 1, uris.size, uri)
            if (result) savedCount += 1 else skippedCount += 1
        }

        runOnUiThread {
            showSearchProgress(getString(R.string.result_batch_complete, savedCount, skippedCount))
            binding.resultText.text = getString(R.string.result_batch_complete, savedCount, skippedCount)
            setBusy(false)
        }
    }
}

/**
 * 일괄 처리를 시작하기 전에 단일 이미지 상태를 비웁니다.
 *
 * 일괄 처리는 항목마다 같은 미리보기 위젯을 재사용하므로, 오래된 이미지와 메트릭을
 * 먼저 지워야 합니다.
 */
private fun MainActivity.resetBatchUi(total: Int) {
    state.originalBitmap = null
    state.protectedBitmap = null
    state.optimalStrength = null
    state.lastAppliedStrength = null
    state.lastEvaluationStrength = null
    clearResultCards()
    binding.noisyImage.setImageDrawable(null)
    binding.recoveredImage.setImageDrawable(null)
    binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
    binding.recommendedStrengthText.text = getString(R.string.recommended_strength_default)
    showSearchProgress(getString(R.string.result_batch_start, total))
    setBusy(true, getString(R.string.result_batch_start, total))
}

/**
 * 일괄 처리 항목 하나를 로드부터 저장까지 실행합니다.
 *
 * 보호 PNG가 실제로 갤러리에 쓰였을 때만 true를 반환합니다.
 */
private fun MainActivity.processBatchItem(itemNumber: Int, total: Int, uri: Uri): Boolean {
    runOnUiThread {
        binding.resultText.text = getString(R.string.result_batch_item_processing, itemNumber, total)
        showSearchProgress(getString(R.string.result_batch_item_processing, itemNumber, total))
    }

    val loaded = ImageStore.loadBitmapFromUri(this, uri)
    if (loaded == null) {
        runOnUiThread {
            binding.resultText.text = getString(R.string.result_batch_item_load_failed, itemNumber, total)
        }
        return false
    }

    // 최신 항목을 UI에 비춰 주어 일괄 처리가 블랙박스처럼 느껴지지 않게 합니다.
    runOnUiThread {
        prepareLoadedImage(loaded)
        setBusy(true, getString(R.string.result_batch_item_processing, itemNumber, total))
    }

    val minStrength = findBatchStrength(loaded, itemNumber, total) ?: return false
    val protected = perturbationModule.applyProtection(loaded, minStrength)
    val defenseReport = if (binding.autoRecoverySwitch.isChecked) {
        defenseEvaluator.evaluateAfterAttack(loaded, protected)
    } else {
        null
    }
    val saveResult = saveImageToGallery(protected)

    runOnUiThread {
        renderBatchResult(loaded, protected, minStrength, defenseReport, saveResult, itemNumber, total)
    }
    return saveResult.success
}
