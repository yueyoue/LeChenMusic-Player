package com.lechenmusic.player

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory

/**
 * 缓存 key 规范化：把「和登录 token 绑定的完整 URL」换成稳定 key，
 * 让预加载写入和播放读取共享同一份缓存，且不受 jwt/凭据变化影响。
 *
 * key 格式：
 *  - 音乐（Subsonic 流）：`song:<songId>`（取 URL 的 `id` 参数，要求路径为 rest/stream）
 *  - 有声书章节流：`ab:<bookId>:<chapterId>`（取 URL 路径，彻底摆脱 query 里的 jwt）
 *  - 其它（电台等）：保持默认行为（`dataSpec.key ?: uri.toString()`）
 *
 * 注意：新 key 只影响「今后写入」的缓存；升级前的旧条目（完整 URL key）无法被播放
 * 复用，会作为孤儿条目被 4GB LRU 逐步淘汰。「缓存音乐」页的 key 解析做了双格式兼容，
 * 新旧 key 混合时都能列出来。
 */
object StableCacheKeyFactory : CacheKeyFactory {

    /** 有声书章节流：/api/audiobook/{bookId}/chapters/{chapterId}/stream */
    private val AUDIOBOOK_STREAM_REGEX = Regex("/api/audiobook/([^/]+)/chapters/([^/]+)/stream")

    override fun buildCacheKey(dataSpec: DataSpec): String {
        // 显式指定的 key（如有）优先
        dataSpec.key?.let { return it }
        return fromUri(dataSpec.uri) ?: dataSpec.uri.toString()
    }

    /**
     * 从 URI 解析稳定缓存 key。音乐 / 有声书返回规范化 key，其它返回 null（走默认行为）。
     * 预加载器也用它算 key，保证与播放链路一致。
     */
    fun fromUri(uri: Uri): String? {
        val path = uri.path ?: return null
        AUDIOBOOK_STREAM_REGEX.find(path)?.let { m ->
            return "ab:${m.groupValues[1]}:${m.groupValues[2]}"
        }
        // 音乐流必须是 rest/stream 路径才按歌曲处理，
        // 避免电台等其它 URL 恰好带 id 参数时被误打成 song: key 造成撞 key。
        if (path.contains("rest/stream")) {
            val id = uri.getQueryParameter("id")
            if (!id.isNullOrEmpty()) return "song:$id"
        }
        return null
    }
}
