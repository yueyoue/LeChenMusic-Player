package com.lechenmusic.ui.screens.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lechenmusic.data.model.TrailerItem
import com.lechenmusic.data.api.VideoApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 预告片轮播组件
 *
 * 显示热门电影的预告片视频，自动播放静音，左右翻页。
 * 点击跳转到影片详情页。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrailerCarousel(
    trailers: List<TrailerItem>,
    serverUrl: String,
    modifier: Modifier = Modifier,
    onItemClick: (TrailerItem) -> Unit = {}
) {
    if (trailers.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { trailers.size })
    var currentPage by remember { mutableIntStateOf(0) }

    // 每个页面独立的 ExoPlayer（避免切换时卡顿）
    val players = remember(trailers) {
        trailers.map { trailer ->
            ExoPlayer.Builder(context).build().apply {
                volume = 0f // 静音
                repeatMode = Player.REPEAT_ONE
                // 通过 LunaTV video-proxy 构建代理 URL
                val proxyUrl = "${serverUrl.trimEnd('/')}/api/video-proxy?url=${java.net.URLEncoder.encode(trailer.trailerUrl, "UTF-8")}&douban_id=${trailer.doubanId}"
                setMediaItem(MediaItem.fromUri(proxyUrl))
                prepare()
            }
        }
    }

    // 当前页播放，其他页暂停
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
        players.forEachIndexed { index, player ->
            if (index == pagerState.currentPage) {
                player.playWhenReady = true
            } else {
                player.playWhenReady = false
                player.seekTo(0)
            }
        }
    }

    // 自动翻页（每 6 秒）
    LaunchedEffect(pagerState.currentPage) {
        if (trailers.size > 1) {
            delay(6000)
            val nextPage = (pagerState.currentPage + 1) % trailers.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    // 释放 Player
    DisposableEffect(trailers) {
        onDispose {
            players.forEach { it.release() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val trailer = trailers[page]
            val player = players[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick(trailer) }
            ) {
                // 视频层
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            // 设置 SurfaceType 为 TextureView 以支持动画
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 底部渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 0f,
                                startYFraction = 0.4f
                            )
                        )
                )

                // 底部信息
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = trailer.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trailer.rate.isNotBlank()) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                trailer.rate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (trailer.year.isNotBlank()) {
                            Text(
                                trailer.year,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // 底部指示器
        if (trailers.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(trailers.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 20.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index == currentPage) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * 简化版轮播（不播放视频，只显示海报）
 * 用于网络慢或不想消耗流量时的降级方案
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrailerCarouselPoster(
    trailers: List<TrailerItem>,
    modifier: Modifier = Modifier,
    onItemClick: (TrailerItem) -> Unit = {}
) {
    if (trailers.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { trailers.size })
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // 自动翻页
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
        if (trailers.size > 1) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % trailers.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val trailer = trailers[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick(trailer) }
            ) {
                // 海报
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(trailer.poster)
                        .crossfade(true)
                        .build(),
                    contentDescription = trailer.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 底部渐变
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                ),
                                startYFraction = 0.5f
                            )
                        )
                )

                // 信息
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = trailer.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trailer.rate.isNotBlank()) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(trailer.rate, fontSize = 13.sp, color = Color(0xFFFFD700))
                        }
                    }
                }
            }
        }

        // 指示器
        if (trailers.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(trailers.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 20.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index == currentPage) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}
