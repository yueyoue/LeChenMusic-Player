package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.model.Audiobook
import com.lechenmusic.data.model.AudiobookWithProgress
import com.lechenmusic.data.model.SlideConfig
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.screens.audiobook.getAudiobookCoverUrl
import com.lechenmusic.ui.responsive.ResponsiveConfig
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabletAudiobookHomeContent(
    audiobooks: List<Audiobook>,
    audiobookWithProgress: List<AudiobookWithProgress>,
    starredAudiobooks: List<Audiobook>,
    viewModel: MainViewModel,
    serverUrl: String,
    username: String,
    password: String,
    slides: List<SlideConfig> = emptyList(),
    responsiveConfig: ResponsiveConfig? = null,
    onNavigateToAudiobook: (String?) -> Unit,
    onNavigateToAudiobookDetail: (String) -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToNarratorList: () -> Unit,
    onNavigateToRecentAudiobookListened: () -> Unit = {}
) {
    val cardSize = responsiveConfig?.albumCardSize ?: 150.dp
    val gap = responsiveConfig?.itemSpacing ?: 16.dp
    val pad = responsiveConfig?.contentPadding ?: 24.dp
    val sectionSpacing = responsiveConfig?.sectionSpacing ?: 20.dp
    val narrators by viewModel.narrators.collectAsState()

    // ===== 平板布局：左侧主内容 + 右侧边栏 =====
    Row(modifier = Modifier.fillMaxSize().padding(start = pad, end = pad, top = pad)) {

        // ── 左侧主内容 ──
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(end = gap, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            // ── 幻灯片（有声书内容） ──
            item {
                AudiobookHero(
                    slides = slides,
                    audiobooks = audiobooks,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    heroHeight = responsiveConfig?.heroHeight ?: 200.dp,
                    onAudiobookClick = onNavigateToAudiobookDetail
                )
            }

            // ── 演播者 ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("演播者", fontSize = responsiveConfig?.sectionTitleSize ?: 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "全部 ›",
                        fontSize = responsiveConfig?.captionFontSize ?: 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToNarratorList() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (narrators.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(narrators.take(8)) { narr ->
                            NarratorAvatarCard(
                                name = narr.name,
                                count = "${narr.count}部",
                                serverUrl = serverUrl,
                                onClick = { onNavigateToNarrator(narr.name) }
                            )
                        }
                    }
                }
            }

            // ── 分类导航按钮 (有声小说、相声、评书、儿童) ──
            item {
                val genreTabs = listOf(
                    "有声小说" to "有声读物",
                    "相声" to "相声",
                    "评书" to "评书",
                    "儿童" to "儿童"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    genreTabs.forEach { (label, genre) ->
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onNavigateToAudiobook(genre) }
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // ── 有声小说 ──
            item {
                SectionHead(title = "有声小说", action = "更多 ›", titleSize = responsiveConfig?.sectionTitleSize ?: 15.sp, captionSize = responsiveConfig?.captionFontSize ?: 12.sp) { onNavigateToAudiobook("有声读物") }
            }
            item {
                val novelBooks = audiobooks.filter { it.genre == "有声读物" || it.genre.isEmpty() }
                if (novelBooks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(novelBooks.take(8)) { book ->
                            AudiobookCompactCard(
                                cardSize = cardSize,
                                book = book,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = { onNavigateToAudiobookDetail(book.id) }
                            )
                        }
                    }
                } else {
                    EmptyGenrePlaceholder("暂无有声小说")
                }
            }

            // ── 相声 ──
            item {
                SectionHead(title = "相声", action = "更多 ›", titleSize = responsiveConfig?.sectionTitleSize ?: 15.sp, captionSize = responsiveConfig?.captionFontSize ?: 12.sp) { onNavigateToAudiobook("相声") }
            }
            item {
                val xiangshengBooks = audiobooks.filter { it.genre == "相声" }
                if (xiangshengBooks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(xiangshengBooks.take(8)) { book ->
                            AudiobookCompactCard(
                                cardSize = cardSize,
                                book = book,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = { onNavigateToAudiobookDetail(book.id) }
                            )
                        }
                    }
                } else {
                    EmptyGenrePlaceholder("暂无相声")
                }
            }

            // ── 评书 ──
            item {
                SectionHead(title = "评书", action = "更多 ›", titleSize = responsiveConfig?.sectionTitleSize ?: 15.sp, captionSize = responsiveConfig?.captionFontSize ?: 12.sp) { onNavigateToAudiobook("评书") }
            }
            item {
                val pingshuBooks = audiobooks.filter { it.genre == "评书" }
                if (pingshuBooks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(pingshuBooks.take(8)) { book ->
                            AudiobookCompactCard(
                                cardSize = cardSize,
                                book = book,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = { onNavigateToAudiobookDetail(book.id) }
                            )
                        }
                    }
                } else {
                    EmptyGenrePlaceholder("暂无评书")
                }
            }

            // ── 儿童 ──
            item {
                SectionHead(title = "儿童", action = "更多 ›", titleSize = responsiveConfig?.sectionTitleSize ?: 15.sp, captionSize = responsiveConfig?.captionFontSize ?: 12.sp) { onNavigateToAudiobook("儿童") }
            }
            item {
                val childBooks = audiobooks.filter { it.genre == "儿童" }
                if (childBooks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(childBooks.take(8)) { book ->
                            AudiobookCompactCard(
                                cardSize = cardSize,
                                book = book,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = { onNavigateToAudiobookDetail(book.id) }
                            )
                        }
                    }
                } else {
                    EmptyGenrePlaceholder("暂无儿童读物")
                }
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }

        // ── 右侧边栏 ──
        LazyColumn(
            modifier = Modifier.width(280.dp).fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            // ── 继续收听（手机UI样式） ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("继续收听", fontSize = responsiveConfig?.sectionTitleSize ?: 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "最近播放 ›",
                        fontSize = responsiveConfig?.captionFontSize ?: 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToRecentAudiobookListened() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val booksWithProgress = audiobookWithProgress.filter { it.progress != null && !it.progress.completed }
            if (booksWithProgress.isNotEmpty()) {
                items(booksWithProgress.take(8)) { bwp ->
                    ContinueListeningTabletRow(
                        book = bwp.toAudiobook(),
                        progress = bwp.progress,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        onClick = { onNavigateToAudiobookDetail(bwp.id) }
                    )
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "还没有收听记录",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "播放有声书后将显示在此处",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ── 热门榜单 ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("热门榜单", fontSize = responsiveConfig?.sectionTitleSize ?: 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
            }
            val popular = audiobooks.sortedByDescending { it.chapterCount }.take(5)
            items(popular.size) { index ->
                PopularBookRow(
                    rank = index + 1,
                    book = popular[index],
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onNavigateToAudiobookDetail(popular[index].id) }
                )
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}


// ==================== 有声书 Hero 幻灯 ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudiobookHero(
    slides: List<SlideConfig>,
    audiobooks: List<Audiobook>,
    serverUrl: String,
    username: String,
    password: String,
    heroHeight: androidx.compose.ui.unit.Dp,
    onAudiobookClick: (String) -> Unit
) {
    // 优先使用服务端配置的幻灯，否则使用有声书列表
    val carouselItems = remember(slides, audiobooks) {
        if (slides.isNotEmpty()) {
            slides.map { slide ->
                AudiobookCarouselItem(
                    title = slide.title.ifEmpty { "有声书推荐" },
                    imageUrl = slide.imageUrl,
                    coverUrl = null,
                    link = slide.link,
                    audiobookId = null
                )
            }
        } else {
            audiobooks.take(5).map { book ->
                AudiobookCarouselItem(
                    title = book.title,
                    imageUrl = null,
                    coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id),
                    link = "",
                    audiobookId = book.id
                )
            }
        }
    }

    if (carouselItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    // 自动轮播
    LaunchedEffect(carouselItems.size) {
        while (true) {
            delay(4000)
            if (pagerState.isScrollInProgress) continue
            val nextPage = (pagerState.currentPage + 1) % carouselItems.size
            try {
                pagerState.animateScrollToPage(nextPage)
            } catch (_: Exception) {}
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(heroHeight)
        ) { page ->
            val item = carouselItems[page]
            val gradients = listOf(
                listOf(Color(0xFFE94560), Color(0xFFFF6B81), Color(0xFFFF8787)),
                listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA), Color(0xFFD4BBFF)),
                listOf(Color(0xFF00B894), Color(0xFF55EFC4), Color(0xFF81ECEC)),
                listOf(Color(0xFFF39C12), Color(0xFFFDCB6E), Color(0xFFFFF3CD)),
                listOf(Color(0xFF3498DB), Color(0xFF74B9FF), Color(0xFFA8D8EA))
            )
            val gradient = gradients[page % gradients.size]

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (item.audiobookId != null) Modifier.clickable { onAudiobookClick(item.audiobookId) }
                        else if (item.link.isNotEmpty() && item.link.startsWith("audiobook:")) {
                            Modifier.clickable { onAudiobookClick(item.link.removePrefix("audiobook:")) }
                        } else Modifier
                    ),
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradient))
                ) {
                    // 背景图
                    if (item.coverUrl != null && item.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    } else if (item.imageUrl != null && item.imageUrl.isNotEmpty()) {
                        val fullUrl = if (item.imageUrl.startsWith("http")) item.imageUrl
                        else "${serverUrl.trimEnd('/')}${item.imageUrl}"
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    }

                    // 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    )

                    // 内容
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "📖 有声书推荐",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            item.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("精选有声书内容，沉浸式听书体验", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // 页面指示器
        if (carouselItems.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(carouselItems.size) { index ->
                    val color = if (pagerState.currentPage == index)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (pagerState.currentPage == index) 16.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

private data class AudiobookCarouselItem(
    val title: String,
    val imageUrl: String?,
    val coverUrl: String?,
    val link: String,
    val audiobookId: String?
)


// ==================== 区块标题 ====================

@Composable
private fun SectionHead(
    title: String,
    action: String,
    titleSize: androidx.compose.ui.unit.TextUnit,
    captionSize: androidx.compose.ui.unit.TextUnit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = titleSize, fontWeight = FontWeight.SemiBold)
        if (action.isNotEmpty()) {
            Text(
                action,
                fontSize = captionSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
        }
    }
}


// ==================== 继续收听 - 平板右侧边栏行（手机UI样式） ====================

@Composable
private fun ContinueListeningTabletRow(
    book: Audiobook,
    progress: com.lechenmusic.data.model.AudiobookProgress?,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("📖", fontSize = 24.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val chapterNum = progress?.chapterNumber ?: 1
                Text(
                    "第${chapterNum}章 / ${book.chapterCount}章",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
                // 进度条
                val progressPercent = if (book.totalDuration > 0 && progress != null) {
                    val totalListened = (progress.chapterNumber - 1).coerceAtLeast(0) * 60 + progress.position
                    (totalListened.toFloat() / book.totalDuration).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (progress != null) {
                    val posMin = progress.position / 60
                    val posSec = progress.position % 60
                    Text(
                        "第${progress.chapterNumber}章 ${posMin}:${"%02d".format(posSec)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            // 播放按钮
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "播放",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


// ==================== 空分类占位 ====================

@Composable
private fun EmptyGenrePlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


// ==================== 演播者头像卡片 ====================

@Composable
private fun NarratorAvatarCard(
    name: String,
    count: String,
    serverUrl: String,
    onClick: () -> Unit
) {
    val avatarUrl = com.lechenmusic.data.api.SubsonicApi.getNarratorAvatarUrl(serverUrl, name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_menu_gallery)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            count,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ==================== 书籍紧凑卡片 ====================

@Composable
private fun AudiobookCompactCard(
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    cardSize: androidx.compose.ui.unit.Dp = 150.dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardSize)
            .clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MenuBook,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            book.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (book.chapterCount > 0) "${book.chapterCount} 集" else "完结",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}


// ==================== 热门榜单行 ====================

@Composable
private fun PopularBookRow(
    rank: Int,
    book: Audiobook,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(rank),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // 封面
            val coverUrl = getAudiobookCoverUrl(serverUrl, username, password, book.id)
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MenuBook,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${book.narrator.ifEmpty { book.author }} · ${book.chapterCount}章",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                Icons.Default.TrendingUp,
                "趋势",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
