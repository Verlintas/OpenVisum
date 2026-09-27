<p align="center">
  <img src="docs/icon.png" width="96" alt="OpenVisum" />
</p>

<h1 align="center">OpenVisum</h1>

<p align="center">
  基于 libVLC 的 Android 视频播放器 · GPL-3.0-or-later
</p>

<p align="center">
  <a href="https://github.com/Verlintas/OpenVisum/releases"><img src="https://img.shields.io/github/v/release/Verlintas/OpenVisum" alt="Latest release" /></a>
  <a href="https://github.com/Verlintas/OpenVisum/actions/workflows/build.yml"><img src="https://github.com/Verlintas/OpenVisum/actions/workflows/build.yml/badge.svg" alt="Build status" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/Verlintas/OpenVisum" alt="License" /></a>
</p>

## 简介

OpenVisum 是一个 Android 视频播放器：播放内核使用 libVLC 3.7，界面与业务逻辑使用 Kotlin + Jetpack Compose 实现。媒体来源支持 MediaStore 媒体库、SAF 文件夹、SMB、WebDAV 与 HTTP / HLS 直链；支持字幕自动匹配、DLNA 投屏与 9 套主题色。

- 要求：Android 14（API 34）及以上，arm64-v8a / armeabi-v7a / x86_64
- 无 GMS 依赖；不包含统计 SDK，不要求账号
- 项目主页（含更新日志与 APK 镜像）：<https://verlintas.github.io/OpenVisum/>

## 截图

<p align="center">
  <img src="docs/screenshots/home.png" width="22%" alt="首页" />
  <img src="docs/screenshots/player.png" width="22%" alt="播放器" />
  <img src="docs/screenshots/library.png" width="22%" alt="媒体库" />
  <img src="docs/screenshots/tablet-home.png" width="22%" alt="平板布局" />
</p>

## 下载与安装

| 架构 | 适用 | 说明 |
| --- | --- | --- |
| `arm64-v8a` | 2017 年后的绝大多数手机 | 默认选择 |
| `armeabi-v7a` | 老旧 32 位设备 | 部分老机可用 |
| `x86_64` | Android 模拟器、x86 平板 | 开发调试用 |

