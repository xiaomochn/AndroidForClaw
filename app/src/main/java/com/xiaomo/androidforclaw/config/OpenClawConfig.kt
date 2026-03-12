package com.xiaomo.androidforclaw.config

import com.google.gson.annotations.SerializedName

/**
 * OpenClaw Main Configuration (openclaw.json)
 *
 * Aligned with OpenClaw config format
 * Reference: OpenClaw src/config/types.config.ts
 *
 * Config file location: /sdcard/.androidforclaw/openclaw.json
 */

/**
 * Main configuration
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

    @SerializedName("plugins")
    val plugins: PluginsConfig = PluginsConfig(),

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
    // Compatibility helper: get providers (priority: models.providers, fallback: providers)
    fun resolveProviders(): Map<String, ProviderConfig> {
        return models?.providers ?: providers
    }

    // Compatibility helper: get default model (priority: agents.defaults.model.primary, fallback: agent.defaultModel)
    fun resolveDefaultModel(): String {
        return agents?.defaults?.model?.primary ?: agent.defaultModel
    }
}

/**
 * Extended Thinking configuration
 */
data class ThinkingConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,  // Whether to enable Extended Thinking

    @SerializedName("budgetTokens")
    val budgetTokens: Int = 10000,  // Thinking budget (tokens)

    @SerializedName("showInUI")
    val showInUI: Boolean = true,  // Whether to show thinking process in UI

    @SerializedName("logToFile")
    val logToFile: Boolean = false  // Whether to log thinking process to file
)

/**
 * Agent configuration
 */
data class AgentConfig(
    @SerializedName("maxIterations")
    val maxIterations: Int = 20,  // Maximum iterations

    @SerializedName("defaultModel")
    val defaultModel: String = "anthropic/claude-opus-4.6",  // Default model (used when not specified in config)

    @SerializedName("timeout")
    val timeout: Long = 300000,  // Timeout (ms)

    @SerializedName("retryOnError")
    val retryOnError: Boolean = true,  // Whether to retry on error

    @SerializedName("maxRetries")
    val maxRetries: Int = 3,  // Maximum retry attempts

    @SerializedName("mode")
    val mode: String = "exploration"  // Mode: "exploration" | "planning"
)

/**
 * Skills configuration
 * Aligned with OpenClaw skills config schema
 */
data class SkillsConfig(
    @SerializedName("bundledPath")
    val bundledPath: String = "assets/skills",  // Bundled skills path

    @SerializedName("workspacePath")
    val workspacePath: String = "/sdcard/.androidforclaw/workspace/skills",  // Workspace skills

    @SerializedName("managedPath")
    val managedPath: String = "/sdcard/.androidforclaw/skills",  // Managed skills

    @SerializedName("autoLoad")
    val autoLoad: List<String> = listOf("mobile-operations"),  // Auto-load skills

    @SerializedName("allowBundled")
    val allowBundled: List<String>? = null,  // Bundled skills whitelist (null = allow all)

    @SerializedName("disabled")
    val disabled: List<String> = emptyList(),  // Disabled skills

    @SerializedName("onDemand")
    val onDemand: Boolean = true,  // Whether to load on demand

    @SerializedName("cacheEnabled")
    val cacheEnabled: Boolean = true,  // Whether to cache skills content

    // --- Aligned with OpenClaw skills.load.* ---

    @SerializedName("extraDirs")
    val extraDirs: List<String> = emptyList(),  // Extra skill directories (lowest priority), aligns with skills.load.extraDirs

    @SerializedName("watch")
    val watch: Boolean = true,  // Watch skill folders and refresh on change, aligns with skills.load.watch

    @SerializedName("watchDebounceMs")
    val watchDebounceMs: Long = 250,  // Debounce for watcher events (ms), aligns with skills.load.watchDebounceMs

    @SerializedName("entries")
    val entries: Map<String, SkillConfig> = emptyMap()  // Skill configs (aligned with OpenClaw)
)

/**
 * Individual skill configuration (aligned with OpenClaw)
 *
 * In OpenClaw:
 * - `enabled`: set false to disable a skill
 * - `env`: environment variables injected for agent run (only if not already set)
 * - `apiKey`: convenience for skills declaring primaryEnv. Supports:
 *   - Plain string: "my-key-value"
 *   - SecretRef object: { "source": "env", "provider": "default", "id": "ENV_VAR_NAME" }
 * - `config`: arbitrary key-value config passed to the skill
 */
