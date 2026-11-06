package org.nwdl.cast

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Debug-only test: the app connects to the headset's own adb daemon over Wi-Fi (adb tcpip 5555 must be active)
 * and allows itself screen capture without the share dialog. The headset asks once to allow the app's adb key.
 */
fun addDebugTools(activity: ComponentActivity, root: LinearLayout, status: TextView) {
    val button = Button(activity).apply { text = "Test: allow capture via local adb" }
    root.addView(button)
    button.setOnClickListener {
        status.text = "Connecting to the local adb… accept the prompt in the headset."
        activity.lifecycleScope.launch {
            status.text = try {
                withContext(Dispatchers.IO) { allowCaptureWithoutDialog(activity.filesDir, activity.packageName) }
            } catch (e: Exception) {
                "Local adb failed: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }
}

private fun allowCaptureWithoutDialog(dir: File, packageName: String): String {
    val privateKey = File(dir, "adbkey")
    val publicKey = File(dir, "adbkey.pub")
    if (!privateKey.exists()) AdbKeyPair.generate(privateKey, publicKey)
    Dadb.create("127.0.0.1", 5555, AdbKeyPair.read(privateKey, publicKey)).use { adb ->
        val result = adb.shell("appops set $packageName PROJECT_MEDIA allow")
        return if (result.exitCode == 0) "Screen capture is now allowed without the share dialog." else "appops failed: ${result.allOutput}"
    }
}
