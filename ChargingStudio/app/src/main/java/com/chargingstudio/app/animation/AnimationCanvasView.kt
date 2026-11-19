package com.chargingstudio.app.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sin

class AnimationCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var editable: Boolean = false
    var batteryPercent: Int = 100
    var onSelectionChanged: ((AnimationElement?) -> Unit)? = null

    private val elements = mutableListOf<AnimationElement>()
    private var selected: AnimationElement? = null
    private var dragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 64f
    }
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#4FD1C5")
    }

    private val bitmapCache = HashMap<String, Bitmap>()
    private val startTime = System.currentTimeMillis()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            handler.postDelayed(this, 33L)
        }
    }

    fun startLoop() {
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stopLoop() {
        running = false
        handler.removeCallbacks(tick)
    }

    fun setElements(list: List<AnimationElement>) {
        elements.clear()
        elements.addAll(list)
        invalidate()
    }

    fun getElements(): List<AnimationElement> = elements

    fun addElement(el: AnimationElement) {
        elements.add(el)
        selected = el
        onSelectionChanged?.invoke(el)
        invalidate()
    }

    fun deleteSelected() {
        selected?.let { elements.remove(it) }
        selected = null
        onSelectionChanged?.invoke(null)
        invalidate()
    }

    fun updateSelectedRotation(deg: Float) {
        selected?.rotation = deg
        invalidate()
    }

    fun updateSelectedScale(s: Float) {
        selected?.scale = s
        invalidate()
    }

    fun updateSelectedColor(color: Int) {
        selected?.color = color
        invalidate()
    }

    fun updateSelectedEffect(effect: EffectType) {
        selected?.effect = effect
        invalidate()
    }

    fun getSelected(): AnimationElement? = selected

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val hit = findElementAt(event.x, event.y)
                selected = hit
                dragging = hit != null
                lastTouchX = event.x
                lastTouchY = event.y
                onSelectionChanged?.invoke(hit)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && selected != null && width > 0 && height > 0) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    selected!!.x = (selected!!.x + dx / width).coerceIn(0.05f, 0.95f)
                    selected!!.y = (selected!!.y + dy / height).coerceIn(0.05f, 0.95f)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
            }
        }
        return true
    }

    private fun findElementAt(px: Float, py: Float): AnimationElement? {
        for (el in elements.reversed()) {
            val cx = el.x * width
            val cy = el.y * height
            val radius = 90f * el.scale
            if ((px - cx) * (px - cx) + (py - cy) * (py - cy) <= radius * radius) return el
        }
        return null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

        for (el in elements) {
            canvas.save()

            var cx = el.x * width
            var cy = el.y * height
            var rot = el.rotation
            var scale = el.scale
            var alpha = 255

            when (el.effect) {
                EffectType.PULSE -> scale *= (1f + 0.15f * sin(elapsed * 3).toFloat())
                EffectType.ROTATE -> rot += ((elapsed * 60) % 360).toFloat()
                EffectType.FADE -> alpha = (150 + 105 * sin(elapsed * 2)).toInt().coerceIn(50, 255)
                EffectType.BOUNCE -> cy += (18 * sin(elapsed * 4)).toFloat()
                EffectType.SLIDE -> cx += (25 * sin(elapsed * 1.5)).toFloat()
                EffectType.NONE -> {}
            }

            canvas.translate(cx, cy)
            canvas.rotate(rot)
            canvas.scale(scale, scale)

            when (el.type) {
                ElementType.TEXT -> {
                    textPaint.color = if (el.color != -1) el.color else Color.WHITE
                    textPaint.alpha = alpha
                    val label = el.text.ifEmpty { "Texte" }
                    canvas.drawText(label, 0f, textPaint.textSize / 3, textPaint)
                }
                ElementType.SHAPE_CIRCLE -> {
                    shapePaint.color = if (el.color != -1) el.color else Color.parseColor("#4FD1C5")
                    shapePaint.alpha = alpha
                    canvas.drawCircle(0f, 0f, 60f, shapePaint)
                }
                ElementType.SHAPE_RECT -> {
                    shapePaint.color = if (el.color != -1) el.color else Color.parseColor("#7C6CF6")
                    shapePaint.alpha = alpha
                    canvas.drawRect(-60f, -40f, 60f, 40f, shapePaint)
                }
                ElementType.IMAGE -> {
                    val bmp = el.imageUri?.let { loadBitmap(it) }
                    if (bmp != null) {
                        shapePaint.alpha = alpha
                        canvas.drawBitmap(
                            bmp,
                            -bmp.width / 2f,
                            -bmp.height / 2f,
                            shapePaint
                        )
                    }
                }
                ElementType.BATTERY -> {
                    textPaint.color = if (el.color != -1) el.color else Color.parseColor("#4FD1C5")
                    textPaint.alpha = alpha
                    canvas.drawText("$batteryPercent%", 0f, textPaint.textSize / 3, textPaint)
                }
            }

            canvas.restore()

            if (editable && el == selected) {
                canvas.drawCircle(cx, cy, 95f * el.scale, selectionPaint)
            }
        }
    }

    private fun loadBitmap(uriString: String): Bitmap? {
        bitmapCache[uriString]?.let { return it }
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri).use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                val maxSize = 260
                val scaleFactor = min(
                    maxSize.toFloat() / original.width,
                    maxSize.toFloat() / original.height
                ).coerceAtMost(1f)
                val bmp = Bitmap.createScaledBitmap(
                    original,
                    (original.width * scaleFactor).toInt().coerceAtLeast(1),
                    (original.height * scaleFactor).toInt().coerceAtLeast(1),
                    true
                )
                bitmapCache[uriString] = bmp
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }
}
