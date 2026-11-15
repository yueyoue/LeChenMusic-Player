package com.lechenmusic.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject

// ===== 在线搜歌（M6）数据模型：与服务端 online 系列接口 JSON 对齐 =====
// 手工解析（不用 Gson 反射直接映射）：服务端部分字段带 omitempty，
// Gson 对缺失字段会绕过 Kotlin 默认值把非空字段留成 null，这里逐字段兜默认值。

private fun JsonObject.str(key: String): String =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""

private fun JsonObject.int(key: String): Int =
    get(key)?.takeIf { it.isJsonPrimitive }?.asInt ?: 0

private fun JsonObject.long(key: String): Long =
    get(key)?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L

private fun JsonObject.bool(key: String): Boolean =
    get(key)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false

/** 下载源音质档位（server/online.QualityOption） */
data class OnlineQualityOption(
    val format: String = "",
    val bitrate: Int = 0,
    val level: String = "",
    val size: String = ""
) {
    /** 展示文案："FLAC 2000k 35.2MB" */
    val label: String
        get() {
            val head = listOf(format.uppercase(), if (bitrate > 0) "${bitrate}k" else "")
                .filter { it.isNotBlank() }
                .joinToString(" ")
            return if (size.isNotBlank()) "$head $size" else head
        }

    companion object {
        fun fromJson(obj: JsonObject): OnlineQualityOption = OnlineQualityOption(
            format = obj.str("format"),
            bitrate = obj.int("bitrate"),
            level = obj.str("level"),
            size = obj.str("size")
        )
    }
}

/** 在线搜索条目（server/online.Song + inLibrary） */
data class OnlineSong(
    val source: String = "",
    val platform: String = "",
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val albumName: String = "",
    val albumId: String = "",
    val coverUrl: String = "",
    val duration: Int = 0,
    val qualities: List<OnlineQualityOption> = emptyList(),
    val time: Long = 0,
    val sign: String = "",
    val inLibrary: Boolean = false,
    /** 服务端标记：已有未完成的下载任务（排队/下载中/刮削中） */
    val downloading: Boolean = false
) {
    val durationFormatted: String
        get() = if (duration <= 0) "" else "%d:%02d".format(duration / 60, duration % 60)

    companion object {
        fun fromJson(obj: JsonObject): OnlineSong {
            val qualities = mutableListOf<OnlineQualityOption>()
            obj.get("qualities")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { e ->
                if (e.isJsonObject) qualities.add(OnlineQualityOption.fromJson(e.asJsonObject))
            }
            return OnlineSong(
                source = obj.str("source"),
                platform = obj.str("platform"),
                id = obj.str("id"),
                name = obj.str("name"),
                artist = obj.str("artist"),
                albumName = obj.str("albumName"),
                albumId = obj.str("albumId"),
                coverUrl = obj.str("coverUrl"),
                duration = obj.int("duration"),
                qualities = qualities,
                time = obj.long("time"),
                sign = obj.str("sign"),
                inLibrary = obj.bool("inLibrary"),
                downloading = obj.bool("downloading")
            )
        }

        /** 解析 GET /api/online/search 响应 {songs:[...], total:N} */
        fun parseSearchResult(json: JsonElement?): OnlineSearchResult {
            val root = json?.takeIf { it.isJsonObject }?.asJsonObject ?: return OnlineSearchResult()
            val songs = mutableListOf<OnlineSong>()
            root.get("songs")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { e ->
                if (e.isJsonObject) songs.add(fromJson(e.asJsonObject))
            }
            return OnlineSearchResult(songs = songs, total = root.int("total"))
        }
    }
}

/** GET /api/online/search 响应 */
data class OnlineSearchResult(
    val songs: List<OnlineSong> = emptyList(),
    val total: Int = 0
)

/** 下载任务（server model.DownloadTask 的 JSON 投影） */
data class OnlineDownloadTask(
    val id: String = "",
    val userId: String = "",
    val songName: String = "",
    val artist: String = "",
    val albumName: String = "",
    val coverUrl: String = "",
    val quality: String = "",
    val actualFormat: String = "",
    val actualBitrate: Int = 0,
    val status: String = "",
    val progressBytes: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String = "",
    val error: String = ""
) {
    val isActive: Boolean get() = status in ACTIVE_STATUSES
    val isDone: Boolean get() = status == "done"
    val isFailed: Boolean get() = status == "failed"

    val progressPercent: Int
        get() = if (totalBytes > 0) minOf(100, (progressBytes * 100 / totalBytes).toInt()) else 0

    val statusLabel: String
        get() = when (status) {
            "queued" -> "排队中"
            "resolving" -> "解析直链"
            "downloading" -> "下载中"
            "tagging" -> "刮削入库"
            "done" -> "已入库"
            "failed" -> "失败"
            else -> status
        }

    /** 实际音质展示："FLAC 2000k"，无则回退期望音质 */
    val qualityLabel: String
        get() {
            if (actualFormat.isNotBlank()) {
                return if (actualBitrate > 0) "${actualFormat.uppercase()} ${actualBitrate}k" else actualFormat.uppercase()
            }
            return quality.ifBlank { "默认" }
        }

    companion object {
        /** 进行中状态集合（与 Web 端一致） */
        val ACTIVE_STATUSES = setOf("queued", "resolving", "downloading", "tagging")

        fun fromJson(obj: JsonObject): OnlineDownloadTask = OnlineDownloadTask(
            id = obj.str("id"),
            userId = obj.str("userId"),
            songName = obj.str("songName"),
            artist = obj.str("artist"),
            albumName = obj.str("albumName"),
            coverUrl = obj.str("coverUrl"),
            quality = obj.str("quality"),
            actualFormat = obj.str("actualFormat"),
            actualBitrate = obj.int("actualBitrate"),
            status = obj.str("status"),
            progressBytes = obj.long("progressBytes"),
            totalBytes = obj.long("totalBytes"),
            filePath = obj.str("filePath"),
            error = obj.str("error")
        )

        /** 解析 GET /api/online/tasks（数组）或 POST /api/online/download（单对象）响应 */
        fun parseList(json: JsonElement?): List<OnlineDownloadTask> {
            val arr = json?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
            val tasks = mutableListOf<OnlineDownloadTask>()
            arr.forEach { e -> if (e.isJsonObject) tasks.add(fromJson(e.asJsonObject)) }
            return tasks
        }

        fun parseOne(json: JsonElement?): OnlineDownloadTask? =
            json?.takeIf { it.isJsonObject }?.let { fromJson(it.asJsonObject) }
    }
}
