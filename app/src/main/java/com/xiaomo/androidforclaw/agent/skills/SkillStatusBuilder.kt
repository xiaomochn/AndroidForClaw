package com.xiaomo.androidforclaw.agent.skills

import android.content.Context
import android.util.Log
import com.xiaomo.androidforclaw.config.ConfigLoader
import java.io.File

/**
 * 技能状态构建器
 *
 * 对齐 OpenClaw 的 buildWorkspaceSkillStatus()
 * 扫描技能目录,评估技能资格,生成状态报告
 */
class SkillStatusBuilder(private val context: Context) {
    companion object {
        private const val TAG = "SkillStatusBuilder"
    }

    private val configLoader = ConfigLoader(context)
    private val parser = SkillFrontmatterParser()

    /**
     * 构建技能状态报告
     *
     * @param workspacePath 工作区路径 (默认: /sdcard/.androidforclaw/workspace)
     * @return SkillStatusReport
     */
    fun buildStatus(workspacePath: String = "/sdcard/.androidforclaw/workspace"): SkillStatusReport {
        val managedSkillsDir = "/sdcard/.androidforclaw/skills"
        val bundledSkillsPath = "skills" // assets 路径

        val skillEntries = mutableListOf<SkillStatusEntry>()

        // 1. 加载内置技能 (bundled)
        Log.d(TAG, "Loading bundled skills from assets://$bundledSkillsPath")
        loadSkillsFromAssets(bundledSkillsPath).forEach { entry ->
            skillEntries.add(buildStatusEntry(entry, SkillSource.BUNDLED))
        }

        // 2. 加载托管技能 (managed)
        val managedDir = File(managedSkillsDir)
        if (managedDir.exists() && managedDir.isDirectory) {
            Log.d(TAG, "Loading managed skills from $managedSkillsDir")
            loadSkillsFromDirectory(managedDir).forEach { entry ->
                skillEntries.add(buildStatusEntry(entry, SkillSource.MANAGED))
            }
        }

        // 3. 加载工作区技能 (workspace)
        val workspaceSkillsDir = File(workspacePath, "skills")
        if (workspaceSkillsDir.exists() && workspaceSkillsDir.isDirectory) {
            Log.d(TAG, "Loading workspace skills from ${workspaceSkillsDir.absolutePath}")
            loadSkillsFromDirectory(workspaceSkillsDir).forEach { entry ->
                skillEntries.add(buildStatusEntry(entry, SkillSource.WORKSPACE))
            }
        }

        Log.i(TAG, "✅ Loaded ${skillEntries.size} skills total")

        return SkillStatusReport(
            workspaceDir = workspacePath,
            managedSkillsDir = managedSkillsDir,
            skills = skillEntries
        )
    }

