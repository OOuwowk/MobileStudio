package com.mobileide.aide.frameworks

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mobileide.aide.frameworks.flutter.FlutterFramework
import com.mobileide.aide.frameworks.reactnative.ReactNativeFramework
import com.mobileide.aide.frameworks.kmp.KMPFramework
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير أطر العمل المسؤول عن إدارة جميع أطر العمل المدعومة في التطبيق
 */
@Singleton
class FrameworkManager @Inject constructor(
    private val context: Context,
    private val frameworkInstaller: FrameworkInstaller
) {
    // قائمة أطر العمل المدعومة
    private val frameworks = mutableMapOf<FrameworkType, Framework>()
    
    // حالة تثبيت أطر العمل
    private val _installationStatus = MutableLiveData<Map<FrameworkType, InstallationStatus>>()
    val installationStatus: LiveData<Map<FrameworkType, InstallationStatus>> = _installationStatus
    
    init {
        // تهيئة أطر العمل المدعومة
        frameworks[FrameworkType.ANDROID_NATIVE] = AndroidNativeFramework(context)
        frameworks[FrameworkType.FLUTTER] = FlutterFramework(context, frameworkInstaller)
        frameworks[FrameworkType.REACT_NATIVE] = ReactNativeFramework(context, frameworkInstaller)
        frameworks[FrameworkType.KOTLIN_MULTIPLATFORM] = KMPFramework(context, frameworkInstaller)
        
        // تحديث حالة التثبيت الأولية
        updateInstallationStatus()
    }
    
    /**
     * الحصول على إطار عمل محدد
     */
    fun getFramework(type: FrameworkType): Framework {
        return frameworks[type] ?: throw IllegalArgumentException("إطار العمل غير مدعوم: $type")
    }
    
    /**
     * الحصول على جميع أطر العمل المدعومة
     */
    fun getAllFrameworks(): List<Framework> {
        return frameworks.values.toList()
    }
    
    /**
     * تحديث حالة تثبيت جميع أطر العمل
     */
    suspend fun updateInstallationStatus() = withContext(Dispatchers.IO) {
        val statusMap = mutableMapOf<FrameworkType, InstallationStatus>()
        
        frameworks.forEach { (type, framework) ->
            statusMap[type] = framework.checkInstallation()
        }
        
        _installationStatus.postValue(statusMap)
    }
    
    /**
     * تثبيت إطار عمل محدد
     */
    suspend fun installFramework(type: FrameworkType): InstallationResult = withContext(Dispatchers.IO) {
        val framework = getFramework(type)
        val result = framework.install()
        
        // تحديث حالة التثبيت بعد محاولة التثبيت
        updateInstallationStatus()
        
        return@withContext result
    }
    
    /**
     * إلغاء تثبيت إطار عمل محدد
     */
    suspend fun uninstallFramework(type: FrameworkType): Boolean = withContext(Dispatchers.IO) {
        val framework = getFramework(type)
        val result = framework.uninstall()
        
        // تحديث حالة التثبيت بعد محاولة إلغاء التثبيت
        updateInstallationStatus()
        
        return@withContext result
    }
    
    /**
     * إنشاء مشروع جديد باستخدام إطار عمل محدد
     */
    suspend fun createProject(
        type: FrameworkType,
        name: String,
        packageName: String,
        templateId: String,
        location: File
    ): Project = withContext(Dispatchers.IO) {
        val framework = getFramework(type)
        
        // التحقق من تثبيت إطار العمل
        val status = framework.checkInstallation()
        if (status != InstallationStatus.INSTALLED) {
            throw IllegalStateException("إطار العمل غير مثبت: $type")
        }
        
        // إنشاء المشروع
        return@withContext framework.createProject(name, packageName, templateId, location)
    }
    
    /**
     * بناء مشروع باستخدام إطار العمل المناسب
     */
    suspend fun buildProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        val frameworkType = project.frameworkType ?: FrameworkType.ANDROID_NATIVE
        val framework = getFramework(frameworkType)
        
        // التحقق من تثبيت إطار العمل
        val status = framework.checkInstallation()
        if (status != InstallationStatus.INSTALLED) {
            return@withContext BuildResult(false, null, "إطار العمل غير مثبت: $frameworkType")
        }
        
        // بناء المشروع
        return@withContext framework.buildProject(project)
    }
    
    /**
     * تشغيل مشروع باستخدام إطار العمل المناسب
     */
    suspend fun runProject(project: Project): RunResult = withContext(Dispatchers.IO) {
        val frameworkType = project.frameworkType ?: FrameworkType.ANDROID_NATIVE
        val framework = getFramework(frameworkType)
        
        // التحقق من تثبيت إطار العمل
        val status = framework.checkInstallation()
        if (status != InstallationStatus.INSTALLED) {
            return@withContext RunResult(false, "إطار العمل غير مثبت: $frameworkType")
        }
        
        // تشغيل المشروع
        return@withContext framework.runProject(project)
    }
    
    /**
     * تصحيح مشروع باستخدام إطار العمل المناسب
     */
    suspend fun debugProject(project: Project): DebugResult = withContext(Dispatchers.IO) {
        val frameworkType = project.frameworkType ?: FrameworkType.ANDROID_NATIVE
        val framework = getFramework(frameworkType)
        
        // التحقق من تثبيت إطار العمل
        val status = framework.checkInstallation()
        if (status != InstallationStatus.INSTALLED) {
            return@withContext DebugResult(false, "إطار العمل غير مثبت: $frameworkType")
        }
        
        // تصحيح المشروع
        return@withContext framework.debugProject(project)
    }
    
    /**
     * الحصول على قوالب المشاريع المتاحة لإطار عمل محدد
     */
    suspend fun getTemplates(type: FrameworkType): List<ProjectTemplate> = withContext(Dispatchers.IO) {
        val framework = getFramework(type)
        return@withContext framework.getTemplates()
    }
}