package com.lechenmusic.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 幻灯标题：显示在幻灯底部，带半透明深色底衬，
 * 保证标题在任何背景图上都清晰可读（不再依赖整张幻灯的遮罩）。
 */
@Composable
fun SlideTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp,
    maxLines: Int = 2
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}