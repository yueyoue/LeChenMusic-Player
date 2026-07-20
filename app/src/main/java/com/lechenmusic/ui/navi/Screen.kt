package com.lechenmusic.ui.navi

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Search : Screen("search")
    object Artists : Screen("artists")
    object Albums : Screen("albums")
    object AllSongs : Screen("all_songs")
    object RecentPlayed : Screen("recent_played")
    object Settings : Screen("settings")
    object Player : Screen("player")
    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: String) = "artist_detail/$artistId"
    }
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String) = "album_detail/$albumId"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
    object Radio : Screen("radio")
    object AllPlaylists : Screen("all_playlists")
    object CachedMusic : Screen("cached_music")

    object Audiobook : Screen("audiobook?genre={genre}") {
        fun createRoute(genre: String? = null): String {
            return if (genre != null) "audiobook?genre=$genre" else "audiobook"
        }
    }
    object AudiobookDetail : Screen("audiobook_detail/{audiobookId}") {
        fun createRoute(audiobookId: String) = "audiobook_detail/$audiobookId"
    }
    object AudiobookPlayer : Screen("audiobook_player")
    object NarratorList : Screen("narrator_list")
    object NarratorDetail : Screen("narrator_detail/{narratorName}") {
        fun createRoute(narratorName: String) = "narrator_detail/$narratorName"
    }

    // 影视模块
    object Video : Screen("video")
    object VideoSearch : Screen("video_search")
    object VideoDetail : Screen("video_detail/{source}/{videoId}") {
        fun createRoute(source: String, videoId: String) = "video_detail/$source/$videoId"
    }
    object VideoPlayer : Screen("video_player/{videoTitle}/{source}/{episodeIndex}") {
        fun createRoute(videoTitle: String, source: String, episodeIndex: Int) = "video_player/$videoTitle/$source/$episodeIndex"
    }
    object VideoPlayerDirect : Screen("video_player_direct")
    object Live : Screen("live")
    object VideoCategory : Screen("video_category/{type}") {
        fun createRoute(type: String) = "video_category/$type"
    }
}