- 下载：[GitHub Releases](https://github.com/Verlintas/OpenVisum/releases/latest) 或[官网镜像](https://verlintas.github.io/OpenVisum/#download)
- 安装时系统会提示「未知来源」权限，属于侧载 APK 的正常流程
- 校验下载文件（SHA-256 见官网 [releases.json](https://verlintas.github.io/OpenVisum/releases.json)）：

  ```bash
  sha256sum app-arm64-v8a-release.apk          # Linux
  shasum -a 256 app-arm64-v8a-release.apk       # macOS
  ```

- 各版本共用同一签名密钥，可覆盖升级

## 功能

**解码**
- libVLC 3.7：DivX / Xvid / RMVB / HEVC / AV1 / DTS / AC3 / EAC3 / TrueHD / FLAC 等
- 硬件解码优先，解码失败自动回退软件解码

**字幕**
- 内嵌字幕轨自动识别，按设置的首选语言选中
- 同目录外挂字幕自动匹配，支持 `srt / ass / ssa / vtt / sub / smi / sami / ttml / dfxp / mpl / txt`
- ASS/SSA 特效渲染；字幕延迟、字号、加粗、颜色可调
- OpenSubtitles 在线搜索与下载（需自行配置 API key，见下）

**音频**
- 多音轨切换、音轨延迟对齐、10 段均衡器、立体声模式
- HDMI 源码输出（实验性，取决于设备音频通路）

**播放**
- 拖动进度条显示画面缩略图与时间码
- 画面比例：适应屏幕 / 拉伸铺满 / 裁剪填满 / 原始大小 / 16:9 / 4:3 / 21:9 / 2.35:1，及画面旋转
- 0.25x–4x 变速、A-B 循环、±10s 快进快退
- 沉浸式全屏播放页，退出自动恢复系统栏

**媒体来源**
- MediaStore 媒体库扫描与 SAF 任意文件夹授权
- SMB（SMB 2.0.2–3.1.1，jcifs-ng）、WebDAV（OkHttp PROPFIND）、HTTP / HTTPS / HLS (m3u8)
- 网络位置凭据使用 Android Keystore + AES-GCM 加密存储
- 继续观看、收藏、搜索、排序（时间 / 名称 / 大小 / 时长）

**投屏**
- DLNA / UPnP 渲染设备发现与投屏（jUPnP）
- 本地视频由应用内置 HTTP 服务器串流给电视，支持电视端拖动进度；网络来源直接投递原地址

**界面**
- 手机常驻迷你侧栏（纯图标 + 存储占用圆环）；展开为纯文字抽屉，显示存储明细
- 宽屏（平板 / 折叠屏展开）自动切换常驻侧边栏与多列网格
- 9 套主题色（含跟随系统动态取色），明暗双色板，切换带 550ms 过渡
- 应用内语言：跟随系统 / 简体中文 / 繁體中文 / English
- 启动隐私缓冲（默认开启）：打开应用先隐藏媒体内容

## 使用说明

### 本地媒体
- 首次启动请求媒体读取权限，媒体库基于 MediaStore 扫描
- 「打开文件」可直接播放单个文件；添加整个文件夹使用 SAF 授权（权限持久化，重启后仍有效）

### 字幕
- 自动匹配规则：与视频同目录、主名一致即可；文件名带 `1080p` / `WEB-DL` / `x265` 等发布标签或 `.chs` / `.eng` 等语言后缀也能识别，按匹配得分取最优
- 自动加载外挂字幕与首选语言可在 设置 → 字幕 中调整（首选语言同时用于内嵌音轨/字幕轨的自动选中）
- 手动添加：播放页「字幕」面板 → 添加字幕文件

### 网络位置
- 在「网络」页添加 SMB / WebDAV 地址；密码加密后保存在本机，不会明文写入
- HTTP / HLS 直链可直接粘贴播放，历史记录保留在「网络」页

### 投屏
- 手机与电视连接同一 Wi-Fi；播放页右上角点投屏图标，选择发现的设备
- 本地视频由手机串流给电视，可正常拖动进度；网络视频直接投原地址
- 搜不到设备时：确认路由器未开启「AP 隔离 / 客户端隔离」，未使用访客网络；部分电视的 UPnP 实现差异可能影响兼容性

### 在线字幕
- 在 <https://www.opensubtitles.com/> 注册并获取 API key
- 设置 → 字幕 → 填入 API key（可选填账号密码以提高配额）

### 隐私与数据
- 所有数据只存在本机：`openvisum.db`（Room：媒体索引 / 进度 / 收藏）、DataStore（设置）、加密凭据文件；卸载应用即全部清除
- 无统计 SDK、无账号体系；网络权限仅用于局域网播放、投屏串流与在线字幕搜索
- 隐私缓冲默认开启，可在主设置页关闭

## 技术说明

### 模块结构

| 模块 | 职责 |
| --- | --- |
| `app` | Compose UI、导航、播放页、设置、网络与媒体库界面 |
| `core:player` | `PlaybackEngine` 抽象与 libVLC 实现、音轨/字幕、投屏与局域网串流服务器 |
| `core:data` | Room、MediaStore 扫描、SAF、SMB/WebDAV、字幕 Provider、DataStore 设置 |
| `core:common` | 纯 Kotlin 工具：字幕匹配、语言归一化、时间/大小格式化 |

依赖方向：`app → core:player / core:data → core:common`。

### 关键实现位置

| 功能 | 文件 |
| --- | --- |
| 播放引擎（媒体打开 / PFD / 轨道 / 重载） | `core/player/.../VlcPlaybackEngine.kt` |
| 投屏串流服务器（Range / 206） | `core/player/.../MediaStreamServer.kt` |
| 字幕匹配打分 | `core/common/.../SubtitleMatcher.kt` |
| 播放页（沉浸模式 / 裁剪渲染 / 进度预览） | `app/.../ui/player/PlayerScreen.kt` |
| 应用外壳（抽屉 / 侧栏 / 迷你侧栏） | `app/.../MainActivity.kt`、`app/.../ui/navigation/` |
| 网络来源（SMB / WebDAV） | `core/data/.../source/SmbBrowser.kt` 等 |

几个实现要点：

- `content://` 通过 `ParcelFileDescriptor` 注入 libVLC（libVLC 无法直接打开 content MRL）；外挂字幕复制到缓存目录后以 `file://` 加载
- 投屏时本地文件由内置 HTTP 服务器暴露为 `http://<局域网 IP>:<port>/media`，实现 Range/206 分段，电视可直接拖动进度
- 「裁剪填满」通过 TextureView + `graphicsLayer` 等比放大、父容器裁边实现（libVLC 的裁剪滤镜在 Android vout 上不可用）
- 进度预览使用 Coil `videoFrameMillis` 按 10 秒分桶取帧，内存缓存并保留上一帧避免闪烁
- 媒体库重扫使用 Room `upsertPreservingUserData`，只更新元数据，不覆盖收藏与观看进度

更多设计细节见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)，厂商系统兼容性见 [docs/OEM_COMPAT.md](docs/OEM_COMPAT.md)。

### 测试

```bash
./gradlew test
```

覆盖字幕匹配（发布标签、语言后缀、模糊匹配）、时间格式化、音轨选择语言优先与回退逻辑。

## 构建

要求：JDK 17、Android SDK Platform 37。

```bash
./gradlew :app:assembleDebug     # 调试构建
./gradlew :app:assembleRelease   # 发布构建（需要签名配置）
```

Release 签名通过 `keystore.properties` 或环境变量配置：

```properties
# keystore.properties
storeFile=/path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

对应环境变量：`OPENVISUM_KEYSTORE_PATH`、`OPENVISUM_KEYSTORE_PASSWORD`、`OPENVISUM_KEY_ALIAS`、`OPENVISUM_KEY_PASSWORD`。

中国大陆网络可在 `~/.gradle/gradle.properties` 加入以下配置启用阿里云镜像：

```properties
openvisum.cnMirrors=true
```

推送 `v*` 标签会触发 CI 构建三个架构的签名 APK 并创建 Release；`main` 分支的官网页面由 GitHub Actions 自动部署。

## 项目结构

```
app/            Compose UI、导航、播放页、设置、网络、媒体库
core/player/    PlaybackEngine 接口 + libVLC 实现、投屏、MediaStreamServer
core/data/      Room、MediaStore 扫描、SAF、SMB/WebDAV、字幕 Provider
core/common/    纯 Kotlin 工具（字幕匹配 / 语言 / 时间）
site/           官网静态页（GitHub Pages）
tools/          主题色生成器、版权头检查脚本
docs/           架构与兼容文档、截图
```

## 已知限制

- 手机作为 DLNA 接收端（接收其他设备投屏）尚未实现，计划 v1.1
- DLNA 投屏兼容性因电视/接收端实现而异；路由器开启 AP 隔离或使用访客网络时无法使用
- HDMI 源码输出依赖设备音频通路，部分机型不可用
- 在线字幕需要 OpenSubtitles 账号（API key 免费申请）
- 要求 Android 14+，未适配更低版本（基于较新的系统 API 与 Compose 特性）

## 参与贡献

- 提交 Issue 前请搜索已有问题；Bug 请使用仓库内置的 Issue 表单，附机型、系统版本与复现步骤
- 开发环境、代码结构、PR 要求见 [CONTRIBUTING.md](CONTRIBUTING.md)
- 安全问题请按 [SECURITY.md](SECURITY.md) 私下报告
- 参与讨论需遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## 更新日志

见 [CHANGELOG.md](CHANGELOG.md)。

## 许可证与第三方依赖

本项目以 [GPL-3.0-or-later](LICENSE) 发布。主要第三方依赖：

| 依赖 | 用途 | 许可 |
| --- | --- | --- |
| [libVLC](https://www.videolan.org/vlc/libvlc.html) | 播放内核 | LGPL-2.1-or-later |
| [jcifs-ng](https://github.com/AgNO3/jcifs-ng) | SMB 访问 | LGPL-2.1 |
| [jUPnP](https://github.com/jupnp/jupnp) | DLNA 发现与投屏 | CDDL-1.0 |
| AndroidX / Jetpack Compose / Room / DataStore | UI 与数据层 | Apache-2.0 |
| [Coil](https://coil-kt.github.io/coil/) | 图片与视频帧加载 | Apache-2.0 |
| [kotlinx.serialization / Coroutines](https://github.com/Kotlin) | 序列化与并发 | Apache-2.0 |

感谢 VideoLAN 社区与上述开源项目。
