#!/bin/bash

# Test LLM API directly
DEVICE_ID="17696b0d"

echo "Testing LLM API on device $DEVICE_ID..."
echo ""

# Get API config from device
echo "1. Checking API configuration..."
adb -s $DEVICE_ID shell cat /sdcard/.androidforclaw/openclaw.json | grep -A10 '"bailian"' | head -15

echo ""
echo "2. Sending test request to dashscope API..."

# Extract API key (note: this is for testing, the actual key should be properly secured)
API_KEY="sk-3792712d47804c088c02e3edc326c488"

curl -v -X POST "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen3.5-plus",
    "messages": [
      {"role": "user", "content": "你好，请回复测试"}
    ],
    "max_tokens": 100
  }' \
  --connect-timeout 10

echo ""
echo "Done."
