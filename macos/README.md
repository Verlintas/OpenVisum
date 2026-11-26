# OpenVisum for macOS

OpenVisum 的 macOS 原生版本（SwiftUI + libVLC / VLCKit 等价的 libVLC 接入）。

当前为 **v0.1 预览版**，交互对齐移动端：

- 可折叠侧栏（展开为文字导航 + 存储明细，收起为图标栏 + 存储圆环）
- 首页 Hero（继续观看/立即播放、收藏、元信息）+ 横向行卡（继续观看、最近添加、收藏）
- 播放器：中心播放控制（上一集/±10 秒/下一集）、底部进度条 + 六宫格入口
- 底部弹层：倍速、音轨、字幕、均衡器（libVLC 预设）、画面比例（适应/裁剪填满）、章节与书签
- 拖动进度实时预览画面（AVFoundation 取帧）；同文件夹自然排序连播
- 媒体库：全部/收藏筛选、搜索、缩略图、进度标记
- 播放进度、收藏、书签持久化；9 套强调色

## 开发环境

- macOS 14+（Apple Silicon 或 Intel）
- Xcode 26+（当前使用 Xcode 27 / Swift 6.4）
- [XcodeGen](https://github.com/yonaskolb/XcodeGen)：`brew install xcodegen`

## 构建

```bash
cd macos
Scripts/fetch-vlc.sh        # 下载 VLC 运行时与 libVLC 头文件到 Vendor/（首次）
xcodegen generate           # 生成 OpenVisumMac.xcodeproj
xcodebuild -project OpenVisumMac.xcodeproj -scheme OpenVisum \
  -configuration Debug -derivedDataPath build build
open build/Build/Products/Debug/OpenVisum.app
```

运行时依赖不写入 git：`Vendor/VLC.app`（universal，arm64 + x86_64）与
`Vendor/include`（libVLC 头文件），由 `fetch-vlc.sh` 获取。

## 打包 libVLC 进 App

开发时应用从 `Vendor/` 加载 libVLC；要生成可分发、不依赖 Vendor 的 App：

```bash
Scripts/bundle-vlc.sh build/Build/Products/Release/OpenVisum.app
```

脚本会把 `libvlc` / `libvlccore` 与插件复制进 `Contents/Frameworks` 与
`Contents/Resources/vlc/plugins`，并做 ad-hoc 签名。正式分发还需要开发者证书
与 notarization（`xcrun notarytool`）。

## 结构

```
Sources/
  App.swift              应用入口、启动参数（直接打开视频文件）
  Player/PlayerCore.swift libVLC 封装（播放、进度、音量、倍速、轨道、外挂字幕）
  Player/VideoSurface.swift  NSViewRepresentable 视频输出
  Library/LibraryStore.swift 文件夹扫描、缩略图（AVFoundation）、进度持久化
  Views/                 首页 / 媒体库 / 设置 / 播放器
  UI/Theme.swift         9 套强调色与暗色面板样式
Resources/AppIcon.icns
Scripts/fetch-vlc.sh     拉取 VLC 运行时
Scripts/bundle-vlc.sh    打包 libVLC 进 App
```

## 与 Android 版的关系

共享设计语言与功能目标（libVLC 内核、字幕自动匹配、进度记忆、主题色），
但实现是独立的 SwiftUI 代码；libVLC 负责解码与渲染，媒体库与界面使用
macOS 原生 API（AVFoundation 缩略图、NSOpenPanel、SwiftUI）。

## 许可证

GPL-3.0-or-later。libVLC 为 LGPL-2.1-or-later。
