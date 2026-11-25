package com.lechenmusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.ui.theme.AppTheme
import com.lechenmusic.ui.theme.AppThemes

/**
 * 主题配色选择弹窗（手机 / 平板共用）。
 * 每项展示：主题名 + 来源项目 + 深色/浅色两套色块预览。
 */
@Composable
fun ThemePickerDialog(
    currentThemeId: String,
    darkTheme: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("主题配色", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "参考 GitHub 开源配色项目，文字与背景对比度已逐项校验",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(AppThemes.all, key = { it.id }) { theme ->
                    ThemeRow(
                        theme = theme,
                        selected = theme.id == currentThemeId,
                        darkTheme = darkTheme,
                        onClick = { onSelect(theme.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun ThemeRow(
    theme: AppTheme,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // 深色 / 浅色两套背景与强调色，直观看出观感
            ThemePreview(colors = AppThemes.previewColors(theme, true))
            ThemePreview(colors = AppThemes.previewColors(theme, false))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(theme.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                theme.source,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                if (theme.id == AppThemes.DEFAULT_ID) "默认主题" else if (darkTheme) "深色变体" else "浅色变体",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "当前主题",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ThemePreview(colors: List<androidx.compose.ui.graphics.Color>) {
    // colors: primary, secondary, background, surface
    Row(
        modifier = Modifier
            .size(width = 34.dp, height = 34.dp)
            .clip(CircleShape)
            .background(colors[2]),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(colors[1])
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(colors[0])
        )
    }
}