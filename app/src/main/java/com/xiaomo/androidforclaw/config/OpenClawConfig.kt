package com.xiaomo.androidforclaw.config

import com.google.gson.annotations.SerializedName

/**
 * OpenClaw 主配置 (openclaw.json)
 *
 * 对齐 OpenClaw 的配置格式
 * 参考：OpenClaw src/config/types.config.ts
 *
 * 配置文件位置：/sdcard/.androidforclaw/config/openclaw.json
 */

/**
 * 主配置
 */
data class OpenClawConfig(
    @SerializedName("thinking")
    val thinking: ThinkingConfig = ThinkingConfig(),

    @SerializedName("agent")
    val agent: AgentConfig = AgentConfig(),

    @SerializedName("agents")
    val agents: AgentsConfig? = null,

    @SerializedName("models")
    val models: ModelsConfig? = null,

    @SerializedName("skills")
    val skills: SkillsConfig = SkillsConfig(),

    @SerializedName("tools")
    val tools: ToolsConfig = ToolsConfig(),

    @SerializedName("gateway")
    val gateway: GatewayConfig = GatewayConfig(),

    @SerializedName("ui")
    val ui: UIConfig = UIConfig(),

    @SerializedName("logging")
    val logging: LoggingConfig = LoggingConfig(),

    @SerializedName("memory")
    val memory: MemoryConfig = MemoryConfig(),

    @SerializedName("session")
    val session: SessionConfig = SessionConfig(),

    @SerializedName("providers")
    val providers: Map<String, ProviderConfig> = emptyMap()
) {
    // 兼容性辅助方法:获取 providers (优先从 models.providers,否则从 providers)
    fun resolveProviders(): Map<String, ProviderConfig> {
        return models?.providers ?: providers
    }

    // 兼容性辅助方法:获取默认模型 (优先从 agents.defaults.model.primary,否则从 agent.defaultModel)
    fun resolveDefaultModel(): String {
        return agents?.defaults?.model?.primary ?: agent.defaultModel
    }
}

/**
 * Extended Thinking 配置
 */
data class ThinkingConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,  // 是否启用 Extended Thinking

    @SerializedName("budgetTokens")
    val budgetTokens: Int = 10000,  // 思考预算 (tokens)

    @SerializedName("showInUI")
    val showInUI: Boolean = true,  // 是否在 UI 中显示思考过程

    @SerializedName("logToFile")
    val logToFile: Boolean = false  // 是否将思考过程记录到文件
)

/**
 * Agent 配置
 */
data class AgentConfig(
    @SerializedName("maxIterations")
    val maxIterations: Int = 20,  // 最大迭代次数

    @SerializedName("defaultModel")
    val defaultModel: String = "anthropic/claude-opus-4.6",  // 默认模型（配置文件未指定时使用）

    @SerializedName("timeout")
    val timeout: Long = 300000,  // 超时时间 (ms)

    @SerializedName("retryOnError")
    val retryOnError: Boolean = true,  // 错误时是否重试

    @SerializedName("maxRetries")
    val maxRetries: Int = 3,  // 最大重试次数

    @SerializedName("mode")
    val mode: String = "exploration"  // 模式: "exploration" | "planning"
)

/**
 * Skills 配置
 */
data class SkillsConfig(
    @SerializedName("bundledPath")
    val bundledPath: String = "assets/skills",  // Bundled skills 路径

    @SerializedName("workspacePath")
    val workspacePath: String = "/sdcard/.androidforclaw/workspace/skills",  // Workspace skills

    @SerializedName("managedPath")
    val managedPath: String = "/sdcard/.androidforclaw/skills",  // Managed skills

    @SerializedName("autoLoad")
    val autoLoad: List<String> = listOf("mobile-operations"),  // 自动加载的 skills

    @SerializedName("allowBundled")
    val allowBundled: List<String>? = null,  // 内置技能白名单 (null = 全部允许)

    @SerializedName("disabled")
    val disabled: List<String> = emptyList(),  // 禁用的 skills

    @SerializedName("onDemand")
    val onDemand: Boolean = true,  // 是否按需加载

    @SerializedName("cacheEnabled")
    val cacheEnabled: Boolean = true,  // 是否缓存 skills 内容

    @SerializedName("entries")
    val entries: Map<String, SkillConfig> = emptyMap()  // 技能配置 (对齐 OpenClaw)
)

/**
 * 单个技能配置 (对齐 OpenClaw)
 */
data class SkillConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,  // 是否启用

    @SerializedName("apiKey")
    val apiKey: String? = null,  // API Key (或 { source: "env", ... })

    @SerializedName("env")
    val env: Map<String, String>? = null  // 环境变量注入
)

/**
 * Tools 配置
 */
