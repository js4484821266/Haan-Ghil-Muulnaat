package com.haanghil.muulnaat

/**
 * 자동 저장 작업자의 항목별 알림 문구를 모아 둡니다.
 *
 * 알림 포매팅을 작은 래퍼로 분리하면 AutoSaveWorker가 알림 문자열 더미가 아니라
 * 작업 파이프라인처럼 읽힙니다.
 */
internal fun AutoSaveProtectionService.notifyLoading(itemNumber: Int, total: Int) {
    updateProgressNotification(
        progress = completedCount(),
        total = total,
        title = getString(R.string.notification_autosave_running_title),
        message = getString(R.string.notification_autosave_loading, itemNumber, total),
        showCancel = true,
    )
}

internal fun AutoSaveProtectionService.notifySkippedLoad(itemNumber: Int, total: Int) {
    notifyRunning(getString(R.string.notification_autosave_load_failed, itemNumber, total))
}

internal fun AutoSaveProtectionService.notifyNoStrength(itemNumber: Int, total: Int) {
    notifyRunning(getString(R.string.notification_autosave_no_strength, itemNumber, total))
}

internal fun AutoSaveProtectionService.notifySaving(itemNumber: Int, total: Int, minStrength: Int) {
    notifyRunning(getString(R.string.notification_autosave_saving, itemNumber, total, minStrength))
}

internal fun AutoSaveProtectionService.notifyItemSaved(itemNumber: Int, total: Int, success: Boolean) {
    val message = if (success) {
        getString(R.string.notification_autosave_item_saved, itemNumber, total)
    } else {
        getString(R.string.notification_autosave_item_failed, itemNumber, total)
    }
    notifyRunning(message)
}

private fun AutoSaveProtectionService.notifyRunning(message: String) {
    updateProgressNotification(
        progress = completedCount(),
        total = totalCount.coerceAtLeast(1),
        title = getString(R.string.notification_autosave_running_title),
        message = message,
        showCancel = true,
    )
}
