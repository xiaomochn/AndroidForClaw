# OpenClaw ↔ AndroidForClaw 映射表

**纯粹的文件和文件夹映射关系,方便快速查找对应实现。**

---

## 📁 顶层目录映射

| OpenClaw | AndroidForClaw | 说明 |
|----------|----------------|------|
| `~/file/forclaw/OpenClaw/` | `~/file/forclaw/phoneforclaw/` | 项目根目录 |
| `src/` | `app/src/main/java/com/xiaomo/androidforclaw/` | 主代码目录 |
| `skills/` | `app/src/main/assets/skills/` | 内置 Skills |
| - | `/sdcard/androidforclaw-workspace/skills/` | 工作区 Skills |
| `extensions/` | `extensions/` | 扩展模块 |
| `test/` | `app/src/test/` | 单元测试 |
| - | `app/src/androidTest/` | Android 测试 |
| `docs/` | - | 文档目录 |
| `apps/` | - | 多应用 (AClaw 单应用) |

---

## 🔧 核心代码目录映射

### Agent Runtime

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/agents/` | `app/src/main/java/com/xiaomo/androidforclaw/agent/` |
| `src/agents/run-agent-loop.ts` | `agent/loop/AgentLoop.kt` |
| `src/agents/tool-loop-detection.ts` | `agent/loop/ToolLoopDetection.kt` ✅ |
| `src/agents/tool-registry.ts` | `agent/tools/AndroidToolRegistry.kt` |
| `src/agents/tool-registry.ts` | `agent/tools/ToolRegistry.kt` |
| `src/agents/skills-loader.ts` | `agent/skills/SkillsLoader.kt` |
| `src/agents/build-context.ts` | `agent/context/ContextBuilder.kt` |
| `src/agents/agent-scope.ts` | - |
| `src/agents/acp-spawn.ts` | - |
| - | `agent/functions/` |
| - | `agent/tools/memory/` |
| - | `agent/skills/browser/` |

### Gateway

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/gateway/` | `app/src/main/java/com/xiaomo/androidforclaw/gateway/` |
| `src/gateway/gateway-server.ts` | `gateway/GatewayServer.kt` |
| `src/gateway/gateway-service.ts` | `gateway/GatewayService.kt` |
| `src/gateway/session-router.ts` | - |
| `src/gateway/websocket-server.ts` | - |
| `src/gateway/methods/` | `gateway/methods/SessionMethods.kt` |
| - | `gateway/MainEntryAgentHandler.kt` |

### Config

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/config/` | `app/src/main/java/com/xiaomo/androidforclaw/config/` |
| `src/config/models-config.ts` | `config/ModelConfig.kt` |
| `src/config/openclaw-config.ts` | - |
| `src/config/config-loader.ts` | `config/ConfigLoader.kt` |
| `~/.openclaw/openclaw.json` | `/sdcard/.androidforclaw/config/openclaw.json` |
| `~/.openclaw/config/models.json` | `/sdcard/.androidforclaw/config/models.json` |

### Memory

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/memory/` | `app/src/main/java/com/xiaomo/androidforclaw/agent/memory/` |
| `src/memory/memory-manager.ts` | `agent/memory/MemoryManager.kt` |
| `src/memory/memory-store.ts` | - |
| `src/memory/semantic-search.ts` | - |

### Sessions

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/sessions/` | `app/src/main/java/com/xiaomo/androidforclaw/session/` |
| `src/sessions/` | `app/src/main/java/com/xiaomo/androidforclaw/agent/session/` |
| `src/sessions/session-manager.ts` | `session/JsonlSessionStorage.kt` |
| `src/sessions/session-manager.ts` | `agent/session/SessionManager.kt` |
| `src/sessions/session-store.ts` | `session/JsonlSessionStorage.kt` |
| - | `gateway/methods/SessionMethods.kt` |

### Channels

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/channels/` | `extensions/` + `app/.../channel/` |
| - | `channel/ChannelDefinition.kt` |
| - | `channel/ChannelManager.kt` |
| `src/channels/feishu/` | `extensions/feishu/` |
| `src/channels/discord/` | `extensions/discord/` |
| `src/channels/telegram/` | - |
| `src/channels/slack/` | - |
| `src/channels/whatsapp/` | - |
| `src/channels/signal/` | - |

