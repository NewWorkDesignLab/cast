package org.nwdl.cast

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Device ID and secret of this headset, provisioned as cast.json in the app's files folder (adb push), never built into the app. */
data class DeviceConfig(val deviceId: String, val deviceSecret: String, val tokenEndpoint: String) {

    /** Asks the cast website for a LiveKit token. Blocking, call off the main thread. */
    fun fetchToken(): Pair<String, String> {
        val connection = URL(tokenEndpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("deviceId", deviceId).put("deviceSecret", deviceSecret).toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            when (val code = connection.responseCode) {
                200 -> Unit
                401 -> throw IllegalStateException("Device ID or secret rejected (401)")
                else -> throw IllegalStateException("Token request failed (HTTP $code)")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            return json.getString("url") to json.getString("token")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val DEFAULT_ENDPOINT = "https://cast.nwdl.org/api/token/device"

        fun file(context: Context) = File(context.getExternalFilesDir(null), "cast.json")

        /** Fails with a readable reason if the file is missing or incomplete. */
        fun load(context: Context): Result<DeviceConfig> = runCatching {
            val file = file(context)
            check(file.exists()) { "No cast.json found. Copy it to ${file.absolutePath} (adb push)." }
            val json = JSONObject(file.readText())
            val id = json.optString("deviceId")
            val secret = json.optString("deviceSecret")
            check(id.isNotEmpty() && secret.isNotEmpty()) { "cast.json needs deviceId and deviceSecret." }
            DeviceConfig(id, secret, json.optString("tokenEndpoint").ifEmpty { DEFAULT_ENDPOINT })
        }
    }
}
