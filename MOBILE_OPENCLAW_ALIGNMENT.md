# 手机版 OpenClaw 对齐策略

## 🎯 目标重新定位

**之前理解(错误):** AndroidForClaw 是 OpenClaw 的远程执行器
**实际目标(正确):** **AndroidForClaw 是手机版的 OpenClaw**

### 项目定位

```
OpenClaw Desktop               AndroidForClaw (手机版 OpenClaw)
├── Gateway (18789)            ├── Gateway (8765)
├── Multi-Channel              ├── Multi-Channel (适配移动)
├── Agent Runtime              ├── Agent Runtime (完全对齐)
├── Skills System              ├── Skills System (完全对齐)
├── Tools (Desktop)            ├── Tools (Android 特化)
└── Web UI                     └── Mobile UI (悬浮窗)
```

**核心理念:** 在 Android 上实现 OpenClaw 的**完整功能**,不是子集!

---

## 📋 全面对齐清单

### ✅ 必须完全对齐

#### 1. Protocol 层 (100% 对齐) 🔴

**当前:** 40% (不兼容)
**目标:** 100% (完全一致)

**必须改:**
```kotlin
// Frame Types
"request" → "req"
"response" → "res"

// Frame 结构
ResponseFrame {
  ok: Boolean          // 新增
  payload: Any?        // 改名 (原 result)
  error: ErrorShape    // 完整结构
}

EventFrame {
  payload: Any?        // 改名 (原 data)
  seq: Long?           // 新增序列号
  stateVersion: String? // 新增
}

// Hello 机制
HelloOkFrame {
  type: "hello-ok"
  protocol: Int
  server: {...}
  features: {...}
  snapshot: {...}
  policy: {...}
}

// Error 格式
ErrorShape {
  code: String         // 新增
  message: String
  details: Any?
  retryable: Boolean?  // 新增
  retryAfterMs: Long?  // 新增
}
```

**为什么:** 手机版 OpenClaw 需要与桌面版、其他客户端完全兼容

---

#### 2. Gateway Methods (逐步对齐) 🟡

**当前:** 11/100 (11%)
**目标:** 至少 40-50 个核心方法

##### Phase 1: 核心 Agent/Session (已完成) ✅
```kotlin
// Agent (4)
"agent"
"agent.wait"
"agent.identity.get"        // 需要改名
"send"                      // 待实现

// Session (6)
"sessions.list"
"sessions.preview"
"sessions.patch"
"sessions.reset"
"sessions.delete"
"sessions.compact"          // 待实现

// Health (3)
"health"
"status"
"ping"                      // 待实现
```

##### Phase 2: Models & Tools (高优先级) 🔴
```kotlin
// Models (1)
"models.list"               // 必须实现

// Tools (3)
"tools.catalog"             // 必须实现
"tools.list"                // 可选
"tools.get"                 // 可选
```

**为什么:** 用户需要查看和切换模型、查看可用工具

##### Phase 3: Skills (高优先级) 🔴
```kotlin
// Skills (4)
"skills.status"             // 必须实现
"skills.bins"               // 可选
"skills.install"            // 必须实现
"skills.update"             // 必须实现
```

**为什么:** Skills 管理是 OpenClaw 核心功能

##### Phase 4: Agents 管理 (中优先级) 🟡
```kotlin
// Agents (7)
"agents.list"               // 必须实现
"agents.create"             // 必须实现
"agents.update"             // 必须实现
"agents.delete"             // 必须实现
"agents.files.list"         // 可选
"agents.files.get"          // 可选
"agents.files.set"          // 可选
```

**为什么:** 多 Agent 管理是重要特性

##### Phase 5: Config (中优先级) 🟡
```kotlin
// Config (5)
"config.get"                // 必须实现
"config.set"                // 必须实现
"config.apply"              // 可选
"config.patch"              // 可选
"config.schema"             // 可选
```

**为什么:** 动态配置是基础功能

##### Phase 6: Channels (适配移动) 🟡
```kotlin
// Channels (2)
"channels.status"           // 必须实现
"channels.logout"           // 可选

// 移动端特有 Channels
- Feishu (已有)
- Discord (已有)
- WhatsApp (未来)
- Telegram (未来)
```

**为什么:** 多渠道是 OpenClaw 核心功能

##### Phase 7: 其他扩展 (低优先级) ⚪
```kotlin
// Cron (可选)
"cron.list"
"cron.add"
"cron.remove"
...

// Node/Device Pairing (可能不需要)
"node.pair.*"
"device.pair.*"

// TTS (Android 原生支持,可能不需要)
"tts.*"

// Exec Approvals (可能需要)
"exec.approval.*"

// Wizard (可选)
"wizard.*"
```

