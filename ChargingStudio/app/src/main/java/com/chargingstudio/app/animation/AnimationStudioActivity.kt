package com.chargingstudio.app.animation

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chargingstudio.app.R

class AnimationStudioActivity : AppCompatActivity() {

    private lateinit var canvasView: AnimationCanvasView
    private var nextId = 1

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { /* certains fournisseurs ne le permettent pas */ }
            canvasView.addElement(
                AnimationElement(
                    id = nextId++,
                    type = ElementType.IMAGE,
                    imageUri = uri.toString()
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animation_studio)

        canvasView = AnimationCanvasView(this)
        canvasView.editable = true
        findViewById<android.widget.FrameLayout>(R.id.canvasContainer).addView(canvasView)
        canvasView.setElements(AnimationRepository.load(this))
        canvasView.startLoop()

        val seekRotation = findViewById<SeekBar>(R.id.seekRotation)
        val seekScale = findViewById<SeekBar>(R.id.seekScale)

        canvasView.onSelectionChanged = { el ->
            seekRotation.progress = (el?.rotation ?: 0f).toInt().coerceIn(0, 360)
            seekScale.progress = (((el?.scale ?: 1f) * 100).toInt()).coerceIn(10, 200)
        }

        findViewById<android.widget.Button>(R.id.btnAddText).setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Ajouter un texte")
                .setView(input)
                .setPositiveButton("Ajouter") { _, _ ->
                    canvasView.addElement(
                        AnimationElement(
                            id = nextId++,
                            type = ElementType.TEXT,
                            text = input.text.toString().ifEmpty { "Texte" }
                        )
                    )
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        findViewById<android.widget.Button>(R.id.btnAddShape).setOnClickListener {
            val options = arrayOf("Cercle", "Rectangle")
            AlertDialog.Builder(this)
                .setTitle("Choisir une forme")
                .setItems(options) { _, which ->
                    val type = if (which == 0) ElementType.SHAPE_CIRCLE else ElementType.SHAPE_RECT
                    canvasView.addElement(AnimationElement(id = nextId++, type = type))
                }
                .show()
        }

        findViewById<android.widget.Button>(R.id.btnAddImage).setOnClickListener {
            pickImage.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        findViewById<android.widget.Button>(R.id.btnAddBattery).setOnClickListener {
            canvasView.addElement(AnimationElement(id = nextId++, type = ElementType.BATTERY))
        }

        findViewById<android.widget.Button>(R.id.btnEffect).setOnClickListener {
            val effects = EffectType.values()
            val names = effects.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Effet d'animation")
                .setItems(names) { _, which ->
                    canvasView.updateSelectedEffect(effects[which])
                }
                .show()
        }

        findViewById<android.widget.Button>(R.id.btnColor).setOnClickListener {
            val colors = listOf(
                "Turquoise" to Color.parseColor("#4FD1C5"),
                "Violet" to Color.parseColor("#7C6CF6"),
                "Blanc" to Color.WHITE,
                "Rouge" to Color.parseColor("#FF5C5C"),
                "Jaune" to Color.parseColor("#FFD166"),
                "Vert" to Color.parseColor("#06D6A0")
            )
            AlertDialog.Builder(this)
                .setTitle("Couleur")
                .setItems(colors.map { it.first }.toTypedArray()) { _, which ->
                    canvasView.updateSelectedColor(colors[which].second)
                }
                .show()
        }

        findViewById<android.widget.Button>(R.id.btnDelete).setOnClickListener {
            canvasView.deleteSelected()
        }

        seekRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) canvasView.updateSelectedRotation(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekScale.progress = 100
        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) canvasView.updateSelectedScale(progress.coerceAtLeast(10) / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<android.widget.Button>(R.id.btnSave).setOnClickListener {
            AnimationRepository.save(this, canvasView.getElements())
            AnimationRepository.setActiveMode(this, "animation")
            com.chargingstudio.app.charging.ChargeMonitorService.start(this)
            Toast.makeText(
                this,
                "Animation enregistrée. Elle s'activera automatiquement au branchement.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        canvasView.stopLoop()
        super.onDestroy()
    }
}
