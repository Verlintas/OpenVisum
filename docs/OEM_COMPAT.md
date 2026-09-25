# OEM 兼容说明

OpenVisum 不依赖 GMS，使用标准 Android API，目标覆盖小米 HyperOS、OPPO ColorOS、vivo OriginOS、荣耀 MagicOS、三星 One UI、华为 HarmonyOS/EMUI 等定制系统。

## 已做的兼容设计

- **无 GMS 依赖**：所有功能基于 AOSP API 与 androidx，不检查 Google Play 服务
- **标准权限流程**：仅申请 `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` / `INTERNET` / 网络状态，运行时弹窗遵循厂商定制
- **SAF 优先**：除 MediaStore 外支持任意文件夹授权，规避部分 ROM 的媒体库索引差异
- **surface 兼容**：libVLC 默认 OpenGL ES 输出，异常时回退
- **配置变更不重建**：横竖屏、分屏、折叠屏切换不销毁 Activity，播放不中断
- **无后台服务**：不申请后台常驻，不涉及厂商后台清理白名单问题（本项目不含后台播放）

## 需用户注意

- 部分 ROM（如 MIUI/HyperOS）首次授予媒体权限时会弹出"仅本次允许"，请选择"始终允许"以获得完整体验
- 个别 ROM 默认亮度/音量键行为不同，不影响播放功能
- HDMI 源码输出取决于设备音频通路，部分机型可能不可用，设置项会显示"当前输出不支持"

## 反馈

如遇特定机型问题，请在 GitHub Issues 中附上机型、系统版本与复现步骤。
