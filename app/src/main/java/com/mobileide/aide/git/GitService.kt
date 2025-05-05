package com.mobileide.aide.git

import android.content.Context
import android.util.Log
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة Git المسؤولة عن إدارة مستودعات Git
 */
@Singleton
class GitService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "GitService"
    }
    
    /**
     * التحقق من وجود Git
     */
    suspend fun isGitAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("git", "--version")
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            return@withContext exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Git availability", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مستودع Git
     */
    suspend fun initRepository(project: Project): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // التحقق من وجود مستودع Git
            val gitDir = File(projectDir, ".git")
            if (gitDir.exists()) {
                return@withContext GitResult(
                    success = true,
                    message = "مستودع Git موجود بالفعل",
                    output = ""
                )
            }
            
            // تهيئة مستودع Git
            val process = ProcessBuilder("git", "init")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // إضافة ملف .gitignore
                createGitIgnore(projectDir)
                
                return@withContext GitResult(
                    success = true,
                    message = "تم تهيئة مستودع Git بنجاح",
                    output = output
                )
            } else {
                return@withContext GitResult(
                    success = false,
                    message = "فشل تهيئة مستودع Git",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل تهيئة مستودع Git: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * استنساخ مستودع Git
     */
    suspend fun cloneRepository(url: String, directory: File): GitResult = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود الدليل
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            // استنساخ المستودع
            val process = ProcessBuilder("git", "clone", url, directory.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم استنساخ المستودع بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل استنساخ المستودع",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cloning Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل استنساخ المستودع: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * إضافة ملفات إلى مستودع Git
     */
    suspend fun addFiles(project: Project, files: List<String>): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // إضافة الملفات
            val command = mutableListOf("git", "add")
            command.addAll(files)
            
            val process = ProcessBuilder(command)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تمت إضافة الملفات بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل إضافة الملفات",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding files to Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل إضافة الملفات: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * إضافة جميع الملفات إلى مستودع Git
     */
    suspend fun addAllFiles(project: Project): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // إضافة جميع الملفات
            val process = ProcessBuilder("git", "add", ".")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تمت إضافة جميع الملفات بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل إضافة جميع الملفات",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding all files to Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل إضافة جميع الملفات: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * عمل commit
     */
    suspend fun commit(project: Project, message: String): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // عمل commit
            val process = ProcessBuilder("git", "commit", "-m", message)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم عمل commit بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل عمل commit",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error committing to Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل عمل commit: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * دفع التغييرات
     */
    suspend fun push(project: Project, remote: String = "origin", branch: String = "main"): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // دفع التغييرات
            val process = ProcessBuilder("git", "push", remote, branch)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم دفع التغييرات بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل دفع التغييرات",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing to Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل دفع التغييرات: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * سحب التغييرات
     */
    suspend fun pull(project: Project, remote: String = "origin", branch: String = "main"): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // سحب التغييرات
            val process = ProcessBuilder("git", "pull", remote, branch)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم سحب التغييرات بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل سحب التغييرات",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling from Git repository", e)
            return@withContext GitResult(
                success = false,
                message = "فشل سحب التغييرات: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * إنشاء فرع جديد
     */
    suspend fun createBranch(project: Project, branchName: String): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // إنشاء فرع جديد
            val process = ProcessBuilder("git", "checkout", "-b", branchName)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم إنشاء الفرع بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل إنشاء الفرع",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Git branch", e)
            return@withContext GitResult(
                success = false,
                message = "فشل إنشاء الفرع: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * التبديل إلى فرع
     */
    suspend fun checkoutBranch(project: Project, branchName: String): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // التبديل إلى فرع
            val process = ProcessBuilder("git", "checkout", branchName)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم التبديل إلى الفرع بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل التبديل إلى الفرع",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking out Git branch", e)
            return@withContext GitResult(
                success = false,
                message = "فشل التبديل إلى الفرع: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * الحصول على حالة المستودع
     */
    suspend fun getStatus(project: Project): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // الحصول على حالة المستودع
            val process = ProcessBuilder("git", "status")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم الحصول على حالة المستودع بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل الحصول على حالة المستودع",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Git status", e)
            return@withContext GitResult(
                success = false,
                message = "فشل الحصول على حالة المستودع: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * الحصول على سجل المستودع
     */
    suspend fun getLog(project: Project, count: Int = 10): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // الحصول على سجل المستودع
            val process = ProcessBuilder("git", "log", "--oneline", "-n", count.toString())
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم الحصول على سجل المستودع بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل الحصول على سجل المستودع",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Git log", e)
            return@withContext GitResult(
                success = false,
                message = "فشل الحصول على سجل المستودع: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * إضافة remote
     */
    suspend fun addRemote(project: Project, name: String, url: String): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // إضافة remote
            val process = ProcessBuilder("git", "remote", "add", name, url)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تمت إضافة remote بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل إضافة remote",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding Git remote", e)
            return@withContext GitResult(
                success = false,
                message = "فشل إضافة remote: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * تعيين إعدادات Git
     */
    suspend fun setConfig(project: Project, key: String, value: String): GitResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // تعيين إعدادات Git
            val process = ProcessBuilder("git", "config", key, value)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return@withContext if (exitCode == 0) {
                GitResult(
                    success = true,
                    message = "تم تعيين إعدادات Git بنجاح",
                    output = output
                )
            } else {
                GitResult(
                    success = false,
                    message = "فشل تعيين إعدادات Git",
                    output = output
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Git config", e)
            return@withContext GitResult(
                success = false,
                message = "فشل تعيين إعدادات Git: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    /**
     * إنشاء ملف .gitignore
     */
    private fun createGitIgnore(projectDir: File) {
        try {
            val gitignoreFile = File(projectDir, ".gitignore")
            
            if (!gitignoreFile.exists()) {
                val content = """
                    # Gradle files
                    .gradle/
                    build/
                    
                    # Local configuration file
                    local.properties
                    
                    # Log Files
                    *.log
                    
                    # Android Studio generated files and folders
                    captures/
                    .externalNativeBuild/
                    .cxx/
                    *.apk
                    output.json
                    
                    # IntelliJ
                    *.iml
                    .idea/
                    
                    # Keystore files
                    *.jks
                    *.keystore
                    
                    # Google Services (e.g. APIs or Firebase)
                    google-services.json
                    
                    # Android Profiling
                    *.hprof
                    
                    # OS specific files
                    .DS_Store
                    Thumbs.db
                """.trimIndent()
                
                gitignoreFile.writeText(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating .gitignore file", e)
        }
    }
}