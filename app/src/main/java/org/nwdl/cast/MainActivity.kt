package org.nwdl.cast

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Must be registered before onCreate
    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) CastSession.start(this, data)
    }

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.RECORD_AUDIO] == true) askForScreenCapture()
        else findViewById<TextView>(R.id.status).text = "Microphone permission is needed for casting."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val status = findViewById<TextView>(R.id.status)
        val start = findViewById<Button>(R.id.start)
        val stop = findViewById<Button>(R.id.stop)

        start.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) askForScreenCapture()
            else permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
        }
        stop.setOnClickListener { CastSession.stop(this) }

        lifecycleScope.launch {
            CastSession.state.collect { state ->
                status.text = when (state) {
                    CastSession.State.Idle -> DeviceConfig.load(this@MainActivity).fold(
                        onSuccess = { "Ready (device ${it.deviceId})" },
                        onFailure = { it.message },
                    )
                    CastSession.State.Starting -> "Starting…"
                    CastSession.State.Casting -> "Casting. You can switch to another app now."
                    is CastSession.State.Failed -> "Casting failed: ${state.reason}"
                }
                start.isEnabled = state == CastSession.State.Idle || state is CastSession.State.Failed
                stop.isEnabled = state == CastSession.State.Casting
            }
        }
    }

    private fun askForScreenCapture() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        // Always the whole view: the consent dialog then offers no choice of a single app
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
        consent.launch(intent)
    }
}
