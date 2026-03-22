# Gateway 使用指南

## 🌐 概述

AndroidForClaw Gateway 提供 WebSocket RPC 接口，允许远程控制 Android 设备执行 AI Agent 任务。

**核心特性**:
- ✅ WebSocket 实时通信
- ✅ Agent 任务异步执行
- ✅ 进度实时推送
- ✅ 会话管理
- ✅ 健康检查

**端口**: `ws://0.0.0.0:8765`

---

## 🚀 快速开始

### 1. 启动 Gateway

Gateway 会在应用启动时自动启动。查看 logcat 确认：

```bash
adb logcat | grep "Gateway"
```

期望输出：
```
I/MyApplication: 🌐 启动 Gateway 服务...
I/MyApplication: ✅ Gateway 服务已启动: ws://0.0.0.0:8765
```

### 2. 连接测试

#### WebSocket 命令行测试

```bash
# 安装 wscat
npm install -g wscat

# 连接到 Gateway
wscat -c ws://192.168.1.100:8765
```

---

## 📡 RPC 接口

### 1. Health Check (健康检查)

**请求**:
```json
{
  "id": "req_001",
  "method": "health",
  "params": null
}
```

**响应**:
```json
{
  "type": "response",
  "id": "req_001",
  "data": {
    "status": "healthy",
    "timestamp": 1234567890,
    "sessions": 1
  }
}
```

---

### 2. Agent Execution (执行任务)

**请求**:
```json
{
  "id": "req_002",
  "method": "agent",
  "params": {
    "message": "截取当前屏幕截图",
    "systemPrompt": null,
    "tools": null,
    "maxIterations": 20
  }
}
```

**参数说明**:
- `message` (必需): 用户指令
- `systemPrompt` (可选): 自定义系统提示词，默认自动生成
- `tools` (可选): 工具列表，默认使用所有可用工具
- `maxIterations` (可选): 最大迭代次数，默认 20

**响应** - 进度推送:

Agent 执行是**异步**的，会实时推送进度：

```json
// 1. 迭代开始
{
  "type": "progress",
  "requestId": "req_002",
  "data": {
    "type": "iteration",
    "number": 1
  }
}

// 2. 思考中
{
  "type": "progress",
  "requestId": "req_002",
  "data": {
    "type": "reasoning",
    "content": "我需要截取屏幕...",
    "duration": 1234
  }
}

// 3. 工具调用
{
  "type": "progress",
  "requestId": "req_002",
  "data": {
    "type": "tool_call",
    "name": "screenshot",
    "arguments": {}
  }
}

// 4. 工具结果
{
  "type": "progress",
  "requestId": "req_002",
  "data": {
    "type": "tool_result",
    "result": "截图已保存到 /sdcard/...",
    "duration": 567
  }
}

// 5. 迭代完成
{
  "type": "progress",
  "requestId": "req_002",
  "data": {
    "type": "iteration_complete",
    "number": 1,
    "iterationDuration": 2000,
    "llmDuration": 1234,
    "execDuration": 567
  }
}

// 6. 最终结果
{
  "type": "response",
  "id": "req_002",
  "data": {
    "success": true,
    "iterations": 1,
    "toolsUsed": ["screenshot"],
    "finalContent": "已完成截图，保存在 /sdcard/...",
    "sessionId": "session_1234567890_5678"
  }
}
```

**错误响应**:
```json
{
  "type": "error",
  "id": "req_002",
  "message": "Agent execution failed: ..."
}
```

---

### 3. Session List (会话列表)

**请求**:
```json
{
  "id": "req_003",
  "method": "session.list",
  "params": null
}
```

**响应**:
```json
{
  "type": "response",
  "id": "req_003",
  "data": {
    "sessions": [
      { "id": "session_1234567890_5678" },
      { "id": "session_1234567891_9012" }
    ]
  }
}
```

---

### 4. Session Reset (重置会话)

**请求**:
```json
{
  "id": "req_004",
  "method": "session.reset",
  "params": {
    "sessionId": "session_1234567890_5678"
  }
}
```

**参数说明**:
- `sessionId` (可选): 要重置的会话 ID，默认为当前会话

**响应**:
```json
{
  "type": "response",
  "id": "req_004",
  "data": {
    "success": true
  }
}
```

