# 贡献指南 Contributing

欢迎提交 Issue 和 Pull Request！在此之前，请花两分钟阅读以下约定。

Thanks for your interest in contributing! Issues and pull requests are welcome.

## 提交 Issue

- **Bug**：请使用内置的 Bug 反馈模板，附上应用版本、设备型号、系统版本与复现步骤，有日志更好
- **功能建议**：请先搜索已有 Issue 与[官网](https://verlintas.github.io/OpenVisum/)，避免重复
- 提交前请确认使用的是[最新版本](https://github.com/Verlintas/OpenVisum/releases/latest)

## 开发环境

- JDK 17、Android SDK Platform 37
- 首次构建：`./gradlew :app:assembleDebug`
- 国内网络可在 `~/.gradle/gradle.properties` 加 `openvisum.cnMirrors=true` 启用阿里云镜像
- Release 签名仅维护者需要，日常开发用 debug 构建即可

## 代码结构

```
app/            Compose UI、导航、播放页、设置、网络、媒体库
core/player/    PlaybackEngine 接口 + libVLC 实现、音轨/字幕/均衡器/投屏、MediaStreamServer
core/data/      Room、MediaStore 扫描、SAF、SMB/WebDAV、字幕 Provider、设置存储
core/common/    纯 Kotlin 工具：时间/大小格式化、语言归一化、字幕匹配
site/           官网静态页（GitHub Pages）
```

依赖方向：`app -> core:player / core:data -> core:common`。跨层改动请保持这个方向，不要把 UI 依赖带进 core。

更详细的模块说明见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 提交 PR

- 一个 PR 解决一件事，标题与提交信息可用 `feat:` / `fix:` / `docs:` / `site:` 前缀
- 提交前请确保：
  - `./gradlew :app:assembleDebug` 构建通过
  - `./gradlew test` 单元测试通过
  - 在真机或模拟器上实际验证过
- 用户可见的改动请在 `CHANGELOG.md` 顶部按既有格式添加条目
- 不要提交密钥、签名文件（`*.jks`）、`local.properties` 或本地凭据
- 界面改动请附上截图

## 代码风格

- 沿用现有代码风格（Kotlin 官方风格、Compose 惯用写法）
- 新逻辑尽量放在 `core` 层并补单元测试（参考 `core:common` 与 `core:player` 的测试）
- 字符串一律走资源文件，中英双语（简中/繁中/英文）

## 测试

```bash
./gradlew test
```

覆盖：字幕匹配（发布标签、语言后缀、模糊匹配）、时间格式化、音轨选择语言优先与回退。
