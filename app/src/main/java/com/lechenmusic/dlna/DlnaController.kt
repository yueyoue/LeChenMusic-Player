package com.lechenmusic.dlna

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * DLNA/UPnP AVTransport 控制器
 * 通过 SOAP 请求控制远程播放器
 */
class DlnaController(private val device: DlnaDevice) {
    private val TAG = "DlnaController"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val controlUrl: String
        get() {
            val base = device.location.substringBeforeLast("/")
            val path = device.controlUrl
            return if (path.startsWith("http")) path else "$base/$path"
        }

    companion object {
        private const val AV_TRANSPORT_NS = "urn:schemas-upnp-org:service:AVTransport:1"
        private const val SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/"
    }

    /**
     * 设置播放URI并开始播放
     * @param url 视频/音频流地址
     * @param title 媒体标题
     */
    suspend fun setUriAndPlay(url: String, title: String = "LeChenMusic"): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. SetAVTransportURI
            val setUriSoap = buildSoap("SetAVTransportURI", """
                <InstanceID>0</InstanceID>
                <CurrentURI>${escapeXml(url)}</CurrentURI>
                <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"&gt;&lt;item id="0" parentID="-1" restricted="1"&gt;&lt;dc:title&gt;${escapeXml(title)}&lt;/dc:title&gt;&lt;res protocolInfo="http-get:*:*:*"&gt;${escapeXml(url)}&lt;/res&gt;&lt;upnp:class&gt;object.item.videoItem&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
            """.trimIndent())
            val setUriResult = sendSoap("SetAVTransportURI", setUriSoap)
            Log.d(TAG, "SetAVTransportURI: $setUriResult")

            // 2. Play
            val playSoap = buildSoap("Play", """
                <InstanceID>0</InstanceID>
                <Speed>1</Speed>
            """.trimIndent())
            val playResult = sendSoap("Play", playSoap)
            Log.d(TAG, "Play: $playResult")

            true
        } catch (e: Exception) {
            Log.e(TAG, "setUriAndPlay 失败: ${e.message}")
            false
        }
    }

    /** 暂停 */
    suspend fun pause(): Boolean = sendAction("Pause")

    /** 继续播放 */
    suspend fun play(): Boolean = sendAction("Play")

    /** 停止 */
    suspend fun stop(): Boolean = sendAction("Stop")

    /**
     * 跳转到指定位置
     * @param seconds 秒数
     */
    suspend fun seek(seconds: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            val timeStr = "%02d:%02d:%02d".format(h, m, s)

            val soap = buildSoap("Seek", """
                <InstanceID>0</InstanceID>
                <Unit>REL_TIME</Unit>
                <Target>$timeStr</Target>
            """.trimIndent())
            val result = sendSoap("Seek", soap)
            Log.d(TAG, "Seek to $timeStr: $result")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Seek 失败: ${e.message}")
            false
        }
    }

    /**
     * 获取播放位置信息
     * @return Pair(当前秒, 总秒) 或 null
     */
    suspend fun getPosition(): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val soap = buildSoap("GetPositionInfo", """
                <InstanceID>0</InstanceID>
            """.trimIndent())
            val result = sendSoap("GetPositionInfo", soap)

            val relTime = extractXmlValue(result, "RelTime") ?: "00:00:00"
            val trackDuration = extractXmlValue(result, "TrackDuration") ?: "00:00:00"

            val currentSec = parseTimeToSeconds(relTime)
            val totalSec = parseTimeToSeconds(trackDuration)

            Pair(currentSec, totalSec)
        } catch (e: Exception) {
            Log.e(TAG, "GetPositionInfo 失败: ${e.message}")
            null
        }
    }

    /**
     * 获取传输状态
     * @return "PLAYING", "PAUSED_PLAYBACK", "STOPPED", "TRANSITIONING" 等
     */
    suspend fun getTransportState(): String = withContext(Dispatchers.IO) {
        try {
            val soap = buildSoap("GetTransportInfo", """
                <InstanceID>0</InstanceID>
            """.trimIndent())
            val result = sendSoap("GetTransportInfo", soap)
            extractXmlValue(result, "CurrentTransportState") ?: "UNKNOWN"
        } catch (e: Exception) {
            Log.e(TAG, "GetTransportInfo 失败: ${e.message}")
            "UNKNOWN"
        }
    }

    /** 设置音量 (0-100) */
    suspend fun setVolume(volume: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val soap = buildSoap("SetVolume", """
                <InstanceID>0</InstanceID>
                <Channel>Master</Channel>
                <DesiredVolume>${volume.coerceIn(0, 100)}</DesiredVolume>
            """.trimIndent())
            sendSoap("SetVolume", soap)
            true
        } catch (e: Exception) {
            Log.e(TAG, "SetVolume 失败: ${e.message}")
            false
        }
    }

    // ==================== 内部方法 ====================

    private suspend fun sendAction(action: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val soap = buildSoap(action, """
                <InstanceID>0</InstanceID>
                <Speed>1</Speed>
            """.trimIndent())
            sendSoap(action, soap)
            true
        } catch (e: Exception) {
            Log.e(TAG, "$action 失败: ${e.message}")
            false
        }
    }

    private fun buildSoap(action: String, body: String): String {
        return """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="$SOAP_ENVELOPE_NS" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <s:Body>
    <u:$action xmlns:u="$AV_TRANSPORT_NS">
      $body
    </u:$action>
  </s:Body>
</s:Envelope>"""
    }

    private fun sendSoap(action: String, soapBody: String): String {
        val mediaType = "text/xml; charset=utf-8".toMediaType()
        val body = soapBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(controlUrl)
            .addHeader("Content-Type", mediaType.toString())
            .addHeader("SOAPAction", "\"$AV_TRANSPORT_NS#$action\"")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val regex = Regex("<$tag>([^<]+)</$tag>")
        return regex.find(xml)?.groupValues?.get(1)
    }

    private fun parseTimeToSeconds(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 3) return 0
        return try {
            parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
        } catch (e: Exception) { 0 }
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
    }
}
