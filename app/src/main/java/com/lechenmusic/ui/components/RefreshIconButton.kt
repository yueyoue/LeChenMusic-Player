package com.lechenmusic.ui.components

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 刷新按钮（乐库 / 专辑等列表页共用）。
 *
 * 反馈设计：
 * - 点击立即弹一条轻提示（[toastMessage]），用户马上知道点到了；
 * - 刷新图标至少旋转 700ms（即使请求瞬间完成也能看到动画）；
 * - [loading] 为 true 期间显示转圈，表示还在拉数据。
 *
 * @param spinningMinDurationMs 点击后的最短旋转时长，保证「点击有效」的手感
 */
@Composable
fun RefreshIconButton(
    contentDescription: String,
    loading: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    toastMessage: String? = null,
    enabled: Boolean = true,
    spinningMinDurationMs: Long = 700L,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clickSpinning by remember { mutableStateOf(false) }
    val spinning = loading || clickSpinning
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(spinning) {
        if (!spinning) {
            rotation.snapTo(0f)
        } else {
            rotation.snapTo(0f)
            while (true) {
                rotation.animateTo(360f, animationSpec = tween(700, easing = LinearEasing))
                rotation.snapTo(0f)
            }
        }
    }

    IconButton(
        onClick = {
            toastMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            clickSpinning = true
            scope.launch {
                delay(spinningMinDurationMs)
                clickSpinning = false
            }
            onClick()
        },
        enabled = enabled && !loading
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                Icons.Default.Refresh,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.rotate(rotation.value)
            )
        }
    }
}