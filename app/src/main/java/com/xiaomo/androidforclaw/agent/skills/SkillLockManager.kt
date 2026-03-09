package com.xiaomo.androidforclaw.agent.skills

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File

/**
 * 技能锁文件管理器
 *
 * 管理 .clawhub/lock.json
 * 记录已安装技能的版本、哈希、安装时间等信息
 */
class SkillLockManager(private val workspacePath: String) {
    companion object {
        private const val TAG = "SkillLockManager"
        private const val LOCK_FILE_NAME = "lock.json"
    }

    private val lockDir = File(workspacePath, ".clawhub")
    private val lockFile = File(lockDir, LOCK_FILE_NAME)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 读取锁文件
     */
    fun readLock(): SkillLockFile {
        if (!lockFile.exists()) {
            Log.d(TAG, "Lock file not found, creating empty")
            return SkillLockFile(skills = emptyList())
        }

        return try {
            val content = lockFile.readText()
            gson.fromJson(content, SkillLockFile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read lock file", e)
            SkillLockFile(skills = emptyList())
        }
    }

    /**
     * 写入锁文件
     */
    fun writeLock(lockFile: SkillLockFile): Result<Unit> {
        return try {
            // 确保目录存在
            lockDir.mkdirs()

            // 写入文件
            val content = gson.toJson(lockFile)
            this.lockFile.writeText(content)

            Log.d(TAG, "✅ Lock file written: ${this.lockFile.absolutePath}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to write lock file", e)
            Result.failure(e)
        }
    }

    /**
     * 添加或更新技能记录
     */
    fun addOrUpdateSkill(entry: SkillLockEntry): Result<Unit> {
        return try {
            val lock = readLock()
            val existingIndex = lock.skills.indexOfFirst { it.slug == entry.slug }

            val updatedSkills = if (existingIndex >= 0) {
                // 更新现有记录
                lock.skills.toMutableList().apply {
                    set(existingIndex, entry)
                }
            } else {
                // 添加新记录
                lock.skills + entry
            }

            writeLock(lock.copy(skills = updatedSkills))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add/update skill", e)
            Result.failure(e)
        }
    }

    /**
     * 移除技能记录
     */
    fun removeSkill(slug: String): Result<Unit> {
        return try {
            val lock = readLock()
            val updatedSkills = lock.skills.filter { it.slug != slug }

            if (updatedSkills.size == lock.skills.size) {
                Log.w(TAG, "Skill not found in lock: $slug")
            }

            writeLock(lock.copy(skills = updatedSkills))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove skill", e)
            Result.failure(e)
        }
    }

    /**
     * 获取技能记录
     */
    fun getSkill(slug: String): SkillLockEntry? {
        val lock = readLock()
        return lock.skills.find { it.slug == slug }
    }

    /**
     * 列出所有已安装技能
     */
    fun listSkills(): List<SkillLockEntry> {
        return readLock().skills
    }

    /**
     * 检查技能是否已安装
     */
    fun isInstalled(slug: String): Boolean {
        return getSkill(slug) != null
    }

    /**
     * 获取已安装版本
     */
    fun getInstalledVersion(slug: String): String? {
        return getSkill(slug)?.version
    }
}

/**
 * 锁文件结构
 */
data class SkillLockFile(
    val skills: List<SkillLockEntry>
)

/**
 * 技能锁条目
 */
data class SkillLockEntry(
    val name: String,
    val slug: String,
    val version: String,
    val hash: String? = null,
    val installedAt: String,
    val source: String = "clawhub"  // clawhub, github, local
)
