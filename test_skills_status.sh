#!/bin/bash

echo "==============================================="
echo "测试 skills.status 方法"
echo "==============================================="
echo ""

adb logcat -c

python3 << 'EOF'
import asyncio
import websockets
import json

async def test_status():
    uri = "ws://localhost:8765/ws"

    try:
        async with websockets.connect(uri) as ws:
            # 跳过欢迎消息
            welcome = await ws.recv()
            print(f"← 连接成功")

            # 测试 skills.status
            req = {
                "type": "req",
                "id": "test-status",
                "method": "skills.status",
                "params": {}
            }

            print(f"\n→ 发送: skills.status")
            await ws.send(json.dumps(req))

            resp = await asyncio.wait_for(ws.recv(), timeout=10.0)
            resp_json = json.loads(resp)

            if resp_json.get("ok"):
                print(f"\n✅ 成功!")
                payload = resp_json.get("payload", {})
                skills = payload.get("skills", [])
                print(f"\n📊 技能数量: {len(skills)}")
                print(f"📁 工作区目录: {payload.get('workspaceDir')}")
                print(f"📦 托管目录: {payload.get('managedSkillsDir')}")

                if skills:
                    print(f"\n技能列表:")
                    for skill in skills[:5]:  # 只显示前5个
                        print(f"  - {skill.get('name')}: {skill.get('description', '')[:50]}...")
                else:
                    print(f"\n⚠️  当前没有加载的技能")
            else:
                error = resp_json.get("error", {})
                print(f"\n❌ 失败: {error.get('message')}")

    except Exception as e:
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()

asyncio.run(test_status())
EOF

sleep 2

echo ""
echo "查看日志:"
echo "=========================================="
adb logcat -d | grep -E "SkillsMethods|skills.status" | tail -20
echo "=========================================="
