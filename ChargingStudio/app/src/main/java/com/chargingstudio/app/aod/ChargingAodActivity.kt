package com.chargingstudio.app.aod

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.chargingstudio.app.R
import com.chargingstudio.app.animation.AnimationRepository

class ChargingAodActivity : AppCompatActivity() {

    private lateinit var adapter: PhotoAdapter
    private val photoUris = mutableListOf<String>()
    private lateinit var tvCount: TextView

    private val pickPhotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { }
                photoUris.add(uri.toString())
            }
            adapter.notifyDataSetChanged()
            tvCount.text = "${photoUris.size} photo(s) sélectionnée(s)"
            AnimationRepository.saveAodPhotos(this, photoUris)
            AnimationRepository.setActiveMode(this, "aod")
            com.chargingstudio.app.charging.ChargeMonitorService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging_aod)

        tvCount = findViewById(R.id.tvPhotoCount)
        photoUris.addAll(AnimationRepository.loadAodPhotos(this))
        tvCount.text = "${photoUris.size} photo(s) sélectionnée(s)"

        adapter = PhotoAdapter(photoUris)
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerPhotos)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnPickPhotos).setOnClickListener {
            pickPhotos.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        findViewById<Button>(R.id.btnPreview).setOnClickListener {
            if (photoUris.isEmpty()) {
                Toast.makeText(this, "Choisissez d'abord des photos", Toast.LENGTH_SHORT).show()
            } else {
                AnimationRepository.saveAodPhotos(this, photoUris)
                val i = Intent(this, AodOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(i)
                } else {
                    startService(i)
                }
                Toast.makeText(this, "Aperçu lancé. Rebranchez ou débranchez pour l'arrêter.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
