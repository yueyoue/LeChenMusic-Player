package com.lechenmusic

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashMessage = intent.getStringExtra("crash_message") ?: "Unknown"
        val crashStack = intent.getStringExtra("crash_stack") ?: "No stack trace"
        val crashScreen = intent.getStringExtra("crash_screen") ?: "unknown"

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CrashScreen(
                    message = crashMessage,
                    stack = crashStack,
                    screen = crashScreen
                )
            }
        }
    }
}

@Composable
fun CrashScreen(message: String, stack: String, screen: String) {
    val context = LocalContext.current
    val fullText = buildString {
        appendLine("Screen: $screen")
        appendLine("Error: $message")
        appendLine()
        appendLine(stack)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(
            "💥 应用崩溃",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "屏幕: $screen",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFBBF24)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F172A), shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            Text(
                text = stack,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE2E8F0),
                lineHeight = 16.sp,
                modifier = Modifier.verticalScroll(scrollState)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("crash", fullText))
                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("复制全部", color = Color.White)
            }

            Button(
                onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                    (context as? Activity)?.finish()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("重启应用", color = Color.White)
            }
        }
    }
}
