package com.lechenmusic

import android.app.Application
import com.lechenmusic.data.repository.LyricsCache
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.player.MusicPlayerManager

class LeChenApp : Application() {
    lateinit var repository: MusicRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var playerManager: MusicPlayerManager
    lateinit var lyricsCache: LyricsCache

    override fun onCreate() {
        super.onCreate()

        // Crash handler - log crashes to file
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashLog = java.io.File(getExternalFilesDir(null), "crash_log.txt")
                val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                crashLog.appendText("\n[$ts] CRASH on ${thread.name}: ${throwable.message}\n")
                throwable.stackTrace.take(20).forEach { crashLog.appendText("  at $it\n") }
                android.util.Log.e("LeChenMusic", "CRASH: ${throwable.message}", throwable)
            } catch (_: Exception) {}
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        instance = this
        repository = MusicRepository()
        settingsRepository = SettingsRepository(this)
        lyricsCache = LyricsCache(this)
        playerManager = MusicPlayerManager(this)
        playerManager.init(repository)
    }

    companion object {
        lateinit var instance: LeChenApp
            private set
    }
}
