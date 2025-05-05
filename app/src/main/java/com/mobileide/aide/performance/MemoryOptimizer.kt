package com.mobileide.aide.performance

import android.content.Context
import android.util.Log
import android.util.LruCache
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * محسن الذاكرة - مسؤول عن تحسين استخدام الذاكرة في التطبيق
 */
@Singleton
class MemoryOptimizer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "MemoryOptimizer"
        
        // حجم ذاكرة التخزين المؤقت الافتراضي (بالميجابايت)
        private const val DEFAULT_CACHE_SIZE_MB = 16
        
        // الحد الأقصى لحجم الملف للتحميل في الذاكرة (بالميجابايت)
        private const val MAX_FILE_SIZE_MB = 5
        
        // الحد الأقصى لعدد الملفات المفتوحة في وقت واحد
        private const val MAX_OPEN_FILES = 10
    }
    
    // ذاكرة التخزين المؤقت للملفات
    private val fileCache: LruCache<String, String>
    
    // قائمة الملفات المفتوحة حاليًا
    private val openFiles = mutableListOf<WeakReference<File>>()
    
    init {
        // تحديد حجم ذاكرة التخزين المؤقت بناءً على الذاكرة المتاحة
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt()
        val cacheSize = maxMemory / 8
        
        // إنشاء ذاكرة التخزين المؤقت
        fileCache = object : LruCache<String, String>(cacheSize) {
            override fun sizeOf(key: String, value: String): Int {
                // حساب حجم القيمة بالكيلوبايت
                return value.length / 1024
            }
        }
        
        Log.d(TAG, "Initialized memory optimizer with cache size: $cacheSize MB")
    }
    
    /**
     * تحميل ملف إلى الذاكرة مع تحسين استخدام الذاكرة
     */
    fun loadFile(file: File): String? {
        try {
            // التحقق من وجود الملف في ذاكرة التخزين المؤقت
            val cachedContent = fileCache.get(file.absolutePath)
            if (cachedContent != null) {
                Log.d(TAG, "Cache hit for file: ${file.name}")
                return cachedContent
            }
            
            // التحقق من حجم الملف
            val fileSizeMB = file.length() / 1024 / 1024
            if (fileSizeMB > MAX_FILE_SIZE_MB) {
                Log.w(TAG, "File too large to load into memory: ${file.name} ($fileSizeMB MB)")
                return loadLargeFile(file)
            }
            
            // إدارة عدد الملفات المفتوحة
            manageOpenFiles()
            
            // قراءة محتوى الملف
            val content = file.readText()
            
            // تخزين المحتوى في ذاكرة التخزين المؤقت
            fileCache.put(file.absolutePath, content)
            
            // إضافة الملف إلى قائمة الملفات المفتوحة
            openFiles.add(WeakReference(file))
            
            return content
        } catch (e: Exception) {
            Log.e(TAG, "Error loading file: ${file.absolutePath}", e)
            return null
        }
    }
    
    /**
     * تحميل ملف كبير بطريقة تحسن استخدام الذاكرة
     */
    private fun loadLargeFile(file: File): String? {
        try {
            // استخدام تقنية تدفق البيانات لقراءة الملف بكفاءة
            val content = StringBuilder()
            file.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    content.append(line).append("\n")
                }
            }
            
            return content.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading large file: ${file.absolutePath}", e)
            return null
        }
    }
    
    /**
     * حفظ ملف مع تحسين استخدام الذاكرة
     */
    fun saveFile(file: File, content: String): Boolean {
        try {
            // حفظ المحتوى في الملف
            file.writeText(content)
            
            // تحديث ذاكرة التخزين المؤقت
            fileCache.put(file.absolutePath, content)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: ${file.absolutePath}", e)
            return false
        }
    }
    
    /**
     * إدارة عدد الملفات المفتوحة
     */
    private fun manageOpenFiles() {
        // إزالة المراجع الضعيفة التي تم جمعها
        openFiles.removeAll { it.get() == null }
        
        // إذا تجاوز عدد الملفات المفتوحة الحد الأقصى، أغلق بعضها
        if (openFiles.size >= MAX_OPEN_FILES) {
            // إزالة أقدم الملفات من ذاكرة التخزين المؤقت
            val filesToRemove = openFiles.take(openFiles.size - MAX_OPEN_FILES + 1)
            filesToRemove.forEach { fileRef ->
                fileRef.get()?.let { file ->
                    fileCache.remove(file.absolutePath)
                    Log.d(TAG, "Removed file from cache: ${file.name}")
                }
            }
            
            // إزالة الملفات من قائمة الملفات المفتوحة
            openFiles.removeAll(filesToRemove)
        }
    }
    
    /**
     * مسح ذاكرة التخزين المؤقت
     */
    fun clearCache() {
        fileCache.evictAll()
        openFiles.clear()
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * الحصول على إحصائيات الذاكرة
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        return MemoryStats(
            usedMemoryMB = usedMemory,
            freeMemoryMB = freeMemory,
            totalMemoryMB = totalMemory,
            maxMemoryMB = maxMemory,
            cacheSize = fileCache.size(),
            cacheHits = fileCache.hitCount(),
            cacheMisses = fileCache.missCount(),
            openFilesCount = openFiles.size
        )
    }
}

/**
 * إحصائيات الذاكرة
 */
data class MemoryStats(
    val usedMemoryMB: Long,
    val freeMemoryMB: Long,
    val totalMemoryMB: Long,
    val maxMemoryMB: Long,
    val cacheSize: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val openFilesCount: Int
)