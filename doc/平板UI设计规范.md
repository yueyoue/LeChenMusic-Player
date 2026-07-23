# LeChenMusic 平板/车机 UI 设计规范

> 最后更新: 2026-07-23
> 本文档是平板/车机端 UI 的唯一标准。所有新页面必须严格遵循本规范。

---

## 1. 设备判定

```kotlin
// 自动判定，无需手动设置
WindowSizeClass.Compact  → 手机布局（底部导航栏）
WindowSizeClass.Medium   → 小平板（左侧导航栏）
WindowSizeClass.Expanded → 大平板/车机（左侧导航栏）
```

判定逻辑在 `ResponsiveUtils.kt` 的 `rememberResponsiveConfig()` 中，返回 `ResponsiveConfig` 对象。

---

## 2. 左侧导航栏（所有平板页面统一）

### 2.1 结构

```
┌────────┐
│  首页   │  ← 选中时高亮
│        │
│  听歌   │  ← 音乐模式
│  听书   │  ← 有声书模式
│  电影   │  ← 影视模式
│  电视   │  ← 直播
│        │
│  搜索   │
│        │
│  收藏   │
│  我的   │
└────────┘
```

### 2.2 规格

| 属性 | 值 |
|------|-----|
| 宽度 | 64dp |
| 背景色 | `surfaceContainerLowest` |
| 图标尺寸 | 22dp |
| 文字字号 | 10sp |
| 图标文字间距 | 1dp |
| 项目内边距 | `vertical = 4.dp` |
| 对齐方式 | 水平居中 + 垂直居中（`Arrangement.Center`） |
| 选中色 | `primary` |
| 未选中色 | `onSurfaceVariant` |
| 选中字重 | `SemiBold` |
| 未选中字重 | `Normal` |

### 2.3 代码位置

`MainActivity.kt` → `LeChenMusicApp()` → `if (useSideNav)` 分支

---

## 3. 字号规范

### 3.1 平板字号（Medium: 600-840dp）

| 用途 | 字号 | 字重 | 示例 |
|------|------|------|------|
| 页面标题 | 18sp | Bold | 播放器页面标题 |
| 区块标题 | 14sp | SemiBold | "最新专辑"、"每日推荐" |
| 卡片标题 | 16sp | Medium | 歌名、专辑名 |
| 卡片副标题 | 14sp | Normal | 歌手名、曲目数 |
| 正文 | 16sp | Normal | 描述文字 |
| 辅助文字 | 13sp | Normal | 时长、标签 |
| 导航标签 | 10sp | Normal/SemiBold | 底部/侧边导航 |
| 按钮文字 | 16sp | SemiBold | "立即播放" |

### 3.2 大平板字号（Expanded: >840dp）

| 用途 | 字号 | 字重 |
|------|------|------|
| 页面标题 | 22sp | Bold |
| 区块标题 | 16sp | SemiBold |
| 卡片标题 | 18sp | Medium |
| 卡片副标题 | 15sp | Normal |
| 正文 | 18sp | Normal |
| 辅助文字 | 14sp | Normal |

### 3.3 车机字号（最小值）

| 用途 | 最小字号 | 推荐字号 |
|------|---------|---------|
| 所有文字 | 18sp | 20-24sp |
| 按钮文字 | 18sp | 20sp |

### 3.4 字重使用规则

| 字重 | 用途 |
|------|------|
| `Bold (700)` | 仅用于页面标题 |
| `SemiBold (600)` | 区块标题、选中状态、按钮文字 |
| `Medium (500)` | 卡片标题、歌曲名 |
| `Normal (400)` | 副标题、正文、辅助文字 |

**禁止**：不要使用 `ExtraBold`、`Black` 等过重字重。

---

## 4. 封面/卡片尺寸

### 4.1 平板尺寸

| 元素 | Medium | Expanded | 比例 |
|------|--------|----------|------|
| 专辑封面 | 170dp | 200dp | 1:1 |
| 歌单封面 | 160dp | 180dp | 1:1 |
| 歌曲列表封面 | 52dp | 56dp | 1:1 |
| Hero 横幅高度 | 220dp | 260dp | 自适应宽度 |
| 有声书封面 | 150dp | 170dp | 1:1 |

### 4.2 圆角

