package com.lechenmusic.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 歌词 API（通过服务器代理获取，解决国内网络问题）
 * 
 * 优先级：
 * 1. 服务器 lrclib.net 代理（带时间戳的 LRC）
 * 2. 服务器纯文本歌词（通过 Subsonic API getLyrics）
 */
object QQLyricsApi {

    private const val USER_AGENT = "LeChenMusic/1.4.5"

    /**
     * 通过服务器代理搜索歌词
     * @param serverUrl 服务器地址（如 http://192.168.1.100:3000）
     * @param artist 歌手名
     * @param title 歌曲名
     * @param durationSec 歌曲时长（秒）
     * @return LRC 格式歌词或纯文本歌词，失败返回 null
     */
    suspend fun fetchLyrics(serverUrl: String, artist: String, title: String, durationSec: Int = 0): String? = withContext(Dispatchers.IO) {
        try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val baseUrl = serverUrl.trimEnd('/')
            val url = "$baseUrl/api/lyrics/search?artist=$encodedArtist&title=$encodedTitle&duration=$durationSec"

            val json = httpGet(url) ?: return@withContext null
            val results = parseSearchResults(json)
            if (results.isEmpty()) return@withContext null

            // 优先返回 syncedLyrics（带时间戳）
            for (result in results) {
                if (!result.syncedLyrics.isNullOrBlank()) return@withContext result.syncedLyrics
            }
            for (result in results) {
                if (!result.plainLyrics.isNullOrBlank()) return@withContext result.plainLyrics
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSearchResults(json: String): List<LyricsResult> {
        val results = mutableListOf<LyricsResult>()
        try {
            val root = org.json.JSONObject(json)
            val arr = root.getJSONArray("data")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                results.add(
                    LyricsResult(
                        syncedLyrics = obj.optString("syncedLyrics", null),
                        plainLyrics = obj.optString("plainLyrics", null),
                        duration = obj.optInt("duration", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return results
    }

    private data class LyricsResult(
        val syncedLyrics: String?,
        val plainLyrics: String?,
        val duration: Int = 0
    )

    private fun httpGet(urlStr: String): String? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