### Providers

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/providers/` | `app/src/main/java/com/xiaomo/androidforclaw/providers/` |
| `src/providers/llm-provider.ts` | `providers/UnifiedLLMProvider.kt` |
| `src/providers/anthropic.ts` | - |
| `src/providers/openai.ts` | - |

### CLI / Entry

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/cli/` | `app/src/main/java/com/xiaomo/androidforclaw/core/` |
| `src/cli/agent.ts` | `core/MainEntryNew.kt` |
| `src/entry.ts` | `core/MyApplication.kt` |
| `openclaw.mjs` | - |
| - | `core/AgentMessageReceiver.kt` |
| - | `core/MessageQueueManager.kt` |
| - | `core/KeyedAsyncQueue.kt` |
| - | `core/ForegroundService.kt` |
| - | `core/AutoTestConfig.kt` |

### Utils

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/utils/` | `app/src/main/java/com/xiaomo/androidforclaw/util/` |
| `src/utils.ts` | `util/` (多个文件) |
| `src/logger.ts` | - (使用 Android Log) |

---

## 🛠️ Tools 文件映射

### 基础 Tool 接口

| OpenClaw | AndroidForClaw |
|----------|----------------|
| - | `agent/tools/Tool.kt` |
| - | `agent/tools/Skill.kt` |

### 文件操作

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/commands/read-file.ts` | `agent/tools/ReadFileTool.kt` |
| `src/commands/write-file.ts` | `agent/tools/WriteFileTool.kt` |
| `src/commands/edit-file.ts` | `agent/tools/EditFileTool.kt` |
| `src/commands/list-dir.ts` | `agent/tools/ListDirTool.kt` |
| `src/commands/delete-file.ts` | - |
| `src/commands/move-file.ts` | - |

### Shell 执行

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/commands/exec.ts` | `agent/tools/ExecTool.kt` |

### 浏览器

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/browser/` | `extensions/BrowserForClaw/` |
| `src/browser/browser-control.ts` | `browser/BrowserToolClient.kt` |
| `src/browser/playwright-wrapper.ts` | - |

### 网络

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/commands/web-fetch.ts` | `agent/tools/WebFetchTool.kt` |

### 记忆

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/commands/memory-search.ts` | `agent/tools/MemorySearchTool.kt` |
| `src/commands/memory-get.ts` | `agent/tools/MemoryGetTool.kt` |
| `src/commands/memory-upsert.ts` | - |

### JavaScript 执行

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/commands/javascript.ts` | `agent/tools/JavaScriptTool.kt` |
| - | `agent/tools/JavaScriptExecutorTool.kt` |

### 系统工具

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/commands/wait.ts` | `agent/tools/WaitSkill.kt` |
| `src/commands/stop.ts` | `agent/tools/StopSkill.kt` |
| `src/commands/log.ts` | `agent/tools/LogSkill.kt` |

### Android 特有工具 (无 OpenClaw 对应)

| AndroidForClaw | 说明 |
|----------------|------|
| `agent/tools/ScreenshotSkill.kt` | 截图 |
| `agent/tools/GetViewTreeSkill.kt` | UI 树 |
| `agent/tools/TapSkill.kt` | 点击 |
| `agent/tools/SwipeSkill.kt` | 滑动 |
| `agent/tools/TypeSkill.kt` | 输入 |
| `agent/tools/LongPressSkill.kt` | 长按 |
| `agent/tools/HomeSkill.kt` | Home 键 |
| `agent/tools/BackSkill.kt` | 返回键 |
| `agent/tools/OpenAppSkill.kt` | 打开应用 |
| `agent/tools/ListInstalledAppsSkill.kt` | 列出应用 |
| `agent/tools/StartActivityTool.kt` | 启动 Activity |

---

## 🧩 Skills 目录映射

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `skills/` | `app/src/main/assets/skills/` |
| `skills/core/` | `assets/skills/core/` |
| `skills/browser/` | - |
| `skills/coding/` | - |
| `~/.openclaw/workspace/skills/` | `/sdcard/.androidforclaw/workspace/skills/` |
| `~/.openclaw/.skills/` | `/sdcard/.androidforclaw/.skills/` |

---

## 📦 数据模型映射

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `src/agents/agent-state.ts` | `data/model/TaskData.kt` |
| - | `data/model/TaskDataManager.kt` |
| `src/sessions/session-data.ts` | - |
| `src/types/` | `data/model/` |

### Data Layer

| OpenClaw | AndroidForClaw |
|----------|----------------|
| - | `data/repository/` |
| - | `data/repository/DifyRepository.kt` |
| - | `data/repository/FeishuRepository.kt` |
| - | `data/repository/LLMRepository.kt` |
| - | `data/network/NetworkProvider.kt` |

