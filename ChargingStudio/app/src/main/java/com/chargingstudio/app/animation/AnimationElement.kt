package com.chargingstudio.app.animation

import org.json.JSONObject

enum class ElementType { TEXT, SHAPE_CIRCLE, SHAPE_RECT, IMAGE, BATTERY }

enum class EffectType { NONE, PULSE, ROTATE, FADE, BOUNCE, SLIDE }

data class AnimationElement(
    var id: Int,
    var type: ElementType,
    var x: Float = 0.5f,
    var y: Float = 0.5f,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var color: Int = -1,
    var text: String = "",
    var imageUri: String? = null,
    var effect: EffectType = EffectType.NONE
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("type", type.name)
        o.put("x", x)
        o.put("y", y)
        o.put("rotation", rotation)
        o.put("scale", scale)
        o.put("color", color)
        o.put("text", text)
        o.put("imageUri", imageUri ?: "")
        o.put("effect", effect.name)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): AnimationElement {
            return AnimationElement(
                id = o.optInt("id"),
                type = ElementType.valueOf(o.optString("type", "TEXT")),
                x = o.optDouble("x", 0.5).toFloat(),
                y = o.optDouble("y", 0.5).toFloat(),
                rotation = o.optDouble("rotation", 0.0).toFloat(),
                scale = o.optDouble("scale", 1.0).toFloat(),
                color = o.optInt("color", -1),
                text = o.optString("text", ""),
                imageUri = o.optString("imageUri", "").ifEmpty { null },
                effect = EffectType.valueOf(o.optString("effect", "NONE"))
            )
        }
    }
}
