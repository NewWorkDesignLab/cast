package org.nwdl.cast

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private companion object {
        const val PAIRING_TEXT = "This headset is not paired yet.\n\n" +
            "On a computer, open cast.nwdl.org/headsets, sign in and choose “Add a headset”. " +
            "Then look at the QR code with this headset."
    }

    private lateinit var status: TextView
    private lateinit var start: Button
    private lateinit var stop: Button
    private lateinit var scanner: QrScanner

    private var pairingMessage: String? = null
    private var pairing = false
    private var rejectedCode: String? = null
    private var askedForCamera = false

    // Must be registered before onCreate
    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) CastSession.start(this, data)
    }

    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.RECORD_AUDIO] == true) askForScreenCapture()
        else status.text = "Microphone permission is needed for casting."
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pairingMessage = if (granted) null else "Camera permission is needed to read the pairing code."
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        start = findViewById(R.id.start)
        stop = findViewById(R.id.stop)
        scanner = QrScanner(this, ::onQrCode)

        start.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) askForScreenCapture()
            else microphonePermission.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
        }
        stop.setOnClickListener { CastSession.stop(this) }
        addDebugTools(this, findViewById(R.id.root), status)

        lifecycleScope.launch { CastSession.state.collect { render() } }
    }

    override fun onResume() {
        super.onResume()
        askedForCamera = false
        render()
    }

    override fun onPause() {
        super.onPause()
        scanner.stop()
    }

    private fun render() {
        val config = DeviceConfig.load(this)
        val state = CastSession.state.value
        val paired = config != null
        start.visibility = if (paired) View.VISIBLE else View.GONE
        stop.visibility = if (paired) View.VISIBLE else View.GONE

        if (config == null) {
            val notice = pairingMessage ?: (state as? CastSession.State.Failed)?.reason
            status.text = listOfNotNull(notice, if (pairing) "Pairing…" else PAIRING_TEXT).joinToString("\n\n")
            if (!pairing) startScanning()
            return
        }

        scanner.stop()
        status.text = when (state) {
            CastSession.State.Idle -> "Ready to cast as ${config.name}."
            CastSession.State.Starting -> "Starting…"
            CastSession.State.Casting -> "Casting. You can switch to another app now."
            is CastSession.State.Ended -> state.reason
            is CastSession.State.Failed -> "Casting failed: ${state.reason}"
        }
        start.isEnabled = state != CastSession.State.Starting && state != CastSession.State.Casting
        stop.isEnabled = state == CastSession.State.Casting
    }

    // The camera only opens while the app has focus, i.e. the headset is worn and the panel is in front
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) render() else scanner.stop()
    }

    private fun startScanning() {
        if (!hasWindowFocus()) return
        if (checkSelfPermission(QrScanner.PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            scanner.start()
        } else if (!askedForCamera) {
            askedForCamera = true
            cameraPermission.launch(QrScanner.PERMISSION)
        }
    }

    private fun onQrCode(text: String) {
        if (pairing || text == rejectedCode || DeviceConfig.load(this) != null) return
        pairing = true
        render()
        lifecycleScope.launch {
            pairingMessage = try {
                withContext(Dispatchers.IO) { DeviceConfig.pair(this@MainActivity, text) }
                null
            } catch (e: Exception) {
                rejectedCode = text // keep scanning, but do not retry the same code
                "Pairing failed: ${e.message ?: e.javaClass.simpleName}"
            }
            pairing = false
            render()
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