---

## 🗄️ 存储映射

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `~/.openclaw/` | `/sdcard/.androidforclaw/` |
| `~/.openclaw/workspace/` | `/sdcard/.androidforclaw/workspace/` |
| `~/.openclaw/config/` | `/sdcard/.androidforclaw/config/` |
| `~/.openclaw/agents/main/sessions/` | `/sdcard/.androidforclaw/agents/main/sessions/` |
| `~/.openclaw/memory/` | `/sdcard/.androidforclaw/workspace/memory/` |

---

## 🎨 UI / Service 映射 (Android 特有)

### Services

| AndroidForClaw | 说明 |
|----------------|------|
| `service/PhoneAccessibilityService.kt` | 无障碍服务 |
| `service/WebService.kt` | Web 服务 |

### Accessibility

| AndroidForClaw | 说明 |
|----------------|------|
| `accessibility/AccessibilityProxy.kt` | 无障碍代理 |
| `accessibility/AccessibilityHealthMonitor.kt` | 健康监控 |

### UI 层

| AndroidForClaw | 说明 |
|----------------|------|
| `ui/activity/` | Activity 层 |
| `ui/adapter/` | Adapter 层 |
| `ui/compose/` | Compose UI |
| `ui/float/` | 悬浮窗 |
| `ui/session/` | 会话 UI |
| `ui/view/` | 视图层 |
| `ui/viewmodel/` | ViewModel 层 |

### Extensions

| AndroidForClaw | 说明 |
|----------------|------|
| `extensions/feishu/` | 飞书集成 |
| `extensions/discord/` | Discord 集成 |
| `extensions/observer/` | Observer APK |
| `extensions/BrowserForClaw/` | BClaw 浏览器 |

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `test/` | `app/src/test/java/com/xiaomo/androidforclaw/` |
| `test/unit/` | `app/src/test/` |
| `test/e2e/` | `app/src/androidTest/` |
| - | `app/src/androidTest/integration/` |
| - | `app/src/androidTest/ui/` |

---

## 📄 配置文件映射

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `package.json` | `app/build.gradle` |
| `package.json` | `settings.gradle` |
| `package.json` | `build.gradle` (root) |
| `tsconfig.json` | - |
| `.env.example` | - |
| `docker-compose.yml` | - |

### 文档

| OpenClaw | AndroidForClaw |
|----------|----------------|
| `README.md` | `README.md` |
| `AGENTS.md` (= CLAUDE.md) | `CLAUDE.md` |
| `CONTRIBUTING.md` | `CONTRIBUTING.md` |
| `SECURITY.md` | `SECURITY.md` |
| `VISION.md` | - |
| - | `ARCHITECTURE.md` |
| - | `REQUIREMENTS.md` |
| - | `MAPPING.md` |
| - | `README_CN.md` |
| - | `INSTALL.md` |
| - | `CODE_OF_CONDUCT.md` |

---

## 🚫 OpenClaw 有但 AndroidForClaw 缺失的重要组件

### 协议与集成

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/acp/` | ACP (Agent Client Protocol) | P1 | - |
| `src/plugin-sdk/` | 插件 SDK | P2 | - |
| `src/hooks/` | 生命周期钩子 | P2 | - |

### 系统功能

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/cron/` | 定时任务 | P1 | - |
| `src/daemon/` | 守护进程 | P1 | - (Android Service 替代) |
| `src/wizard/` | 配置向导 | P2 | - |
| `src/pairing/` | 设备配对 | P2 | - |
| `src/security/` | 安全管理 | P1 | - |
| `src/secrets/` | 密钥管理 | P1 | - |

### 媒体与理解

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/media/` | 媒体处理 | P2 | - |
| `src/media-understanding/` | 媒体理解 | P2 | - |
| `src/link-understanding/` | 链接理解 | P3 | - |
| `src/tts/` | 文字转语音 | P2 | - |

### 基础设施

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/infra/` | 基础设施工具 | P1 | `util/` (部分) |
| `src/logging/` | 日志系统 | P1 | - (Android Log) |
| `src/routing/` | 路由系统 | P1 | - |
| `src/process/` | 进程管理 | P2 | - |

### 渠道 (未实现)

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/telegram/` | Telegram 集成 | P2 | - |
| `src/slack/` | Slack 集成 | P2 | - |
| `src/whatsapp/` | WhatsApp 集成 | P2 | - |
| `src/signal/` | Signal 集成 | P3 | - |
| `src/imessage/` | iMessage 集成 | P3 | - |
| `src/line/` | LINE 集成 | P3 | - |
| `src/web/` | Web 集成 | P2 | - |

### UI 与交互

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/tui/` | 终端 UI | P3 | - (Android 不适用) |
| `src/terminal/` | 终端集成 | P3 | - (Android 不适用) |
| `src/canvas-host/` | Canvas 主机 | P3 | - |

