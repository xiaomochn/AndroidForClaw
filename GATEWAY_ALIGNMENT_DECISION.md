# AndroidForClaw Gateway 对齐决策

基于项目定位和实际需求,明确哪些应该对齐 OpenClaw,哪些可以不对齐。

---

## 🎯 项目定位回顾

**CLAUDE.md 明确定义:**
> **项目定位**: OpenClaw 是大脑,androidforclaw 是手机执行器。
>
> **核心目的**: 赋予 AI 使用 Android 手机的能力 - 观察屏幕、UI 交互、执行任务、数据处理等。

**重要原则:**
> 当前 Android 项目功能和底层逻辑要尽量向 OpenClaw 对齐, 实施的时候不要偷懒

**对齐指南:**
- ✅ **核心架构**: Agent Loop, Skills System, Tools Registry 必须对齐
- ✅ **配置系统**: openclaw.json 格式必须一致
- ✅ **Skills 格式**: AgentSkills.io 兼容格式
- ⚠️ **Android 特有**: Accessibility, MediaProjection 等合理差异

---

## 🔍 使用场景分析

### 场景 1: 独立使用 (当前主要场景)

```
用户 → AndroidForClaw App
     → 悬浮窗控制
     → Agent 执行
     → Android Tools
```

**需求:**
- 悬浮窗 UI
- 本地 Agent 执行
- Android 设备控制
- ❌ 不需要 OpenClaw 通信

### 场景 2: OpenClaw 远程控制 (Gateway 目标)

```
OpenClaw Desktop → WebSocket → AndroidForClaw Gateway
                              → Agent 执行
                              → Android Tools
                              → 返回结果
```

**需求:**
- ✅ Protocol 兼容 (关键!)
- ✅ Agent/Session Methods 对齐
- ✅ Event System 对齐
- ❌ 不需要所有 100 个 Methods

### 场景 3: Multi-Channel (未来可能)

```
WhatsApp/Telegram → OpenClaw → AndroidForClaw Gateway
                              → Android Tools
```

**需求:**
- ✅ Protocol 兼容
- ✅ Session 管理对齐
- ❌ 不需要 Channel 代码

---

## ✅ 必须对齐的部分

### 1. Protocol 层 (必须 100% 对齐) 🔴 高优先级

**原因:** 这是与 OpenClaw 通信的基础!

**当前状态:** ❌ 不兼容 (仅 40% 对齐)

**必须修改:**

```kotlin
// ❌ 错误 (当前)
data class RequestFrame(
    override val type: String = "request",  // 错误!
    val id: String,
    val method: String,
    val params: Map<String, Any?>? = null
)

data class ResponseFrame(
    override val type: String = "response", // 错误!
    val id: String?,
    val result: Any? = null                 // 错误!
)

// ✅ 正确 (必须改为)
data class RequestFrame(
    override val type: String = "req",      // 对齐 OpenClaw
    val id: String,
    val method: String,
    val params: Any? = null
)

data class ResponseFrame(
    override val type: String = "res",      // 对齐 OpenClaw
    val id: String,
    val ok: Boolean,                        // 新增
    val payload: Any? = null,               // 改名
    val error: ErrorShape? = null           // 完整结构
)

data class EventFrame(
    override val type: String = "event",
    val event: String,
    val payload: Any? = null,               // 改名
    val seq: Long? = null,                  // 新增
    val stateVersion: String? = null        // 新增
)

data class HelloOkFrame(
    override val type: String = "hello-ok", // 新增
    val protocol: Int,
    val server: ServerInfo,
    val features: Features,
    val snapshot: Snapshot,
    val policy: Policy
)

data class ErrorShape(
    val code: String,                       // 新增
    val message: String,
    val details: Any? = null,
    val retryable: Boolean? = null,         // 新增
    val retryAfterMs: Long? = null          // 新增
)
```

**影响范围:**
- `ProtocolTypes.kt` - 完全重写
- `FrameSerializer.kt` - 更新序列化
- `GatewayWebSocketServer.kt` - 更新 Hello 逻辑
- 所有 Methods - 返回格式改为 `{ok, payload}` 结构

