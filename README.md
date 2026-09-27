# OpenVisum

开源 Android 视频播放器，由 libVLC 驱动。几乎什么格式都能播，自动识别音轨与字幕。

An open-source Android video player powered by libVLC. Plays virtually any format, with automatic audio-track and subtitle detection.

[![Release](https://img.shields.io/github/v/release/Verlintas/OpenVisum?color=4f6bed)](https://github.com/Verlintas/OpenVisum/releases)
[![License](https://img.shields.io/github/license/Verlintas/OpenVisum?color=4f6bed)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-14%2B-3ddc84)](https://developer.android.com)
[![Website](https://img.shields.io/badge/website-verlintas.github.io-8fa2ff)](https://verlintas.github.io/OpenVisum/)

<p align="center">
  <img src="docs/screenshots/home.png" width="23%" alt="Home" />
  <img src="docs/screenshots/player.png" width="23%" alt="Player with subtitles" />
  <img src="docs/screenshots/library.png" width="23%" alt="Library" />
  <img src="docs/screenshots/tablet-home.png" width="23%" alt="Tablet" />
</p>

## 官网 Website

项目介绍、交互式主题演示、截图画廊与最新版 APK 镜像：[https://verlintas.github.io/OpenVisum/](https://verlintas.github.io/OpenVisum/)

## 特性 Features

**格式与解码**
- 基于 libVLC：DivX、Xvid、RMVB、HEVC、AV1、DTS、AC3/EAC3、TrueHD、FLAC 等
- 硬件解码优先，失败自动回退软件解码
- ASS/SSA 特效字幕、SRT/VTT 等常见格式

**字幕**
- 自动识别内嵌字幕轨并按首选语言自动选中
- 自动匹配视频同目录的同名外挂字幕（媒体库 / SAF / 文件夹）
- 手动添加字幕文件、字幕延迟调节、字号/加粗/颜色自定义
- 在线搜索下载（OpenSubtitles，需自备 API key）

**音频**
- 多音轨自动识别与切换，音频延迟互对齐
- 10 段均衡器（预设 + 自定义频段、前置增益）
- 立体声模式、HDMI 源码输出（实验性）

**播放**
- 拖动进度条时实时显示画面预览缩略图（10 秒分桶取帧 + 时间码）
- 画面比例（适应屏幕 / 拉伸铺满 / **裁剪填满** / 原始大小 / 16:9 / 4:3 / 21:9 / 2.35:1）与应用内旋转；裁剪模式等比放大、裁边不留黑边且不变形
- 0.25x–4x 变速、A-B 循环、±10s 快进快退
- 全屏沉浸播放器：隐藏系统栏，触控与手势区域不遮挡顶部按钮
- 投屏到 DLNA / Chromecast 渲染设备：本地视频经内置局域网串流服务器直传（支持拖动进度），网络来源视频直接投递原地址

**媒体库与网络**
- 媒体库 + 文件夹双模式：MediaStore 扫描、SAF 添加任意文件夹
- 继续观看、收藏、搜索、排序（时间/名称/大小/时长）
- SMB（SMB2/3）与 WebDAV 网络位置，密码经 Android Keystore 加密保存
- HTTP / HTTPS / HLS (m3u8) 直链播放与历史记录

**界面**
- 观影工作区首页：全屏海报 Hero、继续观看/最近/收藏/文件夹分行
- 手机常驻**迷你侧栏**：纯图标导航 + 底部实时存储占用圆环；展开为纯文字抽屉，含存储明细（进度条、已用/总量、媒体库占用与媒体数量）
- 平板/折叠屏适配：宽屏常驻侧边文字栏 + 自适应多列网格 + 内容限宽
- 启动**隐私缓冲**：默认隐藏媒体内容，轻触后才显示，可在设置中关闭
- Material 3 + 官方色彩算法生成的 **9 种主题色**（品牌蓝/青碧/松绿/琥珀/玫瑰/紫罗兰/绯红/石墨 + 跟随系统动态取色），明暗双色板，550ms 平滑换肤
- Manrope 字体、大标题折叠主页、播放器玻璃质感控件、全局 Snackbar 与触感反馈、全套过渡动画
- 应用内语言切换（跟随系统 / 简体中文 / 繁體中文 / English，系统级 per-app language）
- 默认倍速、记忆播放位置、默认字幕样式等完整设置项
- 无 GMS 依赖，适配各大厂商定制 Android 系统

## 系统要求 Requirements

- Android 14 (API 34) 及以上
- 架构：arm64-v8a / armeabi-v7a / x86_64

## 下载 Download

前往 [Releases](https://github.com/Verlintas/OpenVisum/releases) 下载对应架构的 APK（`arm64-v8a` 适用于绝大多数手机）；[官网](https://verlintas.github.io/OpenVisum/) 提供镜像下载与 SHA-256 校验值。

## 构建 Build

需要 JDK 17、Android SDK Platform 37。

```bash
# Debug
./gradlew :app:assembleDebug

# Release（需配置签名，见下）
./gradlew :app:assembleRelease
```

Release 签名可通过环境变量或 `keystore.properties` 配置：

```properties
storeFile=/path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

或环境变量 `OPENVISUM_KEYSTORE_PATH`、`OPENVISUM_KEYSTORE_PASSWORD`、`OPENVISUM_KEY_ALIAS`、`OPENVISUM_KEY_PASSWORD`。

中国大陆网络可开启镜像（阿里云）加速依赖下载，在你的 `~/.gradle/gradle.properties` 加入：

```properties
openvisum.cnMirrors=true
```

## 项目结构 Project layout

```
app/            UI、导航、播放页、设置、网络、媒体库
core/player/    PlaybackEngine 接口 + libVLC 实现、音轨/字幕/均衡器/投屏、局域网串流服务器
core/data/      Room、MediaStore 扫描、SAF、SMB/WebDAV、字幕 Provider
core/common/    工具（时间/语言/字幕匹配）
site/           官网静态页（GitHub Pages）
```

更多设计细节见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)，厂商系统兼容说明见 [docs/OEM_COMPAT.md](docs/OEM_COMPAT.md)。

## 已知限制 Known limitations

- 手机作为 DLNA 接收端（接收其他设备投屏）尚未实现，计划 v1.1
- DLNA 投屏兼容性因电视/接收端实现而异；路由器开启 AP 隔离或使用访客网络时无法发现设备与串流
- HDMI 源码输出依赖设备音频通路，行为因厂商而异（实验性）
- 在线字幕下载需要 OpenSubtitles 账号（API key 免费申请）

## 更新日志 Changelog

见 [CHANGELOG.md](CHANGELOG.md)。

## 许可证 License

GPL-3.0。本项目使用 [libVLC](https://www.videolan.org/vlc/libvlc.html)（LGPL-2.1-or-later），详见 [LICENSE](LICENSE)。
