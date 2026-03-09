#!/bin/bash
# 测试飞书图片发送功能

echo "==================================="
echo "飞书图片发送功能测试"
echo "==================================="
echo ""

# 1. 检查设备连接
echo "1. 检查设备连接..."
adb devices | grep -v "List" | grep "device"
if [ $? -ne 0 ]; then
    echo "❌ 没有找到已连接的设备"
    exit 1
fi
echo "✅ 设备已连接"
echo ""

# 2. 检查应用是否运行
echo "2. 检查应用状态..."
APP_PID=$(adb shell "ps -A | grep com.xiaomo.androidforclaw" | awk '{print $2}')
if [ -z "$APP_PID" ]; then
    echo "⚠️  应用未运行,正在启动..."
    adb shell am start -n com.xiaomo.androidforclaw/.ui.activity.MainActivityCompose
    sleep 3
else
    echo "✅ 应用正在运行 (PID: $APP_PID)"
fi
echo ""

# 3. 清理日志
echo "3. 清理旧日志..."
adb logcat -c
echo "✅ 日志已清理"
echo ""

# 4. 开始监控日志 (后台)
echo "4. 开始监控日志..."
echo "   监控关键字: FeishuChannel, FeishuSendImage, screenshot, send_image, 上传图片"
adb logcat | grep -E "FeishuChannel|FeishuSendImage|screenshot|send_image|上传图片|发送图片|Tool:" > /tmp/feishu_test.log &
LOGCAT_PID=$!
echo "✅ 日志监控已启动 (PID: $LOGCAT_PID)"
echo ""

# 5. 等待用户通过飞书发送测试消息
echo "==================================="
echo "📱 请通过飞书客户端发送测试消息:"
echo ""
echo "   \"Screenshot tool 截一张图 然后飞书发给我\""
echo ""
echo "==================================="
echo ""
echo "监控日志中... (按 Ctrl+C 停止)"
echo ""

# 6. 实时显示日志
tail -f /tmp/feishu_test.log

# 清理
trap "kill $LOGCAT_PID 2>/dev/null; rm /tmp/feishu_test.log 2>/dev/null" EXIT
