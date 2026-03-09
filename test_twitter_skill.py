#!/usr/bin/env python3
"""
Test script for ClawHub Twitter skill installation
Tests: search -> install -> verify
"""

import asyncio
import websockets
import json
import sys

GATEWAY_URL = "ws://localhost:8765/ws"

async def send_rpc(websocket, method, params=None, request_id=1):
    """Send an OpenClaw Protocol request and get response"""
    request = {
        "type": "req",  # OpenClaw protocol
        "id": str(request_id),
        "method": method,
        "params": params or {}
    }

    print(f"→ Sending: {method} with params: {params}")
    await websocket.send(json.dumps(request))

    response_text = await websocket.recv()
    response = json.loads(response_text)

    print(f"← Received: {json.dumps(response, indent=2)[:500]}...")
    print()

    return response

async def test_twitter_skill():
    print("=" * 60)
    print("ClawHub Twitter Skill Installation Test")
    print("=" * 60)
    print()

    try:
        async with websockets.connect(GATEWAY_URL) as websocket:
            # Skip welcome message
            welcome = await websocket.recv()
            print(f"← Welcome: {welcome[:100]}...")
            print()
            # Step 1: Search for Twitter skills
            print("Step 1: Searching for Twitter skills...")
            print()

            search_response = await send_rpc(websocket, "skills.search", {
                "query": "twitter",
                "limit": 10
            }, request_id=1)

            if not search_response.get("ok", False):
                error_msg = search_response.get("error", {}).get("message", "Unknown error")
                print(f"❌ Search failed: {error_msg}")
                return False

            skills = search_response.get("payload", {}).get("skills", [])

            if not skills:
                print("❌ No Twitter skills found in ClawHub")
                return False

            skill = skills[0]
            skill_slug = skill.get("slug")
            skill_name = skill.get("name")

            print(f"✅ Found skill: {skill_name} ({skill_slug})")
            print(f"   Description: {skill.get('description', 'N/A')[:100]}...")
            print()

            # Step 2: Get current skills status
            print("Step 2: Getting current skills status...")
            print()

            status_response = await send_rpc(websocket, "skills.status", {}, request_id=2)

            if not status_response.get("ok", False):
                error_msg = status_response.get("error", {}).get("message", "Unknown error")
                print(f"⚠️  Status check failed: {error_msg}")
            else:
                current_skills = status_response.get("payload", {}).get("skills", [])
                print(f"   Currently loaded skills: {len(current_skills)}")
            print()

            # Step 3: Install the skill
            print(f"Step 3: Installing skill '{skill_slug}'...")
            print()

            install_response = await send_rpc(websocket, "skills.install", {
                "name": skill_name,
                "installId": "download",
                "timeoutMs": 300000
            }, request_id=3)

            if not install_response.get("ok", False):
                error_msg = install_response.get("error", {}).get("message", "Unknown error")
                print(f"❌ Installation failed: {error_msg}")
                return False

            result = install_response.get("payload", {})

            if result.get("ok"):
                print("✅ Skill installed successfully!")
                details = result.get("details", {})
                print(f"   Name: {details.get('name')}")
                print(f"   Version: {details.get('version')}")
                print(f"   Slug: {details.get('slug')}")
                print(f"   Path: {details.get('path')}")
                print(f"   Hash: {details.get('hash', 'N/A')[:16]}...")
            else:
                print(f"❌ Installation failed: {result.get('message')}")
                return False

            print()

            # Step 4: Verify installation
            print("Step 4: Verifying installation...")
            print()

            status_response = await send_rpc(websocket, "skills.status", {}, request_id=4)

            if status_response.get("ok", False):
                updated_skills = status_response.get("payload", {}).get("skills", [])
                twitter_skill = next((s for s in updated_skills if skill_slug in s.get("skillKey", "")), None)

                if twitter_skill:
                    print(f"✅ Skill found in status report!")
                    print(f"   Name: {twitter_skill.get('name')}")
                    print(f"   Source: {twitter_skill.get('source')}")
                    print(f"   Eligible: {twitter_skill.get('eligible')}")
                else:
                    print("⚠️  Skill not found in status report (may need reload)")

            print()
            print("=" * 60)
            print("Test Complete!")
            print("=" * 60)

            return True

    except websockets.exceptions.WebSocketException as e:
        print(f"❌ WebSocket connection failed: {e}")
        print()
        print("Make sure:")
        print("  1. AndroidForClaw app is running on device")
        print("  2. Gateway is enabled and started")
        print("  3. ADB port forwarding is set up:")
        print("     adb forward tcp:8765 tcp:8765")
        return False
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = asyncio.run(test_twitter_skill())
    sys.exit(0 if success else 1)