    /**
     * 从 assets 加载技能
     */
    private fun loadSkillsFromAssets(assetsPath: String): List<SkillEntry> {
        val entries = mutableListOf<SkillEntry>()

        try {
            val assetManager = context.assets
            val skillDirs = assetManager.list(assetsPath) ?: emptyArray()

            for (skillDir in skillDirs) {
                val skillPath = "$assetsPath/$skillDir"
                val files = assetManager.list(skillPath) ?: emptyArray()

                if ("SKILL.md" in files) {
                    val skillMdPath = "$skillPath/SKILL.md"
                    val content = assetManager.open(skillMdPath).bufferedReader().use { it.readText() }

                    val parseResult = parser.parse(content)
                    if (parseResult is SkillFrontmatterParser.ParseResult.Success) {
                        entries.add(
                            SkillEntry(
                                skill = Skill(
                                    name = parseResult.frontmatter.name,
                                    description = parseResult.frontmatter.description,
                                    content = parseResult.content,
                                    filePath = "assets://$skillMdPath"
                                ),
                                frontmatter = parseResult.frontmatter,
                                metadata = parseResult.openclawMetadata
                            )
                        )
                    } else if (parseResult is SkillFrontmatterParser.ParseResult.Error) {
                        Log.w(TAG, "Failed to parse $skillMdPath: ${parseResult.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load skills from assets", e)
        }

        return entries
    }

    /**
     * 从文件系统目录加载技能
     */
    private fun loadSkillsFromDirectory(directory: File): List<SkillEntry> {
        val entries = mutableListOf<SkillEntry>()

        directory.listFiles()?.forEach { skillDir ->
            if (skillDir.isDirectory) {
                val skillMdFile = File(skillDir, "SKILL.md")
                if (skillMdFile.exists()) {
                    try {
                        val content = skillMdFile.readText()
                        val parseResult = parser.parse(content)

                        if (parseResult is SkillFrontmatterParser.ParseResult.Success) {
                            entries.add(
                                SkillEntry(
                                    skill = Skill(
                                        name = parseResult.frontmatter.name,
                                        description = parseResult.frontmatter.description,
                                        content = parseResult.content,
                                        filePath = skillMdFile.absolutePath
                                    ),
                                    frontmatter = parseResult.frontmatter,
                                    metadata = parseResult.openclawMetadata
                                )
                            )
                        } else if (parseResult is SkillFrontmatterParser.ParseResult.Error) {
                            Log.w(TAG, "Failed to parse ${skillMdFile.absolutePath}: ${parseResult.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read ${skillMdFile.absolutePath}", e)
                    }
                }
            }
        }

        return entries
    }

    /**
     * 构建单个技能的状态条目
     */
    private fun buildStatusEntry(entry: SkillEntry, source: SkillSource): SkillStatusEntry {
        val config = configLoader.loadOpenClawConfig()
        val skillConfig = config.skills.entries[entry.skill.name]

        // 检查是否被配置禁用
        val disabled = skillConfig?.enabled == false

        // 检查是否被白名单阻止 (bundled skills 需要在白名单中)
        val blockedByAllowlist = if (source == SkillSource.BUNDLED) {
            val allowBundled = config.skills.allowBundled
            allowBundled != null && entry.skill.name !in allowBundled
        } else {
            false
        }

        // 检查要求和缺失
        val (requirements, missing) = checkRequirements(entry.metadata?.requires)

        // 检查配置项
        val configChecks = checkConfigPaths(entry.metadata?.requires?.config, config)

        // 检查平台兼容性
        val platformCompatible = checkPlatformCompatibility(entry.metadata?.os)

        // 评估资格
        val eligible = !disabled &&
                !blockedByAllowlist &&
                platformCompatible &&
                (missing == null || (missing.bins.isNullOrEmpty() &&
                        missing.anyBins.isNullOrEmpty() &&
                        missing.env.isNullOrEmpty() &&
                        missing.config.isNullOrEmpty()))

        // 构建安装选项
        val installOptions = buildInstallOptions(entry.metadata?.install)

        return SkillStatusEntry(
            name = entry.skill.name,
            description = entry.skill.description,
            source = source,
            bundled = source == SkillSource.BUNDLED,
            filePath = entry.skill.filePath,
            baseDir = File(entry.skill.filePath).parent ?: "",
            skillKey = entry.metadata?.skillKey ?: entry.skill.name,
            primaryEnv = entry.metadata?.primaryEnv,
            emoji = entry.metadata?.emoji,
            homepage = entry.metadata?.homepage,
            always = entry.metadata?.always ?: false,
            disabled = disabled,
            blockedByAllowlist = blockedByAllowlist,
            eligible = eligible,
            requirements = requirements,
            missing = missing,
            configChecks = configChecks,
            install = installOptions
        )
    }

    /**
     * 检查技能要求
     *
     * @return Pair<requirements, missing>
     */
    private fun checkRequirements(requires: SkillRequirements?): Pair<SkillRequirements?, SkillRequirements?> {
        if (requires == null) {
            return Pair(null, null)
        }

        val missingBins = mutableListOf<String>()
        val missingAnyBins = mutableListOf<String>()
        val missingEnv = mutableListOf<String>()
        val missingConfig = mutableListOf<String>()

        // 检查二进制文件 (Android 上不适用,跳过)
        // Android 不使用 PATH 二进制,而是使用权限和 APK

        // 检查环境变量
        requires.env?.forEach { envVar ->
            if (System.getenv(envVar) == null) {
                missingEnv.add(envVar)
            }
        }

        // anyBins - 至少一个存在 (Android 上不适用)

        // config 路径检查在 checkConfigPaths 中处理

        val missing = if (missingBins.isNotEmpty() ||
            missingAnyBins.isNotEmpty() ||
            missingEnv.isNotEmpty() ||
            missingConfig.isNotEmpty()
        ) {
            SkillRequirements(
                bins = missingBins.takeIf { it.isNotEmpty() },
                anyBins = missingAnyBins.takeIf { it.isNotEmpty() },
                env = missingEnv.takeIf { it.isNotEmpty() },
                config = missingConfig.takeIf { it.isNotEmpty() }
            )
        } else {
            null
        }

        return Pair(requires, missing)
    }

    /**
     * 检查配置路径
     */
    private fun checkConfigPaths(
        configPaths: List<String>?,
        config: com.xiaomo.androidforclaw.config.OpenClawConfig
    ): List<SkillConfigCheck> {
        if (configPaths == null) {
            return emptyList()
        }

        return configPaths.map { path ->
            val value = getConfigValue(path, config)
            SkillConfigCheck(
                path = path,
                exists = value != null,
                value = value
            )
        }
    }

    /**
     * 获取配置值 (简化实现)
     */
    private fun getConfigValue(path: String, config: com.xiaomo.androidforclaw.config.OpenClawConfig): Any? {
        // 简单的路径解析 (支持 "gateway.enabled", "agent.maxIterations" 等)
        val parts = path.split(".")
        return when {
            parts.size == 2 && parts[0] == "gateway" && parts[1] == "enabled" -> config.gateway.enabled
            parts.size == 2 && parts[0] == "agent" && parts[1] == "maxIterations" -> config.agent.maxIterations
            // 可扩展更多路径
            else -> null
        }
    }

    /**
     * 检查平台兼容性
     */
    private fun checkPlatformCompatibility(osList: List<String>?): Boolean {
        if (osList == null) {
            return true // 无限制
        }

        // Android 平台标识
        return "android" in osList.map { it.lowercase() }
    }

    /**
     * 构建安装选项
     */
    private fun buildInstallOptions(installSpecs: List<SkillInstallSpec>?): List<SkillInstallOption> {
        if (installSpecs == null) {
            return emptyList()
        }

        return installSpecs.map { spec ->
            val (available, reason) = checkInstallAvailability(spec)

            SkillInstallOption(
                installId = spec.id ?: "${spec.kind.name.lowercase()}-default",
                kind = spec.kind,
                label = spec.label ?: "Install via ${spec.kind.name}",
                available = available,
                reason = reason
            )
        }
    }

    /**
     * 检查安装器可用性
     */
    private fun checkInstallAvailability(spec: SkillInstallSpec): Pair<Boolean, String?> {
        // Android 平台检查
        val platformCompatible = spec.os?.let { osList ->
            "android" in osList.map { it.lowercase() }
        } ?: true

        if (!platformCompatible) {
            return Pair(false, "Platform not supported")
        }

        // 根据安装器类型检查
        return when (spec.kind) {
            InstallKind.APK -> {
                // APK 安装 (Android 特有)
                if (spec.url != null) {
                    Pair(true, null)
                } else {
                    Pair(false, "Missing APK URL")
                }
            }

            InstallKind.DOWNLOAD -> {
                // 直接下载
                if (spec.url != null) {
                    Pair(true, null)
                } else {
                    Pair(false, "Missing download URL")
                }
            }

            else -> {
                // brew, node, go, uv 在 Android 上不可用
                Pair(false, "${spec.kind.name} not available on Android")
            }
        }
    }
}
