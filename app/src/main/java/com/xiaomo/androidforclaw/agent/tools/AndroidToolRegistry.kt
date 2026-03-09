package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.util.Log
import com.xiaomo.androidforclaw.agent.memory.MemoryManager
import com.xiaomo.androidforclaw.agent.tools.memory.MemoryGetSkill
import com.xiaomo.androidforclaw.agent.tools.memory.MemorySearchSkill
import com.xiaomo.androidforclaw.data.model.TaskDataManager
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * Android Tool Registry
 *
 * 管理 Android 平台特定的工具 (Platform-specific tools)
 *
 * 与 OpenClaw 架构对齐：
 * - ToolRegistry: 通用工具 (read, write, exec, web_fetch)
 * - AndroidToolRegistry: Android 平台工具 (tap, screenshot, open_app, memory)
 * - SkillsLoader: Markdown Skills (mobile-operations.md)
 *
 * 参考: OpenClaw 的 pi-tools.ts 中的平台特定能力
 */
class AndroidToolRegistry(
    private val context: Context,
    private val taskDataManager: TaskDataManager,
    private val memoryManager: MemoryManager? = null,
    private val workspacePath: String = "/sdcard/.androidforclaw/workspace"
) {
    companion object {
        private const val TAG = "AndroidToolRegistry"
    }

    private val tools = mutableMapOf<String, Skill>()

    init {
        registerAndroidTools()
        registerMemoryTools()
    }

    /**
     * 注册 Android 平台特定工具
     */
    private fun registerAndroidTools() {
        // === 观察类工具 (Observation) ===
        register(GetViewTreeSkill(context))  // 获取 UI 树（轻量）
        register(ScreenshotSkill(context))   // 截图（完整）

        // === 交互类工具 (Interaction) ===
        register(TapSkill())                 // 点击
        register(SwipeSkill())               // 滑动
        register(TypeSkill(context))         // 输入
        register(LongPressSkill())           // 长按

        // === 导航类工具 (Navigation) ===
        register(HomeSkill())                // 回桌面
        register(BackSkill())                // 返回
        register(OpenAppSkill(context))      // 打开应用

        // === 应用管理工具 (App Management) ===
        register(ListInstalledAppsSkill(context))  // 应用列表
        register(StartActivityTool(context))       // 启动 Activity

        // === 控制类工具 (Control) ===
        register(WaitSkill())                // 等待
        register(StopSkill(taskDataManager)) // 停止
        register(LogSkill())                 // 日志

        // === 浏览器工具 (Browser) ===
        // 注意：browser 工具使用 BrowserForClaw (Android 应用)，是平台特定的
        register(com.xiaomo.androidforclaw.agent.skills.BrowserForClawSkill(context))

        // === 飞书工具 (Feishu) ===
        register(FeishuSendImageSkill(context))

        Log.d(TAG, "✅ Registered ${tools.size} Android platform tools")
    }

    /**
     * 注册记忆工具
     */
    private fun registerMemoryTools() {
        if (memoryManager == null) {
            Log.d(TAG, "⚠️ MemoryManager not provided, skipping memory tools")
            return
        }

        // === 记忆工具 (Memory) ===
        register(MemoryGetSkill(memoryManager, workspacePath))
        register(MemorySearchSkill(memoryManager, workspacePath))

        Log.d(TAG, "✅ Registered memory tools")
    }

    /**
     * 注册一个工具
     */
    private fun register(tool: Skill) {
        tools[tool.name] = tool
        Log.d(TAG, "  📱 ${tool.name}")
    }

    /**
     * 检查是否包含指定工具
     */
    fun contains(name: String): Boolean = tools.containsKey(name)

    /**
     * 执行工具
     */
    suspend fun execute(name: String, args: Map<String, Any?>): SkillResult {
        val tool = tools[name]
        if (tool == null) {
            Log.e(TAG, "Unknown Android tool: $name")
            return SkillResult.error("Unknown Android tool: $name")
        }

        Log.d(TAG, "Executing Android tool: $name with args: $args")
        return try {
            tool.execute(args)
        } catch (e: Exception) {
            Log.e(TAG, "Android tool execution failed: $name", e)
            SkillResult.error("Execution failed: ${e.message}")
        }
    }

    /**
     * 获取所有 Tool Definitions（用于 LLM function calling）
     */
    fun getToolDefinitions(): List<ToolDefinition> {
        return tools.values.map { it.getToolDefinition() }
    }

    /**
     * 获取所有工具的描述（用于构建 system prompt）
     */
    fun getToolsDescription(): String {
        return buildString {
            appendLine("## Android Platform Tools")
            appendLine()
            appendLine("Android 设备专属能力，通过 AccessibilityService 和系统 API 提供：")
            appendLine()

            // 按类别组织
            val categories = mapOf(
                "观察" to listOf("screenshot", "get_view_tree"),
                "交互" to listOf("tap", "swipe", "type", "long_press"),
                "导航" to listOf("home", "back", "open_app"),
                "应用管理" to listOf("list_installed_apps", "start_activity"),
                "控制" to listOf("wait", "stop", "log"),
                "浏览器" to listOf("browser")
            )

            categories.forEach { (category, toolNames) ->
                appendLine("### $category")
                toolNames.forEach { name ->
                    tools[name]?.let { tool ->
                        appendLine("- **${tool.name}**: ${tool.description.lines().first()}")
                    }
                }
                appendLine()
            }
        }
    }

    /**
     * 获取工具数量
     */
    fun getToolCount(): Int = tools.size
}
