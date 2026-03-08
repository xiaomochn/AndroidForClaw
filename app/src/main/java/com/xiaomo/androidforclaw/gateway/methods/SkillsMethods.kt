package com.xiaomo.androidforclaw.gateway.methods

import android.content.Context
import com.xiaomo.androidforclaw.agent.skills.SkillsLoader
import java.io.File

/**
 * Skills RPC methods implementation
 *
 * Provides skills management and status
 */
class SkillsMethods(
    private val context: Context,
    private val skillsLoader: SkillsLoader
) {
    private val workspacePath = "/sdcard/androidforclaw-workspace/skills"

    /**
     * skills.status() - Get skills status
     *
     * Returns loaded skills and their sources
     */
    fun skillsStatus(): SkillsStatusResult {
        val loadedSkills = skillsLoader.loadSkills()

        val skills = loadedSkills.map { (name, skill) ->
            SkillInfo(
                name = name,
                description = skill.description,
                source = "loaded", // SkillDocument 没有 source 字段
                autoLoad = skill.metadata.always
            )
        }

        return SkillsStatusResult(
            skills = skills,
            count = skills.size,
            workspacePath = workspacePath
        )
    }

    /**
     * skills.reload() - Reload all skills
     *
     * Reloads skills from assets and workspace
     */
    fun skillsReload(): SkillsReloadResult {
        return try {
            val skills = skillsLoader.loadSkills()

            SkillsReloadResult(
                success = true,
                message = "Reloaded ${skills.size} skills",
                count = skills.size
            )
        } catch (e: Exception) {
            SkillsReloadResult(
                success = false,
                message = "Failed to reload skills: ${e.message}",
                count = 0
            )
        }
    }

    /**
     * skills.install() - Install a skill from file
     *
     * Copies skill file to workspace
     */
    fun skillsInstall(params: Any?): SkillsInstallResult {
        @Suppress("UNCHECKED_CAST")
        val paramsMap = params as? Map<String, Any?>
            ?: return SkillsInstallResult(false, "params must be an object")

        val name = paramsMap["name"] as? String
            ?: return SkillsInstallResult(false, "name required")

        val content = paramsMap["content"] as? String
            ?: return SkillsInstallResult(false, "content required")

        return try {
            val skillDir = File(workspacePath, name)
            skillDir.mkdirs()

            val skillFile = File(skillDir, "SKILL.md")
            skillFile.writeText(content)

            SkillsInstallResult(
                success = true,
                message = "Skill installed: $name",
                path = skillFile.absolutePath
            )
        } catch (e: Exception) {
            SkillsInstallResult(
                success = false,
                message = "Failed to install skill: ${e.message}"
            )
        }
    }

    /**
     * skills.list() - List all skills (simple)
     */
    fun skillsList(): SkillsListResult {
        val skillNames = skillsLoader.loadSkills().keys.toList()
        return SkillsListResult(skills = skillNames)
    }
}

/**
 * Skills status result
 */
data class SkillsStatusResult(
    val skills: List<SkillInfo>,
    val count: Int,
    val workspacePath: String
)

/**
 * Skill information
 */
data class SkillInfo(
    val name: String,
    val description: String,
    val source: String,
    val autoLoad: Boolean
)

/**
 * Skills reload result
 */
data class SkillsReloadResult(
    val success: Boolean,
    val message: String,
    val count: Int = 0
)

/**
 * Skills install result
 */
data class SkillsInstallResult(
    val success: Boolean,
    val message: String,
    val path: String? = null
)

/**
 * Skills list result (simple)
 */
data class SkillsListResult(
    val skills: List<String>
)
