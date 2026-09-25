# OpenVisum

开源 Android 视频播放器，由 libVLC 驱动，支持几乎所有视频/音频格式、多音轨与多字幕自动识别。

An open-source Android video player powered by libVLC. Plays virtually any format, with automatic audio-track and subtitle detection.

## 特性 Features

- 基于 libVLC：DivX、DTS/AC3/EAC3/TrueHD、HEVC、AV1、ASS/SSA 特效字幕等原生支持
- 自动识别内嵌音轨与字幕，自动匹配同目录同名外挂字幕
- 音轨/字幕切换、延迟调节、10 段均衡器、HDMI 源码输出（实验性）
- 画面比例、旋转、裁剪、变速、A-B 循环
- 媒体库 + 文件夹双模式浏览
- 网络播放：SMB / WebDAV / HTTP / HLS (m3u8)
- Material 3 界面，简体中文 / 繁體中文 / English
- 无 GMS 依赖，适配各厂商定制 Android 系统

## 系统要求 Requirements

- Android 14 (API 34) 及以上
- arm64-v8a / armeabi-v7a / x86_64

## 构建 Build

```bash
./gradlew :app:assembleDebug
```

## 许可证 License

GPL-3.0。本项目使用 libVLC（LGPL-2.1-or-later），详见 [LICENSE](LICENSE)。
