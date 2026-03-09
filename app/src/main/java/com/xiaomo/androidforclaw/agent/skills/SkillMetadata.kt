package com.xiaomo.androidforclaw.agent.skills

/**
 * OpenClaw Skills 元数据定义
 *
 * 完全对齐 OpenClaw 的 skills/types.ts
 * 用于解析 SKILL.md 中的 metadata.openclaw 字段
 */

/**
 * 技能条目 (对齐 SkillEntry)
 */
data class SkillEntry(
    val skill: Skill,
    val frontmatter: ParsedSkillFrontmatter,
    val metadata: OpenClawSkillMetadata? = null,
    val invocation: SkillInvocationPolicy? = null
)

/**
 * 基础技能信息 (来自 pi-coding-agent)
 */
data class Skill(
    val name: String,
    val description: String,
    val content: String,
    val filePath: String
)

/**
 * 解析后的 Frontmatter
 */
data class ParsedSkillFrontmatter(
    val name: String,
    val description: String,
    val metadata: Map<String, Any?>? = null
)

/**
 * OpenClaw 技能元数据 (对齐 OpenClawSkillMetadata)
 */
data class OpenClawSkillMetadata(
    val always: Boolean = false,
    val skillKey: String? = null,
    val primaryEnv: String? = null,
    val emoji: String? = null,
    val homepage: String? = null,
    val os: List<String>? = null,              // darwin, linux, win32, android
    val requires: SkillRequirements? = null,
    val install: List<SkillInstallSpec>? = null
)

/**
 * 技能要求 (对齐 requires 字段)
 */
data class SkillRequirements(
    val bins: List<String>? = null,            // 必须存在的二进制
    val anyBins: List<String>? = null,         // 至少一个必须存在
    val env: List<String>? = null,             // 必须存在的环境变量
    val config: List<String>? = null           // openclaw.json 路径检查
)

/**
 * 技能安装规范 (对齐 SkillInstallSpec)
 */
data class SkillInstallSpec(
    val id: String? = null,
    val kind: InstallKind,
    val label: String? = null,
    val bins: List<String>? = null,
    val os: List<String>? = null,

    // brew 安装
    val formula: String? = null,

    // npm/yarn/pnpm/bun 安装
    val `package`: String? = null,

    // go 安装
    val module: String? = null,

    // 下载安装
    val url: String? = null,
    val archive: String? = null,               // tar.gz, tar.bz2, zip
    val extract: Boolean? = null,
    val stripComponents: Int? = null,
    val targetDir: String? = null
)

/**
 * 安装器类型
 */
enum class InstallKind {
    BREW,       // Homebrew (macOS/Linux)
    NODE,       // npm/yarn/pnpm/bun
    GO,         // go install
    UV,         // uv (Python)
    DOWNLOAD,   // 直接下载
    APK         // Android APK (Android 特有)
}

/**
 * 技能调用策略 (对齐 SkillInvocationPolicy)
 */
data class SkillInvocationPolicy(
    val invocation: InvocationType? = null,
    val acceptFiles: List<String>? = null,
    val outputPaths: List<String>? = null
)

enum class InvocationType {
    NEVER,
    USER,
    ALWAYS
}

/**
 * 技能状态报告 (对齐 SkillStatusReport)
 */
data class SkillStatusReport(
    val workspaceDir: String,
    val managedSkillsDir: String,
    val skills: List<SkillStatusEntry>
)

/**
 * 技能状态条目 (对齐 SkillStatusEntry)
 */
data class SkillStatusEntry(
    val name: String,
    val description: String,
    val source: SkillSource,
    val bundled: Boolean,
    val filePath: String,
    val baseDir: String,
    val skillKey: String,
    val primaryEnv: String? = null,
    val emoji: String? = null,
    val homepage: String? = null,
    val always: Boolean,
    val disabled: Boolean,
    val blockedByAllowlist: Boolean,
    val eligible: Boolean,
    val requirements: SkillRequirements? = null,
    val missing: SkillRequirements? = null,
    val configChecks: List<SkillConfigCheck>,
    val install: List<SkillInstallOption>
)

// SkillSource 已在 SkillDocument.kt 中定义,此处移除避免重复

/**
 * 配置检查结果
 */
data class SkillConfigCheck(
    val path: String,
    val exists: Boolean,
    val value: Any? = null
)

/**
 * 可用的安装选项
 */
data class SkillInstallOption(
    val installId: String,
    val kind: InstallKind,
    val label: String,
    val available: Boolean,
    val reason: String? = null
)

/**
 * 技能限制配置 (对齐 OpenClaw 默认限制)
 */
data class SkillsLimits(
    val maxCandidatesPerRoot: Int = 300,
    val maxSkillsLoadedPerSource: Int = 200,
    val maxSkillsInPrompt: Int = 150,          // Android 可降至 50-100
    val maxSkillsPromptChars: Int = 30_000,
    val maxSkillFileBytes: Int = 256_000
)
