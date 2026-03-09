package com.xiaomo.androidforclaw.agent.skills

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * 技能安装器
 *
 * 提供技能下载、解压、安装、卸载功能
 */
class SkillInstaller(private val context: Context) {
    companion object {
        private const val TAG = "SkillInstaller"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    }

    private val clawHubClient = ClawHubClient()
    private val workspacePath = "/sdcard/.androidforclaw/workspace"
    private val managedSkillsDir = "/sdcard/.androidforclaw/skills"
    private val downloadCacheDir = File(context.cacheDir, "skill-downloads")
    private val lockManager = SkillLockManager(workspacePath)

    init {
        downloadCacheDir.mkdirs()
    }

    /**
     * 从 ClawHub 安装技能
     *
     * @param slug 技能 slug
     * @param version 版本号 (默认 "latest")
     * @param progressCallback 进度回调
     */
    suspend fun installFromClawHub(
        slug: String,
        version: String = "latest",
        progressCallback: ((InstallProgress) -> Unit)? = null
    ): Result<InstallResult> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Installing skill from ClawHub: $slug@$version")

            // 1. 检查是否已安装
            val existingEntry = lockManager.getSkill(slug)
            if (existingEntry != null) {
                progressCallback?.invoke(InstallProgress.Info("Skill already installed: ${existingEntry.version}"))
                Log.d(TAG, "Skill already installed: $slug@${existingEntry.version}")
            }

            // 2. 获取技能详情
            progressCallback?.invoke(InstallProgress.FetchingDetails)
            val detailsResult = clawHubClient.getSkillDetails(slug)
            if (detailsResult.isFailure) {
                return@withContext Result.failure(detailsResult.exceptionOrNull()!!)
            }
            val details = detailsResult.getOrNull()!!
            Log.d(TAG, "Skill details: ${details.name} - ${details.description}")

            // 3. 下载技能包
            progressCallback?.invoke(InstallProgress.Downloading(0, 0))
            val downloadFile = File(downloadCacheDir, "$slug-$version.zip")
            val downloadResult = clawHubClient.downloadSkill(
                slug = slug,
                version = version,
                targetFile = downloadFile
            ) { downloaded, total ->
                progressCallback?.invoke(InstallProgress.Downloading(downloaded, total))
            }

            if (downloadResult.isFailure) {
                return@withContext Result.failure(downloadResult.exceptionOrNull()!!)
            }

            // 4. 计算文件哈希
            progressCallback?.invoke(InstallProgress.VerifyingHash)
            val hash = calculateFileHash(downloadFile)
            Log.d(TAG, "Downloaded file hash: $hash")

            // 5. 解压技能包
            progressCallback?.invoke(InstallProgress.Extracting)
            val targetDir = File(managedSkillsDir, slug)
            val extractResult = extractZip(downloadFile, targetDir)
            if (extractResult.isFailure) {
                return@withContext Result.failure(extractResult.exceptionOrNull()!!)
            }

            // 6. 验证 SKILL.md 存在
            val skillMdFile = File(targetDir, "SKILL.md")
            if (!skillMdFile.exists()) {
                targetDir.deleteRecursively()
                return@withContext Result.failure(
                    Exception("Invalid skill package: SKILL.md not found")
                )
            }

            // 7. 更新锁文件
            progressCallback?.invoke(InstallProgress.UpdatingLock)
            val lockEntry = SkillLockEntry(
                name = details.name,
                slug = slug,
                version = version,
                hash = hash,
                installedAt = DATE_FORMAT.format(Date()),
                source = "clawhub"
            )
            lockManager.addOrUpdateSkill(lockEntry)

            // 8. 清理下载缓存
            downloadFile.delete()

            progressCallback?.invoke(InstallProgress.Complete)
            Log.i(TAG, "✅ Skill installed successfully: $slug@$version")

