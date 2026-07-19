package com.lechenmusic

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * #19: Global error/crash reporter - sends logs to web server
 */
object ErrorReporter {
    private const val TAG = "ErrorReporter"
    private var serverUrl = ""
    private var username = ""
    private var token = ""
    private var appVersion = ""

    fun init(context: Context, server: String, user: String, authToken: String) {
        serverUrl = server.trimEnd('/')
        username = user
        token = authToken
        appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) { "unknown" }

        // Set global uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                sendErrorSync(
                    level = "crash",
                    message = throwable.message ?: "Unknown crash",
                    stack = getStackTrace(throwable),
                    screen = "uncaught_${thread.name}"
                )
            } catch (_: Exception) {}
            // Call original handler (shows system crash dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Set global coroutine exception handler
        val currentDefault = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            Thread.UncaughtExceptionHandler { thread, throwable ->
                try {
                    sendErrorSync(
                        level = "crash",
                        message = throwable.message ?: "Unknown crash",
                        stack = getStackTrace(throwable),
                        screen = "coroutine_${thread.name}"
                    )
                } catch (_: Exception) {}
                // Let default handler take over
                currentDefault?.uncaughtException(thread, throwable)
            }
        )

        Log.i(TAG, "ErrorReporter initialized for $server")
    }

    /**
     * Send an error log to the server (async)
     */
    fun reportError(
        level: String = "error",
        message: String,
        throwable: Throwable? = null,
        screen: String = ""
    ) {
        val stack = throwable?.let { getStackTrace(it) } ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendErrorAsync(level, message, stack, screen)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to report error: ${e.message}")
            }
        }
    }

    /**
     * Report a non-fatal error with context
     */
    fun reportNonFatal(
        message: String,
        throwable: Throwable,
        screen: String = ""
    ) {
        reportError(level = "error", message = message, throwable = throwable, screen = screen)
    }

    private fun sendErrorSync(level: String, message: String, stack: String, screen: String) {
        try {
            val url = URL("$serverUrl/api/error-log/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-ND-Authorization", "Bearer $token")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val body = Gson().toJson(mapOf(
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "os" to "Android ${Build.VERSION.RELEASE}",
                "appVersion" to appVersion,
                "level" to level,
                "message" to message,
                "stack" to stack,
                "screen" to screen,
                "userId" to username
            ))

            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            Log.d(TAG, "Error reported: $code - $message")
        } catch (e: Exception) {
            Log.w(TAG, "sendErrorSync failed: ${e.message}")
        }
    }

    private suspend fun sendErrorAsync(level: String, message: String, stack: String, screen: String) {
        sendErrorSync(level, message, stack, screen)
    }

    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString().take(2000) // Limit stack trace size
    }
}
