package com.mobileide.aide.cloud

import java.io.File

/**
 * واجهة مزود التخزين السحابي
 */
interface CloudProvider {
    /**
     * الحصول على نوع مزود التخزين السحابي
     */
    fun getType(): CloudProviderType
    
    /**
     * التحقق من حالة تسجيل الدخول
     */
    suspend fun isLoggedIn(): Boolean
    
    /**
     * تسجيل الدخول إلى مزود التخزين السحابي
     */
    suspend fun login(): Boolean
    
    /**
     * تسجيل الخروج من مزود التخزين السحابي
     */
    suspend fun logout(): Boolean
    
    /**
     * الحصول على قائمة المشاريع المخزنة في السحابة
     */
    suspend fun listProjects(): List<CloudProject>
    
    /**
     * تحميل مشروع إلى السحابة
     */
    suspend fun uploadProject(projectName: String, projectFile: File): Boolean
    
    /**
     * تنزيل مشروع من السحابة
     */
    suspend fun downloadProject(projectName: String, outputFile: File): Boolean
    
    /**
     * حذف مشروع من السحابة
     */
    suspend fun deleteProject(projectName: String): Boolean
}

/**
 * نوع مزود التخزين السحابي
 */
enum class CloudProviderType {
    /**
     * Google Drive
     */
    GOOGLE_DRIVE,
    
    /**
     * Dropbox
     */
    DROPBOX,
    
    /**
     * OneDrive
     */
    ONEDRIVE,
    
    /**
     * GitHub
     */
    GITHUB
}

/**
 * مشروع مخزن في السحابة
 */
data class CloudProject(
    /**
     * اسم المشروع
     */
    val name: String,
    
    /**
     * معرف المشروع في السحابة
     */
    val id: String,
    
    /**
     * حجم المشروع (بالبايت)
     */
    val size: Long,
    
    /**
     * تاريخ آخر تعديل
     */
    val lastModified: Long,
    
    /**
     * رابط المشروع
     */
    val url: String?
)