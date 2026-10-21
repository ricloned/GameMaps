package com.radarmap.app.map

import com.radarmap.app.model.GeoPoint
import org.json.JSONArray
import org.json.JSONObject

object GeoJson {
    const val EMPTY = "{\"type\":\"FeatureCollection\",\"features\":[]}"

    fun fc(features: List<JSONObject>): String {
        val arr = JSONArray()
        features.forEach { arr.put(it) }
        return JSONObject().put("type", "FeatureCollection").put("features", arr).toString()
    }

    private fun props(p: Map<String, Any>): JSONObject {
        val o = JSONObject()
        for ((k, v) in p) o.put(k, v)
        return o
    }

    fun point(lat: Double, lon: Double, p: Map<String, Any> = emptyMap()): JSONObject {
        val geom = JSONObject().put("type", "Point").put("coordinates", JSONArray().put(lon).put(lat))
        return JSONObject().put("type", "Feature").put("geometry", geom).put("properties", props(p))
    }

    fun line(points: List<GeoPoint>, p: Map<String, Any> = emptyMap()): JSONObject {
        val coords = JSONArray()
        points.forEach { coords.put(JSONArray().put(it.lon).put(it.lat)) }
        val geom = JSONObject().put("type", "LineString").put("coordinates", coords)
        return JSONObject().put("type", "Feature").put("geometry", geom).put("properties", props(p))
    }
}
