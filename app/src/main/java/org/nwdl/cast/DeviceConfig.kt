package org.nwdl.cast

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Thrown when the server no longer knows this headset (removed on the website). */
class NotPairedException : Exception("This headset was removed on the website. Pair it again.")

/** Permanent credentials of this headset, received by pairing with a QR code from the website. */
data class DeviceConfig(val server: String, val deviceId: String, val deviceSecret: String, val name: String) {

    /** Asks the cast website for a LiveKit token. Blocking, call off the main thread. */
    fun fetchToken(): Pair<String, String> {
        val json = post("$server/api/token/device", JSONObject().put("deviceId", deviceId).put("deviceSecret", deviceSecret)) { code ->
            if (code == 401) throw NotPairedException()
        }
        return json.getString("url") to json.getString("token")
    }

    companion object {
        const val PRODUCTION_SERVER = "https://cast.nwdl.org"
        private const val PREFS = "pairing"

        fun load(context: Context): DeviceConfig? {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return DeviceConfig(
                prefs.getString("server", null) ?: return null,
                prefs.getString("deviceId", null) ?: return null,
                prefs.getString("deviceSecret", null) ?: return null,
                prefs.getString("name", null) ?: return null,
            )
        }

        fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()

        /**
         * Reads the QR code of the website ({"server": …, "code": …}) and exchanges the one-time code for credentials.
         * Blocking, call off the main thread. Release builds only pair with [PRODUCTION_SERVER].
         */
        fun pair(context: Context, qrText: String): DeviceConfig {
            val qr = runCatching { JSONObject(qrText) }.getOrNull() ?: throw IllegalArgumentException("This is not a cast pairing code.")
            val server = qr.optString("server").trimEnd('/')
            val code = qr.optString("code")
            require(code.isNotEmpty() && (server.startsWith("https://") || server.startsWith("http://"))) { "This is not a cast pairing code." }
            require(BuildConfig.DEBUG || server == PRODUCTION_SERVER) { "This code is not from cast.nwdl.org." }

            val json = post("$server/api/devices/pair", JSONObject().put("code", code)) { status ->
                if (status == 410) throw IllegalStateException("The code was already used or has expired. Show a new one.")
            }
            val config = DeviceConfig(server, json.getString("deviceId"), json.getString("deviceSecret"), json.getString("name"))
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("server", config.server)
                .putString("deviceId", config.deviceId)
                .putString("deviceSecret", config.deviceSecret)
                .putString("name", config.name)
                .apply()
            return config
        }

        /** POSTs JSON and returns the JSON answer; [onError] may throw a specific exception for a status code. */
        private fun post(url: String, body: JSONObject, onError: (Int) -> Unit): JSONObject {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val status = connection.responseCode
                if (status != 200) {
                    onError(status)
                    throw IllegalStateException("Server answered HTTP $status")
                }
                return JSONObject(connection.inputStream.bufferedReader().readText())
            } finally {
                connection.disconnect()
            }
        }
    }
}
