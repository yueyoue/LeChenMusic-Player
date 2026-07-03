package com.lechenmusic.data.repository

import android.content.Context
import android.content.SharedPreferences

/**
 * 歌词本地缓存
 * 使用 SharedPreferences 存储，key 为歌曲 ID
 * 支持离线播放时读取缓存歌词
 */
class LyricsCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "lyrics_cache", Context.MODE_PRIVATE
    )

    /**
     * 获取缓存的歌词
     * @param songId 歌曲 ID
     * @return 缓存的歌词文本，无缓存返回 null
     */
    fun get(songId: String): String? {
        return prefs.getString(songId, null)
    }

    /**
     * 缓存歌词
     * @param songId 歌曲 ID
     * @param lyrics 歌词文本（LRC 或纯文本）
     */
    fun put(songId: String, lyrics: String) {
        prefs.edit().putString(songId, lyrics).apply()
    }

    /**
     * 检查是否有缓存
     */
    fun has(songId: String): Boolean {
        return prefs.contains(songId)
    }

    /**
     * 清除所有缓存
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    /**
     * 获取缓存的歌词数量
     */
    fun size(): Int {
        return prefs.all.size
    }
}
