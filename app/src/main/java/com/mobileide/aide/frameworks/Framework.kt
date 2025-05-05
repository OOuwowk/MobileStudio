package com.mobileide.aide.frameworks

import com.mobileide.compiler.model.Project
import java.io.File

/**
 * واجهة تمثل إطار عمل مدعوم في التطبيق
 */
interface Framework {
    /**
     * الحصول على نوع إطار العمل
     */
    fun getType(): FrameworkType
    
    /**
     * الحصول على اسم إطار العمل
     */
    fun getName(): String
    
    /**
     * الحصول على وصف إطار العمل
     */
    fun getDescription(): String
    
    /**
     * الحصول على رابط الموقع الرسمي لإطار العمل
     */
    fun getWebsiteUrl(): String
    
    /**
     * الحصول على رابط التوثيق الرسمي لإطار العمل
     */
    fun getDocumentationUrl(): String
    
    /**
     * التحقق من حالة تثبيت إطار العمل
     */
    suspend fun checkInstallation(): InstallationStatus
    
    /**
     * تثبيت إطار العمل
     */
    suspend fun install(): InstallationResult
    
    /**
     * إلغاء تثبيت إطار العمل
     */
    suspend fun uninstall(): Boolean
    
    /**
     * الحصول على قوالب المشاريع المتاحة
     */
    suspend fun getTemplates(): List<ProjectTemplate>
    
    /**
     * إنشاء مشروع جديد
     */
    suspend fun createProject(
        name: String,
        packageName: String,
        templateId: String,
        location: File
    ): Project
    
    /**
     * بناء مشروع
     */
    suspend fun buildProject(project: Project): BuildResult
    
    /**
     * تشغيل مشروع
     */
    suspend fun runProject(project: Project): RunResult
    
    /**
     * تصحيح مشروع
     */
    suspend fun debugProject(project: Project): DebugResult
    
    /**
     * الحصول على إصدار إطار العمل المثبت
     */
    suspend fun getInstalledVersion(): String?
    
    /**
     * الحصول على أحدث إصدار متاح لإطار العمل
     */
    suspend fun getLatestVersion(): String?
    
    /**
     * التحقق من وجود تحديثات لإطار العمل
     */
    suspend fun checkForUpdates(): UpdateInfo
}