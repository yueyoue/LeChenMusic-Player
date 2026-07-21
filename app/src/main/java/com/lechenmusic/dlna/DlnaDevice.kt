package com.lechenmusic.dlna

/**
 * DLNA/UPnP 设备信息
 */
data class DlnaDevice(
    val name: String,
    val location: String,        // 设备描述URL
    val usn: String,             // 唯一服务名
    val server: String = "",
    val controlUrl: String = "", // AVTransport 控制URL
    val friendlyName: String = "",
    val modelName: String = ""
) {
    val displayName: String get() = friendlyName.ifBlank { name }.ifBlank { modelName }
}
