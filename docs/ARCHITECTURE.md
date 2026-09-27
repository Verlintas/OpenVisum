# OpenVisum 架构

## 模块

```
app             Compose UI、导航、ViewModel、AppContainer（手动依赖注入）
core:player     PlaybackEngine 抽象 + VlcPlaybackEngine 实现、TrackSelector、EqualizerPresets、MediaStreamServer
core:data       Room、MediaStore 扫描、SAF、SMB/WebDAV、字幕查找与在线 Provider、设置存储
core:common     纯 Kotlin 工具：时间/大小格式化、语言归一化、字幕文件匹配算法
```

依赖方向：`app -> core:player / core:data -> core:common`。

## 播放引擎

`PlaybackEngine` 接口屏蔽 libVLC 细节，`VlcPlaybackEngine` 是唯一实现：

- **媒体打开**：`content://` 通过 `ParcelFileDescriptor` 打开（libVLC 无法直接读取 content MRL）；`file://`、`http(s)://`、`smb://` 直接走 URI
- **轨道**：监听 `MediaPlayer.Event.ESAdded/ESDeleted/ESSelected` 刷新 `videoTracks/audioTracks/subtitleTracks`，并从已解析的 `IMedia.Track` 补充编码、语言、声道、分辨率
- **自动选择**：`TrackSelector` 按首选语言顺序打分，仅在用户未手动选择时生效
- **外挂字幕**：`addSlave` 前把 `content://` 字幕复制到缓存目录再以 `file://` 加载
- **重载一致性**：改硬件解码、字幕样式、旋转、声道模式时重载媒体，保留播放位置、用户选中的轨道、外挂字幕与媒体 options（`lastMediaOptions`）
- **画面渲染**：`attachViews(..., useTextureView = true)` 使用 TextureView 承载视频，播放页用 `graphicsLayer` 等比放大实现「裁剪填满」（不改动 libVLC 的 scale type，避免拉伸变形）
- **章节**：`titles/chapters` 在时长变化时刷新；`setChapter/nextChapter/previousChapter` 直接驱动 libVLC
- **投屏**：`RendererDiscoverer` 发现 DLNA/UPnP 渲染设备（发现期间持有 Wi-Fi `MulticastLock`）；`MediaPlayer.setRenderer` 切换输出：
  - 本地/SAF 媒体：`MediaStreamServer`（内嵌 HTTP 服务器，支持 Range/206 分段）把文件以 `http://<局域网 IP>:<port>/media` 暴露给电视拉流，支持远程拖动进度；断开投屏后自动恢复本地播放与原位置
  - 网络来源：直接把原始 URL 交给渲染设备
  - 失败时回退本地播放并在状态里给出错误

## DLNA 接收端

`core:player/receiver` 让本机成为投屏目标，其他设备可投到手机：

- `DlnaReceiver` 用 `AndroidUpnpServiceConfiguration` + 自建 `AndroidRouter`（提供 Wi-Fi 组播锁）在进程内托管 `MediaRendererDevice`（jUPnP + Jetty，命名空间 `/upnp`）
- `RendererAvTransportService` / `RendererAudioRenderingControl` / `RendererConnectionManagerService` 实现标准服务；`LastChangeAwareServiceManager` 负责 GENA 事件
- 动作经 `RendererHost` 桥接到 App：`SetAVTransportURI` 通过 `ACTION_VIEW` 拉起播放器，播放/暂停/拖动/音量映射到共享 `PlaybackEngine`
- 生命周期由 Settings 数据流驱动（`lifecycleScope` + `repeatOnLifecycle`），start/stop 幂等，Activity 重建不重启
- 无后台服务设计：接收端随应用进程存活

## 数据层

- **Room**：`media_items`（媒体索引+播放进度+收藏）、`bookmarks`（按媒体的位置书签）、`saf_folders`、`network_sources`、`stream_history`（版本 3，含迁移）
- **扫描**：MediaStore 查询后 `upsertPreservingUserData`——已存在条目只更新元数据，不覆盖收藏与进度
- **SAF**：`DocumentFile` 遍历，持久化 URI 权限
- **网络**：jcifs-ng 浏览 SMB；OkHttp PROPFIND 浏览 WebDAV；凭据 AES-GCM（Android Keystore）加密存储
- **字幕查找**：按 URI 类型分派——文件系统、DocumentsProvider、MediaStore.Files（`MEDIA_TYPE_SUBTITLE`）；结果用 `core:common` 的 `SubtitleMatcher` 打分
- **在线字幕**：`SubtitleProvider` 接口，当前实现 OpenSubtitles（搜索 + 登录 + 下载）
- **设置**：`PreferencesRepository`（DataStore）统一承载主题、语言、默认倍速、隐私缓冲（`hideContentOnLaunch`）、连播（`autoPlayNext`）、DLNA 接收（`dlnaReceiverEnabled`）等
- **同目录播放列表**：`VideoFinder` 解析同文件夹视频（MediaStore / file / SAF），`NaturalOrder` 做「第 2 集 < 第 10 集」的自然排序（含单测）

## UI

- 单 Activity + Navigation Compose
- `AppContainer` 手动创建单例（数据库、仓库、播放引擎），经 `ViewModelProvider.Factory` 注入；`HomeViewModel` 提升到 Activity 作用域，供全局侧栏与首页共享
- **应用外壳**（MainActivity）：手机为 `ModalNavigationDrawer`（纯文字条目 + 存储明细区）；宽屏为常驻 `AppNavigationRail`（自绘 Surface）；手机另有常驻 `MiniSidebar`（纯图标导航 + `StorageRing` 存储圆环），播放路由自动隐藏
- **存储信息**：`StatFs` 读取外部存储占用，注入 `SidebarData`（已用/总量、媒体库大小与数量）供抽屉、侧栏与圆环展示
- **隐私缓冲**：`hideContentOnLaunch` 默认开启，`PrivacyGate` 在首页遮挡媒体内容，轻触后会话内显示
- 播放页：`AndroidView` 承载 `VLCVideoLayout`（TextureView），Compose 覆盖控制层；进入时隐藏系统栏（沉浸模式），退出自动恢复；拖动进度条用 Coil `videoFrameMillis` 取帧预览（10 秒分桶 + 内存缓存 + 保留上一帧）；底部弹出式面板管理音轨/字幕/均衡器/倍速/画面/投屏
- 全局 Snackbar 与触感反馈（`ui/components/Feedback.kt`）
- 播放进度每 5 秒或暂停/退出时写入 Room
- 睡眠定时（`SleepTimerState`）由 ViewModel 协程计时，到时暂停；「播完当前」模式拦截自动连播
- 设置页可导出诊断报告（设备信息 + 最近 logcat，`DiagnosticReport`）

## 测试

- `core:common`：时间格式化、字幕匹配（含发布标签、语言后缀、模糊匹配）
- `core:player`：`TrackSelector` 语言优先与回退逻辑
