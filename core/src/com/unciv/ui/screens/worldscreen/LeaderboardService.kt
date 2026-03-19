package com.unciv.ui.screens.worldscreen

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val score: Int,
    val totalGames: Int
)

class LeaderboardService {
    companion object {
        private const val BASE_URL = "https://api.mobileups.org/apps/apps/leaderboard"
        private const val APP_ID = "com.mobileups.empireforge"
        private const val TIMEOUT_CONNECT = 10000
        private const val TIMEOUT_READ = 15000
    }

    fun submitScore(userId: String, displayName: String, score: Int, category: String = "ALL", onResult: ((Boolean) -> Unit)? = null) {
        thread {
            try {
                val url = URL("$BASE_URL/submit")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = TIMEOUT_CONNECT
                conn.readTimeout = TIMEOUT_READ
                conn.doOutput = true

                val json = """{"appId":"$APP_ID","userId":"$userId","displayName":"${displayName.replace("\"", "\\\"")}","score":$score,"category":"$category"}"""
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json)
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                conn.disconnect()
                onResult?.invoke(responseCode == 200)
            } catch (e: Exception) {
                onResult?.invoke(false)
            }
        }
    }

    fun getLeaderboard(category: String = "ALL", onResult: (List<LeaderboardEntry>) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/$APP_ID/$category")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = TIMEOUT_CONNECT
                conn.readTimeout = TIMEOUT_READ

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val entries = parseLeaderboard(response)
                onResult(entries)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }

    fun getUserRank(userId: String, category: String = "ALL", onResult: (LeaderboardEntry?) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/rank/$APP_ID/$userId/$category")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = TIMEOUT_CONNECT
                conn.readTimeout = TIMEOUT_READ

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val entry = parseUserRank(response)
                onResult(entry)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    private fun parseLeaderboard(json: String): List<LeaderboardEntry> {
        val entries = mutableListOf<LeaderboardEntry>()
        if (json == "[]" || json.isBlank()) return entries

        // Simple JSON array parsing without external dependencies
        val items = json.removeSurrounding("[", "]").split("},")
        for ((index, item) in items.withIndex()) {
            try {
                val clean = item.trim().removeSurrounding("{", "}")  + (if (!item.endsWith("}")) "" else "")
                val rank = extractIntValue(clean, "rank") ?: (index + 1)
                val userId = extractStringValue(clean, "userId") ?: ""
                val displayName = extractStringValue(clean, "displayName") ?: "Player"
                val score = extractIntValue(clean, "score") ?: 0
                val totalGames = extractIntValue(clean, "totalGames") ?: 0
                entries.add(LeaderboardEntry(rank, userId, displayName, score, totalGames))
            } catch (_: Exception) { }
        }
        return entries
    }

    private fun parseUserRank(json: String): LeaderboardEntry? {
        if (json.isBlank() || json == "null") return null
        return try {
            val clean = json.removeSurrounding("{", "}")
            LeaderboardEntry(
                rank = extractIntValue(clean, "rank") ?: 0,
                userId = "",
                displayName = extractStringValue(clean, "displayName") ?: "Player",
                score = extractIntValue(clean, "score") ?: 0,
                totalGames = extractIntValue(clean, "totalGames") ?: 0
            )
        } catch (_: Exception) { null }
    }

    private fun extractStringValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
        val match = Regex(pattern).find(json) ?: return null
        return match.groupValues[1]
    }

    private fun extractIntValue(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)"
        val match = Regex(pattern).find(json) ?: return null
        return match.groupValues[1].toIntOrNull()
    }
}
