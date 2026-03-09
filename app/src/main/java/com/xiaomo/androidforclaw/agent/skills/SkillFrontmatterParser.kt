package com.xiaomo.androidforclaw.agent.skills

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * SKILL.md Frontmatter 解析器
 *
 * 对齐 OpenClaw 格式:
 * - 支持 YAML Frontmatter (--- 分隔符)
 * - metadata.openclaw 是单行 JSON
 * - description 是单行文本
 */
class SkillFrontmatterParser {
    companion object {
        private const val TAG = "SkillFrontmatterParser"
        private val gson = Gson()
    }

    /**
     * 解析 SKILL.md 文件内容
     *
     * 格式:
     * ```
     * ---
     * name: skill-name
     * description: Single line description
     * metadata: { "openclaw": { "always": true } }
     * ---
     *
     * # Skill Content
     * ...
     * ```
     */
    fun parse(content: String): ParseResult {
        try {
            // 1. 检查是否包含 Frontmatter
            if (!content.trim().startsWith("---")) {
                return ParseResult.Error("Missing frontmatter (must start with ---)")
            }

            // 2. 提取 Frontmatter 和 Content
            val lines = content.lines()
            var frontmatterEnd = -1
            var inFrontmatter = false

            for (i in lines.indices) {
                val line = lines[i].trim()
                if (i == 0 && line == "---") {
                    inFrontmatter = true
                    continue
                }
                if (inFrontmatter && line == "---") {
                    frontmatterEnd = i
                    break
                }
            }

            if (frontmatterEnd == -1) {
                return ParseResult.Error("Frontmatter not closed (missing closing ---)")
            }

            // 3. 解析 Frontmatter YAML
            val frontmatterLines = lines.subList(1, frontmatterEnd)
            val frontmatterText = frontmatterLines.joinToString("\n")
            val frontmatterData = parseYamlFrontmatter(frontmatterText)

            // 4. 提取 name 和 description (必需字段)
            val name = frontmatterData["name"] as? String
            if (name.isNullOrBlank()) {
                return ParseResult.Error("Missing required field: name")
            }

            val description = frontmatterData["description"] as? String
            if (description.isNullOrBlank()) {
                return ParseResult.Error("Missing required field: description")
            }

            // 5. 提取 metadata (可选)
            val metadataMap = frontmatterData["metadata"] as? Map<*, *>

            // 6. 提取 OpenClaw 元数据
            val openclawMetadata = metadataMap?.get("openclaw")?.let { openclaw ->
                parseOpenClawMetadata(openclaw)
            }

            // 7. 提取 Content (Frontmatter 之后的所有内容)
            val contentLines = lines.subList(frontmatterEnd + 1, lines.size)
            val contentText = contentLines.joinToString("\n").trim()

            // 8. 返回解析结果
            return ParseResult.Success(
                frontmatter = ParsedSkillFrontmatter(
                    name = name,
                    description = description,
                    metadata = metadataMap as? Map<String, Any?>
                ),
                content = contentText,
                openclawMetadata = openclawMetadata
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SKILL.md", e)
            return ParseResult.Error("Parse failed: ${e.message}")
        }
    }

    /**
     * 解析 YAML Frontmatter (简化版)
     *
     * 支持:
     * - key: value (单行)
     * - key: { "json": "object" } (单行 JSON)
     *
     * 不支持:
     * - 多行值
     * - 数组
     * - 复杂嵌套
     */
    private fun parseYamlFrontmatter(text: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        var currentKey: String? = null
        var currentValue = StringBuilder()

        for (line in text.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue
            }

            // 检查是否是新的 key: value 行
            val colonIndex = trimmedLine.indexOf(':')
            if (colonIndex > 0) {
                // 保存之前的 key-value
                if (currentKey != null) {
                    result[currentKey] = parseYamlValue(currentValue.toString().trim())
                }

                // 开始新的 key-value
                currentKey = trimmedLine.substring(0, colonIndex).trim()
                currentValue = StringBuilder(trimmedLine.substring(colonIndex + 1).trim())
            } else {
                // 继续当前值 (多行,但 OpenClaw 规范要求单行)
                currentValue.append(" ").append(trimmedLine)
            }
        }

        // 保存最后一个 key-value
        if (currentKey != null) {
            result[currentKey] = parseYamlValue(currentValue.toString().trim())
        }

        return result
    }