---

#### 3. Gateway Events (完全对齐) 🟡

**当前:** 3/17 (18%)
**目标:** 至少 12-15 个

##### 必须实现的 Events

```kotlin
// Agent Events (必须)
"agent"                     // ✅ 已有
"agent.iteration"           // 待实现
"agent.tool_call"           // 待实现
"agent.tool_result"         // 待实现

// Chat Events (必须)
"chat"                      // 待实现

// System Events (必须)
"tick"                      // 待实现 (心跳)
"health"                    // 待实现
"shutdown"                  // 待实现

// Presence (可选)
"presence"                  // 待实现

// Heartbeat (可选)
"heartbeat"                 // 待实现
```

##### 可选的 Events (根据功能实现)

```kotlin
"cron"                      // 如果实现 cron
"voicewake.changed"         // 如果实现语音唤醒
"exec.approval.*"           // 如果实现审批
"update.available"          // 如果实现更新检查
```

---

#### 4. Session 管理 (已对齐 95%) ✅

**当前实现:**
- ✅ JSONL 存储
- ✅ Session CRUD
- ✅ Metadata 管理
- ✅ Compaction 机制
- ✅ Token 统计

**待完善:**
- ⏳ sessions.compact() 方法
- ⏳ Export/Import (可选)

---

#### 5. Skills 系统 (基本对齐) ✅

**当前实现:**
- ✅ Skills Loader
- ✅ 内置 Skills
- ✅ 工作区 Skills
- ✅ AgentSkills.io 格式

**待完善:**
- ⏳ Skills 热重载
- ⏳ Skills 版本管理
- ⏳ Skills RPC Methods

---

#### 6. Agent Runtime (完全对齐) ✅

**当前实现:**
- ✅ AgentLoop
- ✅ Tool Registry
- ✅ Skill Registry
- ✅ Context Builder
- ✅ Session Manager
- ✅ 异步执行

**完全符合 OpenClaw 架构!**

---

### ⚪ 可以差异化的部分 (Android 特色)

#### 1. Tools (平台差异) ✅

**OpenClaw Desktop Tools:**
- 文件系统操作
- 命令行执行
- 浏览器自动化 (puppeteer)
- 代码执行

**AndroidForClaw Mobile Tools:**
- ✅ Screenshot (MediaProjection)
- ✅ UI 交互 (Accessibility)
- ✅ App 控制
- ✅ 设备状态
- ✅ 系统设置

**说明:** 工具集不同,但架构对齐

---

#### 2. UI (平台差异) ✅

**OpenClaw Desktop:**
- React Web Dashboard
- Terminal UI
- WebChat

**AndroidForClaw Mobile:**
- ✅ 悬浮窗控制
- ✅ 简单 HTML Gateway UI
- ⏳ 移动友好的 WebChat

**说明:** UI 适配移动,但功能对齐

---

#### 3. Multi-Channel (架构差异) 🟡

**OpenClaw Desktop:**
```
统一 Channel Router
├── WhatsApp
├── Telegram
├── Discord
├── Slack
└── WebChat
```

**AndroidForClaw Mobile (当前):**
```
独立模块
├── Discord (独立)
├── Feishu (独立)
└── WebSocket (Gateway)
```

**建议:** 逐步统一为 Channel Router 架构

---

#### 4. 部署方式 (平台差异) ✅

**OpenClaw Desktop:**
- npm 安装
- 后台服务
- 端口 18789

**AndroidForClaw Mobile:**
- APK 安装
- Android Service
- 端口 8765

**说明:** 部署方式不同,但功能对齐

---

## 📊 对齐路线图

### Phase 1: Protocol 对齐 (2-3天) 🔴 最高优先级

**目标:** Protocol 100% 兼容

- [ ] Frame types: req/res/event
- [ ] Frame 字段: ok, payload, seq, stateVersion
- [ ] ErrorShape 完整结构
- [ ] HelloOkFrame 实现
- [ ] 所有 Methods 返回格式统一

**产出:** 与 OpenClaw 客户端完全兼容的 Protocol

---

### Phase 2: 核心 Methods 扩展 (3-5天) 🔴 高优先级

**目标:** 实现 30-40 个核心方法

##### Week 1: Models & Tools (1-2天)
- [ ] models.list
- [ ] tools.catalog
- [ ] skills.status

##### Week 2: Skills 管理 (1-2天)
- [ ] skills.install
- [ ] skills.update
- [ ] skills.reload

