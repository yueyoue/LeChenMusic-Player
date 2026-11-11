# LeChenMusic 预加载方案 A · 开发步骤

> 对应设计文档：[`LeChenMusic-预加载方案设计.md`](./LeChenMusic-预加载方案设计.md)（方案 A：下一曲/下一章预缓存 + 连接预热）
> 结论：**核心步骤 1~4 一次做完（闭环不可拆），按「两次提交、一次推送出包」落地**；设置项随核心提交，观测后补。
> 日期：2026-11-11

---

## 0. 总体结论与落地节奏

**为什么核心不能拆版本分步上线：**

- 设计文档 §6 的步骤 2/3/4 是一个闭环：`NextItemPreloader` 没有触发点是死代码，触发点没有预加载器是空转，「下一目标解析」又是两边共用的逻辑。拆开任何一步都无法独立验证收益。
- 步骤 1（缓存 key 规范化）与预加载同生共死：预载写入的 key 必须和播放读取的 key 一致，否则预载内容播放时命中不了。

**落地节奏（一次开发、两次提交、一次推送）：**

| 提交 | 内容 | 说明 |
|---|---|---|
| 提交 1 | 缓存 key 规范化（`song:` / `ab:`）+ key 解析双格式兼容 | 独立修掉「有声书换 token 旧缓存全废」的现有 bug；可独立回滚 |
| 提交 2 | `NextItemPreloader` + 3 个触发点 + 下一目标解析 + 设置开关 | 核心功能完整闭环 |
| 可选后补 | 预载命中率/耗时观测日志 | 不阻塞，走 error-log 通道即可 |

