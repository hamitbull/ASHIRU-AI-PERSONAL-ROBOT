package com.ashiru.personalrobot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiClient(context: Context) {
    private val prefs=context.getSharedPreferences("ai_config",Context.MODE_PRIVATE)
    fun ask(prompt:String):String? {
        val endpoint=prefs.getString("endpoint","").orEmpty()
        val key=prefs.getString("key","").orEmpty()
        val model=prefs.getString("model","").orEmpty()
        if(endpoint.isBlank()||key.isBlank()||model.isBlank()) return null
        var connection:HttpURLConnection?=null
        return try {
            connection=(URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod="POST"; connectTimeout=15000; readTimeout=30000; doOutput=true
                setRequestProperty("Content-Type","application/json")
                setRequestProperty("Authorization","Bearer $key")
            }
            val body=JSONObject().put("model",model)
                .put("messages",JSONArray().put(JSONObject().put("role","system").put("content","You are ASHIRU AI, a helpful Hausa and English personal assistant. Be accurate, concise, and clearly state uncertainty."))
                    .put(JSONObject().put("role","user").put("content",prompt)))
                .put("temperature",0.7).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if(connection.responseCode !in 200..299) return null
            val raw=connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
        } catch(_:Exception) { null } finally { connection?.disconnect() }
    }
}