# 安全说明 Security Policy

## 报告安全问题

如果你发现了安全问题（例如凭据存储、网络传输、恶意文件解析等），**请不要公开提交 Issue**。

请通过以下方式私下报告：

1. 打开仓库的 [Security 标签页](https://github.com/Verlintas/OpenVisum/security)
2. 点击 **Report a vulnerability**，填写复现细节与影响范围

我们会在确认后尽快修复，并在发布说明中致谢（如你愿意署名）。

## 范围说明

- 本项目不收集任何用户数据，不含统计 SDK，不要求账号
- SMB / WebDAV 凭据使用 Android Keystore + AES-GCM 加密存储
- 网络权限仅用于局域网播放、投屏串流与在线字幕搜索
- 应用启动默认开启「隐私缓冲」隐藏媒体内容，可在设置中关闭
