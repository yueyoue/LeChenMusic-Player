package com.lechenmusic.ui.screens.albums

import androidx.compose.runtime.Composable
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.responsive.ResponsiveConfig

/**
 * 平板专辑页：与手机页共用 [AlbumsScreen] 的同一套数据流与刷新逻辑。
 *
 * 旧实现是一份独立拷贝，四个筛选 chip 的 type/文案错位（“名称”实际调 starred，
 * 又回退成 newest），且不参与手机页的刷新修复。现在只保留入口，布局由
 * AlbumsScreen 的 isTablet 分支承担，避免两套 UI 行为再次分叉。
 */
@Composable
fun TabletAlbumsScreen(
    viewModel: MainViewModel,
    responsiveConfig: ResponsiveConfig,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    AlbumsScreen(
        viewModel = viewModel,
        responsiveConfig = responsiveConfig,
        onBack = onBack,
        onAlbumClick = onAlbumClick
    )
}