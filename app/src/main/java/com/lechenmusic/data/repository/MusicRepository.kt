package com.lechenmusic.data.repository

import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.api.SubsonicApi
import com.lechenmusic.data.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody

class MusicRepository {
    private var api: SubsonicApi? = null
    private var serverUrl: String = ""
    private var username: String = ""
    private var password: String = ""

    // Separate API instance with JWT auth for audiobook endpoints
    private var audiobookApi: SubsonicApi? = null

    fun configure(baseUrl: String, user: String, pass: String) {
        serverUrl = baseUrl
        username = user
        password = pass
        api = ApiClient.getApi(baseUrl)
        audiobookApi = ApiClient.getAudiobookApi(baseUrl)
    }

    /** Authenticate with Navidrome and store JWT token for audiobook API */
    suspend fun authenticateNavidrome(): Boolean {
        android.util.Log.d("LeChenMusic", "authenticateNavidrome: Starting for $serverUrl")
        val token = ApiClient.authenticateNavidrome(serverUrl, username, password)
        if (token != null) {
            android.util.Log.d("LeChenMusic", "authenticateNavidrome: Success, token length=${token.length}")
        } else {
            android.util.Log.e("LeChenMusic", "authenticateNavidrome: Failed! Token is null")
        }
        return token != null
    }

    fun getCoverArtUrl(coverArtId: String?): String? {
        return ApiClient.getCoverArtUrl(serverUrl, username, password, coverArtId)
    }

    fun getStreamUrl(songId: String): String {
        return ApiClient.getStreamUrl(serverUrl, username, password, songId)
    }

