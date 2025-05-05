package com.mobileide.aide.cloud

import android.content.Context
import android.util.Log
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة التخزين السحابي - مسؤولة عن مزامنة المشاريع مع خدمات التخزين السحابي
 */
@Singleton
class CloudStorageService @Inject constructor(
    private val context: Context,
    private val cloudProviderFactory: CloudProviderFactory
) {
    companion object {
        private const val TAG = "CloudStorageService"
    }
    
    // مزودو التخزين السحابي المتاحون
    private val availableProviders = mutableMapOf<CloudProviderType, CloudProvider>()
    
    // مزود التخزين السحابي النشط
    private var activeProvider: CloudProvider? = null
    
    init {
        // تهيئة مزودي التخزين السحابي
        CloudProviderType.values().forEach { providerType ->
            try {
                val provider = cloudProviderFactory.createProvider(providerType)
                availableProviders[providerType] = provider
                
                Log.d(TAG, "Initialized cloud provider: $providerType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize cloud provider: $providerType", e)
            }
        }
    }
    
    /**
     * تعيين مزود التخزين السحابي النشط
     */
    fun setActiveProvider(providerType: CloudProviderType): Boolean {
        return try {
            val provider = availableProviders[providerType]
            if (provider != null) {
                activeProvider = provider
                Log.d(TAG, "Set active cloud provider: $providerType")
                true
            } else {
                Log.e(TAG, "Cloud provider not available: $providerType")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting active cloud provider", e)
            false
        }
    }
    
    /**
     * الحصول على مزود التخزين السحابي النشط
     */
    fun getActiveProvider(): CloudProvider? {
        return activeProvider
    }
    
    /**
     * الحصول على مزودي التخزين السحابي المتاحين
     */
    fun getAvailableProviders(): List<CloudProviderType> {
        return availableProviders.keys.toList()
    }
    
    /**
     * التحقق من حالة تسجيل الدخول
     */
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            activeProvider?.isLoggedIn() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status", e)
            false
        }
    }
    
    /**
     * تسجيل الدخول إلى مزود التخزين السحابي
     */
    suspend fun login(): CloudResult = withContext(Dispatchers.IO) {
        try {
            val provider = activeProvider
            if (provider == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "لم يتم تعيين مزود تخزين سحابي نشط",
                    data = null
                )
            }
            
            val result = provider.login()
            
            return@withContext if (result) {
                CloudResult(
                    success = true,
                    message = "تم تسجيل الدخول بنجاح",
                    data = null
                )
            } else {
                CloudResult(
                    success = false,
                    message = "فشل تسجيل الدخول",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in to cloud provider", e)
            return@withContext CloudResult(
                success = false,
                message = "فشل تسجيل الدخول: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تسجيل الخروج من مزود التخزين السحابي
     */
    suspend fun logout(): CloudResult = withContext(Dispatchers.IO) {
        try {
            val provider = activeProvider
            if (provider == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "لم يتم تعيين مزود تخزين سحابي نشط",
                    data = null
                )
            }
            
            val result = provider.logout()
            
            return@withContext if (result) {
                CloudResult(
                    success = true,
                    message = "تم تسجيل الخروج بنجاح",
                    data = null
                )
            } else {
                CloudResult(
                    success = false,
                    message = "فشل تسجيل الخروج",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging out from cloud provider", e)
            return@withContext CloudResult(
                success = false,
                message = "فشل تسجيل الخروج: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الحصول على قائمة المشاريع المخزنة في السحابة
     */
    suspend fun listProjects(): CloudResult = withContext(Dispatchers.IO) {
        try {
            val provider = activeProvider
            if (provider == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "لم يتم تعيين مزود تخزين سحابي نشط",
                    data = null
                )
            }
            
            if (!provider.isLoggedIn()) {
                return@withContext CloudResult(
                    success = false,
                    message = "غير مسجل الدخول",
                    data = null
                )
            }
            
            val projects = provider.listProjects()
            
            return@withContext CloudResult(
                success = true,
                message = "تم الحصول على قائمة المشاريع بنجاح",
                data = projects
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error listing cloud projects", e)
            return@withContext CloudResult(
                success = false,
                message = "فشل الحصول على قائمة المشاريع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تحميل مشروع إلى السحابة
     */
    suspend fun uploadProject(project: Project): CloudResult = withContext(Dispatchers.IO) {
        try {
            val provider = activeProvider
            if (provider == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "لم يتم تعيين مزود تخزين سحابي نشط",
                    data = null
                )
            }
            
            if (!provider.isLoggedIn()) {
                return@withContext CloudResult(
                    success = false,
                    message = "غير مسجل الدخول",
                    data = null
                )
            }
            
            // ضغط المشروع
            val zipFile = compressProject(project)
            if (zipFile == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "فشل ضغط المشروع",
                    data = null
                )
            }
            
            // تحميل المشروع
            val result = provider.uploadProject(project.name, zipFile)
            
            // حذف ملف الضغط المؤقت
            zipFile.delete()
            
            return@withContext if (result) {
                CloudResult(
                    success = true,
                    message = "تم تحميل المشروع بنجاح",
                    data = null
                )
            } else {
                CloudResult(
                    success = false,
                    message = "فشل تحميل المشروع",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading project to cloud", e)
            return@withContext CloudResult(
                success = false,
                message = "فشل تحميل المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تنزيل مشروع من السحابة
     */
    suspend fun downloadProject(projectName: String): CloudResult = withContext(Dispatchers.IO) {
        try {
            val provider = activeProvider
            if (provider == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "لم يتم تعيين مزود تخزين سحابي نشط",
                    data = null
                )
            }
            
            if (!provider.isLoggedIn()) {
                return@withContext CloudResult(
                    success = false,
                    message = "غير مسجل الدخول",
                    data = null
                )
            }
            
            // تنزيل المشروع
            val zipFile = File(context.cacheDir, "$projectName.zip")
            val result = provider.downloadProject(projectName, zipFile)
            
            if (!result) {
                return@withContext CloudResult(
                    success = false,
                    message = "فشل تنزيل المشروع",
                    data = null
                )
            }
            
            // فك ضغط المشروع
            val projectDir = File(context.filesDir, "projects/$projectName")
            val extractResult = extractProject(zipFile, projectDir)
            
            // حذف ملف الضغط المؤقت
            zipFile.delete()
            
            if (!extractResult) {
                return@withContext CloudResult(
                    success = false,
                    message = "فشل فك ضغط المشروع",
                    data = null
                )
            }
            
            // إنشاء كائن المشروع
            val project = Project(
                id = 0, // سيتم تعيينه عند حفظ المشروع في قاعدة البيانات
                name = projectName,
                packageName = "com.cloud.$projectName",
                path = projectDir.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            
            return@withContext CloudResult(
                success = true,
                message = "تم تنزيل المشروع بنجاح",
                data = project
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading project from cloud", e)
            return@withContext CloudResult(
                success = false,
                message = "فشل تنزيل المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * حذف مشروع من السحابة
     */
    suspend fun deleteProject(projectName: String): CloudResult = withContext(Dispatchers.IO) {
        try {
            val provider = activeProvider
            if (provider == null) {
                return@withContext CloudResult(
                    success = false,
                    message = "لم يتم تعيين مزود تخزين سحابي نشط",
                    data = null
                )
            }
            
            if (!provider.isLoggedIn()) {
                return@withContext CloudResult(
                    success = false,
                    message = "غير مسجل الدخول",
                    data = null
                )
            }
            
            // حذف المشروع
            val result = provider.deleteProject(projectName)
            
            return@withContext if (result) {
                CloudResult(
                    success = true,
                    message = "تم حذف المشروع بنجاح",
                    data = null
                )
            } else {
                CloudResult(
                    success = false,
                    message = "فشل حذف المشروع",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project from cloud", e)
            return@withContext CloudResult(
                success = false,
                message = "فشل حذف المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * ضغط المشروع
     */
    private fun compressProject(project: Project): File? {
        try {
            val projectDir = File(project.path)
            val zipFile = File(context.cacheDir, "${project.name}.zip")
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                compressDirectory(projectDir, projectDir.name, zipOut)
            }
            
            return zipFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing project", e)
            return null
        }
    }
    
    /**
     * ضغط دليل
     */
    private fun compressDirectory(directory: File, parentPath: String, zipOut: ZipOutputStream) {
        directory.listFiles()?.forEach { file ->
            val path = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            
            if (file.isDirectory) {
                compressDirectory(file, path, zipOut)
            } else {
                FileInputStream(file).use { fileIn ->
                    val entry = ZipEntry(path)
                    zipOut.putNextEntry(entry)
                    fileIn.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
    }
    
    /**
     * فك ضغط المشروع
     */
    private fun extractProject(zipFile: File, projectDir: File): Boolean {
        try {
            // إنشاء دليل المشروع
            projectDir.mkdirs()
            
            // فك ضغط الملف
            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val file = File(projectDir, entry.name)
                    
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        
                        FileOutputStream(file).use { fileOut ->
                            zipIn.copyTo(fileOut)
                        }
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting project", e)
            return false
        }
    }
}

/**
 * نتيجة عملية التخزين السحابي
 */
data class CloudResult(
    val success: Boolean,
    val message: String,
    val data: Any?
)