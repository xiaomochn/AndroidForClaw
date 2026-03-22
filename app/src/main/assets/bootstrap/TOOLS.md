# TOOLS

## Tool Usage Guidelines

### Observation Tools

**Always observe before acting**:
- `screenshot()` - Capture current screen state
- `get_ui_tree()` - Get UI element hierarchy

**Best Practice**: Call `screenshot` before and after every action to verify state.

### Action Tools

**Coordinate-based actions**:
- `tap(x, y)` - Single tap at coordinates
- `swipe(startX, startY, endX, endY, duration)` - Swipe gesture
- `long_press(x, y, duration)` - Long press
- `type(text)` - Input text (requires focused input field)

**Tips**:
- Use screenshot to find target coordinates
- Verify input focus before typing
- Handle on-screen keyboard visibility

### Navigation Tools

**System navigation**:
- `home()` - Go to home screen
- `back()` - Go back (equivalent to back button)
- `open_app(package_name)` - Launch app by package name

**Common package names**:
- Chrome: `com.android.chrome`
- Settings: `com.android.settings`
- Contacts: `com.android.contacts`

### Control Flow

- `wait(seconds)` - Pause execution (use for loading states)
- `stop()` - Stop execution and return result

### Special Tools

**browser** - Web automation through BrowserForClaw:
```json
{
  "operation": "navigate|click|type|get_content|wait|back|forward|scroll|screenshot",
  ...operation-specific parameters
}
```

See the `browser` skill for detailed usage.

## Anti-Patterns

❌ **Don't**:
- Call the same failing action repeatedly
- Assume state without observing
- Skip verification screenshots
- Use hardcoded coordinates without checking

✅ **Do**:
- Observe → Think → Act → Verify
- Try alternative approaches when blocked
- Log your reasoning
- Handle timeouts gracefully
