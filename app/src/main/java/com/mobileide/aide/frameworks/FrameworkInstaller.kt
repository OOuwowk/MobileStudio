package com.mobileide.aide.frameworks

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مثبت أطر العمل المسؤول عن تنزيل وتثبيت أطر العمل المختلفة
 */
@Singleton
class FrameworkInstaller @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "FrameworkInstaller"
        private const val BUFFER_SIZE = 8192
    }
    
    /**
     * الحصول على دليل التثبيت لإطار عمل محدد
     */
    fun getInstallationDirectory(type: FrameworkType): File {
        val frameworksDir = File(context.filesDir, "frameworks")
        return File(frameworksDir, type.name.toLowerCase())
    }
    
    /**
     * تنزيل ملف من URL محدد
     */
    suspend fun downloadFile(url: String, destination: File, progressCallback: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            // إنشاء الدليل الأب إذا لم يكن موجودًا
            destination.parentFile?.mkdirs()
            
            // فتح اتصال URL
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            
            // الحصول على حجم الملف
            val fileLength = connection.contentLength
            
            // تنزيل الملف
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val data = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    var count: Int
                    
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        
                        // تحديث التقدم
                        if (fileLength > 0) {
                            val progress = total.toFloat() / fileLength.toFloat()
                            progressCallback(progress)
                        }
                    }
                }
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file: $url", e)
            return@withContext false
        }
    }
    
    /**
     * فك ضغط ملف ZIP إلى دليل محدد
     */
    suspend fun unzipFile(zipFile: File, destinationDir: File, progressCallback: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            // إنشاء الدليل إذا لم يكن موجودًا
            destinationDir.mkdirs()
            
            // الحصول على عدد الإدخالات في ملف ZIP
            var totalEntries = 0
            ZipInputStream(zipFile.inputStream()).use { zis ->
                while (zis.nextEntry != null) {
                    totalEntries++
                }
            }
            
            // فك ضغط الملف
            var extractedEntries = 0
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                
                while (entry != null) {
                    val newFile = File(destinationDir, entry.name)
                    
                    // إنشاء الدليل الأب إذا لم يكن موجودًا
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        // إنشاء الدليل الأب إذا لم يكن موجودًا
                        newFile.parentFile?.mkdirs()
                        
                        // استخراج الملف
                        FileOutputStream(newFile).use { fos ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    
                    // تحديث التقدم
                    extractedEntries++
                    progressCallback(extractedEntries.toFloat() / totalEntries.toFloat())
                    
                    // الانتقال إلى الإدخال التالي
                    entry = zis.nextEntry
                }
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error unzipping file: ${zipFile.absolutePath}", e)
            return@withContext false
        }
    }
    
    /**
     * تنفيذ أمر في دليل محدد
     */
    suspend fun executeCommand(command: String, workingDir: File): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder()
                .command("sh", "-c", command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
            
            // قراءة الإخراج
            val output = process.inputStream.bufferedReader().use { it.readText() }
            
            // انتظار انتهاء العملية
            val exitCode = process.waitFor()
            
            return@withContext CommandResult(exitCode == 0, output, exitCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            return@withContext CommandResult(false, e.message ?: "Unknown error", -1)
        }
    }
    
    /**
     * تعيين أذونات التنفيذ لملف
     */
    suspend fun setExecutablePermissions(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            file.setExecutable(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting executable permissions: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * التحقق من وجود أداة في مسار النظام
     */
    suspend fun isToolAvailable(toolName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val process = Runtime.getRuntime().exec("which $toolName")
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking tool availability: $toolName", e)
            false
        }
    }
}