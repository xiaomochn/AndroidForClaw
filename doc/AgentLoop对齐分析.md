# AgentLoop 对齐分析: OpenClaw vs AndroidForClaw

**日期**: 2026-03-08
**对齐度**: 70% (核心逻辑对齐,外围支持功能差异较大)

---

## 🏗️ 架构差异

### OpenClaw
```
pi-embedded-runner/run.ts (1400+ 行)
    ↓
run/attempt.ts (1868 行) - 单次执行尝试
    ↓ 使用 SDK
@mariozechner/pi-coding-agent (createAgentSession)
    ↓ SDK 内部
Agent Loop 迭代逻辑 (LLM → Tools → Observation)
```

### AndroidForClaw
```
AgentLoop.kt (478 行) - 完整自实现
    ↓ 直接实现
LLM → Tool Call → Execution → Repeat
```

**关键差异**: OpenClaw 使用成熟的 SDK,AndroidForClaw 自己实现 loop 逻辑。

---

## ✅ 已对齐的功能

### 1. 核心循环逻辑
| 功能 | OpenClaw | AndroidForClaw | 对齐度 |
|------|----------|----------------|--------|
| LLM 推理 | ✅ SDK | ✅ UnifiedLLMProvider | 100% |
| Tool Calling | ✅ SDK | ✅ ToolRegistry + AndroidToolRegistry | 100% |
| 迭代执行 | ✅ SDK | ✅ while loop (max 40) | 100% |
| Extended Thinking | ✅ thinkLevel | ✅ reasoningEnabled | 100% |

### 2. 循环检测 (2026-03-08 新增)
| 功能 | OpenClaw | AndroidForClaw | 对齐度 |
|------|----------|----------------|--------|
| generic_repeat | ✅ | ✅ | 100% |
| known_poll_no_progress | ✅ | ✅ | 100% |
| ping_pong | ✅ | ✅ | 100% |
| global_circuit_breaker | ✅ | ✅ | 100% |
| 工具调用历史 | ✅ 30 条 | ✅ 30 条 | 100% |
| 结果哈希检测 | ✅ SHA-256 | ✅ SHA-256 | 100% |

### 3. 上下文管理
| 功能 | OpenClaw | AndroidForClaw | 对齐度 |
|------|----------|----------------|--------|
| Context Overflow 检测 | ✅ | ✅ ContextManager | 100% |
| Auto Compaction | ✅ 3 次尝试 | ✅ 3 次尝试 | 100% |
| Tool Result Truncation | ✅ | ✅ | 100% |

---

## ❌ 未对齐的功能

### 1. 外围重试机制 (OpenClaw 的 run.ts)

**OpenClaw 有完整的外层重试逻辑**:
```typescript
// run.ts: 外层循环 (MAX_RUN_LOOP_ITERATIONS)
while (true) {
  if (runLoopIterations >= MAX_RUN_LOOP_ITERATIONS) {
    return error("retry_limit");
  }
  runLoopIterations++;

  // 1. Auth profile rotation (多账号切换)
  // 2. Context overflow compaction (3 次)
  // 3. Tool result truncation
  // 4. Copilot token refresh
  // 5. Thinking level fallback
  // 6. Model failover

  const attempt = await runEmbeddedAttempt(...);

  // 处理各种错误并决定是否 retry
  if (contextOverflow) {
    // 尝试 compaction
    if (compacted) continue;  // retry
  }

  if (authFailure) {
    // 切换账号
    if (rotated) continue;  // retry
  }

  if (rateLimitFailure) {
    // Mark profile cooldown
    if (advanceAuthProfile()) continue;  // retry
  }

  // 成功 -> break
  break;
}
```

**AndroidForClaw 只有单层循环**:
```kotlin
// AgentLoop.kt: 单层循环
while (iteration < maxIterations && !shouldStop) {
  iteration++

  val response = llmProvider.chatWithTools(...)

  if (response.toolCalls != null) {
    // 执行工具
    for (toolCall in response.toolCalls) {
      val result = executeTool(toolCall)
      messages.add(toolResult)
    }
    continue  // 继续下一轮
  }

  // 无工具调用 -> 完成
  finalContent = response.content
  break
}
```

**影响**: AndroidForClaw 缺少:
- ❌ 多账号切换 (auth profile rotation)
- ❌ Model failover (模型降级/切换)
- ❌ Thinking level fallback (reasoning 降级)
- ❌ Rate limit cooldown (账号冷却)
- ❌ Copilot token refresh (特定 provider)

### 2. Auth Profile 管理

