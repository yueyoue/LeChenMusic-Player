# LeChenMusic 网盘资源预加载方案设计

> 目标：网盘（W）资源播放/切下一曲时，用"预加载下一曲（下一章）"消除首字节等待，提升加载速度。
> 范围：APP 端（音乐 + 有声书，手机/平板所有 UI）；服务端仅做少量可选配合。
> 日期：2026-10-05

---

## 1. 现状分析（代码事实）

### 1.1 缓存现状
| | 音乐 | 有声书 |
|---|---|---|
| 播放通道 | ExoPlayer + `DefaultMediaSourceFactory(CacheDataSource)` | 同一个 ExoPlayer、同一个 CacheDataSource ✅ |
| 磁盘缓存 | ✅ `SimpleCache`（cacheDir/music_cache，4GB LRU） | ✅ **其实也有**，和音乐共用同一个 SimpleCache |
| 缓存内容 | 播放过程中边下边存；完整播完的歌记入 `fullyPlayedSongIds`（"缓存歌曲"页） | 只缓存"听到过的部分"，无完整播放记录 |
| 缓存 key | `rest/stream?u=..&p=..&id=<songId>`，稳定 ✅ | `.../chapters/{id}/stream?jwt=<token>`，**含 JWT，token 一换 key 就变，缓存全部失效** ⚠️ |
| 下一曲预载 | ❌ 无（ExoPlayer 不预缓冲播放列表的下一项） | ❌ 无 |

结论（回答你的问题）：
- **音乐**：已播过的曲目确实有缓存，再次播放命中缓存不需要预加载 ✅（用 `SimpleCache.getCachedBytes(key,0,N)` 判断，命中就跳过预载）。
- **有声书**：**不是没有缓存**——它和音乐走同一条 CacheDataSource/4GB SimpleCache，只是(a)只缓存听过的部分、(b)jwt 缓存 key 有缺陷。预加载对有声书**同样有效且收益更大**（单章大、自动下一章频繁）。

### 1.2 为什么"下一曲是网盘"会慢
网盘链路 = APP → 服务端 `/stream` → 302 直链（或中转）→ 网盘 CDN。
切歌瞬间才发起这一整条链路：DNS + TLS + 服务端查库 + 请求网盘拿直链 + CDN 首字节，叠加起来就是 1~5 秒起步的"转圈"。本地（B）资源只是本地磁盘读，没有这个问题——所以**只对 W 资源做预载**是精准的。

### 1.3 同类软件的成熟做法
1. **Media3 官方预加载框架**（Google 官方文档 "Preload manager"）：`PreloadMediaSource` + `DefaultPreloadManager`（media3 1.2.x 起的实验 API，在 `androidx.media3.exoplayer.source.preload` 包；更新版本有更简单的 `player.preloadConfiguration`）。为"播放列表/轮播里按顺序播放的下一个条目"设计：按距离当前播放位置的远近排优先级、滑动窗口控制内存。约束：必须用同一个 Builder 构建的 ExoPlayer、MediaSource 要从 preloadManager 取。
2. **CacheWriter 手动预缓存**（业界最通用的轻量做法）：对下一曲的 URL 用 `CacheWriter` 把开头 N 字节写进 SimpleCache。播放开始时直接命中缓存，等价于"提前缓冲"。AntennaPod（播客 App）的"预加载下一集"、大量音乐 App 的"智能预加载"（进度到 80% 开始预载下一首）都是这个思路。
3. **双播放器无缝切换**（部分音乐 App）：进度 ≥80% 时把下一首 prepare 到第二个 ExoPlayer，切歌淡入。效果最好但内存/复杂度高，对有声书章节切换意义不大，**不推荐**。

**选型结论：先做 CacheWriter 方案（方案A，小改动大收益），保留升级 Media3 PreloadManager（方案B）的路径。**

---

## 2. 方案 A（推荐）：下一曲/下一章 预缓存 + 连接预热

### 2.1 核心逻辑
新增 `NextItemPreloader`（APP 内组件，约 250 行）：

```
触发点：
  1) 当前项 STATE_READY 后延迟 3s          —— 趁早开始，给预载最多时间
  2) 播放进度 ≥50%                          —— 兜底补一次
  3) onMediaItemTransition（自动切/手动切）  —— 立刻为目标项的"下一个"排队

决策（每条队列任务先过闸）：
  下一曲/下一章存在？
    ├─ 音乐：playlist[currentIndex+1]；有声书：chapters[currentIndex+1]
    ├─ 电台(live) → 跳过
    ├─ 是本地(B)资源 → 跳过（本地读很快，没必要）
    ├─ 缓存已命中（getCachedBytes ≥ 阈值 或 fullyPlayedSongIds 包含）→ 跳过
    └─ 通过 → 入队执行预缓存

执行（单工作线程，串行，低优先级）：
  CacheWriter(
      cacheDataSourceFactory.createDataSource(),
      DataSpec(uri, offset=0, length=PRELOAD_BYTES),
      /* 上次已缓存 */ ..., progressListener
  ).cache()
  → 失败只打日志（播放时 ExoPlayer 自己还会请求，不阻塞任何流程）
```