| 元素 | 圆角值 |
|------|--------|
| 封面卡片 | 16dp |
| 列表封面 | 10dp |
| Hero 横幅 | 20dp |
| 按钮/标签 | 50dp（胶囊形） |
| 普通卡片 | 14dp |

---

## 5. 间距规范

### 5.1 平板间距

| 用途 | Medium | Expanded |
|------|--------|----------|
| 页面内边距 | 24dp | 32dp |
| 区块间距 | 18dp | 22dp |
| 卡片间距 | 16dp | 20dp |
| 列表项间距 | 12dp | 16dp |

### 5.2 车机间距

| 用途 | 值 |
|------|-----|
| 页面内边距 | 32dp |
| 元素间距 | 24dp |
| 最小触摸目标 | 60×60dp |
| 推荐触摸目标 | 72×72dp |

---

## 6. 网格列数

列数由 `calculateAdaptiveColumns(screenWidthDp, minCardWidthDp)` 自动计算：

```
列数 = (屏幕宽度 - 侧边栏宽度) / (最小卡片宽度 + 间距)
```

| 元素 | 最小卡片宽度 | 结果（10寸平板） |
|------|------------|----------------|
| 专辑/歌单网格 | 150dp | 4-5 列 |
| 歌曲列表 | — | 2 列并排 |
| 有声书网格 | 150dp | 4-5 列 |

---

## 7. 首页布局标准

### 7.1 音乐首页

```
┌──────────────────────────────────────────┐
│ [快捷入口: 歌手/专辑/乐库/歌单/电台/缓存]  │
├──────────────────────────────────────────┤
│ [Hero Banner 2:1]  │ [今日热榜卡片]       │
│                    │  1. 歌名 - 歌手       │
│                    │  2. 歌名 - 歌手       │
│                    │  3. 歌名 - 歌手       │
├──────────────────────────────────────────┤
│ 最新专辑                    更多 ›        │
│ [封面] [封面] [封面] [封面] [封面]         │
├──────────────────────────────────────────┤
│ 每日推荐                    换一批 ↻      │
│ [左列歌曲列表]  [右列歌曲列表]             │
├──────────────────────────────────────────┤
│ 歌单                       更多 ›        │
│ [封面] [封面] [封面] [封面] [封面]         │
├──────────────────────────────────────────┤
│ 随机专辑                    换一批 ↻      │
│ [封面] [封面] [封面] [封面] [封面]         │
├──────────────────────────────────────────┤
│ 最近播放                    更多 ›        │
│ [左列歌曲列表]  [右列歌曲列表]             │
├──────────────────────────────────────────┤
│ 电台                                      │
│ [电台卡片] [电台卡片] [电台卡片] [电台卡片]│
└──────────────────────────────────────────┘
```

### 7.2 区块标题规范

| 属性 | 值 |
|------|-----|
| 字号 | `config.sectionTitleSize` (14sp/16sp) |
| 字重 | SemiBold |
| 左侧 | 无图标，纯文字 |
| 右侧操作 | "更多 ›" 或 "换一批 ↻"，字号 `config.captionFontSize`，色 `primary` |

### 7.3 快捷入口规范

| 属性 | 值 |
|------|-----|
| 图标尺寸 | 24dp |
| 图标背景 | 48dp × 48dp，圆角 14dp，颜色 alpha=0.15 |
| 文字字号 | `config.cardSubtitleSize` |
| 文字字重 | Medium |
| 排列 | 水平均匀分布（`SpaceEvenly`） |

---

## 8. 横向滚动列表

所有横向滚动区域使用 `LazyRow`：

```kotlin
LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
    items(list) { item ->
        // 卡片组件
    }
}
```

- `gap` = `config.itemSpacing`（16dp / 20dp）
- 不需要额外的 `contentPadding`（页面内边距已包含）

---

## 9. 歌曲列表

### 9.1 单列（手机）

```kotlin
SongRow(song, serverUrl, username, password) { onClick() }
```

### 9.2 双列（平板）

```kotlin
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
    Column(modifier = Modifier.weight(1f)) {
        leftSongs.forEach { TabletSongRow(it, ...) }
    }
    Column(modifier = Modifier.weight(1f)) {
        rightSongs.forEach { TabletSongRow(it, ...) }
    }
}
```

### 9.3 TabletSongRow 规格

