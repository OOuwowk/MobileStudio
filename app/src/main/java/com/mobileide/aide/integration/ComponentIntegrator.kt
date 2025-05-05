package com.mobileide.aide.integration

import android.content.Context
import android.util.Log
import com.mobileide.aide.analyzer.StaticCodeAnalyzer
import com.mobileide.aide.frameworks.FrameworkManager
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.aide.git.GitService
import com.mobileide.compiler.CompilerService
import com.mobileide.compiler.model.Project
import com.mobileide.debugger.DebuggerService
import com.mobileide.editor.EditorService
import com.mobileide.designer.DesignerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مكامل المكونات - مسؤول عن دمج جميع مكونات التطبيق وتنسيق التفاعل بينها
 */
@Singleton
class ComponentIntegrator @Inject constructor(
    private val context: Context,
    private val editorService: EditorService,
    private val designerService: DesignerService,
    private val compilerService: CompilerService,
    private val debuggerService: DebuggerService,
    private val frameworkManager: FrameworkManager,
    private val staticCodeAnalyzer: StaticCodeAnalyzer,
    private val gitService: GitService
) {
    companion object {
        private const val TAG = "ComponentIntegrator"
    }
    
    /**
     * فتح مشروع في جميع المكونات
     */
    suspend fun openProject(project: Project): IntegrationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Opening project: ${project.name}")
            
            // تحديد إطار العمل المستخدم في المشروع
            val frameworkType = detectFrameworkType(project)
            val framework = frameworkManager.getFramework(frameworkType)
            
            // فتح المشروع في المحرر
            val editorResult = editorService.openProject(project)
            if (!editorResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل فتح المشروع في المحرر: ${editorResult.message}",
                    data = null
                )
            }
            
            // فتح المشروع في المصمم
            val designerResult = designerService.openProject(project)
            if (!designerResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل فتح المشروع في المصمم: ${designerResult.message}",
                    data = null
                )
            }
            
            // تهيئة المترجم للمشروع
            val compilerResult = compilerService.initializeForProject(project, frameworkType)
            if (!compilerResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل تهيئة المترجم للمشروع: ${compilerResult.message}",
                    data = null
                )
            }
            
            // تهيئة المصحح للمشروع
            val debuggerResult = debuggerService.initializeForProject(project, frameworkType)
            if (!debuggerResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل تهيئة المصحح للمشروع: ${debuggerResult.message}",
                    data = null
                )
            }
            
            // إنشاء بيانات التكامل
            val integrationData = IntegrationData(
                project = project,
                frameworkType = frameworkType,
                framework = framework,
                editorData = editorResult.data,
                designerData = designerResult.data,
                compilerData = compilerResult.data,
                debuggerData = debuggerResult.data
            )
            
            return@withContext IntegrationResult(
                success = true,
                message = "تم فتح المشروع بنجاح في جميع المكونات",
                data = integrationData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening project", e)
            return@withContext IntegrationResult(
                success = false,
                message = "فشل فتح المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * بناء المشروع باستخدام جميع المكونات
     */
    suspend fun buildProject(project: Project): IntegrationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building project: ${project.name}")
            
            // تحديد إطار العمل المستخدم في المشروع
            val frameworkType = detectFrameworkType(project)
            val framework = frameworkManager.getFramework(frameworkType)
            
            // حفظ جميع الملفات المفتوحة في المحرر
            val saveResult = editorService.saveAllFiles()
            if (!saveResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل حفظ الملفات: ${saveResult.message}",
                    data = null
                )
            }
            
            // تحليل الكود قبل البناء
            val analysisResult = staticCodeAnalyzer.analyzeProject(project)
            val criticalIssues = analysisResult.issues.filter { it.severity.name == "CRITICAL" || it.severity.name == "ERROR" }
            
            if (criticalIssues.isNotEmpty()) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "يوجد مشاكل حرجة في الكود تمنع البناء: ${criticalIssues.size} مشكلة",
                    data = analysisResult
                )
            }
            
            // بناء المشروع باستخدام المترجم المناسب
            val buildResult = framework.buildProject(project, compilerService)
            
            if (!buildResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل بناء المشروع: ${buildResult.message}",
                    data = buildResult
                )
            }
            
            // إنشاء بيانات التكامل
            val integrationData = IntegrationData(
                project = project,
                frameworkType = frameworkType,
                framework = framework,
                editorData = null,
                designerData = null,
                compilerData = buildResult,
                debuggerData = null
            )
            
            return@withContext IntegrationResult(
                success = true,
                message = "تم بناء المشروع بنجاح",
                data = integrationData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building project", e)
            return@withContext IntegrationResult(
                success = false,
                message = "فشل بناء المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تشغيل المشروع باستخدام جميع المكونات
     */
    suspend fun runProject(project: Project): IntegrationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Running project: ${project.name}")
            
            // بناء المشروع أولاً
            val buildResult = buildProject(project)
            if (!buildResult.success) {
                return@withContext buildResult
            }
            
            // تحديد إطار العمل المستخدم في المشروع
            val frameworkType = detectFrameworkType(project)
            val framework = frameworkManager.getFramework(frameworkType)
            
            // تشغيل المشروع
            val runResult = framework.runProject(project, compilerService)
            
            if (!runResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل تشغيل المشروع: ${runResult.message}",
                    data = runResult
                )
            }
            
            // إنشاء بيانات التكامل
            val integrationData = IntegrationData(
                project = project,
                frameworkType = frameworkType,
                framework = framework,
                editorData = null,
                designerData = null,
                compilerData = buildResult.data,
                debuggerData = null
            )
            
            return@withContext IntegrationResult(
                success = true,
                message = "تم تشغيل المشروع بنجاح",
                data = integrationData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error running project", e)
            return@withContext IntegrationResult(
                success = false,
                message = "فشل تشغيل المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تصحيح المشروع باستخدام جميع المكونات
     */
    suspend fun debugProject(project: Project): IntegrationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Debugging project: ${project.name}")
            
            // بناء المشروع أولاً في وضع التصحيح
            val buildResult = compilerService.buildProjectForDebugging(project)
            if (!buildResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل بناء المشروع للتصحيح: ${buildResult.message}",
                    data = null
                )
            }
            
            // تحديد إطار العمل المستخدم في المشروع
            val frameworkType = detectFrameworkType(project)
            val framework = frameworkManager.getFramework(frameworkType)
            
            // بدء جلسة التصحيح
            val debugResult = framework.startDebugging(project, debuggerService)
            
            if (!debugResult.success) {
                return@withContext IntegrationResult(
                    success = false,
                    message = "فشل بدء التصحيح: ${debugResult.message}",
                    data = null
                )
            }
            
            // إنشاء بيانات التكامل
            val integrationData = IntegrationData(
                project = project,
                frameworkType = frameworkType,
                framework = framework,
                editorData = null,
                designerData = null,
                compilerData = buildResult,
                debuggerData = debugResult
            )
            
            return@withContext IntegrationResult(
                success = true,
                message = "تم بدء تصحيح المشروع بنجاح",
                data = integrationData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error debugging project", e)
            return@withContext IntegrationResult(
                success = false,
                message = "فشل تصحيح المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تحليل المشروع باستخدام محلل الكود الثابت
     */
    suspend fun analyzeProject(project: Project): IntegrationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing project: ${project.name}")
            
            // تحليل الكود
            val analysisResult = staticCodeAnalyzer.analyzeProject(project)
            
            // إنشاء بيانات التكامل
            val integrationData = IntegrationData(
                project = project,
                frameworkType = detectFrameworkType(project),
                framework = frameworkManager.getFramework(detectFrameworkType(project)),
                editorData = null,
                designerData = null,
                compilerData = null,
                debuggerData = null,
                analysisResult = analysisResult
            )
            
            return@withContext IntegrationResult(
                success = true,
                message = "تم تحليل المشروع بنجاح: ${analysisResult.issues.size} مشكلة",
                data = integrationData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing project", e)
            return@withContext IntegrationResult(
                success = false,
                message = "فشل تحليل المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تحديد نوع إطار العمل المستخدم في المشروع
     */
    private fun detectFrameworkType(project: Project): FrameworkType {
        val projectDir = File(project.path)
        
        // التحقق من وجود ملفات Flutter
        if (File(projectDir, "pubspec.yaml").exists()) {
            return FrameworkType.FLUTTER
        }
        
        // التحقق من وجود ملفات React Native
        if (File(projectDir, "package.json").exists() && 
            File(projectDir, "node_modules").exists() && 
            File(projectDir, "android").exists()) {
            return FrameworkType.REACT_NATIVE
        }
        
        // التحقق من وجود ملفات Kotlin Multiplatform
        if (File(projectDir, "shared").exists() && 
            File(projectDir, "androidApp").exists() && 
            File(projectDir, "iosApp").exists()) {
            return FrameworkType.KOTLIN_MULTIPLATFORM
        }
        
        // افتراضيًا، استخدم Android Native
        return FrameworkType.ANDROID_NATIVE
    }
}