### 2.2 关键参数
| 参数 | 建议值 | 说明 |
|---|---|---|
| 预载大小 PRELOAD_BYTES | 音乐 1.5MB / 有声书 3MB | 音乐≈前 60s，有声书≈前 2 分钟，够消除切章/切歌的等待 |
| 移动网络 | 默认只预载 512KB（可设置里关掉） | 控流量 |
| 并发 | 1 个预载任务（串行队列，新任务顶掉旧任务） | 避免抢播放带宽 |
| 暂停条件 | 播放器进入 BUFFERING 时暂停预载，READY 后恢复 | 保证当前播放优先 |
| 命中判定阈值 | 已缓存 ≥ 256KB 即视为不需要预载 | |

### 2.3 必须配套修复：缓存 key 规范化 ⚠️
现在的缓存 key 是完整 URL：有声书 URL 带 `?jwt=<token>`，**token 刷新后 key 变化 → 旧缓存全部白存**，预加载也会被这个问题毁掉。
修复：给 `CacheDataSource.Factory` 配自定义 `CacheKeyFactory`：
- 有声书：key = `ab:<bookId>:<chapterId>`
- 音乐：key = `song:<songId>`（从 URL 的 `id` 参数取）
- 其它（电台等）：保持默认（不缓存也行）

这样预载写入和播放读取共享同一份缓存，且与登录 token 无关。
（副作用：旧缓存条目会变成孤儿被 LRU 逐步淘汰，可接受；也可发版时清一次 music_cache 目录。）

### 2.4 有声书额外考虑
- 自动下一章（`onPlaybackCompleted` → `audiobookNextChapter`）是最高频的"切下一曲"场景，预载收益最直接：**听当前章时下一章开头已进缓存，切章秒开**。
- 章节列表整个在内存（`_currentAudiobookChapters`），拿"下一章"零成本。
- 有声书的"已听部分"缓存复用同一机制即可，不需要完整下载整章（整章下载=流量黑洞，交给"离线下载"功能去做，别混在一起）。

### 2.5 设置项（设置页）
- `预加载下一首/章`：开 / 关（默认开）
- `预加载网络`：仅 WiFi（默认） / WiFi + 移动网络
- 老人机/流量敏感用户可整体关闭。

---

## 3. 方案 B（后续增强）：Media3 官方 PreloadManager

升级 media3（1.2.1 → 新版，或直接用 1.2.x 实验 API `DefaultPreloadManager`）：
- 用 `DefaultPreloadManager.Builder` 构建 ExoPlayer，播放列表每个 MediaItem `add()` 进 preloadManager，`setCurrentPlayingIndex()` 告知当前位置，框架按邻近度自动预缓冲下一项，还带 `PreloadManagerListener` 回调（可观测预载成功率）。
- 适合音乐播放列表"用户随意上下滑切歌"的场景（能预载当前项前后各 1-2 项）。
- 代价：媒体源必须从 preloadManager 获取、和现有 `playUrl` 单曲路径（有声书/电台）融合要做适配；media3 升级回归成本。

**建议**：方案 A 先上线观察（改动集中在一个新类 + cache key + 触发点），验证收益后如果想覆盖"用户随机切歌"再上方案 B。

---

## 4. 服务端可选配合（不阻塞，低优先级）

1. 直链预取接口（可选）：APP 预载时其实就是提前发了一次 `/stream` 请求，服务端会提前向网盘申请直链——**自然完成预热，无需改动**。
2. 观察指标：网盘直链申请的耗时（openlist gateway）是网盘链路的最大变量，服务端可在 `/stream` 打个耗时日志；若直链申请慢，考虑服务端缓存直链（TTL 内复用）。
3. 中转(relay)模式下预载同样生效（预热的是服务端→网盘的连接 + 本地缓存）。

---

## 5. 预期收益与风险

**收益**
- 切歌/切章：从"DNS+TLS+直链申请+首字节"串行等待（1~5s）变成"本地缓存直读"（≈0s 起播，边播边续传）。
- 网盘链路抖动被"预载时间窗"吸收：听当前曲/章的几分钟里下一首的开头已在本地。
- 已缓存曲目零成本跳过，不浪费流量。

**风险与对策**
| 风险 | 对策 |
|---|---|
| 流量消耗 | 大小上限 + 仅 WiFi 默认 + 可关闭 |
| 与当前播放抢带宽 | 串行单任务 + BUFFERING 时暂停 + 低线程优先级 |
| 预载内容被 LRU 淘汰 | 4GB 池、只预载"下一个"，被淘汰概率低；被淘汰也只是回到现状 |
| 网盘直链过期/失效 | 预载失败静默放弃；播放时重新走完整链路（和今天一样，不会更差） |
| 缓存 key 迁移 | 发版说明里注明旧缓存自动失效重建 |

---

## 6. 实施拆解（方案 A）

| 步骤 | 内容 | 改动面 |
|---|---|---|
| 1 | `CacheKeyFactory` key 规范化（song:/ab: 前缀） | MusicPlayerManager.initCache 附近 |
| 2 | `NextItemPreloader` 类（队列、CacheWriter、参数闸门） | 新文件 ~250 行 |
| 3 | 触发点接入：STATE_READY/进度 50%/切项回调 | MusicPlayerManager + MainViewModel 各 3~5 行 |
| 4 | 音乐/有声书"下一目标"解析（playlist+1 / chapters+1、W 判定、缓存命中判定） | 复用 SourceBadge 的 openlist 判定逻辑 |
| 5 | 设置项（开关 + 网络策略） | SettingsRepository + 设置 UI |
| 6 | 观测：预载命中率/耗时打日志（可上报 error-log 通道） | 可选 |

预计 1~2 天可完成方案 A 并出测试包。
