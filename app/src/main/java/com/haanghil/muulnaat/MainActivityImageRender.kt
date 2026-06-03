package com.haanghil.muulnaat

import android.graphics.Bitmap

/**
 * 메인 화면의 세 이미지 패널을 그립니다.
 */
internal fun MainActivity.renderProtectedImage(reference: Bitmap, protected: Bitmap) {
    binding.noisyImage.setImageBitmap(protected)
    val meanAbsDelta = computeMeanAbsoluteDifference(reference, protected)
    binding.perturbationSummaryText.text =
        getString(R.string.perturbation_magnitude_format, meanAbsDelta, (meanAbsDelta / 255.0) * 100.0)
}

internal fun MainActivity.renderRecoveredImage(recovered: Bitmap?) {
    binding.recoveredImage.setImageBitmap(recovered)
}
