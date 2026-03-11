# AndroidForClaw Release APKs

最新构建时间: 2026-03-11

## 📦 包含的 APK

### 1. AndroidForClaw.apk (31 MB)
**主应用 - 必须安装**
- AI Agent 核心引擎
- AgentLoop 执行循环
- Skills 系统
- 飞书/Discord 集成
- Gateway WebSocket 服务

### 2. ObserverService.apk (4.4 MB)
**无障碍服务扩展 - 推荐安装**
- UI 树观察能力
- 截图功能增强
- 手势操作支持
- 权限管理界面

### 3. BrowserForClaw.apk (8.4 MB)
**浏览器扩展 - 可选安装**
- 基于 Unity WebView
- 支持网页操作
- 与主应用集成

## 🚀 安装顺序

1. **AndroidForClaw.apk** (主应用,必须先安装)
2. **ObserverService.apk** (无障碍服务,推荐安装)
3. **BrowserForClaw.apk** (浏览器,可选安装)

## 📝 更新说明

### 本次更新 (2026-03-11)

#### 修复的问题
1. ✅ Skills metadata 解析失败 → Always Skills 从 0 恢复到 2
2. ✅ 飞书消息发送失败(表格超限) → 自动降级为纯文本
3. ✅ AgentLoop 缺少全局错误兜底 → 添加 try-catch 确保错误反馈
4. ✅ 修复 .gitignore 配置错误 → WorkspaceInitializer.kt 现在可以被提交

#### 新增功能
- 飞书插件表格数量预检查 (maxTablesPerCard = 3)
- 发送失败自动降级重试机制
- AgentLoop 全局错误兜底

## ⚙️ 首次配置

安装后需要配置:

1. **创建配置文件**: `/sdcard/.androidforclaw/openclaw.json`
2. **配置 API Key**: 在配置文件中设置 LLM provider
3. **授予权限**:
   - 主应用: 存储权限、悬浮窗权限
   - ObserverService: 无障碍服务权限
4. **启动飞书连接** (可选): 配置飞书 App ID 和 Secret

## 🔧 构建信息

- **Gradle**: 8.9
- **Kotlin**: 1.9.22
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 21 (Android 5.0+)

## 📋 签名信息

Release 版本已使用 keystore 签名,可直接安装。

## 🆘 问题排查

如果遇到问题:
1. 检查日志: `adb logcat | grep AndroidForClaw`
2. 查看配置: `/sdcard/.androidforclaw/openclaw.json`
3. 检查权限: 设置 → 应用 → AndroidForClaw

---

**项目地址**: https://github.com/xiaomochn/AndroidForClaw
