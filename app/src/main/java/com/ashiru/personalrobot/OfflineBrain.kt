package com.ashiru.personalrobot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.util.Locale

/** Bundled offline knowledge plus user-approved private memory stored in SQLite. */
class OfflineBrain(context: Context) : SQLiteOpenHelper(context.applicationContext, "ashiru_personal_memory.db", null, 2) {
    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE memories (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL UNIQUE COLLATE NOCASE, created_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE memories_new (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL UNIQUE COLLATE NOCASE, created_at INTEGER NOT NULL)")
            db.execSQL("INSERT OR IGNORE INTO memories_new(content, created_at) SELECT content, created_at FROM memories")
            db.execSQL("DROP TABLE memories")
            db.execSQL("ALTER TABLE memories_new RENAME TO memories")
        }
    }

    fun remember(content: String) {
        val clean = content.trim().take(4000)
        if (clean.isEmpty()) return
        val values = ContentValues().apply { put("content", clean); put("created_at", System.currentTimeMillis()) }
        writableDatabase.insertWithOnConflict("memories", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getMemories(): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT content FROM memories ORDER BY created_at DESC", null).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun forgetAllMemories() { writableDatabase.delete("memories", null, null) }

    fun findMemory(query: String): String? {
        val queryWords = tokens(query)
        if (queryWords.isEmpty()) return null
        var bestText: String? = null
        var bestScore = 0
        readableDatabase.rawQuery("SELECT content FROM memories ORDER BY created_at DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val content = cursor.getString(0)
                val score = queryWords.count { it in tokens(content) }
                if (score > bestScore) { bestScore = score; bestText = content }
            }
        }
        return if (bestScore >= 2 || (queryWords.size == 1 && bestScore == 1)) bestText else null
    }

    fun answer(query: String): String? {
        val queryWords = tokens(query)
        if (queryWords.isEmpty()) return null
        return try {
            var bestScore = 0
            var bestAnswer: String? = null
            for (asset in listOf("knowledge_ha.json", "knowledge_en.json")) {
                val raw = try { appContext.assets.open(asset).bufferedReader().use { it.readText() } } catch (_: Exception) { continue }
                val entries = JSONArray(raw)
                for (i in 0 until entries.length()) {
                    val item = entries.getJSONObject(i)
                    val searchable = buildString {
                        append(item.optString("category")).append(' ')
                        append(item.optString("answer")).append(' ')
                        val questions = item.optJSONArray("questions") ?: JSONArray()
                        for (j in 0 until questions.length()) append(questions.optString(j)).append(' ')
                        val tags = item.optJSONArray("tags") ?: JSONArray()
                        for (j in 0 until tags.length()) append(tags.optString(j)).append(' ')
                    }
                    val score = queryWords.count { it in tokens(searchable) }
                    if (score > bestScore) { bestScore = score; bestAnswer = item.optString("answer").takeIf { it.isNotBlank() } }
                }
            }
            if (bestScore >= 2 || (queryWords.size == 1 && bestScore == 1)) bestAnswer else null
        } catch (_: Exception) { null }
    }

    private fun tokens(text: String): Set<String> = text.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .split(Regex("\\s+")).filter { it.length > 2 }.toSet()
}