package com.chargingstudio.app.aod

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.min
import kotlin.random.Random

class AodPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var batteryPercent: Int = 100
    private var photoUris: List<String> = emptyList()

    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var currentX = 0.5f
    private var currentY = 0.4f
    private var nextX = 0.5f
    private var nextY = 0.4f
    private var crossfade = 1f // 1 = showing current fully
    private var photoIndex = -1

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 130f
        isFakeBoldText = true
    }
    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FD1C5")
        textAlign = Paint.Align.CENTER
        textSize = 46f
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val cycleRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            showNextPhoto()
            handler.postDelayed(this, 8000L)
        }
    }
    private val fadeRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            if (crossfade < 1f) {
                crossfade = min(1f, crossfade + 0.08f)
            }
            invalidate()
            handler.postDelayed(this, 30L)
        }
    }

    fun setPhotos(uris: List<String>) {
        photoUris = uris
        if (uris.isNotEmpty()) {
            photoIndex = 0
            currentBitmap = loadBitmap(uris[0])
            currentX = randomX()
            currentY = randomY()
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.post(fadeRunnable)
        if (photoUris.size > 1) handler.postDelayed(cycleRunnable, 8000L)
        handler.post(tickTime)
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private val tickTime = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            handler.postDelayed(this, 1000L)
        }
    }

    private fun showNextPhoto() {
        if (photoUris.isEmpty()) return
        photoIndex = (photoIndex + 1) % photoUris.size
        val bmp = loadBitmap(photoUris[photoIndex]) ?: return
        currentBitmap = nextBitmap ?: currentBitmap
        currentX = nextX.takeIf { crossfade >= 1f } ?: currentX
        nextBitmap = bmp
        nextX = randomX()
        nextY = randomY()
        crossfade = 0f
        // swap: treat nextBitmap as the one fading in over currentBitmap
    }

    private fun randomX() = 0.2f + Random.nextFloat() * 0.6f
    private fun randomY() = 0.25f + Random.nextFloat() * 0.45f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // draw current fading out, next fading in
        currentBitmap?.let { bmp ->
            imgPaint.alpha = (255 * (1f - crossfade)).toInt().coerceIn(0, 255)
            if (imgPaint.alpha > 0) {
                canvas.drawBitmap(
                    bmp,
                    currentX * width - bmp.width / 2f,
                    currentY * height - bmp.height / 2f,
                    imgPaint
                )
            }
        }
        nextBitmap?.let { bmp ->
            imgPaint.alpha = (255 * crossfade).toInt().coerceIn(0, 255)
            canvas.drawBitmap(
                bmp,
                nextX * width - bmp.width / 2f,
                nextY * height - bmp.height / 2f,
                imgPaint
            )
            if (crossfade >= 1f) {
                currentBitmap = bmp
                currentX = nextX
                currentY = nextY
            }
        }

        val cal = Calendar.getInstance()
        val timeText = DateFormat.format(
            if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", cal
        ).toString()
        canvas.drawText(timeText, width / 2f, height * 0.85f, timePaint)
        canvas.drawText("$batteryPercent% • en charge", width / 2f, height * 0.85f + 60f, batteryPaint)
    }

    private fun loadBitmap(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri).use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                val maxSize = 520
                val scale = min(
                    maxSize.toFloat() / original.width,
                    maxSize.toFloat() / original.height
                ).coerceAtMost(1f)
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
