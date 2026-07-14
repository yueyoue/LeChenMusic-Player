package com.lechenmusic.data.api

import com.lechenmusic.data.model.SubsonicResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicApi {

    // Authentication
    @GET("rest/ping")
    suspend fun ping(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Albums
    @GET("rest/getAlbumList2")
    suspend fun getAlbumList2(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("type") type: String, // newest, recent, frequent, random, starred, alphabeticalByName, alphabeticalByArtist
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getAlbum")
    suspend fun getAlbum(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Artists
    @GET("rest/getArtists")
    suspend fun getArtists(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getArtist")
    suspend fun getArtist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Search
    @GET("rest/search3")
    suspend fun search(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("query") query: String,
        @Query("songCount") songCount: Int = 50,
        @Query("albumCount") albumCount: Int = 20,
        @Query("artistCount") artistCount: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getSongsByGenre")
    suspend fun getSongsByGenre(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("genre") genre: String,
        @Query("count") count: Int = 500,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Playlists
    @GET("rest/getPlaylists")
    suspend fun getPlaylists(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getPlaylist")
    suspend fun getPlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Songs
    @GET("rest/getRandomSongs")
    suspend fun getRandomSongs(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("size") size: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getStarred2")
    suspend fun getStarred(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/star")
    suspend fun star(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/unstar")
    suspend fun unstar(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Scrobble (report play)
    @GET("rest/scrobble")
    suspend fun scrobble(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("submission") submission: Boolean = true,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Get lyrics
    @GET("rest/getLyrics")
    suspend fun getLyrics(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("artist") artist: String,
        @Query("title") title: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Get similar songs
    @GET("rest/getSimilarSongs2")
    suspend fun getSimilarSongs2(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("count") count: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Get structured lyrics by song ID (with timestamps)
    @GET("rest/getLyricsBySongId")
    suspend fun getLyricsBySongId(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Update playlist (add/remove songs)
    @GET("rest/updatePlaylist")
    suspend fun updatePlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("playlistId") playlistId: String,
        @Query("songIdToAdd") songIdToAdd: String? = null,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") public: Boolean? = null,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Create playlist
    @GET("rest/createPlaylist")
    suspend fun createPlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("name") name: String,
        @Query("songId") songId: String? = null,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Delete playlist
    @GET("rest/deletePlaylist")
    suspend fun deletePlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Remove song from playlist by index (Subsonic API uses songIndexToRemove)
    @GET("rest/updatePlaylist")
    suspend fun removeFromPlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("playlistId") playlistId: String,
        @Query("songIndexToRemove") songIndexToRemove: Int,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Get indexes (all artists grouped by index)
    @GET("rest/getIndexes")
    suspend fun getIndexes(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Internet Radio Stations
    @GET("rest/getInternetRadioStations")
    suspend fun getInternetRadioStations(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse


    // ===== Audiobook API =====

    @GET("api/audiobook")
    suspend fun getAudiobooks(
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @GET("api/audiobook/with-progress")
    suspend fun getAudiobooksWithProgress(
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @GET("api/audiobook/recent-progress")
    suspend fun getRecentAudiobookProgress(
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @GET("api/audiobook/{id}")
    suspend fun getAudiobook(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @GET("api/audiobook/{id}/chapters")
    suspend fun getAudiobookChapters(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @GET("api/audiobook/{id}/progress")
    suspend fun getAudiobookProgress(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @retrofit2.http.PUT("api/audiobook/{id}/progress")
    suspend fun saveAudiobookProgress(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: okhttp3.RequestBody,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @GET("api/audiobook/starred")
    suspend fun getStarredAudiobooks(
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @retrofit2.http.POST("api/audiobook/{id}/star")
    suspend fun starAudiobook(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    @retrofit2.http.HTTP(method = "DELETE", path = "api/audiobook/{id}/star", hasBody = false)
    suspend fun unstarAudiobook(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    // Audiobook search
    @GET("api/audiobook/search")
    suspend fun searchAudiobooks(
        @Query("q") query: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    // Narrator list
    @GET("api/audiobook/narrators")
    suspend fun getNarrators(
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    // Narrator detail
    @GET("api/audiobook/narrator/{name}")
    suspend fun getNarratorDetail(
        @retrofit2.http.Path("name") name: String,
        @retrofit2.http.Header("X-ND-Authorization") authHeader: String
    ): retrofit2.Response<com.google.gson.JsonElement>

    companion object {
        /** Build narrator avatar URL for loading with Coil/AsyncImage */
        fun getNarratorAvatarUrl(serverUrl: String, narratorName: String, token: String? = null): String {
            val base = serverUrl.trimEnd('/')
            val safeName = java.net.URLEncoder.encode(narratorName, "UTF-8")
            // Use public scrape image endpoint (no auth required)
            return "$base/api/scrape/image/narrator/$safeName"
        }
    }

}