    /**
     * 解析 YAML 值
     *
     * 支持:
     * - 字符串 (默认)
     * - JSON 对象 { ... }
     * - 布尔值 true/false
     * - 数字
     */
    private fun parseYamlValue(value: String): Any? {
        if (value.isEmpty()) {
            return null
        }

        // JSON 对象
        if (value.startsWith("{") && value.endsWith("}")) {
            return try {
                val jsonObject = JsonParser.parseString(value).asJsonObject
                jsonObjectToMap(jsonObject)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON value: $value", e)
                value
            }
        }

        // 布尔值
        if (value == "true") return true
        if (value == "false") return false

        // 数字
        value.toIntOrNull()?.let { return it }
        value.toDoubleOrNull()?.let { return it }

        // 字符串
        return value
    }

    /**
     * JsonObject 转 Map
     */
    private fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        for (key in jsonObject.keySet()) {
            val value = jsonObject.get(key)
            map[key] = when {
                value.isJsonObject -> jsonObjectToMap(value.asJsonObject)
                value.isJsonArray -> {
                    value.asJsonArray.map { element ->
                        when {
                            element.isJsonPrimitive -> element.asJsonPrimitive.let {
                                when {
                                    it.isString -> it.asString
                                    it.isNumber -> it.asNumber
                                    it.isBoolean -> it.asBoolean
                                    else -> null
                                }
                            }
                            element.isJsonObject -> jsonObjectToMap(element.asJsonObject)
                            else -> null
                        }
                    }
                }
                value.isJsonPrimitive -> value.asJsonPrimitive.let {
                    when {
                        it.isString -> it.asString
                        it.isNumber -> it.asNumber
                        it.isBoolean -> it.asBoolean
                        else -> null
                    }
                }
                value.isJsonNull -> null
                else -> null
            }
        }
        return map
    }

    /**
     * 解析 OpenClaw 元数据
     */
    private fun parseOpenClawMetadata(data: Any?): OpenClawSkillMetadata? {
        if (data == null) return null

        return try {
            val map = data as? Map<*, *> ?: return null

            OpenClawSkillMetadata(
                always = map["always"] as? Boolean ?: false,
                skillKey = map["skillKey"] as? String,
                primaryEnv = map["primaryEnv"] as? String,
                emoji = map["emoji"] as? String,
                homepage = map["homepage"] as? String,
                os = (map["os"] as? List<*>)?.mapNotNull { it as? String },
                requires = (map["requires"] as? Map<*, *>)?.let { req ->
                    SkillRequirements(
                        bins = (req["bins"] as? List<*>)?.mapNotNull { it as? String },
                        anyBins = (req["anyBins"] as? List<*>)?.mapNotNull { it as? String },
                        env = (req["env"] as? List<*>)?.mapNotNull { it as? String },
                        config = (req["config"] as? List<*>)?.mapNotNull { it as? String }
                    )
                },
                install = (map["install"] as? List<*>)?.mapNotNull { installSpec ->
                    parseInstallSpec(installSpec)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OpenClaw metadata", e)
            null
        }
    }

    /**
     * 解析安装规范
     */
    private fun parseInstallSpec(data: Any?): SkillInstallSpec? {
        if (data == null) return null

        return try {
            val map = data as? Map<*, *> ?: return null

            val kindStr = map["kind"] as? String ?: return null
            val kind = when (kindStr.lowercase()) {
                "brew" -> InstallKind.BREW
                "node" -> InstallKind.NODE
                "go" -> InstallKind.GO
                "uv" -> InstallKind.UV
                "download" -> InstallKind.DOWNLOAD
                "apk" -> InstallKind.APK
                else -> return null
            }

            SkillInstallSpec(
                id = map["id"] as? String,
                kind = kind,
                label = map["label"] as? String,
                bins = (map["bins"] as? List<*>)?.mapNotNull { it as? String },
                os = (map["os"] as? List<*>)?.mapNotNull { it as? String },
                formula = map["formula"] as? String,
                `package` = map["package"] as? String,
                module = map["module"] as? String,
                url = map["url"] as? String,
                archive = map["archive"] as? String,
                extract = map["extract"] as? Boolean,
                stripComponents = (map["stripComponents"] as? Number)?.toInt(),
                targetDir = map["targetDir"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse install spec", e)
            null
        }
    }

    /**
     * 解析结果
     */
    sealed class ParseResult {
        data class Success(
            val frontmatter: ParsedSkillFrontmatter,
            val content: String,
            val openclawMetadata: OpenClawSkillMetadata? = null
        ) : ParseResult()

        data class Error(val message: String) : ParseResult()
    }
}