---

## 🐍 Python 客户端示例

### 基础示例

```python
import asyncio
import json
import websockets

async def test_agent():
    uri = "ws://192.168.1.100:8765"

    async with websockets.connect(uri) as websocket:
        # 1. 接收欢迎消息
        welcome = await websocket.recv()
        print(f"Welcome: {welcome}")

        # 2. 执行 Agent 任务
        request = {
            "id": "req_001",
            "method": "agent",
            "params": {
                "message": "打开设置应用",
                "maxIterations": 10
            }
        }

        await websocket.send(json.dumps(request))
        print(f"Sent: {request}")

        # 3. 接收进度和结果
        while True:
            response = await websocket.recv()
            data = json.loads(response)

            if data["type"] == "response":
                print(f"✅ Final: {data}")
                break
            elif data["type"] == "progress":
                print(f"📊 Progress: {data['data']['type']}")
            elif data["type"] == "error":
                print(f"❌ Error: {data['message']}")
                break

asyncio.run(test_agent())
```

---

## 🔧 故障排查

### 1. 连接失败

**症状**: `ConnectionRefusedError`

**排查步骤**:

1. **确认 Gateway 已启动**:
   ```bash
   adb logcat | grep "Gateway"
   ```
   应看到: `✅ Gateway 服务已启动`

2. **确认手机 IP 正确**:
   ```bash
   adb shell ip addr show wlan0
   ```

3. **确认端口未被占用**:
   ```bash
   adb shell netstat -an | grep 8765
   ```

4. **确认防火墙/网络**:
   - 手机和电脑在同一局域网
   - 没有防火墙阻止 8765 端口

---

### 2. 连接中断

**症状**: 连接建立后立即断开

**排查步骤**:

1. **查看 Gateway 日志**:
   ```bash
   adb logcat | grep "GatewayService"
   ```

2. **检查异常**:
   ```bash
   adb logcat | grep "Exception"
   ```

---

### 3. Agent 执行失败

**症状**: 收到 `type: "error"` 响应

**排查步骤**:

1. **查看 MainEntryAgentHandler 日志**:
   ```bash
   adb logcat | grep "MainEntryAgentHandler"
   ```

2. **查看 AgentLoop 日志**:
   ```bash
   adb logcat | grep "AgentLoop"
   ```

3. **检查工具执行日志**:
   ```bash
   adb logcat | grep "Skill"
   ```

---

## 🎯 使用场景

### 1. 远程自动化测试

```python
# 远程执行测试用例
await execute_agent("测试微信登录功能")
await execute_agent("验证支付流程")
await execute_agent("检查 UI 显示")
```

### 2. 持续监控

```python
# 定期检查应用状态
while True:
    await execute_agent("检查应用是否崩溃")
    await asyncio.sleep(60)
```

### 3. 多设备协同

```python
# 同时控制多台设备
devices = ["192.168.1.100", "192.168.1.101", "192.168.1.102"]

async def control_device(ip):
    uri = f"ws://{ip}:8765"
    async with websockets.connect(uri) as ws:
        await execute_agent(ws, "打开设置")

await asyncio.gather(*[control_device(ip) for ip in devices])
```

### 4. Web Dashboard

```javascript
// 网页控制台
const ws = new WebSocket('ws://192.168.1.100:8765');

ws.onopen = () => {
  ws.send(JSON.stringify({
    id: 'req_001',
    method: 'agent',
    params: {
      message: '打开音乐应用',
      maxIterations: 10
    }
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Progress:', data);
};
```

---

## 📚 相关文档

- [ARCHITECTURE.md](../ARCHITECTURE.md) - 架构设计
- [CLAUDE.md](../CLAUDE.md) - 开发指南

---

## 🔮 未来扩展

- [ ] `agent.wait` 方法（阻塞等待任务完成）
- [ ] 多渠道接入（WhatsApp, Telegram, Web UI）
- [ ] 安全认证（API Key, Pairing）
- [ ] 会话持久化（保存对话历史）
- [ ] 分布式部署（Gateway 与 Runtime 分离）
- [ ] 任务队列（支持任务排队）
- [ ] 日志流式传输（实时查看 logcat）

---

**AndroidForClaw Gateway** - 让 AI 远程控制 Android 设备 🤖📱