data class ToolsConfig(
    @SerializedName("screenshot")
    val screenshot: ScreenshotToolConfig = ScreenshotToolConfig(),

    @SerializedName("accessibility")
    val accessibility: AccessibilityToolConfig = AccessibilityToolConfig(),

    @SerializedName("exec")
    val exec: ExecToolConfig = ExecToolConfig(),

    @SerializedName("browser")
    val browser: BrowserToolConfig = BrowserToolConfig()
)

/**
 * 截图工具配置
 */
data class ScreenshotToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("quality")
    val quality: Int = 85,  // JPEG 质量 (0-100)

    @SerializedName("maxWidth")
    val maxWidth: Int = 1080,  // 最大宽度 (自动缩放)

    @SerializedName("format")
    val format: String = "jpeg",  // "jpeg" | "png" | "webp"

    @SerializedName("hideFloatingWindow")
    val hideFloatingWindow: Boolean = true  // 截图时隐藏悬浮窗
)

/**
 * Accessibility 工具配置
 */
data class AccessibilityToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("gestureDuration")
    val gestureDuration: Long = 100,  // 手势持续时间 (ms)

    @SerializedName("enableUITree")
    val enableUITree: Boolean = true,  // 是否启用 UI 树获取

    @SerializedName("maxUITreeDepth")
    val maxUITreeDepth: Int = 20  // UI 树最大深度
)

/**
 * 执行工具配置
 */
data class ExecToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("allowRoot")
    val allowRoot: Boolean = false,  // 是否允许 root 命令

    @SerializedName("timeout")
    val timeout: Long = 30000,  // 命令超时 (ms)

    @SerializedName("blocklist")
    val blocklist: List<String> = listOf("rm -rf /", "dd if=", "format")  // 禁止的命令
)

/**
 * 浏览器工具配置
 */
data class BrowserToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("userAgent")
    val userAgent: String? = null,  // 自定义 User-Agent

    @SerializedName("timeout")
    val timeout: Long = 30000  // 页面加载超时 (ms)
)

/**
 * Gateway 配置
 */
data class GatewayConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("port")
    val port: Int = 8080,  // WebSocket/HTTP 端口

    @SerializedName("host")
    val host: String = "0.0.0.0",  // 监听地址

    @SerializedName("security")
    val security: SecurityConfig = SecurityConfig(),

    @SerializedName("channels")
    val channels: List<String> = listOf("app", "webui", "adb"),  // 启用的渠道

    @SerializedName("feishu")
    val feishu: FeishuChannelConfig = FeishuChannelConfig(),

    @SerializedName("discord")
    val discord: DiscordChannelConfig? = null
)

/**
 * 安全配置
 */
data class SecurityConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,  // 是否启用安全机制

    @SerializedName("pairingRequired")
    val pairingRequired: Boolean = false,  // 是否需要配对

    @SerializedName("allowlist")
    val allowlist: List<String> = emptyList(),  // 白名单 (用户/设备 ID)

    @SerializedName("rateLimit")
    val rateLimit: RateLimitConfig = RateLimitConfig()
)

/**
 * 速率限制配置
 */
data class RateLimitConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("maxRequests")
    val maxRequests: Int = 100,  // 最大请求数

    @SerializedName("windowMs")
    val windowMs: Long = 60000  // 时间窗口 (ms)
)

/**
 * UI 配置
 */
data class UIConfig(
    @SerializedName("floatingWindow")
    val floatingWindow: FloatingWindowConfig = FloatingWindowConfig(),

    @SerializedName("theme")
    val theme: String = "auto",  // "light" | "dark" | "auto"

    @SerializedName("language")
    val language: String = "zh"  // "zh" | "en"
)

/**
 * 悬浮窗配置
 */
data class FloatingWindowConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("showProgress")
    val showProgress: Boolean = true,  // 显示进度

    @SerializedName("showReasoningContent")
    val showReasoningContent: Boolean = true,  // 显示思考内容

    @SerializedName("autoHide")
    val autoHide: Boolean = false,  // 自动隐藏

    @SerializedName("opacity")
    val opacity: Float = 0.9f,  // 不透明度 (0.0 - 1.0)

    @SerializedName("position")
    val position: String = "top-right"  // "top-left" | "top-right" | "bottom-left" | "bottom-right"
)

/**
 * 日志配置
 */
