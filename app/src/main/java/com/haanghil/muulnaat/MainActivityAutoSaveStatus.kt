package com.haanghil.muulnaat

internal fun MainActivity.startAutoSaveStatusUpdates() {
    state.autoSaveStatusSubscription?.close()
    state.autoSaveStatusSubscription = AutoSaveStatusStore.subscribe { status ->
        binding.resultText.text = status.message
        showSearchProgress(status.message)
        setBusy(status.running)
    }
}

internal fun MainActivity.stopAutoSaveStatusUpdates() {
    state.autoSaveStatusSubscription?.close()
    state.autoSaveStatusSubscription = null
}