data class SkillConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,  // Whether enabled

    @SerializedName("apiKey")
    val apiKey: Any? = null,  // API Key — String or SecretRef map

    @SerializedName("env")
    val env: Map<String, String>? = null,  // Environment variable injection (aligns with OpenClaw)

    @SerializedName("config")
    val config: Map<String, Any>? = null  // Arbitrary skill-level config
) {
    /**
     * Resolve the apiKey to a plain string value.
     * If it's a SecretRef map like { source: "env", id: "GEMINI_API_KEY" },
     * resolve from System.getenv.
     */
    fun resolveApiKey(): String? {
        return when (apiKey) {
            is String -> apiKey
            is Map<*, *> -> {
                val source = apiKey["source"] as? String
                val id = apiKey["id"] as? String
                if (source == "env" && id != null) {
                    System.getenv(id)
                } else {
                    null
                }
            }
            else -> null
        }
    }
}

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
    val quality: Int = 85,  // JPEG quality (0-100)

    @SerializedName("maxWidth")
    val maxWidth: Int = 1080,  // Maximum width (auto-scale)

    @SerializedName("format")
    val format: String = "jpeg",  // "jpeg" | "png" | "webp"

    @SerializedName("hideFloatingWindow")
    val hideFloatingWindow: Boolean = true  // Hide floating window when taking screenshot
)

/**
 * Accessibility 工具配置
 */
data class AccessibilityToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("gestureDuration")
    val gestureDuration: Long = 100,  // Gesture duration (ms)

    @SerializedName("enableUITree")
    val enableUITree: Boolean = true,  // Whether to enable UI tree retrieval

    @SerializedName("maxUITreeDepth")
    val maxUITreeDepth: Int = 20  // Maximum UI tree depth
)

/**
 * 执行工具配置
 */
data class ExecToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("allowRoot")
    val allowRoot: Boolean = false,  // Whether to allow root commands

    @SerializedName("timeout")
    val timeout: Long = 30000,  // Command timeout (ms)

    @SerializedName("blocklist")
    val blocklist: List<String> = listOf("rm -rf /", "dd if=", "format")  // Blocked commands
)

/**
 * 浏览器工具配置
 */
data class BrowserToolConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("userAgent")
    val userAgent: String? = null,  // Custom User-Agent

    @SerializedName("timeout")
    val timeout: Long = 30000  // Page load timeout (ms)
)

/**
 * Gateway 配置
 */
data class GatewayConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("port")
    val port: Int = 8080,  // WebSocket/HTTP port

    @SerializedName("host")
    val host: String = "0.0.0.0",  // Listen address

    @SerializedName("security")
    val security: SecurityConfig = SecurityConfig(),

    @SerializedName("channels")
    val channels: List<String> = listOf("app", "webui", "adb"),  // Enabled channels

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
    val enabled: Boolean = false,  // Whether to enable security mechanism

    @SerializedName("pairingRequired")
    val pairingRequired: Boolean = false,  // Whether pairing is required

    @SerializedName("allowlist")
    val allowlist: List<String> = emptyList(),  // Whitelist (user/device ID)

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
    val maxRequests: Int = 100,  // Maximum requests

    @SerializedName("windowMs")
    val windowMs: Long = 60000  // Time window (ms)
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
    val showProgress: Boolean = true,  // Show progress

    @SerializedName("showReasoningContent")
    val showReasoningContent: Boolean = true,  // Show reasoning content

    @SerializedName("autoHide")
    val autoHide: Boolean = false,  // Auto hide

    @SerializedName("opacity")
    val opacity: Float = 0.9f,  // Opacity (0.0 - 1.0)

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
    val maxFiles: Int = 5,  // Maximum files to keep

    @SerializedName("includeTimestamp")
    val includeTimestamp: Boolean = true,

    @SerializedName("logLLMCalls")
    val logLLMCalls: Boolean = true,  // Log LLM calls

    @SerializedName("logToolCalls")
    val logToolCalls: Boolean = true  // Log tool calls
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
    val maxEntries: Int = 1000  // Maximum memory entries
)

/**
 * Session 配置
 */
