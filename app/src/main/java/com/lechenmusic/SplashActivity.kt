package com.lechenmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.ui.theme.LeChenMusicTheme
import kotlinx.coroutines.delay

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
                        startActivity(Intent(this, MainActivity::class.java))
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
        delay(splashDuration * 1000L)
        onFinished()
    }

    // Fade-in animation
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
    }

    // Loading dots animation
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = StartOffset(200)), label = "d2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = StartOffset(400)), label = "d3"
    )

    val warmColor = Color(0xFFE8833A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            )
    ) {
        // Decorative circle top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 40.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(warmColor.copy(alpha = 0.15f))
        )

        // Background splash image (if configured)
        if (splashImageUrl != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = splashImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(alpha.value),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1A1A2E))))
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo icon — 120dp rounded square
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(warmColor),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵", fontSize = 56.sp)
                // Sparkle badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                "悦音",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                "MUSIC · AUDIOBOOK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = warmColor,
                letterSpacing = 0.15.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(warmColor.copy(alpha = dotAlpha1)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(warmColor.copy(alpha = dotAlpha2)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(warmColor.copy(alpha = dotAlpha3)))
            }

            // Copyright
            Text(
                "© 2024 悦音 · 音乐有声书",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }
    }
}