**工作量:** 2-3 天
**收益:** OpenClaw 可以直接连接和控制

---

### 2. 核心 Agent Methods (必须对齐) 🟡 中优先级

**原因:** OpenClaw 需要通过这些方法控制 Android Agent

**必须保留的 Methods:**

```kotlin
// ✅ 已对齐
"agent"              → agent()
"agent.wait"         → agentWait()

// ⚠️ 需要修改命名
"agent.identity"     → "agent.identity.get"  // 改名对齐 OpenClaw
```

**必须对齐的行为:**
- ✅ 异步执行 (已实现)
- ✅ runId 机制 (已实现)
- ✅ timeout 支持 (已实现)
- ⏳ 返回格式改为 `{ok: true, payload: {...}}`

**工作量:** 1 天
**收益:** OpenClaw 可以执行 Android Agent

---

### 3. 核心 Session Methods (必须对齐) 🟡 中优先级

**原因:** Session 管理是核心功能,必须与 OpenClaw 兼容

**必须保留的 Methods:**

```kotlin
// ✅ 已对齐
"sessions.list"      → sessionsList()
"sessions.preview"   → sessionsPreview()
"sessions.patch"     → sessionsPatch()
"sessions.reset"     → sessionsReset()
"sessions.delete"    → sessionsDelete()
```

**可选的 Method:**
```kotlin
"sessions.compact"   → 可选实现 (OpenClaw 有,我们可以加)
```

**必须对齐的行为:**
- ✅ JSONL 存储 (已对齐)
- ✅ Session 结构 (已对齐)
- ⏳ 返回格式改为 `{ok: true, payload: {...}}`

**工作量:** 1 天
**收益:** Session 数据与 OpenClaw 互通

---

### 4. 核心 Events (必须对齐) 🟡 中优先级

**原因:** OpenClaw 需要接收 Agent 执行状态

**必须保留的 Events:**

```kotlin
// ✅ 已实现
"agent"              → agent.start/complete/error

// ⏳ 待实现 (如果 AgentLoop 支持)
"agent.iteration"    → agent 迭代进度
"agent.tool_call"    → 工具调用
"agent.tool_result"  → 工具结果
```

**可以忽略的 Events:**
```kotlin
"tick"               → OpenClaw 心跳 (不需要)
"cron"               → 定时任务 (不需要)
"node.pair.*"        → 节点配对 (不需要)
"device.pair.*"      → 设备配对 (不需要)
"exec.approval.*"    → 执行审批 (不需要)
```

**必须对齐的格式:**
```kotlin
// 改为
{
  "type": "event",
  "event": "agent",
  "payload": {...},      // 改名
  "seq": 123             // 新增序列号
}
```

**工作量:** 1 天
**收益:** OpenClaw 实时监控 Android Agent

---

### 5. 配置系统 (已对齐) ✅

**原因:** CLAUDE.md 明确要求

**当前状态:** ✅ 已对齐
- 使用 `openclaw.json` 格式
- 包含 Gateway 配置
- 包含 Models 配置

**无需改动**

---

## ⚪ 可以不对齐的部分

### 1. 扩展 Methods (70+ 个) - 不需要

**原因:** Android 执行器不需要这些控制平面功能

**OpenClaw 独有 Methods (不需要实现):**

```typescript
// Config Management (5)
"config.get"
"config.set"
"config.apply"
"config.patch"
"config.schema"

// Skills Management (4)
"skills.status"
"skills.bins"
"skills.install"
"skills.update"

// Cron Jobs (7)
"cron.list"
"cron.status"
"cron.add"
"cron.update"
"cron.remove"
"cron.run"
"cron.runs"

// Node/Device Pairing (13)
"node.pair.*"
"node.invoke.*"
"device.pair.*"
"device.token.*"

// TTS (6)
"tts.status"
"tts.providers"
"tts.enable"
"tts.disable"
"tts.convert"
"tts.setProvider"

// Exec Approvals (7)
"exec.approvals.*"
"exec.approval.*"

// Wizard (4)
"wizard.start"
"wizard.next"
"wizard.cancel"
"wizard.status"

// Channels (2)
"channels.status"
"channels.logout"

// ... 其他 40+ Methods
```

