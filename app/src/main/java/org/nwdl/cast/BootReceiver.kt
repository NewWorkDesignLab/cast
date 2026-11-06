package org.nwdl.cast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Brings the app back into standby after a reboot, until it is quit. Starting from the background needs the
 * SYSTEM_ALERT_WINDOW app op that the setup script grants.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!CastSession.autostart(context) || DeviceConfig.load(context) == null) return
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_AUTOSTART, true),
        )
    }
}
