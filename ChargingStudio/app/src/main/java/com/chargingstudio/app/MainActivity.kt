package com.chargingstudio.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.chargingstudio.app.animation.AnimationRepository
import com.chargingstudio.app.animation.AnimationStudioActivity
import com.chargingstudio.app.aod.ChargingAodActivity
import com.chargingstudio.app.charging.ChargeMonitorService

class MainActivity : AppCompatActivity() {

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.cardAnimationStudio).setOnClickListener {
            startActivity(Intent(this, AnimationStudioActivity::class.java))
        }
        findViewById<CardView>(R.id.cardAod).setOnClickListener {
            startActivity(Intent(this, ChargingAodActivity::class.java))
        }

        val group = findViewById<RadioGroup>(R.id.modeGroup)
        when (AnimationRepository.getActiveMode(this)) {
            "animation" -> group.check(R.id.radioAnimation)
            "aod" -> group.check(R.id.radioAod)
            else -> group.check(R.id.radioNone)
        }
        group.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioAnimation -> "animation"
                R.id.radioAod -> "aod"
                else -> "none"
            }
            AnimationRepository.setActiveMode(this, mode)
            if (mode == "none") ChargeMonitorService.stop(this) else ChargeMonitorService.start(this)
        }

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Autorisation déjà accordée", Toast.LENGTH_SHORT).show()
            }
        }

        if (AnimationRepository.getActiveMode(this) != "none") ChargeMonitorService.start(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
