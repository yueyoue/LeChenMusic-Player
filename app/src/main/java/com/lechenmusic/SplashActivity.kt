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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

    // Animations
    val alpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.6f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
        logoScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f))
        titleAlpha.animateTo(1f, animationSpec = tween(500, delayMillis = 300))
        subtitleAlpha.animateTo(1f, animationSpec = tween(500, delayMillis = 500))
    }

    // Floating music notes animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val noteOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -60f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "n1"
    )
    val noteAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "na1"
    )
    val noteOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -80f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "n2"
    )
    val noteAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "na2"
    )
    val noteOffset3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -50f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse), label = "n3"
    )
    val noteAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse), label = "na3"
    )

    // Pulsing ring animation
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "ring"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "ringA"
    )

    val primaryColor = Color(0xFF6C63FF)
    val accentColor = Color(0xFFFF6584)
    val bgDark = Color(0xFF0D0D1A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        // Gradient background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        // Background splash image (if configured)
        if (splashImageUrl != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = splashImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(alpha.value * 0.3f).blur(20.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    bgDark.copy(alpha = 0.7f),
                                    bgDark.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            }
        }

        // Floating music notes (decorative)
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                "♪",
                fontSize = 28.sp,
                color = primaryColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 60.dp, top = 180.dp)
                    .offset(y = noteOffset1.dp)
                    .alpha(noteAlpha1)
            )
            Text(
                "♫",
                fontSize = 22.sp,
                color = accentColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 50.dp, top = 220.dp)
                    .offset(y = noteOffset2.dp)
                    .alpha(noteAlpha2)
            )
            Text(
                "♪",
                fontSize = 18.sp,
                color = primaryColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp)
                    .offset(y = noteOffset3.dp)
                    .alpha(noteAlpha3)
            )
            Text(
                "♫",
                fontSize = 24.sp,
                color = accentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 70.dp, bottom = 200.dp)
                    .offset(y = noteOffset1.dp)
                    .alpha(noteAlpha1)
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.2f))

            // Logo with pulsing ring
            Box(contentAlignment = Alignment.Center) {
                // Pulsing ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(ringScale)
                        .alpha(ringAlpha)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.3f))
                )
                // Logo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale.value)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(primaryColor, accentColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎵", fontSize = 52.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                "悦音",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle with gradient-like effect
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            ) {
                Text(
                    "MUSIC",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 3.sp
                )
                Text(
                    " · ",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    "AUDIOBOOK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Loading indicator
            Box(
                modifier = Modifier.padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = primaryColor.copy(alpha = 0.6f),
                    strokeWidth = 2.dp
                )
            }

            // Copyright
            Text(
                "© 2024 悦音 · 音乐有声书",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}