##### Week 3: Agents 管理 (1-2天)
- [ ] agents.list
- [ ] agents.create
- [ ] agents.update
- [ ] agents.delete

##### Week 4: Config 管理 (1天)
- [ ] config.get
- [ ] config.set
- [ ] config.reload

**产出:** 功能完整的手机版 OpenClaw

---

### Phase 3: Events 完善 (2-3天) 🟡 中优先级

**目标:** 实现 12-15 个核心事件

- [ ] agent.iteration
- [ ] agent.tool_call
- [ ] agent.tool_result
- [ ] chat
- [ ] tick
- [ ] health
- [ ] shutdown

**产出:** 实时状态同步

---

### Phase 4: Channel Router (3-5天) 🟡 中优先级

**目标:** 统一多渠道架构

- [ ] 创建 ChannelRouter
- [ ] 整合 Discord
- [ ] 整合 Feishu
- [ ] 添加 WebChat
- [ ] channels.status
- [ ] channels.logout

**产出:** 统一的多渠道管理

---

### Phase 5: 高级功能 (可选) ⚪ 低优先级

**根据需求实现:**
- [ ] Cron 定时任务
- [ ] Exec Approvals
- [ ] Voice Wake
- [ ] Update 管理
- [ ] Logs API
- [ ] Metrics

---

## 📊 最终目标对齐度

| 维度 | 当前 | 短期目标 (1月) | 长期目标 (3月) |
|------|------|----------------|----------------|
| **Protocol** | 40% | **100%** ✅ | 100% |
| **Methods** | 11/100 | **35/100** (35%) | **60/100** (60%) |
| **Events** | 3/17 | **12/17** (70%) | **15/17** (88%) |
| **Session** | 95% | **100%** ✅ | 100% |
| **Skills** | 80% | **95%** ✅ | 100% |
| **Agent Runtime** | 100% | 100% | 100% |
| **Channel Router** | 0% | **60%** | **90%** |
| **总体** | **60%** | **80%** | **90%** |

---

## 🎯 核心原则

### 1. 功能对齐 > 代码对齐

**目标:** 实现相同功能,而非复制代码
- ✅ 提供相同的 RPC Methods
- ✅ 发送相同格式的 Events
- ✅ 保持相同的数据结构
- ⚠️ 但实现可以是 Kotlin/Android 特有的

### 2. 移动优先 > 完全复制

**目标:** 适配移动平台特点
- ✅ 轻量级实现
- ✅ 省电优化
- ✅ 移动友好 UI
- ⚠️ 但核心功能不能少

### 3. 逐步对齐 > 一次全做

**目标:** 迭代式接近 OpenClaw
- Phase 1: Protocol (必须)
- Phase 2: 核心 Methods (重要)
- Phase 3: Events (重要)
- Phase 4: Channel Router (可选)
- Phase 5: 高级功能 (可选)

---

## 🚀 立即行动

### 当前最紧迫的任务 (下周)

**1. Protocol 对齐 (2-3天) 🔴**
- 重写 ProtocolTypes.kt
- 更新 FrameSerializer
- 实现 HelloOkFrame
- 测试兼容性

**2. 核心 Methods (2天) 🔴**
- models.list
- tools.catalog
- skills.status
- skills.install

**3. 测试验证 (1天) 🟡**
- Python 客户端测试
- 与桌面版对比验证

---

## 📝 总结

### 理解修正

**之前(错误):**
> AndroidForClaw 是 OpenClaw 的远程执行器
> - 只需要核心 11 个方法
> - 不需要完整功能
> - Protocol 可以简化

**现在(正确):**
> **AndroidForClaw 是手机版的 OpenClaw**
> - 需要大部分功能 (60-80%)
> - Protocol 必须 100% 兼容
> - 架构完全对齐
> - 只有 Tools 和 UI 因平台差异

### 对齐策略

**必须对齐 (100%):**
- ✅ Protocol 层
- ✅ Agent Runtime
- ✅ Skills System
- ✅ Session 管理
- ✅ 配置系统

**逐步对齐 (60-80%):**
- 🟡 Gateway Methods (35 → 60 个)
- 🟡 Gateway Events (12 → 15 个)
- 🟡 Channel Router

**合理差异:**
- ⚪ Tools (Android 特化)
- ⚪ UI (移动优化)
- ⚪ 部署方式

---

**下一步:** 立即开始 Protocol 100% 对齐!

---

生成时间: 2026-03-08
定位: 手机版 OpenClaw (不是执行器!)
目标: 在 Android 上实现 OpenClaw 的完整功能
