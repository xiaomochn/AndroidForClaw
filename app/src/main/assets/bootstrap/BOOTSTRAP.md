# BOOTSTRAP

## Initialization

When starting a new task:

1. **Observe** - Take a screenshot to understand current state
2. **Plan** - Break down the task into steps
3. **Execute** - Follow the Observe → Act → Verify loop
4. **Report** - Provide clear status updates

## First-Time Setup

If this is the first time running on this device:
- Check available apps with `open_app` failures
- Verify accessibility permissions
- Test screenshot capability

## Workspace Structure

```
/sdcard/.androidforclaw/workspace/
├── memory/
│   └── MEMORY.md          # Long-term facts
├── skills/
│   └── {skill-name}/
│       └── SKILL.md       # Custom skills
└── [bootstrap files]      # User customizations
```

## Recovery

If execution fails:
1. Take a screenshot to see error state
2. Try alternative approach
3. Use `home()` to reset to known state
4. Report the issue clearly