> **⚠️ 状态更正（2026-11-11）**：上述表格为计划；实际落地状态见 [§7 落地状态](#7-落地状态)，其中「可选后补的观测日志」未做。

设置项（设计文档 §2.5）跟提交 2 一起，十几行，不值得单独一步。

---

## 1. 开发前必知：代码事实补充（含设计文档没写的坑）

以下位置已逐行核实（以当前 `main` 为基准）：

### 1.1 ⚠️ 坑：`getCachedSongIds()` 用正则解析缓存 key

`MusicPlayerManager.kt` ≈ L364-388（正则在 L369）：

```kotlin
for (key in cache.keys) {
    val match = Regex("[?&]id=([^&]+)").find(key)   // ← 从完整 URL key 里抠 songId
    ...
}
```

这是「缓存音乐」页的唯一数据源。key 换成 `song:<id>` 后**该正则直接失配，缓存页会变空**。
→ 步骤 1 必须同步改这里，做**双格式兼容**：旧 key 认 `?id=`/`&id=`，新 key 认 `song:`/`ab:` 前缀。
→ 全仓 grep 确认 key 解析点仅此一处；`clearMusicCache()`（≈ L390）遍历删除不挑格式，安全。

### 1.2 缓存工厂有两处，都要挂 CacheKeyFactory

`MusicPlayerManager.initCache` 有两处 `CacheDataSource.Factory()` 构造（≈ L334、L352，对应普通初始化和改缓存容量后重建），新的 `CacheKeyFactory` **两处都要 `setCacheKeyFactory(...)`**，否则其中一条路径仍会生成旧格式 key。

### 1.3 有声书流 URL 的格式与 JWT 位置

`MusicRepository.kt` ≈ L870-873：

```kotlin
"$normalizedUrl/api/audiobook/$bookId/chapters/$chapterId/stream?jwt=$token"   // 有 token
"$normalizedUrl/api/audiobook/$bookId/chapters/$chapterId/stream"              // 无 token
```

- bookId / chapterId 在**路径**里（稳定），jwt 在 query（会变）→ key 从路径提取即可彻底摆脱 token。
- 音乐 URL 为 `rest/stream?u=..&p=..&id=<songId>`（设计文档 §1.1）。

### 1.4 有声书播放入口已带稳定 mediaId

`MainViewModel.kt` ≈ L2119 / L2254 / L2269 调用 `playerManager.playUrl(url, ..., mediaId = "audiobook_${book.id}_${chapter.id}", ...)`。
但注意：**CacheWriter 预载走的是我们自己构造的 DataSpec，拿不到 mediaId**；播放链路的 DataSpec 也未必带 mediaId。
→ key 提取以 **URI 解析为准**（两条链路都拿得到 URL），`DataSpec.key` 有值时优先用它作为兜底增强。

### 1.5 W（网盘）/ B（本地）判定字段

`ui/components/Components.kt` `SourceBadge`（≈ L238）：`val isCloud = path.startsWith("openlist:")`。
下一目标解析时复用同一判定（Song 的 `path` 字段），别另写一套逻辑。

---

## 2. 提交 1：缓存 key 规范化

### 2.1 目标

- 预载与播放共享同一份缓存、且**与登录 token 无关**。
- 顺手修掉现有 bug：有声书 jwt 刷新后旧缓存 key 失配、缓存全部白存。

### 2.2 key 格式规范

| 资源 | 新 key | 提取来源 |
|---|---|---|
| 音乐 | `song:<songId>` | URI query `id` 参数 |
| 有声书章节 | `ab:<bookId>:<chapterId>` | URI 路径 `/api/audiobook/{bookId}/chapters/{chapterId}/stream` |
| 其它（电台等） | 默认（`dataSpec.key ?: uri.toString()`） | 不缓存或维持现状 |

### 2.3 改动点

1. 新增 `StableCacheKeyFactory : CacheKeyFactory`（放在 `player/` 包，约 40 行）：
   - `buildCacheKey(DataSpec)`：先看 `dataSpec.key`，再按 URI 特征匹配上面两张表，都不匹配回退默认行为。
2. `MusicPlayerManager.initCache` 两处 `CacheDataSource.Factory()`（≈ L334/L352）都 `setCacheKeyFactory(StableCacheKeyFactory)`。
3. `getCachedSongIds()` 双格式兼容（≈ L369）：
   ```kotlin
   val songId = when {
       key.startsWith("song:") -> key.removePrefix("song:")
       else -> Regex("[?&]id=([^&]+)").find(key)?.groupValues?.get(1)
   }
   ```
   `ab:` 开头的 key 天然不会被当成歌曲（原正则也匹配不到），无需特殊处理。

### 2.4 自测清单（提交 1）

- [ ] 播放一首网盘音乐 → 查 `music_cache` 出现 `song:<id>` 的新 key，播放正常
- [ ] 播放一章有声书 → key 为 `ab:<book>:<chapter>`，**退出登录重登（token 变化）后章节仍命中缓存**
- [ ] 「缓存音乐」页正常显示已缓存歌曲（新旧 key 混合时也要能列出来）
- [ ] 设置里「清空缓存」正常
- [ ] 电台/其它流媒体播放无回归

---

## 3. 提交 2：NextItemPreloader + 触发点 + 设置

### 3.1 `NextItemPreloader`（新文件 `player/NextItemPreloader.kt`，约 250 行）

严格按设计文档 §2.1/§2.2 实现：

- **触发点接入（3 处）**：
  1. 当前项 `STATE_READY` 后延迟 3s —— `MusicPlayerManager` 的 Player.Listener；
  2. 播放进度 ≥50% 兜底补一次 —— 复用现有 200ms 进度轮询（`MainViewModel` init 里的 `updateProgress` 循环）加个阈值判断，**别新开轮询**；
  3. `onMediaItemTransition` / 有声书 `audiobookNextChapter()` 入口 —— 立刻为新的「下一项」排队。
- **决策闸门**（顺序执行）：下一项存在 → 跳过电台(live) → 跳过 B 本地资源（`path` 不以 `openlist:` 开头）→ 跳过已缓存（`getCachedBytes ≥ 256KB` 或 `fullyPlayedSongIds` 命中）→ 入队。
- **执行**：单工作线程串行队列，`CacheWriter(cacheDataSourceFactory.createDataSource(), DataSpec(uri, 0, PRELOAD_BYTES), null, progressListener).cache()`；新任务顶掉旧任务；播放器 `BUFFERING` 时暂停、`READY` 恢复；失败只打日志不阻塞播放。
- **参数**：音乐 1.5MB / 有声书 3MB；移动网络默认 512KB 且受设置项控制；命中阈值 256KB。

### 3.2 下一目标解析（放 Preloader 内部或独立小函数）

- 音乐：`playerManager` 当前播放列表 `currentIndex + 1`；
- 有声书：`MainViewModel._currentAudiobookChapters` 当前章节 index + 1（列表在内存，零成本）；
- **注意跨类边界**：有声书触发点在 `MainViewModel`，音乐触发点在 `MusicPlayerManager`——通过回调/注入把「下一项的 URL + 类型」交给 Preloader，**不要让 Preloader 反向依赖 ViewModel**，也别改动现有播放/切歌链路的一行行为。

### 3.3 设置项（`SettingsRepository` + 设置页 UI）

- `预加载下一首/章`：开（默认） / 关
- `预加载网络`：仅 WiFi（默认） / WiFi + 移动网络
- 开关关闭时 Preloader 不入队（保留对象，打开即时生效）。

### 3.4 自测清单（提交 2）

- [ ] 网盘音乐连播：切下一首起播明显变快（对照关闭预载开关）
- [ ] 网盘有声书自动下一章：切章秒开
- [ ] 预载不抢带宽：弱网下当前播放不卡顿（BUFFERING 暂停生效）
- [ ] 开关关掉后无任何预载请求（抓包确认）
- [ ] 仅 WiFi 默认下，移动网络不预载；切换策略后生效
- [ ] B 本地资源与电台不触发预载（日志确认）
- [ ] 全量回归：音乐/有声书播放、暂停、拖动、上一首/下一首、锁屏控制、缓存音乐页

---

## 4. 风险与回滚

| 风险 | 对策 |
|---|---|
| key 迁移导致旧缓存成为孤儿 | 双格式兼容读取（§2.3），旧条目由 4GB LRU 自然淘汰，不清库也可平滑过渡 |
| 播放主链路回归 | 两次提交独立可回滚；提交 2 不改动现有播放代码，只加触发与旁路 |
| 流量消耗 | 大小上限 + 仅 WiFi 默认 + 可整体关闭（设计文档 §5） |
| 网盘直链过期导致预载失败 | 静默放弃，播放时走完整链路（与现状一致，不会更差） |

---

## 5. 工作量预估

| 项 | 预估 |
|---|---|
| 提交 1（key 规范化 + 兼容） | 0.5 天（含自测） |
| 提交 2（Preloader + 触发 + 设置） | 1 天（含自测） |
| 观测日志（可选） | 0.5 天 |

合计 1~2 天出测试包，与设计文档 §6 预估一致。

---

## 6. 开发评审补充（2026-11-11，落地时逐行核对后修正/澄清）

以下为实现时发现的小问题与处理，不改变总体方案：

1. **移动网络默认值两处表述矛盾**（设计文档 §2.2 vs §2.5/§3.3）：以设置项为准——**默认“仅 WiFi”**；用户开启“WiFi + 移动网络”后，移动网络预载上限 **512KB**。
2. **触发点 3 的有声书入口覆盖不全**：只 hook `audiobookNextChapter()` 会漏掉「手动选章」「续听/首次播放」入口（后两者直接调 `playUrl`）。实际落地：在 `playAudiobookChapter()` 与 `resumeAudiobook()` 的两个 `playUrl` 调用点后统一触发 `preloadNextAudiobookChapter()`，全覆盖。
3. **专辑边界自动续播不在预载范围**：`MainViewModel.playNextAlbum()`（专辑末曲播完自动播下专辑首曲）不是 `playlist[currentIndex+1]`，**明确列为范围外**（否则需预取专辑歌曲列表，改动面超出“不影响其他功能”约束）。
4. **CacheKeyFactory 音乐匹配加路径限定**：key 从 URL 的 `id` 参数取时，要求路径含 `rest/stream`，避免电台等其它流 URL 恰好带 `id=` 参数时被误打成 `song:` key 造成撞 key。
5. **有声书 W/B 判定字段**：`AudiobookChapter` 也有 `path` 字段（开发文档未提），与 Song 同口径判 `openlist:` 前缀即可。
6. **`fullyPlayedSongIds` 跳过预载的已知边界**：「清除缓存」只删缓存资源、不清该名单，清缓存后名单里的歌会被跳过预载（只是失去预载收益，不影响正确性）。保留文档行为。
7. **设置页 UI 实际是三处布局**：`SettingsScreen` 手机分支 + 平板分支、`TabletSettingsScreen`，三处都要加设置项。
8. **CacheWriter 第三个参数是 temporaryBuffer**（设计文档 §2.1 伪代码注释“上次已缓存”是笔误，传 null 即可）；`CacheWriter.cache()` 天然跳过已缓存字节、支持断点续跑，“BUFFERING 暂停”用进度回调协作式阻塞实现。

---

## 7. 落地状态

> 更新于 2026-11-11，代码已推送 `main`，共 3 个提交（含文档提交则为 4 个）。

### 7.1 已完成 ✅

| 项 | 提交 | 说明 |
|---|---|---|
| 提交 1：缓存 key 规范化 | `8e7f047` | `StableCacheKeyFactory`（`song:<songId>` / `ab:<bookId>:<chapterId>`）+ `initCache`/`updateCacheSize` 两处挂载 + `getCachedSongIds()` 双格式兼容 |
| 提交 2：预加载核心闭环 | `981e543` | `NextItemPreloader`（串行队列 + CacheWriter + BUFFERING 暂停 + 新任务顶旧任务 + 失败静默）+ 3 个触发点 + 决策闸门 + 下一目标解析 |
| 设置项（§2.5） | `981e543` | 「预加载下一首/章」开关（默认开）+「预加载网络」（默认仅 WiFi）；手机/平板三处布局（`SettingsScreen` 手机分支 + 平板分支 + `TabletSettingsScreen`）全部接入，即时生效 |
| 文档勘误 + 本落地状态 | `e8d8753` 及本提交 | §6 评审补充 8 条 |

实现与文档的差异均已收敛到 §6 的 8 条（含移动网络默认值、有声书触发点入口、CacheKeyFactory 路径限定等）。

### 7.2 未做 ❌

| 项 | 原因 |
|---|---|
| 预载命中率/耗时观测日志（§6 步骤 6 / §0“可选后补”） | 文档定位即“可选后补、不阻塞”，本期未实现；后续要接 error-log 通道再补 |
| 专辑边界自动续播（`playNextAlbum`）的预载 | 评审时明确范围外：需要预取下一专辑歌曲列表，改动面超出“不影响其他功能”约束，留二期决策 |
| 本地编译验证 | 按要求跳过，交由 CI（Build APK）与真机自测清单（§2.4/§3.4）验证 |

### 7.3 待验证（自测清单）

按 §2.4（提交 1）与 §3.4（提交 2）的清单逐项验证，重点：

- [ ] 升级后播放过的歌/章出现 `song:`/`ab:` 新 key；退出重登（token 变化）后章节仍命中缓存
- [ ] 「缓存音乐」页在新旧 key 混合时正常显示
- [ ] 网盘音乐连播 / 有声书自动下一章的起播明显变快（对照关闭开关）
- [ ] 开关关闭后抓包确认无预载请求；“仅 WiFi”下移动网络不预载
- [ ] 全量回归：播放/暂停/拖动/上一首下一首/锁屏控制/缓存页/电台无回归
