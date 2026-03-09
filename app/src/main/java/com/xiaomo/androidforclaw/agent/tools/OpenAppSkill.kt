package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * Open App Skill
 * Open a specified app
 */
class OpenAppSkill(private val context: Context) : Skill {
    companion object {
        private const val TAG = "OpenAppSkill"
    }

    override val name = "open_app"
    override val description = "打开指定的应用程序。需要提供应用的包名。"

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "package_name" to PropertySchema("string", "应用的包名，例如 'com.android.settings'")
                    ),
                    required = listOf("package_name")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val packageName = args["package_name"] as? String

        if (packageName == null) {
            return SkillResult.error("Missing required parameter: package_name")
        }

        Log.d(TAG, "Opening app: $packageName")
        return try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                // Wait for app launch (app launch typically takes 1-2 seconds)
                // ⚡ Optimization: reduce to 1 second, use get_view_tree to confirm launch completion
                Log.d(TAG, "Waiting for app to launch...")
                kotlinx.coroutines.delay(1000)

                SkillResult.success(
                    "App opened: $packageName (waited 1.0s for launch)",
                    mapOf("package" to packageName, "wait_time_ms" to 1000)
                )
            } else {
                SkillResult.error("App not found: $packageName")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName", e)
            SkillResult.error("Package not found: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Open app failed", e)
            SkillResult.error("Open app failed: ${e.message}")
        }
    }
}