| 属性 | 值 |
|------|-----|
| 封面尺寸 | `config.songCoverSize` (52dp) |
| 封面圆角 | 10dp |
| 歌名字号 | `config.bodyFontSize` (16sp) |
| 歌手字号 | `config.captionFontSize` (13sp) |
| 时长字号 | `config.captionFontSize` (13sp) |
| 行内边距 | `vertical = 8.dp` |

---

## 10. 颜色规范

### 10.1 主题色

| 用途 | 浅色 | 深色 |
|------|------|------|
| Primary | `#6C5CE7` | `#7C5CFC` |
| Background | `#F5F5FA` | `#0A0A14` |
| Surface | `#FFFFFF` | `#16162A` |
| SurfaceVariant | `#F0F0F8` | `#1E1E38` |
| OnBackground | `#1A1A2E` | `#EEEEF5` |
| OnSurfaceVariant | `#666680` | `#9898B8` |

### 10.2 强调色（快捷入口）

| 颜色 | 值 | 用途 |
|------|------|------|
| AccentPurple | `#A78BFA` | 歌手 |
| AccentBlue | `#5352ED` | 专辑 |
| AccentGreen | `#34D399` | 乐库/歌单 |
| AccentOrange | `#FBBF24` | 缓存/评书 |
| AccentRed | `#FF4D6A` | 电台 |
| AccentLightBlue | `#60A5FA` | 电台/其他 |

---

## 11. 车机专项

### 11.1 强制要求

| 规范 | 值 |
|------|-----|
| 最小字号 | 18sp |
| 最小触摸目标 | 60×60dp |
| 对比度 | ≥ 4.5:1 |
| 默认主题 | 深色（不可切换） |
| 屏幕方向 | 锁定横屏 |

### 11.2 布局

- 左侧：当前播放信息 + 大封面
- 右侧：推荐内容 / 列表
- 按钮大而少，减少驾驶分心

---

## 12. 新页面开发 Checklist

开发新页面时，按以下清单检查：

- [ ] 使用 `ResponsiveConfig` 获取字号/间距/列数
- [ ] 手机/平板布局分支（`config.isCompact` 判断）
- [ ] 区块标题使用 `TabletSecHd()` 组件
- [ ] 歌曲列表平板版使用双列布局
- [ ] 封面图片使用 `ContentScale.Crop`，不拉伸
- [ ] 横向列表使用 `LazyRow` + `Arrangement.spacedBy(gap)`
- [ ] 字重遵循规范（标题 SemiBold，卡片 Medium，正文 Normal）
- [ ] 触摸目标 ≥ 48dp（平板）/ 60dp（车机）
- [ ] 深色模式下对比度 ≥ 4.5:1

---

## 13. 组件复用清单

| 组件 | 文件 | 用途 |
|------|------|------|
| `TabletSecHd` | HomeScreen.kt | 区块标题（标题 + 右侧操作） |
| `TabletAlbumCard` | HomeScreen.kt | 专辑封面卡片 |
| `TabletPlaylistCard` | HomeScreen.kt | 歌单封面卡片 |
| `TabletSongRow` | HomeScreen.kt | 歌曲列表行（双列用） |
| `TabletRadioRow` | HomeScreen.kt | 电台列表行 |
| `CoverImage` | Components.kt | 通用封面图加载 |
| `MiniPlayer` | MiniPlayer.kt | 底部迷你播放条 |
| `ResponsiveConfig` | ResponsiveUtils.kt | 响应式配置 |

---

## 14. 文件结构

```
ui/
├── responsive/
│   ├── ResponsiveUtils.kt    ← 自适应配置（字号/间距/列数）
│   └── ResponsiveScaffold.kt ← 通用响应式脚手架
├── screens/
│   ├── home/
│   │   ├── HomeScreen.kt     ← 首页（含平板布局）
│   │   └── ...
│   ├── albums/
│   ├── artists/
│   ├── audiobook/
│   ├── video/
│   └── ...
├── components/
│   ├── Components.kt         ← 通用组件
│   └── MiniPlayer.kt
└── theme/
    ├── Color.kt
    └── Theme.kt
```

**规则**：平板布局代码与手机布局代码写在同一个文件中，通过 `ResponsiveConfig` 分支，不要创建单独的平板文件。