**OpenClaw**:
```typescript
// 多 auth profile 支持
const profileCandidates = [profile1, profile2, profile3];
let profileIndex = 0;

const advanceAuthProfile = async (): Promise<boolean> => {
  // 跳过 cooldown 的 profile
  while (nextIndex < profileCandidates.length) {
    if (isProfileInCooldown(candidate)) {
      nextIndex++;
      continue;
    }
    await applyApiKeyInfo(candidate);
    return true;
  }
  return false;
};

// Rate limit 后切换 profile
if (rateLimitFailure && await advanceAuthProfile()) {
  continue;  // retry
}
```

**AndroidForClaw**:
```kotlin
// 单一 API key 配置
val apiKey = config.get("OPENAI_API_KEY")
// 无 profile 切换机制
```

**影响**:
- ❌ 无法处理单账号 rate limit (只能失败)
- ❌ 无法利用多账号提高 quota 上限
- ❌ 无法实现账号冷却 (cooldown)

### 3. Model Failover

**OpenClaw**:
```typescript
// Model fallback 配置
{
  "fallbacks": {
    "model": ["claude-opus-4-6", "claude-sonnet-4-6", "gpt-5.3"]
  }
}

// 自动降级
throw new FailoverError(message, {
  reason: "rate_limit",
  provider,
  model
});
// -> Gateway 捕获并切换到 fallback model
```

**AndroidForClaw**:
```kotlin
// 无 model failover 机制
// 单一 model 配置
val model = config.get("model") ?: "claude-opus-4-6"
```

**影响**:
- ❌ 遇到 rate limit 直接失败
- ❌ 无法利用 fallback models 提高可用性

### 4. Session 持久化细节

**OpenClaw**:
```typescript
// SessionManager (JSONL 格式)
sessionManager.appendCustomEntry("openclaw:prompt-error", {
  error: errorText,
  timestamp: Date.now()
});

// Cache TTL timestamp
appendCacheTtlTimestamp(sessionManager, {
  timestamp: Date.now(),
  provider,
  modelId
});

// Branch and merge
sessionManager.branch(parentId);
sessionManager.resetLeaf();
```

**AndroidForClaw**:
```kotlin
// JsonlSessionStorage - 基础实现
val messages = mutableListOf<Message>()
// 无 custom entries
// 无 branch/merge
// 无 cache TTL tracking
```

**影响**:
- ❌ 无法追踪 prompt errors
- ❌ 无法利用 cache-ttl pruning
- ❌ 无法实现 branch/merge (多路对话)

### 5. Hooks 系统

**OpenClaw**:
```typescript
// 丰富的 hook 点
hookRunner.runBeforeModelResolve({ prompt });
hookRunner.runBeforeAgentStart({ prompt });
hookRunner.runBeforePromptBuild({ prompt, messages });
hookRunner.runLlmInput({ systemPrompt, prompt, historyMessages });
hookRunner.runLlmOutput({ response, usage });
hookRunner.runAfterToolResult({ toolName, result });
```

**AndroidForClaw**:
```kotlin
// 无 hooks 系统
// 所有逻辑硬编码在 AgentLoop 中
```

**影响**:
- ❌ 无法插件化扩展
- ❌ 无法实现 before/after 拦截
- ❌ 无法外部定制行为

### 6. Sandbox 支持

**OpenClaw**:
```typescript
// Sandbox context
const sandbox = resolveSandboxContext({
  cfg: params.config,
  agentId,
  sessionKey
});

// Isolated workspace
const effectiveWorkspace = sandbox?.enabled
  ? sandbox.workspaceDir
  : resolvedWorkspace;

// FS bridge for sandboxed access
sandbox?.fsBridge
```

**AndroidForClaw**:
```kotlin
// 无 sandbox 机制
// 工具直接访问文件系统
// 无隔离保护
```

**影响**:
- ❌ 安全风险 (agent 可访问所有文件)
- ❌ 无法隔离多个 agent 的工作空间
- ❌ 无法限制危险操作

### 7. 图片处理

**OpenClaw**:
```typescript
// 自动检测 prompt 中的图片引用
const imageResult = await detectAndLoadPromptImages({
  prompt,
  workspaceDir,
  model,
  existingImages,
  maxBytes: MAX_IMAGE_BYTES,
  maxDimensionPx,
  workspaceOnly,
  sandbox
});

// 自动修剪历史图片 (节省 token)
pruneProcessedHistoryImages(activeSession.messages);
```

**AndroidForClaw**:
```kotlin
// 无图片处理
// 只支持文本
```

**影响**:
- ❌ 无法处理 vision tasks
- ❌ 无法利用 Claude 的视觉能力

### 8. 流式输出优化

**OpenClaw**:
```typescript
// 多种 streaming 策略
params.onPartialReply  // 部分回复
params.onBlockReply    // 分块回复
params.onBlockReplyFlush  // 刷新块
params.blockReplyChunking  // 分块配置

// Reasoning streaming
params.onReasoningStream  // 思考过程流式输出
params.onReasoningEnd     // 思考完成
```

