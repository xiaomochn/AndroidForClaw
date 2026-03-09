#!/bin/bash

# Test script for ClawHub Twitter skill installation
# Tests the complete flow: search -> install -> verify

echo "=========================================="
echo "ClawHub Twitter Skill Installation Test"
echo "=========================================="
echo ""

# Configuration
GATEWAY_HOST="localhost"
GATEWAY_PORT="8765"
WS_URL="ws://${GATEWAY_HOST}:${GATEWAY_PORT}"

echo "Step 1: Searching for Twitter skills..."
echo ""

# Search for Twitter skills
SEARCH_RESULT=$(wscat -c "$WS_URL" -x '{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "skills.search",
  "params": {
    "query": "twitter",
    "limit": 10
  }
}' 2>&1 | grep -v "Connected" | grep -v "Disconnected")

echo "Search Result:"
echo "$SEARCH_RESULT"
echo ""

# Extract first skill slug (assuming JSON response with skills array)
SKILL_SLUG=$(echo "$SEARCH_RESULT" | jq -r '.result.skills[0].slug // empty' 2>/dev/null)

if [ -z "$SKILL_SLUG" ]; then
    echo "❌ No Twitter skills found in ClawHub"
    exit 1
fi

echo "✅ Found skill: $SKILL_SLUG"
echo ""

echo "Step 2: Getting skill details..."
echo ""

# Get skill status to find install options
STATUS_RESULT=$(wscat -c "$WS_URL" -x '{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "skills.status",
  "params": {}
}' 2>&1 | grep -v "Connected" | grep -v "Disconnected")

echo "Status Result (truncated):"
echo "$STATUS_RESULT" | head -20
echo ""

echo "Step 3: Installing skill '$SKILL_SLUG'..."
echo ""

# Install the skill
INSTALL_RESULT=$(wscat -c "$WS_URL" -x "{
  \"jsonrpc\": \"2.0\",
  \"id\": 3,
  \"method\": \"skills.install\",
  \"params\": {
    \"name\": \"$SKILL_SLUG\",
    \"installId\": \"download\",
    \"timeoutMs\": 300000
  }
}" 2>&1 | grep -v "Connected" | grep -v "Disconnected")

echo "Install Result:"
echo "$INSTALL_RESULT"
echo ""

# Check if installation was successful
INSTALL_OK=$(echo "$INSTALL_RESULT" | jq -r '.result.ok // false' 2>/dev/null)

if [ "$INSTALL_OK" == "true" ]; then
    echo "✅ Skill installed successfully!"

    INSTALLED_VERSION=$(echo "$INSTALL_RESULT" | jq -r '.result.details.version // "unknown"' 2>/dev/null)
    INSTALLED_PATH=$(echo "$INSTALL_RESULT" | jq -r '.result.details.path // "unknown"' 2>/dev/null)

    echo "   Version: $INSTALLED_VERSION"
    echo "   Path: $INSTALLED_PATH"
else
    echo "❌ Installation failed"
    ERROR_MESSAGE=$(echo "$INSTALL_RESULT" | jq -r '.error.message // "Unknown error"' 2>/dev/null)
    echo "   Error: $ERROR_MESSAGE"
    exit 1
fi

echo ""
echo "Step 4: Verifying installation..."
echo ""

# Check lock file
echo "Checking lock.json..."
adb shell "cat /sdcard/.androidforclaw/workspace/.clawhub/lock.json" 2>/dev/null | jq . || echo "Lock file not found"

echo ""

# Check skill directory
echo "Checking skill directory..."
adb shell "ls -la /sdcard/.androidforclaw/skills/$SKILL_SLUG/" 2>/dev/null || echo "Skill directory not found"

echo ""
echo "=========================================="
echo "Test Complete!"
echo "=========================================="
