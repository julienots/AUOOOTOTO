package com.chargingstudio.app.charging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.chargingstudio.app.animation.AnimationRepository

/** Au demarrage du telephone, relance la surveillance si un mode est actif. */
class ChargeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            AnimationRepository.getActiveMode(context) != "none"
        ) {
            ChargeMonitorService.start(context)
        }
    }

    companion object {
        fun launch(context: Context, cls: Class<*>) {
            val i = Intent(context, cls)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }
}
