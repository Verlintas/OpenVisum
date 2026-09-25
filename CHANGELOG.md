# Changelog

All notable changes to this project are documented in this file.

## [1.0.0] - 2026-09-25

首个正式版本。First stable release.

### 新增 Added

- **播放内核**：libVLC 3.7.6，硬件解码优先、失败自动回退软解；支持 DivX、HEVC、AV1、DTS、AC3/EAC3、TrueHD、FLAC 等格式
- **媒体库**：MediaStore 扫描 + SAF 自定义文件夹双模式；继续观看、收藏、搜索、排序；视频帧缩略图
- **字幕**：内嵌字幕轨识别与按语言自动选中；同目录外挂字幕自动匹配（媒体库/SAF/文件夹）；手动添加；延迟调节；字号/加粗/颜色样式；ASS/SSA 渲染；OpenSubtitles 在线搜索下载
- **音频**：多音轨切换、音频延迟、10 段均衡器、立体声模式、HDMI 源码输出（实验性）
- **播放控制**：0.25x–4x 变速、A-B 循环、画面比例与旋转、快进/快退 10 秒
- **网络**：HTTP/HTTPS/HLS 直链与历史；SMB(SMB2/3)/WebDAV 浏览播放；密码经 Android Keystore 加密
- **投屏**：Chromecast / DLNA 渲染设备发现与投屏
- **界面**：Material 3 动态取色、深色模式、简体中文/繁體中文/English

### 说明 Notes

- 最低支持 Android 14 (API 34)
- 不包含手势控制、后台播放与画中画（设计取舍）
