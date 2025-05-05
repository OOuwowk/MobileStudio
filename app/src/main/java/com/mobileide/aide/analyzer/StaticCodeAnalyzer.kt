package com.mobileide.aide.analyzer

import android.content.Context
import android.util.Log
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * محلل الكود الثابت المسؤول عن تحليل الكود وتحديد المشاكل المحتملة
 */
@Singleton
class StaticCodeAnalyzer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "StaticCodeAnalyzer"
    }
    
    // قائمة المحللات المتخصصة
    private val analyzers = mapOf(
        FrameworkType.ANDROID_NATIVE to listOf(
            JavaAnalyzer(),
            KotlinAnalyzer(),
            AndroidManifestAnalyzer(),
            AndroidResourceAnalyzer()
        ),
        FrameworkType.FLUTTER to listOf(
            DartAnalyzer(),
            FlutterResourceAnalyzer()
        ),
        FrameworkType.REACT_NATIVE to listOf(
            JavaScriptAnalyzer(),
            TypeScriptAnalyzer(),
            ReactNativeResourceAnalyzer()
        ),
        FrameworkType.KOTLIN_MULTIPLATFORM to listOf(
            KotlinAnalyzer(),
            KotlinMultiplatformAnalyzer()
        )
    )
    
    /**
     * تحليل مشروع كامل
     */
    suspend fun analyzeProject(project: Project): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return@withContext AnalysisResult(
                    success = false,
                    issues = emptyList(),
                    errorMessage = "دليل المشروع غير موجود: ${project.path}"
                )
            }
            
            // تحديد المحللات المناسبة للمشروع
            val frameworkType = project.frameworkType ?: FrameworkType.ANDROID_NATIVE
            val projectAnalyzers = analyzers[frameworkType] ?: analyzers[FrameworkType.ANDROID_NATIVE]!!
            
            // تحليل المشروع باستخدام كل محلل
            val allIssues = mutableListOf<CodeIssue>()
            
            projectAnalyzers.forEach { analyzer ->
                val analyzerIssues = analyzer.analyzeProject(projectDir)
                allIssues.addAll(analyzerIssues)
            }
            
            // ترتيب المشاكل حسب الشدة
            val sortedIssues = allIssues.sortedByDescending { it.severity }
            
            return@withContext AnalysisResult(
                success = true,
                issues = sortedIssues,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing project", e)
            return@withContext AnalysisResult(
                success = false,
                issues = emptyList(),
                errorMessage = "فشل تحليل المشروع: ${e.message}"
            )
        }
    }
    
    /**
     * تحليل ملف محدد
     */
    suspend fun analyzeFile(file: File): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.isFile) {
                return@withContext AnalysisResult(
                    success = false,
                    issues = emptyList(),
                    errorMessage = "الملف غير موجود: ${file.absolutePath}"
                )
            }
            
            // تحديد المحلل المناسب للملف
            val fileAnalyzer = getAnalyzerForFile(file)
            
            if (fileAnalyzer == null) {
                return@withContext AnalysisResult(
                    success = false,
                    issues = emptyList(),
                    errorMessage = "لا يوجد محلل مناسب لهذا النوع من الملفات: ${file.extension}"
                )
            }
            
            // تحليل الملف
            val issues = fileAnalyzer.analyzeFile(file)
            
            // ترتيب المشاكل حسب الشدة
            val sortedIssues = issues.sortedByDescending { it.severity }
            
            return@withContext AnalysisResult(
                success = true,
                issues = sortedIssues,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing file", e)
            return@withContext AnalysisResult(
                success = false,
                issues = emptyList(),
                errorMessage = "فشل تحليل الملف: ${e.message}"
            )
        }
    }
    
    /**
     * الحصول على المحلل المناسب لملف محدد
     */
    private fun getAnalyzerForFile(file: File): CodeAnalyzer? {
        val extension = file.extension.toLowerCase()
        val fileName = file.name.toLowerCase()
        
        return when {
            extension == "java" -> JavaAnalyzer()
            extension == "kt" || extension == "kts" -> KotlinAnalyzer()
            extension == "xml" && fileName == "androidmanifest.xml" -> AndroidManifestAnalyzer()
            extension == "xml" && (fileName.startsWith("layout_") || file.parentFile?.name == "layout") -> AndroidResourceAnalyzer()
            extension == "dart" -> DartAnalyzer()
            extension == "js" || extension == "jsx" -> JavaScriptAnalyzer()
            extension == "ts" || extension == "tsx" -> TypeScriptAnalyzer()
            extension == "yaml" || extension == "yml" -> FlutterResourceAnalyzer()
            extension == "json" -> ReactNativeResourceAnalyzer()
            else -> null
        }
    }
    
    /**
     * تصفية المشاكل حسب الشدة
     */
    fun filterIssuesBySeverity(issues: List<CodeIssue>, minSeverity: IssueSeverity): List<CodeIssue> {
        return issues.filter { it.severity.ordinal >= minSeverity.ordinal }
    }
    
    /**
     * تصفية المشاكل حسب النوع
     */
    fun filterIssuesByType(issues: List<CodeIssue>, types: List<IssueType>): List<CodeIssue> {
        return issues.filter { it.type in types }
    }
    
    /**
     * تصفية المشاكل حسب الملف
     */
    fun filterIssuesByFile(issues: List<CodeIssue>, filePath: String): List<CodeIssue> {
        return issues.filter { it.file.absolutePath == filePath }
    }
    
    /**
     * تصدير نتائج التحليل إلى ملف
     */
    suspend fun exportAnalysisResult(result: AnalysisResult, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // إنشاء محتوى التقرير
            val reportContent = buildString {
                appendLine("# تقرير تحليل الكود")
                appendLine()
                
                if (!result.success) {
                    appendLine("## خطأ في التحليل")
                    appendLine(result.errorMessage)
                    return@buildString
                }
                
                appendLine("## ملخص")
                appendLine("- عدد المشاكل: ${result.issues.size}")
                appendLine("- مشاكل حرجة: ${result.issues.count { it.severity == IssueSeverity.CRITICAL }}")
                appendLine("- مشاكل خطيرة: ${result.issues.count { it.severity == IssueSeverity.ERROR }}")
                appendLine("- تحذيرات: ${result.issues.count { it.severity == IssueSeverity.WARNING }}")
                appendLine("- اقتراحات: ${result.issues.count { it.severity == IssueSeverity.INFO }}")
                appendLine()
                
                appendLine("## المشاكل المكتشفة")
                result.issues.forEachIndexed { index, issue ->
                    appendLine("### ${index + 1}. ${issue.message}")
                    appendLine("- الشدة: ${issue.severity}")
                    appendLine("- النوع: ${issue.type}")
                    appendLine("- الملف: ${issue.file.absolutePath}")
                    appendLine("- السطر: ${issue.line}")
                    appendLine("- العمود: ${issue.column}")
                    appendLine("- الوصف: ${issue.description}")
                    if (issue.suggestion != null) {
                        appendLine("- الاقتراح: ${issue.suggestion}")
                    }
                    appendLine()
                }
            }
            
            // كتابة التقرير إلى الملف
            outputFile.writeText(reportContent)
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting analysis result", e)
            return@withContext false
        }
    }
}