### 开发工具

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/test-helpers/` | 测试辅助 | P2 | `app/src/test/` (部分) |
| `src/test-utils/` | 测试工具 | P2 | - |
| `src/compat/` | 兼容层 | P3 | - |

### 其他

| OpenClaw | 说明 | 优先级 | AClaw 对应 |
|----------|------|--------|-----------|
| `src/auto-reply/` | 自动回复 | P2 | - |
| `src/markdown/` | Markdown 处理 | P2 | - |
| `src/i18n/` | 国际化 | P3 | - |
| `src/node-host/` | Node 主机 | P3 | - (Android 不适用) |

### 优先级说明

- **P0**: 核心功能,必须实现
- **P1**: 重要功能,强烈建议实现
- **P2**: 有用功能,可选实现
- **P3**: 低优先级或平台不适用

---

## 🔍 快速查找

### 从 OpenClaw 找 AndroidForClaw

**示例 1**: `src/agents/run-agent-loop.ts`
- → `app/src/main/java/com/xiaomo/androidforclaw/agent/loop/AgentLoop.kt`

**示例 2**: `src/config/models-config.ts`
- → `app/src/main/java/com/xiaomo/androidforclaw/config/ModelConfig.kt`

**示例 3**: `src/commands/read-file.ts`
- → `app/src/main/java/com/xiaomo/androidforclaw/agent/tools/ReadFileSkill.kt`

### 从 AndroidForClaw 找 OpenClaw

**示例 1**: `agent/loop/AgentLoop.kt`
- → `src/agents/run-agent-loop.ts`

**示例 2**: `config/ConfigLoader.kt`
- → `src/config/config-loader.ts`

**示例 3**: `agent/tools/ScreenshotSkill.kt`
- → 无对应 (Android 特有)

---

## 📝 符号说明

| 符号 | 含义 |
|------|------|
| `path/to/file` | 文件路径 |
| `path/to/dir/` | 目录路径 |
| `-` | 无对应实现 |

---

**最后更新**: 2026-03-08

---

## 📝 最近更新

### 2026-03-08: 工具循环检测系统

**实现**: `agent/loop/ToolLoopDetection.kt`

参考 OpenClaw 的 `src/agents/tool-loop-detection.ts` 实现了完整的工具循环检测机制:

#### 功能特性
- ✅ 4 种检测器:
  - `generic_repeat` - 通用重复调用检测
  - `known_poll_no_progress` - 轮询工具无进展检测
  - `ping_pong` - 两工具来回调用检测
  - `global_circuit_breaker` - 全局断路器 (严重循环)

- ✅ 工具调用历史追踪:
  - Sliding window (30 条记录)
  - 记录工具名称、参数哈希、结果哈希
  - 时间戳和 toolCallId

- ✅ 无进展检测:
  - 通过结果哈希判断相同输入是否产生相同输出
  - 连续 N 次无进展触发警告/中断

- ✅ 分级处理:
  - **Warning** (10 次): 注入警告消息,跳过本次调用
  - **Critical** (20 次): 中断 AgentLoop 执行
  - **Circuit Breaker** (30 次): 强制停止会话

#### 集成点
- `AgentLoop.kt`: 在工具执行前检测循环,执行后记录结果
- `ProgressUpdate.LoopDetected`: 新增进度事件类型
- `MainEntryNew.kt`: 处理循环检测事件并更新 UI
- `MainEntryAgentHandler.kt`: 转发循环检测到 Gateway
- `AgentMethods.kt`: 广播循环检测事件给客户端

#### 测试结果
测试 case: "截个图发给我"
- ✅ 权限错误后不再重试,直接给出合理提示
- ✅ 2 次迭代完成任务 (无任务偏离)
- ✅ 避免了之前的问题: 成功截图后搜索配置文件

#### 技术细节
1. **哈希算法**: SHA-256 + 稳定序列化 (排序 Map keys)
2. **历史管理**: ArrayDeque 实现固定大小滑动窗口
3. **警告去重**: reportedWarnings Set 避免重复警告
4. **已知轮询工具**: wait, wait_for_element, command_status

对齐度: 95% (核心逻辑完全对齐,Android 平台特定优化)
