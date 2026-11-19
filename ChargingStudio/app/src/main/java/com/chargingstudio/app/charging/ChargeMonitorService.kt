package com.chargingstudio.app.charging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chargingstudio.app.MainActivity
import com.chargingstudio.app.R
import com.chargingstudio.app.animation.AnimationRepository
import com.chargingstudio.app.aod.AodOverlayService

/**
 * Service permanent qui ecoute le branchement/debranchement du chargeur.
 * Necessaire car depuis Android 8, ACTION_POWER_CONNECTED n'est plus livre
 * aux receivers declares dans le manifest.
 */
class ChargeMonitorService : Service() {

    private var receiver: BroadcastReceiver? = null

    companion object {
        private const val CHANNEL_ID = "charge_monitor_channel"
        private const val NOTIF_ID = 1000

        fun start(context: Context) {
            val i = Intent(context, ChargeMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChargeMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        when (AnimationRepository.getActiveMode(context)) {
                            "animation" -> ChargeReceiver.launch(context, ChargingOverlayService::class.java)
                            "aod" -> ChargeReceiver.launch(context, AodOverlayService::class.java)
                        }
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        context.stopService(Intent(context, ChargingOverlayService::class.java))
                        context.stopService(Intent(context, AodOverlayService::class.java))
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            receiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) { }
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Surveillance de charge", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Charging Animation Studio")
            .setContentText("En attente du branchement du chargeur")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }
}