    suspend fun ping(): Result<Unit> {
        return try {
            val response = api!!.ping(username, password)
            if (response.subsonicResponse.status == "ok") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.subsonicResponse.error?.message ?: "连接失败"))
            }
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("非JSON响应") == true -> "服务器地址错误或服务器未运行"
                e.message?.contains("格式错误") == true -> "服务器响应格式错误，请检查地址"
                e.message?.contains("timeout") == true -> "连接超时，请检查服务器地址"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查是否正确"
                e.message?.contains("Connection refused") == true -> "连接被拒绝，请检查服务器端口"
                e.message?.contains("SSL") == true -> "SSL证书错误，请检查HTTPS配置"
                else -> e.message ?: "连接失败"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun getNewestAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "newest", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "recent", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFrequentAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "frequent", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "random", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbumList2(type: String, size: Int = 50, offset: Int = 0): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = type, size = size, offset = offset)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbum(id: String): Result<AlbumDetail> {
        return try {
            val response = api!!.getAlbum(username, password, id)
            val album = response.subsonicResponse.album
            if (album != null) Result.success(album)
            else Result.failure(Exception("专辑不存在"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArtists(): Result<List<Artist>> {
        return try {
            val response = api!!.getArtists(username, password)
            val artists = response.subsonicResponse.artists?.index?.flatMap { it.artist ?: emptyList() } ?: emptyList()
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArtist(id: String): Result<ArtistDetail> {
        return try {
            val response = api!!.getArtist(username, password, id)
            val artist = response.subsonicResponse.artist
            if (artist != null) Result.success(artist)
            else Result.failure(Exception("歌手不存在"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String): Result<SearchResult> {
        return try {
            val response = api!!.search(username, password, query)
            Result.success(response.subsonicResponse.searchResult3 ?: SearchResult())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylists(): Result<List<Playlist>> {
        return try {
            val response = api!!.getPlaylists(username, password)
            Result.success(response.subsonicResponse.playlists?.playlist ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylist(id: String): Result<PlaylistDetail> {
        return try {
            val response = api!!.getPlaylist(username, password, id)
            val playlist = response.subsonicResponse.playlist
            if (playlist != null) Result.success(playlist)
            else Result.failure(Exception("歌单不存在"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomSongs(size: Int = 20): Result<List<Song>> {
        return try {
            val response = api!!.getRandomSongs(username, password, size)
            // Subsonic API may return songs in "songs" or "randomSongs" field
            val songs = response.subsonicResponse.songs?.song
                ?: response.subsonicResponse.randomSongs?.song
                ?: emptyList()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStarred(): Result<StarredData> {
        return try {
            val response = api!!.getStarred(username, password)
            val body = response.subsonicResponse
            val starred = body.starred2 ?: body.starred
            Result.success(StarredData(
                songs = starred?.song ?: emptyList(),
                albums = starred?.album ?: emptyList(),
                artists = starred?.artist ?: emptyList()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLyrics(artist: String, title: String): Result<String?> {
        return try {
            val response = api!!.getLyrics(username, password, artist, title)
            // Check both 'value' and 'lyrics' fields for lyrics text
            val lyrics = response.subsonicResponse.lyrics?.text
            Result.success(lyrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServerStats(): Result<ServerStats> {
        return try {
            // Get real counts from server
            val playlistsResult = api!!.getPlaylists(username, password)
            val playlistCount = playlistsResult.subsonicResponse.playlists?.playlist?.size ?: 0

            // Get artist count from indexes
            val indexesResult = api!!.getIndexes(username, password)
            val artistCount = indexesResult.subsonicResponse.indexes?.index
                ?.flatMap { it.artist ?: emptyList() }?.size ?: 0

            // Get album count by paginating through all albums
            var albumCount = 0
            var albumOffset = 0
            val albumPageSize = 500
            while (true) {
                val page = api!!.getAlbumList2(username, password, type = "newest", size = albumPageSize, offset = albumOffset)
                val albums = page.subsonicResponse.albumList2?.album ?: emptyList()
                albumCount += albums.size
                if (albums.size < albumPageSize) break
                albumOffset += albumPageSize
                if (albumOffset > 10000) break // safety limit
            }

            // Get song count by paginating through all albums and summing songCount
            var songCount = 0
            var songAlbumOffset = 0
            while (true) {
                val page = api!!.getAlbumList2(username, password, type = "newest", size = albumPageSize, offset = songAlbumOffset)
                val albums = page.subsonicResponse.albumList2?.album ?: emptyList()
                songCount += albums.sumOf { it.songCount }
                if (albums.size < albumPageSize) break
                songAlbumOffset += albumPageSize
                if (songAlbumOffset > 10000) break
            }

            // Get audiobook count
            var audiobookCount = 0
            try {
                val token = com.lechenmusic.data.api.NavidromeAuth.token
                if (token != null) {
                    val abResponse = audiobookApi!!.getAudiobooks("Bearer $token")
                    if (abResponse.isSuccessful && abResponse.body() != null) {
                        val jsonObj = abResponse.body()!!.asJsonObject
                        val dataArray = jsonObj.getAsJsonArray("data")
                        audiobookCount = dataArray?.size() ?: 0
                    }
                }
            } catch (_: Exception) {}

            Result.success(ServerStats(
                songCount = songCount,
                albumCount = albumCount,
                playlistCount = playlistCount,
                artistCount = artistCount,
                audiobookCount = audiobookCount
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            api!!.updatePlaylist(username, password, playlistId, songIdToAdd = songId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, songIndex: Int): Result<Unit> {
        return try {
            api!!.removeFromPlaylist(username, password, playlistId, songIndex)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(name: String, songId: String? = null, isPublic: Boolean = false): Result<String> {
        return try {
            val response = api!!.createPlaylist(username, password, name, songId = songId?.ifBlank { null })
            val playlistId = response.subsonicResponse.playlist?.id ?: ""
            // Set public visibility if requested
            if (playlistId.isNotBlank() && isPublic) {
                try {
                    api!!.updatePlaylist(username, password, playlistId, public = true)
                } catch (_: Exception) { }
            }
            Result.success(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSongs(): Result<List<Song>> {
        return try {
            val allSongs = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()

            // Strategy 1: Paginate through ALL albums to get every song
            val albumTypes = listOf("newest", "recent", "frequent", "random", "starred", "alphabeticalByName")
            for (type in albumTypes) {
                var offset = 0
                val pageSize = 500
                while (true) {
                    try {
                        val page = api!!.getAlbumList2(username, password, type = type, size = pageSize, offset = offset)
                        val albums = page.subsonicResponse.albumList2?.album ?: emptyList()
                        for (album in albums) {
                            try {
                                val detail = api!!.getAlbum(username, password, album.id)
                                detail.subsonicResponse.album?.song?.forEach { song ->
                                    if (seenIds.add(song.id)) {
                                        allSongs.add(song)
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                        if (albums.size < pageSize) break
                        offset += pageSize
                        if (offset > 50000) break // safety limit
                    } catch (_: Exception) { break }
                }
            }

            // Strategy 2: Supplement with search queries for songs not in any album
            val searchQueries = listOf("a", "e", "i", "o", "u", "the", "s", "t", "n", "r", "l", "m", "d", "c", "b")
            for (query in searchQueries) {
                try {
                    val result = api!!.search(username, password, query, songCount = 500)
                    val songs = result.subsonicResponse.searchResult3?.song ?: emptyList()
                    for (song in songs) {
                        if (seenIds.add(song.id)) {
                            allSongs.add(song)
                        }
                    }
                } catch (_: Exception) { }
            }

            if (allSongs.isNotEmpty()) {
                Result.success(allSongs)
            } else {
                // Final fallback: getRandomSongs
                val response = api!!.getRandomSongs(username, password, 500)
                val songs = response.subsonicResponse.songs?.song
                    ?: response.subsonicResponse.randomSongs?.song
                    ?: emptyList()
                Result.success(songs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIndexes(): Result<List<Artist>> {
        return try {
            val response = api!!.getIndexes(username, password)
            val artists = response.subsonicResponse.indexes?.index?.flatMap { it.artist ?: emptyList() } ?: emptyList()
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun star(id: String): Result<Unit> {
        return try {
            api!!.star(username, password, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unstar(id: String): Result<Unit> {
        return try {
            api!!.unstar(username, password, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scrobble(id: String): Result<Unit> {
        return try {
            api!!.scrobble(username, password, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistPublic(playlistId: String, isPublic: Boolean): Result<Unit> {
        return try {
            api!!.updatePlaylist(username, password, playlistId, public = isPublic)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInternetRadioStations(): Result<List<InternetRadioStation>> {
        return try {
            val response = api!!.getInternetRadioStations(username, password)
            val stations = response.subsonicResponse.internetRadioStations?.internetRadioStation ?: emptyList()
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Audiobook Methods =====

    data class AudiobookListResponse(val data: List<com.lechenmusic.data.model.Audiobook>? = null)
    data class AudiobookDetailApiResponse(val data: AudiobookDetailData? = null)
    data class AudiobookDetailData(
        val book: com.lechenmusic.data.model.Audiobook? = null,
        val chapters: List<com.lechenmusic.data.model.AudiobookChapter>? = null,
        val progress: com.lechenmusic.data.model.AudiobookProgress? = null
    )

    private fun handleAudiobookResponse(response: retrofit2.Response<com.google.gson.JsonElement>): Result<List<com.lechenmusic.data.model.Audiobook>> {
        android.util.Log.d("LeChenMusic", "getAudiobooks: HTTP ${response.code()} ok=${response.isSuccessful}")
        if (!response.isSuccessful) {
            val msg = when (response.code()) {
                401 -> "认证失败(401)，请重新登录"
                403 -> "无权限访问(403)"
                404 -> "接口不存在(404)，请检查服务器版本"
                500 -> "服务器内部错误(500)"
                else -> "HTTP ${response.code()}"
            }
            android.util.Log.e("LeChenMusic", "getAudiobooks: $msg")
            return Result.failure(Exception(msg))
        }
        if (response.body() == null) {
            return Result.failure(Exception("服务器返回空数据"))
        }
        return try {
            val gson = com.google.gson.Gson()
            val bodyElement = response.body()!!
            val jsonObj = bodyElement.asJsonObject
            val dataArray = jsonObj.getAsJsonArray("data")
            if (dataArray != null) {
                val books = gson.fromJson<List<com.lechenmusic.data.model.Audiobook>>(
                    dataArray,
                    object : com.google.gson.reflect.TypeToken<List<com.lechenmusic.data.model.Audiobook>>() {}.type
                )
                android.util.Log.d("LeChenMusic", "getAudiobooks: parsed ${books.size} books")
                Result.success(books)
            } else {
                android.util.Log.w("LeChenMusic", "getAudiobooks: no 'data' field in response")
                Result.failure(Exception("服务器返回格式错误: 缺少data字段"))
            }
        } catch (e: Exception) {
            android.util.Log.e("LeChenMusic", "getAudiobooks: parse error", e)
            Result.failure(Exception("数据解析失败: ${e.message}"))
        }
    }

    suspend fun getAudiobooks(): Result<List<com.lechenmusic.data.model.Audiobook>> {
        return try {
            var token = com.lechenmusic.data.api.NavidromeAuth.token
            if (token == null) {
                android.util.Log.w("LeChenMusic", "getAudiobooks: token is null, re-authenticating...")
                val authSuccess = authenticateNavidrome()
                token = com.lechenmusic.data.api.NavidromeAuth.token
                if (token == null) {
                    val msg = if (!authSuccess) "Native API认证失败，请检查用户名密码" else "Token获取失败"
                    android.util.Log.e("LeChenMusic", "getAudiobooks: $msg")
                    return Result.failure(Exception(msg))
                }
            }
            val response = audiobookApi!!.getAudiobooks("Bearer $token")
            handleAudiobookResponse(response)
        } catch (e: Exception) {
            android.util.Log.e("LeChenMusic", "getAudiobooks: Exception", e)
            Result.failure(e)
        }
    }

    suspend fun getAudiobookDetail(id: String): Result<com.lechenmusic.data.model.AudiobookDetail> {
        return try {
            var token = com.lechenmusic.data.api.NavidromeAuth.token
            if (token == null) {
                android.util.Log.w("LeChenMusic", "getAudiobookDetail: token is null! Re-authenticating...")
                authenticateNavidrome()
                token = com.lechenmusic.data.api.NavidromeAuth.token
                if (token == null) {
                    return Result.failure(Exception("Not authenticated"))
                }
            }
            android.util.Log.d("LeChenMusic", "getAudiobookDetail: calling API for id=$id")
            val response = audiobookApi!!.getAudiobook(id, "Bearer $token")
            android.util.Log.d("LeChenMusic", "getAudiobookDetail: HTTP ${response.code()} ok=${response.isSuccessful}")
            if (response.isSuccessful && response.body() != null) {
                val gson = com.google.gson.Gson()
                val bodyElement = response.body()!!
                val jsonObj = bodyElement.asJsonObject
                val dataObj = jsonObj.getAsJsonObject("data")
                if (dataObj != null) {
                    val bookObj = dataObj.getAsJsonObject("book")
                    val chaptersArray = dataObj.getAsJsonArray("chapters")
                    val book = if (bookObj != null) gson.fromJson(bookObj, com.lechenmusic.data.model.Audiobook::class.java) else com.lechenmusic.data.model.Audiobook()
                    val chapters = if (chaptersArray != null) {
                        gson.fromJson<List<com.lechenmusic.data.model.AudiobookChapter>>(
                            chaptersArray,
                            object : com.google.gson.reflect.TypeToken<List<com.lechenmusic.data.model.AudiobookChapter>>() {}.type
                        )
                    } else emptyList()
                    android.util.Log.d("LeChenMusic", "getAudiobookDetail: parsed book=${book.title}, chapters=${chapters.size}")
                    Result.success(com.lechenmusic.data.model.AudiobookDetail(book = book, chapters = chapters))
                } else {
                    android.util.Log.w("LeChenMusic", "getAudiobookDetail: no 'data' field in response")
                    Result.failure(Exception("No data in response"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "unknown"
                android.util.Log.e("LeChenMusic", "getAudiobookDetail: HTTP ${response.code()} error=$errorBody")
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("LeChenMusic", "getAudiobookDetail: Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getAudiobookCoverUrl(bookId: String): String? {
        val normalizedUrl = serverUrl.trimEnd('/')
        val token = com.lechenmusic.data.api.NavidromeAuth.token
        return if (token != null) {
            "$normalizedUrl/api/audiobook/$bookId/cover?token=$token"
        } else {
            val passBytes = password.toByteArray()
            val encodedPass = if (password.startsWith("enc:")) password else "enc:" + passBytes.joinToString("") { "%02x".format(it) }
            "$normalizedUrl/api/audiobook/$bookId/cover?u=$username&p=$encodedPass"
        }
    }

    fun getAudiobookChapterStreamUrl(bookId: String, chapterId: String): String {
        val normalizedUrl = serverUrl.trimEnd('/')
        val token = com.lechenmusic.data.api.NavidromeAuth.token
        return if (token != null) {
            // Use ?jwt= param (matches server's jwtauth.TokenFromQuery)
            "$normalizedUrl/api/audiobook/$bookId/chapters/$chapterId/stream?jwt=$token"
        } else {
            "$normalizedUrl/api/audiobook/$bookId/chapters/$chapterId/stream"
        }
    }


    suspend fun saveAudiobookProgress(bookId: String, chapterId: String, chapterNumber: Int, positionSeconds: Int): Result<Unit> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.failure(Exception("Not authenticated"))
            val body = okhttp3.RequestBody.create(
                "application/json".toMediaType(),
                com.google.gson.Gson().toJson(mapOf(
                    "chapterId" to chapterId,
                    "chapterNumber" to chapterNumber,
                    "position" to positionSeconds
                ))
            )
            val response = audiobookApi!!.saveAudiobookProgress(bookId, body, "Bearer $token")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAudiobookProgress(bookId: String): Result<com.lechenmusic.data.model.AudiobookProgress?> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.success(null)
            val response = audiobookApi!!.getAudiobookProgress(bookId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val gson = com.google.gson.Gson()
                val body = response.body()
                val jsonObj = body?.asJsonObject
                val dataObj = jsonObj?.getAsJsonObject("data")
                if (dataObj != null && !dataObj.isJsonNull) {
                    Result.success(gson.fromJson(dataObj, com.lechenmusic.data.model.AudiobookProgress::class.java))
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStarredAudiobooks(): Result<List<com.lechenmusic.data.model.Audiobook>> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.success(emptyList())
            val response = audiobookApi!!.getStarredAudiobooks("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val gson = com.google.gson.Gson()
                val parsed = gson.fromJson(response.body(), AudiobookListResponse::class.java)
                Result.success(parsed?.data ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun starAudiobook(id: String): Result<Unit> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.failure(Exception("Not authenticated"))
            val response = audiobookApi!!.starAudiobook(id, "Bearer $token")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unstarAudiobook(id: String): Result<Unit> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.failure(Exception("Not authenticated"))
            val response = audiobookApi!!.unstarAudiobook(id, "Bearer $token")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Narrator Methods =====

    data class NarratorInfo(val name: String = "", val count: Int = 0)

    suspend fun getNarrators(): Result<List<NarratorInfo>> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.success(emptyList())
            val response = audiobookApi!!.getNarrators("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val gson = com.google.gson.Gson()
                val jsonObj = response.body()?.asJsonObject
                val dataArray = jsonObj?.getAsJsonArray("data")
                val narrators = mutableListOf<NarratorInfo>()
                dataArray?.forEach { item ->
                    val obj = item.asJsonObject
                    narrators.add(NarratorInfo(
                        name = obj.get("name")?.asString ?: "",
                        count = obj.get("count")?.asInt ?: 0
                    ))
                }
                Result.success(narrators)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNarratorDetail(name: String): Result<List<com.lechenmusic.data.model.Audiobook>> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.success(emptyList())
            val response = audiobookApi!!.getNarratorDetail(name, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val gson = com.google.gson.Gson()
                val jsonObj = response.body()?.asJsonObject
                val dataObj = jsonObj?.getAsJsonObject("data")
                val worksArray = dataObj?.getAsJsonArray("works")
                val type = object : com.google.gson.reflect.TypeToken<List<com.lechenmusic.data.model.Audiobook>>() {}.type
                val works: List<com.lechenmusic.data.model.Audiobook> = gson.fromJson(worksArray ?: com.google.gson.JsonArray(), type)
                Result.success(works)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAudiobooks(query: String): Result<List<com.lechenmusic.data.model.Audiobook>> {
        return try {
            val token = com.lechenmusic.data.api.NavidromeAuth.token ?: return Result.success(emptyList())
            val response = audiobookApi!!.searchAudiobooks(query, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val gson = com.google.gson.Gson()
                val parsed = gson.fromJson(response.body(), AudiobookListResponse::class.java)
                Result.success(parsed?.data ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

data class StarredData(val songs: List<com.lechenmusic.data.model.Song> = emptyList(), val albums: List<com.lechenmusic.data.model.Album> = emptyList(), val artists: List<com.lechenmusic.data.model.Artist> = emptyList())
data class ServerStats(val songCount: Int = 0, val albumCount: Int = 0, val playlistCount: Int = 0, val artistCount: Int = 0, val audiobookCount: Int = 0)