            Result.success(
                InstallResult(
                    slug = slug,
                    name = details.name,
                    version = version,
                    path = targetDir.absolutePath,
                    hash = hash
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            progressCallback?.invoke(InstallProgress.Error(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * 从本地文件安装技能
     *
     * @param zipFile ZIP 文件路径
     * @param name 技能名称 (可选,从 SKILL.md 提取)
     */
    suspend fun installFromFile(
        zipFile: File,
        name: String? = null
    ): Result<InstallResult> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Installing skill from file: ${zipFile.absolutePath}")

            // 1. 解压到临时目录
            val tempDir = File(downloadCacheDir, "temp-${System.currentTimeMillis()}")
            val extractResult = extractZip(zipFile, tempDir)
            if (extractResult.isFailure) {
                return@withContext Result.failure(extractResult.exceptionOrNull()!!)
            }

            // 2. 验证并解析 SKILL.md
            val skillMdFile = File(tempDir, "SKILL.md")
            if (!skillMdFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(
                    Exception("Invalid skill package: SKILL.md not found")
                )
            }

            val parser = SkillFrontmatterParser()
            val parseResult = parser.parse(skillMdFile.readText())
            if (parseResult is SkillFrontmatterParser.ParseResult.Error) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(
                    Exception("Invalid SKILL.md: ${parseResult.message}")
                )
            }

            val skill = (parseResult as SkillFrontmatterParser.ParseResult.Success).frontmatter
            val skillName = name ?: skill.name

            // 3. 移动到托管目录
            val targetDir = File(managedSkillsDir, skillName)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.parentFile?.mkdirs()

            if (!tempDir.renameTo(targetDir)) {
                // 重命名失败,尝试复制
                tempDir.copyRecursively(targetDir, overwrite = true)
                tempDir.deleteRecursively()
            }

            // 4. 计算哈希
            val hash = calculateFileHash(zipFile)

            // 5. 更新锁文件
            val lockEntry = SkillLockEntry(
                name = skillName,
                slug = skillName,  // 本地安装使用 name 作为 slug
                version = "local",
                hash = hash,
                installedAt = DATE_FORMAT.format(Date()),
                source = "local"
            )
            lockManager.addOrUpdateSkill(lockEntry)

            Log.i(TAG, "✅ Skill installed from file: $skillName")

            Result.success(
                InstallResult(
                    slug = skillName,
                    name = skillName,
                    version = "local",
                    path = targetDir.absolutePath,
                    hash = hash
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Installation from file failed", e)
            Result.failure(e)
        }
    }

    /**
     * 卸载技能
     *
     * @param slug 技能 slug
     */
    suspend fun uninstall(slug: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Uninstalling skill: $slug")

            // 1. 检查是否已安装
            val entry = lockManager.getSkill(slug)
                ?: return@withContext Result.failure(
                    Exception("Skill not installed: $slug")
                )

            // 2. 删除技能目录
            val skillDir = File(managedSkillsDir, slug)
            if (skillDir.exists()) {
                skillDir.deleteRecursively()
                Log.d(TAG, "Deleted skill directory: ${skillDir.absolutePath}")
            }

            // 3. 从锁文件中移除
            lockManager.removeSkill(slug)

            Log.i(TAG, "✅ Skill uninstalled: $slug")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Uninstallation failed", e)
            Result.failure(e)
        }
    }

    /**
     * 解压 ZIP 文件
     */
    private fun extractZip(zipFile: File, targetDir: File): Result<Unit> {
        return try {
            targetDir.mkdirs()

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry

                while (entry != null) {
                    val file = File(targetDir, entry.name)

                    // 安全检查: 防止 ZIP 路径遍历攻击
                    if (!file.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Zip entry outside target directory: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    }

                    entry = zis.nextEntry
                }
            }

            Log.d(TAG, "✅ Extracted ZIP to ${targetDir.absolutePath}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract ZIP", e)
            Result.failure(e)
        }
    }

    /**
     * 计算文件 SHA-256 哈希
     */
    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/**
 * 安装进度
 */
sealed class InstallProgress {
    data class Info(val message: String) : InstallProgress()
    object FetchingDetails : InstallProgress()
    data class Downloading(val downloaded: Long, val total: Long) : InstallProgress()
    object VerifyingHash : InstallProgress()
    object Extracting : InstallProgress()
    object UpdatingLock : InstallProgress()
    object Complete : InstallProgress()
    data class Error(val message: String) : InstallProgress()
}

/**
 * 安装结果
 */
data class InstallResult(
    val slug: String,
    val name: String,
    val version: String,
    val path: String,
    val hash: String
)
