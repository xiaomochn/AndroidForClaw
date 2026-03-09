package com.xiaomo.androidforclaw.gateway.methods

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.xiaomo.androidforclaw.agent.skills.*
import com.xiaomo.androidforclaw.config.ConfigLoader
import com.xiaomo.androidforclaw.gateway.protocol.GatewayError
import kotlinx.coroutines.runBlocking

/**
 * Skills Gateway Methods
 *
 * 完全对齐 OpenClaw Gateway Protocol
 *
 * 方法:
 * - skills.status  - 获取技能状态报告
 * - skills.bins    - 获取所有二进制依赖
 * - skills.install - 安装技能
 * - skills.update  - 更新技能配置
 */
class SkillsMethods(private val context: Context) {
    companion object {
        private const val TAG = "SkillsMethods"
    }

    private val statusBuilder = SkillStatusBuilder(context)
    private val installer = SkillInstaller(context)
    private val configLoader = ConfigLoader(context)

    /**
     * skills.status - 获取技能状态报告
     *
     * 参数:
     * {
     *   agentId?: string  // 可选,默认为主 agent
     * }
     *
     * 返回: SkillStatusReport
     */
    fun status(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.status called")

            // 1. 解析参数
            val agentId = params.get("agentId")?.asString

            // 2. 验证 agent (暂时跳过,单 agent 模式)
            if (agentId != null) {
                Log.d(TAG, "  agentId: $agentId (ignored, single-agent mode)")
            }

            // 3. 构建状态报告
            val report = statusBuilder.buildStatus()

            // 4. 转换为 JSON
            val result = JsonObject().apply {
                addProperty("workspaceDir", report.workspaceDir)
                addProperty("managedSkillsDir", report.managedSkillsDir)
                add("skills", com.google.gson.Gson().toJsonTree(report.skills))
            }

            Log.i(TAG, "✅ skills.status: ${report.skills.size} skills")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "skills.status failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_STATUS_FAILED",
                    message = "Failed to get skills status: ${e.message}"
                )
            )
        }
    }

    /**
     * skills.bins - 获取所有二进制依赖
     *
     * 参数: {}
     *
     * 返回:
     * {
     *   bins: string[]
     * }
     */
    fun bins(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.bins called")

            // 1. 获取所有技能
            val report = statusBuilder.buildStatus()

            // 2. 收集所有二进制依赖
            val binsSet = mutableSetOf<String>()
            report.skills.forEach { skill ->
                skill.requirements?.bins?.forEach { bin ->
                    binsSet.add(bin)
                }
                skill.requirements?.anyBins?.forEach { bin ->
                    binsSet.add(bin)
                }
            }

            // 3. 返回结果
            val result = JsonObject().apply {
                add("bins", com.google.gson.Gson().toJsonTree(binsSet.sorted()))
            }

            Log.i(TAG, "✅ skills.bins: ${binsSet.size} binaries")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "skills.bins failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_BINS_FAILED",
                    message = "Failed to get skills bins: ${e.message}"
                )
            )
        }
    }

    /**
     * skills.install - 安装技能
     *
     * 参数:
     * {
     *   name: string;           // 技能名称
     *   installId: string;      // 安装器 ID
     *   timeoutMs?: number;     // 超时(默认 300 秒,最多 900 秒)
     * }
     *
     * 返回:
     * {
     *   ok: boolean;
     *   message: string;
     *   stdout: string;
     *   stderr: string;
     *   code: number | null;
     *   warnings?: string[];
     * }
     */
    fun install(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.install called")

            // 1. 解析参数
            val name = params.get("name")?.asString
                ?: return Result.failure(
                    GatewayError(code = "INVALID_PARAMS", message = "Missing required parameter: name")
                )

            val installId = params.get("installId")?.asString
                ?: return Result.failure(
                    GatewayError(code = "INVALID_PARAMS", message = "Missing required parameter: installId")
                )

            val timeoutMs = params.get("timeoutMs")?.asInt ?: 300_000

            Log.d(TAG, "  name: $name")
            Log.d(TAG, "  installId: $installId")
            Log.d(TAG, "  timeoutMs: $timeoutMs")

            // 2. 查找技能
            val report = statusBuilder.buildStatus()
            val skill = report.skills.find { it.name == name }
                ?: return Result.failure(
                    GatewayError(code = "SKILL_NOT_FOUND", message = "Skill not found: $name")
                )

            // 3. 查找安装选项
            val installOption = skill.install.find { it.installId == installId }
                ?: return Result.failure(
                    GatewayError(code = "INSTALL_ID_NOT_FOUND", message = "Install ID not found: $installId")
                )

            // 4. 检查可用性
            if (!installOption.available) {
                return Result.failure(
                    GatewayError(
                        code = "INSTALL_NOT_AVAILABLE",
                        message = installOption.reason ?: "Installation not available"
                    )
                )
            }

            // 5. 执行安装
            Log.i(TAG, "Installing skill: $name (${installOption.kind})")

            val installResult = runBlocking {
                when (installOption.kind) {
                    InstallKind.APK, InstallKind.DOWNLOAD -> {
                        // 从 ClawHub 安装
                        installer.installFromClawHub(
                            slug = name,
                            version = "latest"
                        ) { progress ->
                            Log.d(TAG, "Install progress: $progress")
                        }
                    }
                    else -> {
                        Result.failure(Exception("Install kind not supported on Android: ${installOption.kind}"))
                    }
                }
            }

            if (installResult.isFailure) {
                val error = installResult.exceptionOrNull()!!
                Log.e(TAG, "Installation failed", error)
                return Result.failure(
                    GatewayError(
                        code = "INSTALLATION_FAILED",
                        message = "Failed to install skill: ${error.message}"
                    )
                )
            }

            val installed = installResult.getOrNull()!!

            // 6. 返回成功结果
            val result = JsonObject().apply {
                addProperty("ok", true)
                addProperty("message", "Skill installed successfully")
                addProperty("stdout", "Installed ${installed.name}@${installed.version} to ${installed.path}")
                addProperty("stderr", "")
                addProperty("code", 0)
                add("details", JsonObject().apply {
                    addProperty("slug", installed.slug)
                    addProperty("name", installed.name)
                    addProperty("version", installed.version)
                    addProperty("path", installed.path)
                    addProperty("hash", installed.hash)
                })
            }

            Log.i(TAG, "✅ skills.install: $name@${installed.version}")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "skills.install failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_INSTALL_FAILED",
                    message = "Failed to install skill: ${e.message}"
                )
            )
        }
    }

    /**
     * skills.update - 更新技能配置
     *
     * 参数:
     * {
     *   skillKey: string;
     *   enabled?: boolean;
     *   apiKey?: string;
     *   env?: Record<string, string>;
     * }
     *
     * 返回:
     * {
     *   ok: true;
     *   skillKey: string;
     *   config: { ... }
     * }
     */
    fun update(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.update called")

            // 1. 解析参数
            val skillKey = params.get("skillKey")?.asString
                ?: return Result.failure(
                    GatewayError(code = "INVALID_PARAMS", message = "Missing required parameter: skillKey")
                )

            val enabled = params.get("enabled")?.asBoolean
            val apiKey = params.get("apiKey")?.asString
            val env = params.getAsJsonObject("env")?.let { envObj ->
                envObj.entrySet().associate { it.key to it.value.asString }
            }

            Log.d(TAG, "  skillKey: $skillKey")
            Log.d(TAG, "  enabled: $enabled")
            Log.d(TAG, "  apiKey: ${if (apiKey != null) "***" else "null"}")
            Log.d(TAG, "  env: $env")

            // 2. 加载当前配置
            val config = configLoader.loadOpenClawConfig()

            // 3. 更新技能配置
            val existingConfig = config.skills.entries[skillKey] ?: com.xiaomo.androidforclaw.config.SkillConfig()

            val updatedConfig = existingConfig.copy(
                enabled = enabled ?: existingConfig.enabled,
                apiKey = apiKey ?: existingConfig.apiKey,
                env = env ?: existingConfig.env
            )

            // 4. 写回配置文件
            val updatedEntries = config.skills.entries.toMutableMap()
            updatedEntries[skillKey] = updatedConfig

            val newConfig = config.copy(
                skills = config.skills.copy(
                    entries = updatedEntries
                )
            )

            val saved = configLoader.saveOpenClawConfig(newConfig)
            if (!saved) {
                return Result.failure(
                    GatewayError(
                        code = "CONFIG_SAVE_FAILED",
                        message = "Failed to save config"
                    )
                )
            }

            // 5. 返回结果
            val result = JsonObject().apply {
                addProperty("ok", true)
                addProperty("skillKey", skillKey)
                add("config", JsonObject().apply {
                    addProperty("enabled", updatedConfig.enabled)
                    updatedConfig.apiKey?.let { addProperty("apiKey", it) }
                    updatedConfig.env?.let { add("env", com.google.gson.Gson().toJsonTree(it)) }
                })
            }

            Log.i(TAG, "✅ skills.update: $skillKey")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "skills.update failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_UPDATE_FAILED",
                    message = "Failed to update skill: ${e.message}"
                )
            )
        }
    }

    /**
     * skills.reload - 重新加载所有技能
     *
     * 参数: {}
     *
     * 返回:
     * {
     *   ok: true;
     *   message: string;
     *   count: number;
     * }
     */
    fun reload(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.reload called")

            // 1. 重新加载配置
            configLoader.reloadOpenClawConfig()

            // 2. 重新构建技能状态
            val report = statusBuilder.buildStatus()

            // 3. 返回结果
            val result = JsonObject().apply {
                addProperty("ok", true)
                addProperty("message", "Skills reloaded successfully")
                addProperty("count", report.skills.size)
                add("skills", com.google.gson.JsonArray().apply {
                    report.skills.forEach { skill ->
                        add(skill.name)
                    }
                })
            }

            Log.i(TAG, "✅ skills.reload: ${report.skills.size} skills")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "skills.reload failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_RELOAD_FAILED",
                    message = "Failed to reload skills: ${e.message}"
                )
            )
        }
    }

    /**
     * skills.search - 搜索 ClawHub 技能
     *
     * 参数:
     * {
     *   query: string;
     *   limit?: number;
     *   offset?: number;
     * }
     *
     * 返回: SkillSearchResult
     */
    fun search(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.search called")

            val query = params.get("query")?.asString
                ?: return Result.failure(
                    GatewayError(code = "INVALID_PARAMS", message = "Missing required parameter: query")
                )

            val limit = params.get("limit")?.asInt ?: 20
            val offset = params.get("offset")?.asInt ?: 0

            Log.d(TAG, "  query: $query")
            Log.d(TAG, "  limit: $limit, offset: $offset")

            // 调用 ClawHub API
            val clawHubClient = ClawHubClient()
            val searchResult = runBlocking {
                clawHubClient.searchSkills(query, limit, offset)
            }

            if (searchResult.isFailure) {
                return Result.failure(
                    GatewayError(
                        code = "SEARCH_FAILED",
                        message = "Failed to search skills: ${searchResult.exceptionOrNull()?.message}"
                    )
                )
            }

            val result = searchResult.getOrNull()!!
            val json = com.google.gson.Gson().toJsonTree(result).asJsonObject

            Log.i(TAG, "✅ skills.search: ${result.skills.size} results")
            Result.success(json)

        } catch (e: Exception) {
            Log.e(TAG, "skills.search failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_SEARCH_FAILED",
                    message = "Failed to search skills: ${e.message}"
                )
            )
        }
    }

    /**
     * skills.uninstall - 卸载技能
     *
     * 参数:
     * {
     *   slug: string;
     * }
     *
     * 返回:
     * {
     *   ok: true;
     *   message: string;
     * }
     */
    fun uninstall(params: JsonObject): Result<JsonObject> {
        return try {
            Log.d(TAG, "skills.uninstall called")

            val slug = params.get("slug")?.asString
                ?: return Result.failure(
                    GatewayError(code = "INVALID_PARAMS", message = "Missing required parameter: slug")
                )

            Log.d(TAG, "  slug: $slug")

            // 执行卸载
            val uninstallResult = runBlocking {
                installer.uninstall(slug)
            }

            if (uninstallResult.isFailure) {
                return Result.failure(
                    GatewayError(
                        code = "UNINSTALL_FAILED",
                        message = "Failed to uninstall skill: ${uninstallResult.exceptionOrNull()?.message}"
                    )
                )
            }

            val result = JsonObject().apply {
                addProperty("ok", true)
                addProperty("message", "Skill uninstalled successfully")
                addProperty("slug", slug)
            }

            Log.i(TAG, "✅ skills.uninstall: $slug")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "skills.uninstall failed", e)
            Result.failure(
                GatewayError(
                    code = "SKILLS_UNINSTALL_FAILED",
                    message = "Failed to uninstall skill: ${e.message}"
                )
            )
        }
    }
}