data class SessionConfig(
    @SerializedName("defaultKey")
    val defaultKey: String = "default",

    @SerializedName("storagePath")
    val storagePath: String = "/sdcard/.androidforclaw/workspace/sessions",

    @SerializedName("autoSave")
    val autoSave: Boolean = true,

    @SerializedName("maxMessages")
    val maxMessages: Int = 100,  // Maximum messages per session

    @SerializedName("compression")
    val compression: Boolean = false  // Whether to compress storage
)

/**
 * 飞书 Channel 配置
 * Aligned with clawdbot-feishu config structure
 */
data class FeishuChannelConfig(
    // ===== Basic Config =====
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

    // ===== Domain Config =====
    @SerializedName("domain")
    val domain: String = "feishu",  // "feishu", "lark", or custom domain

    // ===== Connection Mode =====
    @SerializedName("connectionMode")
    val connectionMode: String = "websocket",  // "websocket" | "webhook"

    @SerializedName("webhookPath")
    val webhookPath: String = "/feishu/webhook",

    @SerializedName("webhookPort")
    val webhookPort: Int = 8765,

    // ===== DM Policy =====
    @SerializedName("dmPolicy")
    val dmPolicy: String = "pairing",  // "open" | "pairing" | "allowlist"

    @SerializedName("allowFrom")
    val allowFrom: List<String> = emptyList(),

    // ===== Group Policy =====
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

    // ===== Session Mode =====
    @SerializedName("topicSessionMode")
    val topicSessionMode: String = "disabled",  // "disabled" | "enabled"

    // ===== History =====
    @SerializedName("historyLimit")
    val historyLimit: Int = 20,

    @SerializedName("dmHistoryLimit")
    val dmHistoryLimit: Int = 100,

    // ===== Message Chunking =====
    @SerializedName("textChunkLimit")
    val textChunkLimit: Int = 4000,

    @SerializedName("chunkMode")
    val chunkMode: String = "length",  // "length" | "newline"

    // ===== Media Config =====
    @SerializedName("mediaMaxMb")
    val mediaMaxMb: Double = 20.0,

    @SerializedName("audioMaxDurationSec")
    val audioMaxDurationSec: Int = 300,

    // ===== Tools Config =====
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

    // ===== Queue Config (aligned with OpenClaw) =====
    @SerializedName("queueMode")
    val queueMode: String? = "followup",  // "interrupt" | "steer" | "followup" | "collect" | "queue"

    @SerializedName("queueCap")
    val queueCap: Int = 10,  // Queue capacity limit

    @SerializedName("queueDropPolicy")
    val queueDropPolicy: String = "old",  // "old" | "new" | "summarize"

    @SerializedName("queueDebounceMs")
    val queueDebounceMs: Int = 100,  // Debounce time (ms)

    // ===== Other Config =====
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
 * Aligned with OpenClaw Discord extension config structure
 */
data class DiscordChannelConfig(
    // ===== Basic Config =====
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("name")
    val name: String? = null,

    // ===== DM (Direct Message) Config =====
    @SerializedName("dm")
    val dm: DmPolicyConfig? = null,

    // ===== Guild (Server) Config =====
    @SerializedName("groupPolicy")
    val groupPolicy: String? = null,  // "open", "allowlist", "denylist"

    @SerializedName("guilds")
    val guilds: Map<String, GuildPolicyConfig>? = null,

    // ===== Message Config =====
    @SerializedName("replyToMode")
    val replyToMode: String? = null,  // "off", "always", "threads"

    // ===== Multi-Account Config =====
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
    val model: ModelSelectionConfig? = null,

    // Bootstrap file budget (aligned with OpenClaw bootstrap-budget.ts)
    @SerializedName("bootstrapMaxChars")
    val bootstrapMaxChars: Int = 20_000,       // Per-file max chars

    @SerializedName("bootstrapTotalMaxChars")
    val bootstrapTotalMaxChars: Int = 150_000   // Total max chars across all bootstrap files
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

/**
 * Plugins configuration (aligns with OpenClaw plugins config)
 *
 * Plugins can ship their own skills by declaring skills directories.
 * When a plugin is enabled, its skills participate in the normal loading order.
 */
data class PluginsConfig(
    @SerializedName("entries")
    val entries: Map<String, PluginEntry> = emptyMap()
)

/**
 * Individual plugin entry (aligns with OpenClaw plugin.json + config)
 */
data class PluginEntry(
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("skills")
    val skills: List<String> = emptyList()  // Relative skill directory paths within the plugin
)