data class LoggingConfig(
    @SerializedName("level")
    val level: String = "INFO",  // "DEBUG" | "INFO" | "WARN" | "ERROR"

    @SerializedName("logToFile")
    val logToFile: Boolean = true,

    @SerializedName("logPath")
    val logPath: String = "/sdcard/.androidforclaw/logs",

    @SerializedName("maxFileSize")
    val maxFileSize: Long = 10 * 1024 * 1024,  // 10MB

    @SerializedName("maxFiles")
    val maxFiles: Int = 5,  // 最多保留文件数

    @SerializedName("includeTimestamp")
    val includeTimestamp: Boolean = true,

    @SerializedName("logLLMCalls")
    val logLLMCalls: Boolean = true,  // 记录 LLM 调用

    @SerializedName("logToolCalls")
    val logToolCalls: Boolean = true  // 记录工具调用
)

/**
 * 内存配置
 */
data class MemoryConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("path")
    val path: String = "/sdcard/.androidforclaw/workspace/memory",

    @SerializedName("autoSave")
    val autoSave: Boolean = true,

    @SerializedName("maxEntries")
    val maxEntries: Int = 1000  // 最大记忆条目数
)

/**
 * Session 配置
 */
data class SessionConfig(
    @SerializedName("defaultKey")
    val defaultKey: String = "default",

    @SerializedName("storagePath")
    val storagePath: String = "/data/data/com.xiaomo.androidforclaw/files/sessions",

    @SerializedName("autoSave")
    val autoSave: Boolean = true,

    @SerializedName("maxMessages")
    val maxMessages: Int = 100,  // 单个 session 最大消息数

    @SerializedName("compression")
    val compression: Boolean = false  // 是否压缩存储
)

/**
 * 飞书 Channel 配置
 * 对齐 clawdbot-feishu 配置结构
 */
data class FeishuChannelConfig(
    // ===== 基础配置 =====
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("appId")
    val appId: String = "",

    @SerializedName("appSecret")
    val appSecret: String = "",

    @SerializedName("encryptKey")
    val encryptKey: String? = null,

    @SerializedName("verificationToken")
    val verificationToken: String? = null,

    // ===== 域名配置 =====
    @SerializedName("domain")
    val domain: String = "feishu",  // "feishu", "lark", or custom domain

    // ===== 连接模式 =====
    @SerializedName("connectionMode")
    val connectionMode: String = "websocket",  // "websocket" | "webhook"

    @SerializedName("webhookPath")
    val webhookPath: String = "/feishu/webhook",

    @SerializedName("webhookPort")
    val webhookPort: Int = 8765,

    // ===== DM 策略 =====
    @SerializedName("dmPolicy")
    val dmPolicy: String = "pairing",  // "open" | "pairing" | "allowlist"

    @SerializedName("allowFrom")
    val allowFrom: List<String> = emptyList(),

    // ===== 群组策略 =====
    @SerializedName("groupPolicy")
    val groupPolicy: String = "allowlist",  // "open" | "allowlist" | "disabled"

    @SerializedName("groupAllowFrom")
    val groupAllowFrom: List<String> = emptyList(),

    @SerializedName("requireMention")
    val requireMention: Boolean = true,

    @SerializedName("groupCommandMentionBypass")
    val groupCommandMentionBypass: String = "never",  // "never" | "single_bot" | "always"

    @SerializedName("allowMentionlessInMultiBotGroup")
    val allowMentionlessInMultiBotGroup: Boolean = false,

    // ===== 会话模式 =====
    @SerializedName("topicSessionMode")
    val topicSessionMode: String = "disabled",  // "disabled" | "enabled"

    // ===== 历史记录 =====
    @SerializedName("historyLimit")
    val historyLimit: Int = 20,

    @SerializedName("dmHistoryLimit")
    val dmHistoryLimit: Int = 100,

    // ===== 消息分块 =====
    @SerializedName("textChunkLimit")
    val textChunkLimit: Int = 4000,

    @SerializedName("chunkMode")
    val chunkMode: String = "length",  // "length" | "newline"

    // ===== 媒体配置 =====
    @SerializedName("mediaMaxMb")
    val mediaMaxMb: Double = 20.0,

    @SerializedName("audioMaxDurationSec")
    val audioMaxDurationSec: Int = 300,

    // ===== 工具配置 =====
    @SerializedName("enableDocTools")
    val enableDocTools: Boolean = true,

    @SerializedName("enableWikiTools")
    val enableWikiTools: Boolean = true,

    @SerializedName("enableDriveTools")
    val enableDriveTools: Boolean = true,

    @SerializedName("enableBitableTools")
    val enableBitableTools: Boolean = true,

    @SerializedName("enableTaskTools")
    val enableTaskTools: Boolean = true,

    @SerializedName("enableChatTools")
    val enableChatTools: Boolean = true,

    @SerializedName("enablePermTools")
    val enablePermTools: Boolean = true,

    @SerializedName("enableUrgentTools")
    val enableUrgentTools: Boolean = true,

    // ===== 队列配置（对齐 OpenClaw）=====
    @SerializedName("queueMode")
    val queueMode: String? = "followup",  // "interrupt" | "steer" | "followup" | "collect" | "queue"

    @SerializedName("queueCap")
    val queueCap: Int = 10,  // 队列容量上限

    @SerializedName("queueDropPolicy")
    val queueDropPolicy: String = "old",  // "old" | "new" | "summarize"

    @SerializedName("queueDebounceMs")
    val queueDebounceMs: Int = 100,  // 防抖时间（ms）

    // ===== 其他配置 =====
    @SerializedName("typingIndicator")
    val typingIndicator: Boolean = true,

    @SerializedName("reactionDedup")
    val reactionDedup: Boolean = true,

    @SerializedName("debugMode")
    val debugMode: Boolean = false
)

