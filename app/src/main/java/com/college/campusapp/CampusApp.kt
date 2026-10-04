package com.college.campusapp

import android.app.Application
import org.osmdroid.config.Configuration

class CampusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Boost OSM Map Tile Loading Speed globally with 8 parallel download threads & high-speed memory cache
        val config = Configuration.getInstance()
        config.userAgentValue = packageName
        config.tileDownloadThreads = 8
        config.tileFileSystemThreads = 8
        config.tileDownloadMaxQueueSize = 60
        config.tileFileSystemMaxQueueSize = 60
        config.cacheMapTileCount = 40
        config.osmdroidBasePath = filesDir
        config.osmdroidTileCache = java.io.File(filesDir, "osmdroid/tiles")
        config.load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
    }
}

object CampusVoteRepository {
    private const val PREF_NAME = "campus_votes_prefs"
    private const val KEY_VOTES = "votes_data"

    fun recordVote(context: android.content.Context, campusName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_VOTES, "{}") ?: "{}"
        try {
            val json = com.google.gson.JsonParser.parseString(currentJson).asJsonObject
            val key = campusName.trim().lowercase(java.util.Locale.ROOT)
            val currentCount = if (json.has(key)) json.get(key).asInt else 0
            json.addProperty(key, currentCount + 1)
            prefs.edit().putString(KEY_VOTES, json.toString()).apply()
        } catch (e: Exception) {
            val newObj = com.google.gson.JsonObject()
            newObj.addProperty(campusName.trim().lowercase(java.util.Locale.ROOT), 1)
            prefs.edit().putString(KEY_VOTES, newObj.toString()).apply()
        }
    }

    fun getVotes(context: android.content.Context): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_VOTES, "{}") ?: "{}"
        val result = mutableListOf<Pair<String, Int>>()
        try {
            val json = com.google.gson.JsonParser.parseString(currentJson).asJsonObject
            for (entry in json.entrySet()) {
                val formattedName = entry.key.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                result.add(Pair(formattedName, entry.value.asInt))
            }
        } catch (e: Exception) {}
        
        // Add default sample seed data if empty so admin dashboard has real preview
        if (result.isEmpty()) {
            return listOf(
                Pair("LPU Phagwara", 142),
                Pair("VIT Chennai", 98),
                Pair("SRM KTR", 64),
                Pair("IIT Bombay", 45),
                Pair("Amity Noida", 31)
            )
        }
        return result.sortedByDescending { it.second }
    }

    fun clearVotes(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_VOTES).apply()
    }
}
