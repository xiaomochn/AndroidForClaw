# P0 飞书图片发送功能测试指南

## 功能说明

实现了完整的飞书图片发送功能,Agent 可以通过 `send_image` 工具将截图发送到飞书当前对话。

## 已完成的实现

### 1. MyApplication.kt
- 添加 `getFeishuChannel()` 静态方法暴露 feishuChannel
- 在 `handleFeishuEvent()` 中调用 `updateCurrentChatContext()` 更新当前对话上下文

### 2. FeishuChannel.kt
- 实现 `ChatContext` 数据类
- 实现 `updateCurrentChatContext()` 方法
- 实现 `getCurrentChatContext()` 方法
- 实现 `sendImageToCurrentChat()` 完整方法:
  - 检查上下文有效性
  - 上传图片到飞书
  - 发送图片消息到当前对话
  - 返回 message_id

### 3. FeishuSendImageSkill.kt
- 更新为使用真实的 Feishu API
- 调用 `MyApplication.getFeishuChannel()` 获取 channel
- 调用 `sendImageToCurrentChat()` 发送图片
- 返回详细的执行结果

### 4. AndroidToolRegistry.kt
- 已注册 `FeishuSendImageSkill`

## 测试流程

### 测试用例
"Screenshot tool 截一张图 然后飞书发给我"

### 预期结果
1. Agent 调用 `screenshot` 工具截图
2. Agent 调用 `send_image` 工具发送图片
3. 图片成功上传到飞书
4. 图片消息发送到当前对话
5. 用户在飞书中收到截图

### 测试方法

**方法 1: 通过真实飞书消息测试 (推荐)**
1. 确保 Feishu Channel 已启动
2. 在飞书中向机器人发送消息: "Screenshot tool 截一张图 然后飞书发给我"
3. 观察日志:
   ```bash
   adb logcat | grep -E "FeishuChannel|FeishuSendImageSkill|screenshot|send_image|上传图片|发送图片"
   ```
4. 检查飞书是否收到截图

**方法 2: 通过 Broadcast 测试**
1. 确保应用已启动且 MainActivityCompose 在前台
2. 发送测试消息:
   ```bash
   adb shell am broadcast -a PHONE_FORCLAW_SEND_MESSAGE --es message "Screenshot tool 截一张图 然后飞书发给我"
   ```
3. 观察日志
4. 检查飞书是否收到截图

## 日志关键字

观察以下日志确认执行流程:

```bash
# 消息接收
"📨 收到飞书消息"
"✅ 已更新当前对话上下文"

# Agent 执行
"🔧 Function: screenshot"
"🔧 Function: send_image"

# 图片上传
"📤 Sending image to current chat"
"Uploading image:"
"Image uploaded successfully. image_key:"

# 图片发送
"Sending image to"
"✅ Image sent successfully. message_id:"
```

## 错误处理

### 常见错误

1. **"Feishu channel not active"**
   - 原因: Feishu Channel 未启动
   - 解决: 检查配置文件中 `gateway.feishu.enabled = true`

2. **"No active chat context"**
   - 原因: 没有当前对话上下文
   - 解决: 先通过飞书发送消息给机器人,建立对话上下文

3. **"Chat context is stale"**
   - 原因: 上下文超过 5 分钟
   - 解决: 重新发送消息更新上下文

4. **"Failed to upload image"**
   - 原因: 飞书 API 上传失败
   - 解决: 检查网络连接和 Feishu 配置

## 成功标准

测试成功的标准:
- ✅ 连续 3 次测试都成功
- ✅ 每次都能正确截图
- ✅ 每次都能成功发送到飞书
- ✅ 用户在飞书中能看到清晰的截图
- ✅ Agent Loop 迭代次数 ≤ 3 (理想情况是 2 次:screenshot + send_image)

## 构建和部署

```bash
# 清理
./gradlew clean

# 构建
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 重启应用
adb shell am force-stop com.xiaomo.androidforclaw
adb shell am start -n com.xiaomo.androidforclaw/.ui.activity.MainActivityCompose
```

## 相关文件

- `app/src/main/java/com/xiaomo/androidforclaw/core/MyApplication.kt` - 第 918-927 行
- `app/src/main/java/com/xiaomo/androidforclaw/agent/tools/FeishuSendImageSkill.kt` - 完整文件
- `extensions/feishu/src/main/java/com/xiaomo/feishu/FeishuChannel.kt` - 第 48-156 行
- `app/src/main/java/com/xiaomo/androidforclaw/agent/tools/AndroidToolRegistry.kt` - 第 73 行

## 下一步

完成测试后,可以进行以下优化:
1. 添加图片压缩 (大图优化)
2. 支持批量发送多张图片
3. 添加图片描述文字
4. 实现进度反馈
