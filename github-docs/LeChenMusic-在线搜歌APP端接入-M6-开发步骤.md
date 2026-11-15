# LeChenMusic 在线搜歌 APP 端接入（M6）· 开发步骤

> 配套服务端文档：`LeChenMusic-Server/docs/在线搜歌下载功能-开发文档.md`（§8 APP 端设计）。
> 服务端 `/api/online/*`（M1–M5）已上线 Web 管理后台，本文档记录 APP 端接入的落地过程。

## 1. 目标

- 搜索页增加「搜全网」入口（无结果时显式引导"曲库没有？去全网搜"）；
- 复用服务端 `/api/online/*` 接口：在线搜索 → 选音质/模糊匹配 → 下载入库；
- 下载任务以任务页形式展示进度（排队/解析/下载/刮削/完成/失败）；
- 下载完成收到推送后可直接播放（系统通知 + 应用内横幅，点击即搜库播放）。

## 2. 接口对接（复用服务端，零改动）

| 功能 | 接口 | 鉴权 |
|------|------|------|
| 在线搜索 | `GET /api/online/search?keyword=&page=&size=` | `X-ND-Authorization: Bearer <JWT>` |
| 创建下载任务 | `POST /api/online/download` | 同上 |
| 我的任务 | `GET /api/online/tasks?mine=1` | 同上 |
| 重试/删除任务 | `POST /api/online/tasks/{id}/retry` / `DELETE /api/online/tasks/{id}` | 同上 |

- 复用 `ApiClient.getAudiobookApi()` 的 JWT retrofit（自动带 `X-ND-Authorization`、401 自动重登重试 `withAudiobookAuthRetry`）；
- 请求体字段对齐 `onlineDownloadRequest`：`source/platform/songId/name/artist/album/coverUrl/quality/fuzzy/time/sign`；
- 错误映射：`409` → "曲库已有这首歌（开启模糊匹配可下载其他版本）"、`429` → 今日配额、`403` → 权限模式提示。

## 3. 改动清单

| 文件 | 内容 |
|------|------|
| `data/model/OnlineModels.kt`（新增） | `OnlineSong` / `OnlineQualityOption` / `OnlineDownloadTask` / `OnlineSearchResult`；手工 JSON 解析（服务端部分字段 `omitempty`，Gson 反射会把非空字段留 null） |
| `data/api/SubsonicApi.kt` | +5 个 `/api/online/*` 端点 |
| `data/repository/MusicRepository.kt` | `onlineSearch / onlineDownload / onlineTasks / onlineRetryTask / onlineDeleteTask` + 错误文案映射 |
| `ui/MainViewModel.kt` | 搜索/下载/任务状态、3s 轮询（仅有进行中任务时）、完成跃变检测、通知点击播放请求、按歌名搜库播放（带扫描入库重试） |
| `ui/screens/search/OnlineSearchScreen.kt`（新增） | 搜歌下载 tab（关键词/音质/模糊匹配/结果列表/下载）+ 下载任务 tab（进度/重试/删除/播放） |
| `ui/screens/search/SearchScreen.kt` | 两处入口：空白态卡片「搜全网下载」+ 音乐 tab 无结果引导「曲库没有？去全网搜」 |
| `ui/navi/Screen.kt` | `Screen.OnlineSearch` 路由 |
| `MainActivity.kt` | 路由接线、全局「已入库可播放」横幅（15s 自动消失）、通知点击 extra 一次性消费 → 自动搜库播放 |

## 4. 关键设计

1. **关键词传递**：入口 → 目标页用 `MainViewModel.pendingOnlineKeyword`（一次性消费）而非路由参数，规避 NavDeepLink 对空格/特殊字符的编码坑；进入页面回落到上次搜索词。
2. **任务轮询**：仅当存在 `queued/resolving/downloading/tagging` 任务时每 3s 轮询一次；全部终态自动停止。
3. **完成提醒**：`taskId -> 上次状态` 记录只在状态**跃变**到 `done` 时提醒（打开页面时的历史已完成任务不重复轰炸，与 Web 端行为一致）。
4. **推送→直接播放**：系统通知（best-effort，Android 13+ 首次进任务页申请 `POST_NOTIFICATIONS`）+ 应用内全局横幅双通道；点击后按歌名搜曲库（搜索重试 4 次 × 2.5s 等扫描入库），命中即 `playSong` 并跳转播放页。
5. **已在曲库的条目**：置灰 + chip 提示，未开模糊匹配下载会被服务端 `409` 拦截并提示开启模糊匹配。

## 5. 落地状态

- [x] 搜全网入口（搜索页空白态 + 无结果引导）
- [x] 在线搜索（关键词/音质档位/模糊匹配开关）
- [x] 下载入库（自动刮削由服务端完成）
- [x] 任务页进度（进行中进度条 / 失败重试删除 / 完成播放）
- [x] 完成推送（系统通知 + 应用内横幅）→ 点击直接播放
- [ ] 端到端联调（真实上游下载 → 入库 → 播放）→ M8
