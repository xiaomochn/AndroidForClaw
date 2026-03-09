package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.util.Log
import com.xiaomo.androidforclaw.core.MyApplication
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition
import java.io.File

/**
 * 飞书发送图片工具
 *
 * 用途: Agent 调用此工具发送图片到飞书当前对话
 * 场景: 截图后发送给用户
 *
 * 实现: 使用 FeishuChannel 的当前对话上下文发送图片
 */
class FeishuSendImageSkill(private val context: Context) : Skill {
    companion object {
        private const val TAG = "FeishuSendImageSkill"
    }

    override val name = "send_image"
    override val description = "Send an image to the user via Feishu. Use this after taking a screenshot."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "image_path" to PropertySchema(
                            type = "string",
                            description = "Path to the image file. Use the path returned by the screenshot tool."
                        )
                    ),
                    required = listOf("image_path")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val imagePath = args["image_path"] as? String
            ?: return SkillResult.error("Missing required parameter: image_path")

        Log.d(TAG, "Sending image: $imagePath")

        try {
            // 检查文件
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                return SkillResult.error("Image file not found: $imagePath")
            }

            if (!imageFile.canRead()) {
                return SkillResult.error("Cannot read image file: $imagePath")
            }

            // 获取 FeishuChannel
            val feishuChannel = MyApplication.getFeishuChannel()
            if (feishuChannel == null) {
                Log.e(TAG, "❌ Feishu channel not active")
                return SkillResult.error("Feishu channel is not active. Make sure Feishu is enabled in config.")
            }

            // 发送图片到当前对话
            Log.i(TAG, "📤 Sending image to current chat: ${imageFile.name} (${imageFile.length()} bytes)")
            val result = feishuChannel.sendImageToCurrentChat(imageFile)

            if (result.isSuccess) {
                val messageId = result.getOrNull()
                Log.i(TAG, "✅ Image sent successfully. message_id: $messageId")
                return SkillResult.success(
                    content = "Image sent successfully to Feishu. message_id: $messageId",
                    metadata = mapOf(
                        "message_id" to (messageId ?: "unknown"),
                        "file_size" to imageFile.length(),
                        "file_name" to imageFile.name
                    )
                )
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "❌ Failed to send image", error)
                return SkillResult.error("Failed to send image: ${error?.message ?: "Unknown error"}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send image", e)
            return SkillResult.error("Failed to send image: ${e.message}")
        }
    }
}
