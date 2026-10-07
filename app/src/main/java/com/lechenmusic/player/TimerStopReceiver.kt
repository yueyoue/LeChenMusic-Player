package com.lechenmusic.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lechenmusic.LeChenApp

/**
 * 定时停止（睡眠定时）的静态接收器：Manifest 声明，闹钟到点时即使
 * APP 在后台、切到微信/抖音播放音视频，甚至进程被系统杀掉后被闹钟唤醒，
 * 都能收到 STOP_PLAYBACK 并执行停止。
 *
 * 为什么必须静态注册：MusicPlayerManager 里还有一套动态注册的接收器处理
 * 通知栏按钮，但动态注册随进程消亡——进程被杀后 AlarmManager 的广播就没人接，
 * 定时"到点不停"。两者并存，静态这条保证送达（handleTimerExpired 幂等，重复送达无副作用）。
 */
class TimerStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != MusicPlayerManager.ACTION_STOP_PLAYBACK) return
        try {
            val app = context?.applicationContext as? LeChenApp ?: return
            app.playerManager.handleTimerExpired()
        } catch (_: Exception) {
            // 定时停止永不因异常闪退
        }
    }
}
