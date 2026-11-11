package com.lechenmusic.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File

/**
 * 通用「列表磁盘缓存」工具：把 List<T> 以 JSON 数组形式流式写入/读出 filesDir 下的文件。
 *
 * 背景：乐库 / 缓存 / 专辑 / 搜索 这些列表每次冷启动都要等网络拉取才有内容。
 * 这里把列表落到手机存储，打开界面先上屏本地副本，再由后台静默同步服务端数据
 * （stale-while-revalidate），用户全程无感等待。
 *
 * 实现要点（与 all_songs_cache.json 同一套路）：
 * - 流式读写（JsonReader/JsonWriter 逐条解析），不把整个列表拼成一个巨型 String，
 *   大曲库下也不会打爆堆内存；
 * - 读写都吞异常：缓存只是加速手段，坏了就当没有缓存，绝不影响主流程；
 * - 要求 T 是 Gson 友好的数据类（序列化会带上全部非空字段，反序列化可完整还原）。
 *
 * 调用方负责在 IO 线程调用（read/write 内部不切线程）。
 */
object ListJsonCache {

    private val gson = Gson()

    /** 把列表写入 [dir]/[name]（JSON 数组，流式）。写失败静默忽略。 */
    fun <T> write(dir: File, name: String, list: List<T>, cls: Class<T>) {
        try {
            JsonWriter(java.io.BufferedWriter(File(dir, name).writer())).use { w ->
                w.beginArray()
                for (item in list) gson.toJson(item, cls, w)
                w.endArray()
            }
        } catch (_: Exception) {
        }
    }

    /** 读取 [dir]/[name] 的 JSON 数组。文件不存在或解析失败返回空列表。 */
    fun <T> read(dir: File, name: String, cls: Class<T>): List<T> {
        val file = File(dir, name)
        if (!file.exists()) return emptyList()
        return try {
            JsonReader(java.io.BufferedReader(file.reader())).use { r ->
                val out = mutableListOf<T>()
                r.beginArray()
                while (r.hasNext()) out.add(gson.fromJson(r, cls))
                r.endArray()
                out
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 便捷方法：读字符串列表（如热门搜索词）。 */
    fun readStrings(dir: File, name: String): List<String> = read(dir, name, String::class.java)

    /** 便捷方法：写字符串列表。 */
    fun writeStrings(dir: File, name: String, list: List<String>) = write(dir, name, list, String::class.java)
}
