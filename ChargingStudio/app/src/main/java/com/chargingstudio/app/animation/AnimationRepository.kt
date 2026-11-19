package com.chargingstudio.app.animation

import android.content.Context
import org.json.JSONArray

object AnimationRepository {
    private const val PREFS = "charging_studio_prefs"
    private const val KEY_ELEMENTS = "animation_elements"
    const val KEY_ACTIVE_MODE = "active_mode" // "none" | "animation" | "aod"
    const val KEY_AOD_PHOTOS = "aod_photos"

    fun save(context: Context, elements: List<AnimationElement>) {
        val arr = JSONArray()
        elements.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_ELEMENTS, arr.toString()).apply()
    }

    fun load(context: Context): MutableList<AnimationElement> {
        val json = prefs(context).getString(KEY_ELEMENTS, null) ?: return mutableListOf()
        val arr = JSONArray(json)
        val list = mutableListOf<AnimationElement>()
        for (i in 0 until arr.length()) {
            list.add(AnimationElement.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun setActiveMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_ACTIVE_MODE, mode).apply()
    }

    fun getActiveMode(context: Context): String {
        return prefs(context).getString(KEY_ACTIVE_MODE, "none") ?: "none"
    }

    fun saveAodPhotos(context: Context, uris: List<String>) {
        val arr = JSONArray()
        uris.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_AOD_PHOTOS, arr.toString()).apply()
    }

    fun loadAodPhotos(context: Context): MutableList<String> {
        val json = prefs(context).getString(KEY_AOD_PHOTOS, null) ?: return mutableListOf()
        val arr = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