/**
 * 配置常量
 */
object ConfigDefaults {
    // Extended Thinking
    const val DEFAULT_THINKING_BUDGET = 10000
    const val MIN_THINKING_BUDGET = 1000
    const val MAX_THINKING_BUDGET = 50000

    // Agent
    const val DEFAULT_MAX_ITERATIONS = 20
    const val MIN_MAX_ITERATIONS = 1
    const val MAX_MAX_ITERATIONS = 100

    const val DEFAULT_TIMEOUT_MS = 300000L  // 5 minutes
    const val MIN_TIMEOUT_MS = 10000L
    const val MAX_TIMEOUT_MS = 3600000L  // 1 hour

    // Screenshot
    const val DEFAULT_SCREENSHOT_QUALITY = 85
    const val MIN_SCREENSHOT_QUALITY = 10
    const val MAX_SCREENSHOT_QUALITY = 100

    const val DEFAULT_SCREENSHOT_MAX_WIDTH = 1080

    // Accessibility
    const val DEFAULT_GESTURE_DURATION = 100L
    const val MIN_GESTURE_DURATION = 50L
    const val MAX_GESTURE_DURATION = 5000L

    // Gateway
    const val DEFAULT_GATEWAY_PORT = 8080
    const val MIN_GATEWAY_PORT = 1024
    const val MAX_GATEWAY_PORT = 65535

    // Logging
    const val DEFAULT_LOG_MAX_FILE_SIZE = 10 * 1024 * 1024L  // 10MB
    const val DEFAULT_LOG_MAX_FILES = 5
}

/**
 * Discord Channel 配置
 * 对齐 OpenClaw Discord 扩展配置结构
 */
data class DiscordChannelConfig(
    // ===== 基础配置 =====
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("name")
    val name: String? = null,

    // ===== DM (私聊) 配置 =====
    @SerializedName("dm")
    val dm: DmPolicyConfig? = null,

    // ===== Guild (服务器) 配置 =====
    @SerializedName("groupPolicy")
    val groupPolicy: String? = null,  // "open", "allowlist", "denylist"

    @SerializedName("guilds")
    val guilds: Map<String, GuildPolicyConfig>? = null,

    // ===== 消息配置 =====
    @SerializedName("replyToMode")
    val replyToMode: String? = null,  // "off", "always", "threads"

    // ===== 多账户配置 =====
    @SerializedName("accounts")
    val accounts: Map<String, DiscordAccountPolicyConfig>? = null
)

/**
 * Discord DM 策略配置
 */
data class DmPolicyConfig(
    @SerializedName("policy")
    val policy: String? = "pairing",  // "open", "pairing", "allowlist", "denylist"

    @SerializedName("allowFrom")
    val allowFrom: List<String>? = null
)

/**
 * Discord Guild 策略配置
 */
data class GuildPolicyConfig(
    @SerializedName("channels")
    val channels: List<String>? = null,  // Channel IDs

    @SerializedName("requireMention")
    val requireMention: Boolean? = true,

    @SerializedName("toolPolicy")
    val toolPolicy: String? = null  // "default", "restricted", "full"
)

/**
 * Discord 账户策略配置
 */
data class DiscordAccountPolicyConfig(
    @SerializedName("enabled")
    val enabled: Boolean? = true,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("dm")
    val dm: DmPolicyConfig? = null,

    @SerializedName("guilds")
    val guilds: Map<String, GuildPolicyConfig>? = null
)

/**
 * Agents 配置 (OpenClaw 格式)
 */
data class AgentsConfig(
    @SerializedName("defaults")
    val defaults: AgentDefaultsConfig = AgentDefaultsConfig()
)

/**
 * Agent 默认配置
 */
data class AgentDefaultsConfig(
    @SerializedName("model")
    val model: ModelSelectionConfig? = null
)

/**
 * Model 选择配置
 */
data class ModelSelectionConfig(
    @SerializedName("primary")
    val primary: String? = null,

    @SerializedName("fallbacks")
    val fallbacks: List<String>? = null
)
