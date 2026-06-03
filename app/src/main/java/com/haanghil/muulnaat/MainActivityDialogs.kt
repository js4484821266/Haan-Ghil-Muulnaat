package com.haanghil.muulnaat

import androidx.appcompat.app.AlertDialog

/**
 * 작은 도움말 모달의 진입점입니다.
 */
internal fun MainActivity.showManualDialog() {
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.help_dialog_title))
        .setMessage(getString(R.string.help_dialog_message))
        .setPositiveButton(getString(R.string.help_dialog_positive)) { dialog, _ -> dialog.dismiss() }
        .show()
}