**说明:** 这些是 OpenClaw Desktop 的控制功能,Android 执行器不需要。

---

### 2. 扩展 Events (12 个) - 不需要

**OpenClaw 独有 Events (不需要实现):**

```typescript
"tick"                      // 心跳
"cron"                      // 定时任务
"node.pair.requested"       // 节点配对
"node.pair.resolved"
"node.invoke.request"
"device.pair.requested"     // 设备配对
"device.pair.resolved"
"voicewake.changed"         // 语音唤醒
"exec.approval.requested"   // 执行审批
"exec.approval.resolved"
"update.available"          // 更新通知
"connect.challenge"         // 连接挑战
```

**说明:** 这些是 OpenClaw 生态的高级功能,Android 不需要。

---

### 3. Multi-Channel Router - 不需要

**OpenClaw 架构:**
```
Channel Router (统一管理)
├── WhatsApp
├── Telegram
├── Discord
├── Slack
└── WebChat
```

**AndroidForClaw 架构:**
```
独立模块 (各自运行)
├── Discord (独立扩展)
├── Feishu (独立扩展)
└── WebSocket (Gateway 直连)
```

**说明:**
- Android 不需要作为多渠道路由器
- 保持简单架构
- 各 Channel 独立运行即可

---

### 4. Web Dashboard - 不需要

**OpenClaw:** React 完整 Dashboard
**AndroidForClaw:** 简单 HTML Homepage + 悬浮窗

**说明:**
- Android 资源有限
- 悬浮窗已足够
- 简单 HTML 状态页即可

---

### 5. ACP Bridge - 不需要

**OpenClaw:** 与 VSCode 深度集成
**AndroidForClaw:** 无 IDE 集成需求

**说明:** Android 设备不需要 IDE 集成

---

## 📊 对齐决策总结

### 必须对齐 (Protocol + 核心功能)

| 组件 | 当前状态 | 必须改 | 优先级 | 工作量 |
|------|----------|--------|--------|--------|
| **Protocol 层** | ❌ 40% | ✅ 是 | 🔴 最高 | 2-3天 |
| Frame types | ❌ 错误 | ✅ 是 | 🔴 | 1天 |
| Frame 字段 | ❌ 不全 | ✅ 是 | 🔴 | 1天 |
| Hello 机制 | ❌ 不同 | ✅ 是 | 🔴 | 1天 |
| **Agent Methods** | 🟡 75% | ⚠️ 小改 | 🟡 高 | 1天 |
| agent() | ✅ | ⚠️ 格式 | 🟡 | 0.5天 |
| agent.wait() | ✅ | ⚠️ 格式 | 🟡 | 0.5天 |
| agent.identity | ⚠️ 命名 | ✅ 改名 | 🟡 | 0.5天 |
| **Session Methods** | 🟢 83% | ⚠️ 格式 | 🟡 高 | 1天 |
| sessions.* | ✅ | ⚠️ 格式 | 🟡 | 1天 |
| **Events** | 🟡 60% | ⚠️ 格式 | 🟡 中 | 1天 |
| agent events | ✅ | ⚠️ 格式 | 🟡 | 1天 |
| **Total** | **60%** | | | **5-7天** |

### 可以不对齐 (扩展功能)

