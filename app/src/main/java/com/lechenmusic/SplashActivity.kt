package com.lechenmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.ui.theme.LeChenMusicTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val serverUrl = prefs.getString("serverUrl", "") ?: ""
        val username = prefs.getString("username", "") ?: ""
        val password = prefs.getString("password", "") ?: ""
        val isLoggedIn = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

        setContent {
            LeChenMusicTheme(darkTheme = true) {
                SplashScreen(
                    isLoggedIn = isLoggedIn,
                    onFinished = {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var splashImageUrl by remember { mutableStateOf<String?>(null) }
    var splashDuration by remember { mutableStateOf(3) }

    // Fetch splash config from server
    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val serverUrl = prefs.getString("serverUrl", "http://j.tthsdd.top:3334") ?: "http://j.tthsdd.top:3334"

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder()
                .url("${serverUrl.trimEnd('/')}/api/app/config")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                    val data = json.getAsJsonObject("data")
                    val imgUrl = data?.get("splashImageUrl")?.asString
                    if (!imgUrl.isNullOrBlank()) {
                        splashImageUrl = if (imgUrl.startsWith("http")) imgUrl else "${serverUrl.trimEnd('/')}$imgUrl"
                    }
                    data?.get("splashDuration")?.asInt?.let { splashDuration = it }
                }
            }
        } catch (_: Exception) {}
    }

    // Auto-dismiss after duration
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(splashDuration * 1000L)
        onFinished()
    }

    // Fade-in animation
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (splashImageUrl != null) {
            // Use Fit for tablet/car to avoid cropping important content
            // Add gradient overlay at bottom for better text visibility
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = splashImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(alpha.value),
                    contentScale = ContentScale.Fit
                )
                // Bottom gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF1A1A2E))
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier.alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("悦音", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("音乐 · 有声书", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.6f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
