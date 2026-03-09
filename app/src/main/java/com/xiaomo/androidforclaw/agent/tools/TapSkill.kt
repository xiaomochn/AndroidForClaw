package com.xiaomo.androidforclaw.agent.tools

import android.util.Log
import com.xiaomo.androidforclaw.accessibility.AccessibilityProxy
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * Tap Skill
 * Tap at specified screen coordinates
 */
class TapSkill : Skill {
    companion object {
        private const val TAG = "TapSkill"
    }

    override val name = "tap"
    override val description: String
        get() {
            val isAccessibilityEnabled = AccessibilityProxy.isConnected.value == true && AccessibilityProxy.isServiceReady()
            val statusNote = if (!isAccessibilityEnabled) {
                "\n\n⚠️ **当前状态：不可用** - 无障碍服务未连接"
            } else {
                "\n✅ **当前状态：可用**"
            }
            return "点击屏幕上的坐标位置。用于点击按钮、输入框、列表项等可交互元素。$statusNote"
        }

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "x" to PropertySchema("integer", "X 坐标"),
                        "y" to PropertySchema("integer", "Y 坐标")
                    ),
                    required = listOf("x", "y")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        if (!AccessibilityProxy.isConnected.value!!) {
            return SkillResult.error("Accessibility service not connected")
        }

        val x = (args["x"] as? Number)?.toInt()
        val y = (args["y"] as? Number)?.toInt()

        if (x == null || y == null) {
            return SkillResult.error("Missing required parameters: x, y")
        }

        Log.d(TAG, "Tapping at ($x, $y)")
        return try {
            val success = AccessibilityProxy.tap(x, y)
            if (!success) {
                return SkillResult.error("Tap operation failed")
            }

            // Wait for UI response (animation, page transitions, etc.)
            kotlinx.coroutines.delay(200)

            SkillResult.success(
                "Tapped at ($x, $y)",
                mapOf("x" to x, "y" to y, "wait_time_ms" to 200)
            )
        } catch (e: IllegalStateException) {
            SkillResult.error("Service disconnected: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Tap failed", e)
            SkillResult.error("Tap failed: ${e.message}")
        }
    }
}
