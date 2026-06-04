package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import com.haanghil.muulnaat.databinding.ActivityMainBinding
import java.io.Closeable

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding
    internal val state = MainActivityState()
    internal val perturbationModule: PerturbationModule = NoiseEngine
    internal val defenseEvaluator: DefenseEvaluator = RestorationAttackProbe()

    internal val writeStoragePermissionLauncher =
        registerForActivityResult(RequestPermission()) { granted ->
            val pendingAction = state.pendingStoragePermissionAction
            state.pendingStoragePermissionAction = null
            if (granted) {
                pendingAction?.invoke()
            } else {
                Toast.makeText(this, getString(R.string.toast_storage_permission_denied), Toast.LENGTH_SHORT).show()
                binding.resultText.text = getString(R.string.toast_storage_permission_denied)
                setBusy(false)
            }
        }

    internal val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri == null) {
                binding.resultText.text = getString(R.string.result_no_image_selected)
                return@registerForActivityResult
            }

            processSingleImageUri(uri, autoSaveAfterProtection = false)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureUiActions()
        handleForwardedShareIntent(intent)
        handleBackgroundStatusIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        startAutoSaveStatusUpdates()
    }

    override fun onStop() {
        stopAutoSaveStatusUpdates()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleForwardedShareIntent(intent)
        handleBackgroundStatusIntent(intent)
    }
}

internal class MainActivityState {
    var originalBitmap: Bitmap? = null
    var protectedBitmap: Bitmap? = null
    var optimalStrength: Int? = null
    var lastAppliedStrength: Int? = null
    var lastEvaluationStrength: Int? = null
    var pendingStoragePermissionAction: (() -> Unit)? = null
    var autoSaveStatusSubscription: Closeable? = null
}