**AndroidForClaw**:
```kotlin
// 基础流式输出
_progressFlow.emit(ProgressUpdate.Reasoning(reasoning))
// 无细粒度控制
// 无分块优化
```

**影响**:
- ❌ 用户体验较差 (无实时反馈)
- ❌ 无法优化长文本输出

### 9. 诊断和监控

**OpenClaw**:
```typescript
// 详细的诊断日志
log.debug(`[context-diag] pre-prompt: ...`);
log.debug(`[compaction-diag] decision diagId=...`);
log.warn(`[context-overflow-diag] sessionKey=...`);

// Cache trace
cacheTrace?.recordStage("prompt:before", { ... });
cacheTrace?.recordStage("prompt:images", { ... });

// Anthropic payload logging
anthropicPayloadLogger.log(request, response);

// Session manager access tracking
trackSessionManagerAccess(sessionId);
```

**AndroidForClaw**:
```kotlin
// 基础日志
writeLog("✅ LLM 响应已收到")
writeLog("🔧 Function: $functionName")

// 无结构化诊断
// 无 cache trace
// 无 payload logging
```

**影响**:
- ❌ 难以调试复杂问题
- ❌ 无法分析性能瓶颈
- ❌ 无法追踪 API 调用

---

## 📊 功能对齐度总结

| 分类 | OpenClaw 功能数 | AndroidForClaw 已实现 | 对齐度 |
|------|----------------|----------------------|--------|
| 核心循环 | 5 | 5 | 100% |
| 循环检测 | 6 | 6 | 100% |
| 上下文管理 | 3 | 3 | 100% |
| 外围重试 | 6 | 0 | 0% |
| Auth 管理 | 5 | 0 | 0% |
| Model Failover | 3 | 0 | 0% |
| Session 持久化 | 5 | 2 | 40% |
| Hooks 系统 | 6 | 0 | 0% |
| Sandbox | 3 | 0 | 0% |
| 图片处理 | 2 | 0 | 0% |
| 流式优化 | 5 | 1 | 20% |
| 诊断监控 | 6 | 2 | 33% |
| **总计** | **55** | **19** | **35%** |

**核心功能对齐度**: 100% (LLM loop + 循环检测 + 上下文管理)
**整体功能对齐度**: 35% (包含所有外围支持功能)

---

## 🎯 优先级建议

### P0 (核心稳定性) - ✅ 已完成
- [x] 循环检测系统 (tool-loop-detection)
- [x] 上下文溢出处理 (ContextManager)
- [x] 基础 AgentLoop 实现

### P1 (可用性提升) - 建议实现
1. **Multi-model failover** (遇到 rate limit 自动切换模型)
2. **Auth profile rotation** (多账号支持,提高 quota)
3. **Session persistence** (完整的 JSONL 持久化)
4. **Better error handling** (细化错误类型,自动重试)

### P2 (高级功能) - 可选
1. **Hooks 系统** (插件化扩展点)
2. **Sandbox 隔离** (安全隔离 agent 工作空间)
3. **图片处理** (Vision tasks 支持)
4. **流式优化** (分块输出,实时反馈)

### P3 (诊断工具) - 长期优化
1. **Cache trace** (性能分析)
2. **Payload logging** (API 调用追踪)
3. **Structured diagnostics** (结构化诊断日志)
4. **Session metrics** (会话统计和监控)

---

## 💡 关键洞察

### OpenClaw 的设计哲学
1. **分层重试**: 外层处理基础设施错误 (auth, rate limit),内层处理业务逻辑 (tool calls)
2. **降级策略**: 多个 fallback 层级 (profile → model → thinking level)
3. **插件化**: 通过 hooks 实现可扩展性
4. **细粒度控制**: 每个环节都有配置项和监控点

### AndroidForClaw 的简化设计
1. **单层循环**: 所有逻辑在一个 loop 内完成
2. **直接失败**: 遇到错误直接返回,无降级
3. **硬编码**: 所有逻辑写死在 AgentLoop 中
4. **基础功能**: 只实现核心 LLM loop,外围功能缺失

### 建议的演进路径
1. **第一阶段**: 保持简化设计,专注核心稳定性 (当前状态)
2. **第二阶段**: 逐步添加 P1 功能 (failover, multi-auth)
3. **第三阶段**: 插件化改造 (hooks 系统)
4. **第四阶段**: 完整对齐 OpenClaw (sandbox, 诊断工具)

---

**结论**: AndroidForClaw 的核心循环逻辑已完全对齐 OpenClaw (100%),但缺少大量外围支持功能。建议优先实现 P1 功能 (failover + multi-auth),大幅提升可用性。
