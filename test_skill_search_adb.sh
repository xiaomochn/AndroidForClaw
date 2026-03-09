#!/bin/bash

echo "==============================================="
echo "测试 skills.search 方法 (通过 adb + WebSocket)"
echo "==============================================="
echo ""

# 1. 清空日志
echo "步骤 1: 清空日志..."
adb logcat -c
echo "✅ 日志已清空"
echo ""

# 2. 发送测试消息 (模拟 skills.search 调用)
echo "步骤 2: 通过 Python 脚本调用 skills.search..."
python3 << 'EOF'
import asyncio
import websockets
import json

async def test_search():
    uri = "ws://localhost:8765/ws"

    try:
        async with websockets.connect(uri) as ws:
            # 跳过欢迎消息
            await ws.recv()

            # 发送 skills.search 请求
            req = {
                "type": "req",
                "id": "test-1",
                "method": "skills.search",
                "params": {"query": "twitter", "limit": 10}
            }

            print(f"→ 发送请求: {json.dumps(req, indent=2)}")
            await ws.send(json.dumps(req))

            # 等待响应 (最多30秒)
            try:
                resp = await asyncio.wait_for(ws.recv(), timeout=30.0)
                print(f"\n← 收到响应: {json.dumps(json.loads(resp), indent=2)}")
            except asyncio.TimeoutError:
                print("\n❌ 超时: 30秒内未收到响应")

    except Exception as e:
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()

asyncio.run(test_search())
EOF

echo ""
echo ""

# 3. 等待处理完成
echo "步骤 3: 等待5秒,让服务器处理..."
sleep 5
echo ""

# 4. 查看日志
echo "步骤 4: 查看相关日志..."
echo "=========================================="
adb logcat -d | grep -E "SkillsMethods|ClawHubClient|skills.search" | tail -50
echo "=========================================="
echo ""

echo "✅ 测试完成!"
