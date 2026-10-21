package com.radarmap.app.data

import android.content.Context
import com.radarmap.app.model.SavedRoute
import com.radarmap.app.model.Settings
import org.json.JSONArray
import org.json.JSONObject

/** Локальное хранение настроек и своих маршрутов (без аккаунтов). */
class Storage(ctx: Context) {
    private val sp = ctx.getSharedPreferences("radarmap", Context.MODE_PRIVATE)

    fun loadSettings(): Settings = try {
        Settings.fromJson(JSONObject(sp.getString("settings", null) ?: "{}"))
    } catch (e: Exception) {
        Settings()
    }

    fun saveSettings(s: Settings) {
        sp.edit().putString("settings", s.toJson().toString()).apply()
    }

    fun loadRoutes(): List<SavedRoute> = try {
        val arr = JSONArray(sp.getString("routes", null) ?: "[]")
        (0 until arr.length()).map { SavedRoute.fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveRoutes(list: List<SavedRoute>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("routes", arr.toString()).apply()
    }
}