| 组件 | OpenClaw | AndroidForClaw | 决策 | 原因 |
|------|----------|----------------|------|------|
| 扩展 Methods | 70+ | 11 | ✅ 不对齐 | 不需要控制功能 |
| 扩展 Events | 12+ | 3 | ✅ 不对齐 | 不需要生态事件 |
| Channel Router | ✅ | ❌ | ✅ 不对齐 | 独立模块架构 |
| Web Dashboard | React | HTML | ✅ 不对齐 | 资源限制 |
| ACP Bridge | ✅ | ❌ | ✅ 不对齐 | 无 IDE 需求 |
| Config API | ✅ | ❌ | ✅ 不对齐 | 文件配置足够 |
| Skills API | ✅ | ❌ | ✅ 不对齐 | 文件管理足够 |
| Cron | ✅ | ❌ | ✅ 不对齐 | 不需要定时任务 |
| Node/Device Pair | ✅ | ❌ | ✅ 不对齐 | 不需要配对 |
| TTS | ✅ | ❌ | ✅ 不对齐 | Android TTS 独立 |

---

## 🎯 最终目标

### 修正后的对齐目标

**核心对齐 (必须):**
- Protocol: 40% → **95%** ✅
- Agent Methods: 75% → **90%** ✅
- Session Methods: 83% → **90%** ✅
- Events: 60% → **80%** ✅

**扩展功能 (不需要):**
- 扩展 Methods: 11% → **保持 11%** ✅ (刻意不对齐)
- 扩展 Events: 18% → **保持 18%** ✅ (刻意不对齐)
- Channel Router: 0% → **保持 0%** ✅ (刻意不对齐)

**总体评估:**
- **通信兼容性**: 60% → **95%** (关键提升!)
- **功能覆盖率**: 11% → **保持 11%** (合理简化)
- **架构对齐度**: 75% → **90%** (核心对齐)

---

## 📋 修正清单

### Phase 1: Protocol 对齐 (2-3天) 🔴 最高优先级

- [ ] 修改 RequestFrame type: `"request"` → `"req"`
- [ ] 修改 ResponseFrame type: `"response"` → `"res"`
- [ ] ResponseFrame 添加 `ok: Boolean` 字段
- [ ] ResponseFrame 改名: `result` → `payload`
- [ ] EventFrame 改名: `data` → `payload`
- [ ] EventFrame 添加 `seq: Long?` 字段
- [ ] 创建 HelloOkFrame 类型
- [ ] 创建 ErrorShape 完整结构
- [ ] 更新 FrameSerializer
- [ ] 更新 GatewayWebSocketServer Hello 逻辑
- [ ] 更新所有 Methods 返回格式

### Phase 2: Methods 对齐 (1天) 🟡 高优先级

- [ ] 改名: `agent.identity` → `agent.identity.get`
- [ ] 更新所有 Methods 返回为 `{ok, payload}` 格式
- [ ] 测试与 OpenClaw 客户端通信

### Phase 3: Events 对齐 (1天) 🟡 中优先级

- [ ] 更新 Event 格式为 `{event, payload, seq}`
- [ ] 添加序列号机制
- [ ] 测试 Event 接收

### Phase 4: 测试验证 (1天) 🟢 低优先级

- [ ] 编写 Python 测试客户端
- [ ] 测试完整 RPC 流程
- [ ] 测试 Event 接收
- [ ] 文档更新

---

## 🎯 结论

### 必须对齐 ✅

**Protocol 层 + 核心 Methods/Events** - 这是与 OpenClaw 通信的基础

**原因:**
1. **项目定位** - "OpenClaw 是大脑,androidforclaw 是手机执行器"
2. **使用场景** - OpenClaw 需要远程控制 Android
3. **架构原则** - CLAUDE.md 明确要求对齐
4. **兼容性** - 必须能够与 OpenClaw 通信

**投入:** 5-7 天开发 + 测试
**收益:** OpenClaw 可以直接控制 Android 设备

### 可以不对齐 ✅

**70+ 扩展 Methods + 扩展功能** - 控制平面功能

**原因:**
1. **定位差异** - Android 是执行器,不是控制中心
2. **资源限制** - 移动设备资源有限
3. **架构简化** - 保持轻量级
4. **实用主义** - 实现不需要的功能是浪费

**决策:** 保持 11 个核心 Methods,不实现扩展功能

---

**下一步:** 立即开始 Phase 1 - Protocol 对齐!

---

生成时间: 2026-03-08
决策依据: CLAUDE.md + 项目定位 + OpenClaw 源码分析
