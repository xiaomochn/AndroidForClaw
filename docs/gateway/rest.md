# REST API Reference

AndroidForClaw Gateway 的 HTTP REST API 文档。

---

## 🌐 Base URL

```
http://localhost:8080/api
```

**注意**: 需要通过 ADB 端口转发访问:
```bash
adb forward tcp:8080 tcp:8080
```

---

## 📋 API Endpoints

### GET /health

健康检查。

**请求**:
```bash
curl http://localhost:8080/api/health
```

**响应**:
```json
{
  "status": "ok",
  "version": "3.0.0",
  "timestamp": 1709740800000
}
```

**状态码**:
- `200` - 服务正常

---

### GET /device/status

获取设备状态。

**请求**:
```bash
curl http://localhost:8080/api/device/status
```

**响应**:
```json
{
  "connected": true,
  "deviceId": "abc123",
  "deviceModel": "Vendor 12",
  "androidVersion": "13",
  "apiLevel": 33,
  "permissions": {
    "accessibility": true,
    "overlay": true,
    "mediaProjection": false
  }
}
```

**字段说明**:
- `connected`: 设备是否就绪
- `deviceId`: 设备唯一 ID
- `deviceModel`: 设备型号
- `androidVersion`: Android 版本
- `apiLevel`: API Level
- `permissions`: 权限状态
  - `accessibility`: Accessibility Service
  - `overlay`: 悬浮窗权限
  - `mediaProjection`: 截图权限

**状态码**:
- `200` - 成功

---

## 🔄 WebSocket Upgrade

### Endpoint: /

WebSocket 连接入口。

**连接**:
```javascript
const ws = new WebSocket('ws://localhost:8080/');
```

**协议**: 见 [WebSocket API](./websocket.md)

---

## 🌐 Static Files

### Endpoint: /*

提供 WebUI 静态文件。

**路径映射**:
```
/           → /webui/index.html
/assets/*   → /webui/assets/*
/favicon.ico → /webui/favicon.ico
```

**MIME Types**:
- `.html` → `text/html`
- `.js` → `application/javascript`
- `.css` → `text/css`
- `.json` → `application/json`
- `.png` → `image/png`
- `.svg` → `image/svg+xml`

**404 处理**:
```html
<h1>404 - File Not Found</h1>
<p>WebUI not built yet. Run: <code>cd ui && npm run build</code></p>
```

---

## 📊 完整 API 列表

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/health` | 健康检查 | ✅ 已实现 |
| GET | `/api/device/status` | 设备状态 | ✅ 已实现 |
| WS | `/` | WebSocket 连接 | ✅ 已实现 |
| GET | `/*` | 静态文件服务 | ✅ 已实现 |
| POST | `/api/agent/execute` | 执行 Agent 任务 | 📅 规划中 |
| GET | `/api/sessions` | 会话列表 | 📅 规划中 |
| POST | `/api/sessions` | 创建会话 | 📅 规划中 |
| GET | `/api/sessions/{id}` | 会话详情 | 📅 规划中 |
| DELETE | `/api/sessions/{id}` | 删除会话 | 📅 规划中 |
| GET | `/api/tools` | 工具列表 | 📅 规划中 |

---

## 🔒 安全性

### 当前状态

- ⚠️ 仅本地访问 (127.0.0.1)
- ⚠️ 无认证机制
- ⚠️ 无 HTTPS

**生产环境警告**: 不要将 Gateway 暴露到公网，存在安全风险！

### 未来计划

- [ ] API Key 认证
- [ ] Rate limiting
- [ ] HTTPS 支持
- [ ] Allowlist (IP 白名单)

---

## 🧪 测试 API

### 使用 curl

```bash
# Health check
curl http://localhost:8080/api/health

# Device status
curl http://localhost:8080/api/device/status

# WebUI
curl http://localhost:8080/
```

### 使用 Python

```python
import requests

# Health check
response = requests.get('http://localhost:8080/api/health')
print(response.json())

# Device status
response = requests.get('http://localhost:8080/api/device/status')
print(response.json())
```

### 使用 curl 测试

```bash
# 端口转发
adb forward tcp:8080 tcp:8080

# 测试 health 接口
curl http://localhost:8080/api/health
```

**输出**:
```
🌐 Testing Gateway API...
✅ Health check passed
✅ Device status retrieved
📱 Device: Vendor 12, Android 13
```

---

## 📚 相关文档

- [WebSocket API](./websocket.md) - WebSocket 协议
- [Gateway Overview](./overview.md) - Gateway 架构
- [Testing Guide](../debug/testing.md) - 测试方法

---

**Last Updated**: 2026-03-06
**API Version**: v3.0.0
