package com.lechenmusic.dlna

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * DLNA/UPnP 设备发现 - 使用 SSDP M-SEARCH
 * 纯 OkHttp + Java DatagramSocket，不依赖第三方 DLNA 库
 */
object DlnaDiscovery {
    private const val TAG = "DlnaDiscovery"
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val SEARCH_TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1"

    private val M_SEARCH = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("MX: 3\r\n")
        append("ST: $SEARCH_TARGET\r\n")
        append("\r\n")
    }

    private val discoveredDevices = ConcurrentHashMap<String, DlnaDevice>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * 搜索局域网 DLNA 设备
     * @param timeoutMs 搜索超时(毫秒)
     * @return 发现的设备列表
     */
    suspend fun search(timeoutMs: Long = 5000): List<DlnaDevice> = withContext(Dispatchers.IO) {
        discoveredDevices.clear()
        val devices = mutableListOf<DlnaDevice>()

        try {
            val socket = DatagramSocket()
            socket.soTimeout = timeoutMs.toInt()
            val group = InetAddress.getByName(SSDP_ADDRESS)
            val searchBytes = M_SEARCH.toByteArray()

            // 发送 M-SEARCH 请求
            val packet = DatagramPacket(searchBytes, searchBytes.size, group, SSDP_PORT)
            socket.send(packet)
            Log.d(TAG, "SSDP M-SEARCH 发送完毕，等待响应...")

            // 接收响应
            val buffer = ByteArray(4096)
            val deadline = System.currentTimeMillis() + timeoutMs

            while (System.currentTimeMillis() < deadline) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    val response = String(responsePacket.data, 0, responsePacket.length)

                    // 解析响应头
                    val location = extractHeader(response, "LOCATION")
                    val usn = extractHeader(response, "USN")
                    val server = extractHeader(response, "SERVER")

                    if (location != null && usn != null && !discoveredDevices.containsKey(usn)) {
                        // 获取设备描述
                        val device = fetchDeviceDescription(location, usn, server ?: "")
                        if (device != null) {
                            discoveredDevices[usn] = device
                            devices.add(device)
                            Log.d(TAG, "发现设备: ${device.displayName} @ $location")
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    break
                }
            }

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "SSDP 搜索失败: ${e.message}")
        }

        devices
    }

    /** 获取设备描述XML，提取 friendlyName 和 AVTransport 控制URL */
    private fun fetchDeviceDescription(location: String, usn: String, server: String): DlnaDevice? {
        return try {
            val request = Request.Builder().url(location).build()
            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: return null

            val friendlyName = extractXmlValue(xml, "friendlyName")
            val modelName = extractXmlValue(xml, "modelName")
            val controlUrl = extractAvTransportControlUrl(xml)

            DlnaDevice(
                name = friendlyName ?: modelName ?: "Unknown",
                location = location,
                usn = usn,
                server = server,
                controlUrl = controlUrl,
                friendlyName = friendlyName ?: "",
                modelName = modelName ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取设备描述失败: ${e.message}")
            null
        }
    }

    /** 从设备描述XML中提取 AVTransport controlURL */
    private fun extractAvTransportControlUrl(xml: String): String {
        // 查找 AVTransport 服务的 controlURL
        val serviceType = "urn:schemas-upnp-org:service:AVTransport"
        val idx = xml.indexOf(serviceType)
        if (idx < 0) return ""

        // 从 serviceType 位置向后找 controlURL
        val subXml = xml.substring(idx)
        val controlUrlMatch = Regex("<controlURL>([^<]+)</controlURL>").find(subXml)
        return controlUrlMatch?.groupValues?.get(1) ?: ""
    }

    private fun extractHeader(response: String, header: String): String? {
        val regex = Regex("$header:\\s*(.+)", RegexOption.IGNORE_CASE)
        return regex.find(response)?.groupValues?.get(1)?.trim()
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val regex = Regex("<$tag>([^<]+)</$tag>")
        return regex.find(xml)?.groupValues?.get(1)
    }
}
