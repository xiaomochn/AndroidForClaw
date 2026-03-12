package com.xiaomo.androidforclaw.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.draco.ladb.R
import com.draco.ladb.databinding.ActivityModelSetupBinding
import com.xiaomo.androidforclaw.config.ConfigLoader
import com.xiaomo.androidforclaw.config.ModelDefinition
import com.xiaomo.androidforclaw.config.ModelsConfig
import com.xiaomo.androidforclaw.config.ProviderConfig

/**
 * Model Setup Guide — first-run wizard for API configuration.
 *
 * Launched automatically when no valid API key is detected.
 * Guides user through:
 * 1. Choose provider (OpenRouter / Anthropic / OpenAI / Custom)
 * 2. Enter API Base URL + API Key
 * 3. Select default model
 * 4. Save to openclaw.json
 */
class ModelSetupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ModelSetupActivity"

        /** Intent extra: if true, setup was triggered by user (not auto-detect) */
        const val EXTRA_MANUAL = "manual"

        /**
         * Check if model setup guide is needed.
         *
         * Returns true if:
         * - Setup has never been completed (MMKV flag) AND
         * - No valid API key is configured in any provider
         */
        fun isNeeded(context: android.content.Context): Boolean {
            try {
                val mmkv = com.tencent.mmkv.MMKV.defaultMMKV()
                if (mmkv.decodeBool("model_setup_completed", false)) {
                    return false  // Already completed or skipped
                }

                // Check if any provider has a real (non-placeholder) API key
                val configLoader = ConfigLoader(context)
                val config = configLoader.loadOpenClawConfig()
                val providers = config.resolveProviders()

                val hasRealKey = providers.values.any { provider ->
                    val key = provider.apiKey
                    !key.isNullOrBlank() &&
                            !key.startsWith("\${") &&  // Not an env var placeholder
                            key != "未配置"
                }

                return !hasRealKey
            } catch (e: Exception) {
                Log.w(TAG, "Error checking setup need", e)
                return false  // Don't block on error
            }
        }

        // Provider presets
        private val PROVIDERS = mapOf(
            "openrouter" to ProviderPreset(
                name = "OpenRouter",
                baseUrl = "https://openrouter.ai/api/v1",
                api = "openai-completions",
                hint = "OpenRouter 聚合了 Claude、GPT、Gemini 等多个模型，一个 Key 即可使用全部。\n注册: openrouter.ai",
                models = listOf(
                    ModelPreset("anthropic/claude-sonnet-4", "Claude Sonnet 4 (推荐)"),
                    ModelPreset("anthropic/claude-opus-4", "Claude Opus 4"),
                    ModelPreset("openai/gpt-4.1", "GPT-4.1"),
                    ModelPreset("google/gemini-2.5-pro", "Gemini 2.5 Pro"),
                    ModelPreset("deepseek/deepseek-r1", "DeepSeek R1")
                ),
                authHeader = true
            ),
            "anthropic" to ProviderPreset(
                name = "Anthropic",
                baseUrl = "https://api.anthropic.com/v1",
                api = "anthropic-messages",
                hint = "Anthropic 官方 API，直连 Claude 模型。\n注册: console.anthropic.com",
                models = listOf(
                    ModelPreset("claude-sonnet-4-20250514", "Claude Sonnet 4 (推荐)"),
                    ModelPreset("claude-opus-4-20250514", "Claude Opus 4"),
                    ModelPreset("claude-haiku-3-5-20241022", "Claude 3.5 Haiku (快速)")
                )
            ),
            "openai" to ProviderPreset(
                name = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                api = "openai-completions",
                hint = "OpenAI 官方 API。\n注册: platform.openai.com",
                models = listOf(
                    ModelPreset("gpt-4.1", "GPT-4.1 (推荐)"),
                    ModelPreset("gpt-4.1-mini", "GPT-4.1 Mini (快速)"),
                    ModelPreset("o3", "o3 (推理)")
                )
            ),
            "custom" to ProviderPreset(
                name = "自定义",
                baseUrl = "",
                api = "openai-completions",
                hint = "填入你的 API Base URL 和 Key。\n支持任何兼容 OpenAI API 的服务（如 vLLM、Ollama、OneAPI 等）。",
                models = listOf(
                    ModelPreset("", "手动输入模型 ID")
                )
            )
        )
    }

    private lateinit var binding: ActivityModelSetupBinding
    private val configLoader by lazy { ConfigLoader(this) }
    private var selectedProvider = "openrouter"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupProviderSelection()
        applyProviderPreset("openrouter")
        setupButtons()
    }

    private fun setupProviderSelection() {
        binding.chipGroupProvider.setOnCheckedStateChangeListener { _, checkedIds ->
            val provider = when {
                checkedIds.contains(R.id.chip_openrouter) -> "openrouter"
                checkedIds.contains(R.id.chip_anthropic) -> "anthropic"
                checkedIds.contains(R.id.chip_openai) -> "openai"
                checkedIds.contains(R.id.chip_custom) -> "custom"
                else -> "openrouter"
            }
            selectedProvider = provider
            applyProviderPreset(provider)
        }
    }

    private fun applyProviderPreset(providerKey: String) {
        val preset = PROVIDERS[providerKey] ?: return

        binding.apply {
            // Set base URL
            etSetupApiBase.setText(preset.baseUrl)

            // Custom provider: make base URL editable and show it
            if (providerKey == "custom") {
                tilApiBase.visibility = View.VISIBLE
                etSetupApiBase.isEnabled = true
            } else {
                tilApiBase.visibility = View.VISIBLE
                etSetupApiBase.isEnabled = false  // Lock preset URL
            }

            // Set hint text
            tvProviderHint.text = preset.hint

            // Populate model dropdown
            val modelNames = preset.models.map { it.displayName }
            val adapter = ArrayAdapter(
                this@ModelSetupActivity,
                android.R.layout.simple_dropdown_item_1line,
                modelNames
            )
            actModel.setAdapter(adapter)

            // Select first model
            if (modelNames.isNotEmpty()) {
                actModel.setText(modelNames[0], false)
            }

            // For custom provider, allow free text model input
            if (providerKey == "custom") {
                actModel.inputType = android.text.InputType.TYPE_CLASS_TEXT
                actModel.threshold = 100  // Effectively disable dropdown filtering
            } else {
                actModel.inputType = android.text.InputType.TYPE_NULL
                actModel.threshold = 1
            }
        }
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener {
            Log.i(TAG, "用户跳过模型配置引导")
            markSetupSeen()
            finish()
        }

        binding.btnStart.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun saveAndFinish() {
        val apiKey = binding.etSetupApiKey.text?.toString()?.trim()
        val apiBase = binding.etSetupApiBase.text?.toString()?.trim()
        val selectedModelDisplay = binding.actModel.text?.toString()?.trim()

        // Validate
        if (apiKey.isNullOrEmpty()) {
            binding.tilApiKey.error = "请输入 API Key"
            return
        }
        binding.tilApiKey.error = null

        if (selectedProvider == "custom" && apiBase.isNullOrEmpty()) {
            binding.tilApiBase.error = "请输入 API Base URL"
            return
        }
        binding.tilApiBase.error = null

        // Resolve model ID from display name
        val preset = PROVIDERS[selectedProvider] ?: return
        val modelId = if (selectedProvider == "custom") {
            selectedModelDisplay ?: ""
        } else {
            preset.models.find { it.displayName == selectedModelDisplay }?.id
                ?: preset.models.firstOrNull()?.id
                ?: ""
        }

        try {
            // Load current config
            val config = configLoader.loadOpenClawConfig()

            // Build updated provider
            val providerName = if (selectedProvider == "custom") "custom" else selectedProvider
            val newProvider = ProviderConfig(
                baseUrl = apiBase ?: preset.baseUrl,
                apiKey = apiKey,
                api = preset.api,
                models = listOf(
                    ModelDefinition(
                        id = modelId,
                        name = selectedModelDisplay ?: modelId,
                        reasoning = modelId.contains("o3") || modelId.contains("r1") || modelId.contains("opus"),
                        contextWindow = 200000,
                        maxTokens = 16384
                    )
                ),
                authHeader = preset.authHeader
            )

            // Merge into existing providers (don't wipe others)
            val existingModels = config.models ?: ModelsConfig()
            val updatedProviders = existingModels.providers.toMutableMap()
            updatedProviders[providerName] = newProvider

            // Set default model
            val defaultModelId = if (selectedProvider == "custom") {
                "custom/$modelId"
            } else {
                "$providerName/$modelId"
            }

            val updatedAgents = config.agents?.copy(
                defaults = config.agents.defaults?.copy(
                    model = config.agents.defaults.model?.copy(primary = defaultModelId)
                        ?: com.xiaomo.androidforclaw.config.ModelSelectionConfig(primary = defaultModelId)
                )
            ) ?: com.xiaomo.androidforclaw.config.AgentsConfig(
                defaults = com.xiaomo.androidforclaw.config.AgentDefaultsConfig(
                    model = com.xiaomo.androidforclaw.config.ModelSelectionConfig(primary = defaultModelId)
                )
            )

            val updatedConfig = config.copy(
                models = existingModels.copy(providers = updatedProviders),
                agents = updatedAgents
            )

            // Save
            configLoader.saveOpenClawConfig(updatedConfig)

            Log.i(TAG, "✅ 模型配置已保存: provider=$providerName, model=$modelId")
            markSetupSeen()
            Toast.makeText(this, "配置已保存，开始使用吧！", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "保存配置失败", e)
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Mark setup as completed so it won't show again.
     */
    private fun markSetupSeen() {
        try {
            val mmkv = com.tencent.mmkv.MMKV.defaultMMKV()
            mmkv.encode("model_setup_completed", true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark setup as seen", e)
        }
    }

    // Internal data classes
    private data class ProviderPreset(
        val name: String,
        val baseUrl: String,
        val api: String,
        val hint: String,
        val models: List<ModelPreset>,
        val authHeader: Boolean = false
    )

    private data class ModelPreset(
        val id: String,
        val displayName: String
    )
}
