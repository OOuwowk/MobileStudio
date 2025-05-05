package com.mobileide.aide.ai

import android.content.Context
import android.util.Log
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة مساعد الذكاء الاصطناعي - مسؤولة عن توفير مساعدة ذكية للمطورين
 */
@Singleton
class AiAssistantService @Inject constructor(
    private val context: Context,
    private val aiModelManager: AiModelManager
) {
    companion object {
        private const val TAG = "AiAssistantService"
    }
    
    /**
     * توليد اقتراحات الإكمال التلقائي
     */
    suspend fun generateCompletions(
        code: String,
        language: String,
        cursorPosition: Int,
        maxCompletions: Int = 5
    ): AiResult<List<CodeCompletion>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating completions for language: $language")
            
            // الحصول على نموذج الإكمال التلقائي
            val model = aiModelManager.getModel(AiModelType.CODE_COMPLETION)
            if (model == null) {
                return@withContext AiResult.Error("نموذج الإكمال التلقائي غير متوفر")
            }
            
            // إعداد المدخلات
            val input = mapOf(
                "code" to code,
                "language" to language,
                "cursor_position" to cursorPosition,
                "max_completions" to maxCompletions
            )
            
            // تنفيذ النموذج
            val result = model.execute(input)
            
            // تحليل النتائج
            @Suppress("UNCHECKED_CAST")
            val completions = result["completions"] as? List<Map<String, Any>> ?: emptyList()
            
            // تحويل النتائج إلى كائنات CodeCompletion
            val codeCompletions = completions.map { completion ->
                CodeCompletion(
                    text = completion["text"] as String,
                    displayText = completion["display_text"] as String,
                    type = completion["type"] as String,
                    score = (completion["score"] as Double).toFloat()
                )
            }
            
            return@withContext AiResult.Success(codeCompletions)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating completions", e)
            return@withContext AiResult.Error("فشل توليد الاقتراحات: ${e.message}")
        }
    }
    
    /**
     * توليد تعليقات التوثيق
     */
    suspend fun generateDocumentation(
        code: String,
        language: String
    ): AiResult<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating documentation for language: $language")
            
            // الحصول على نموذج التوثيق
            val model = aiModelManager.getModel(AiModelType.DOCUMENTATION)
            if (model == null) {
                return@withContext AiResult.Error("نموذج التوثيق غير متوفر")
            }
            
            // إعداد المدخلات
            val input = mapOf(
                "code" to code,
                "language" to language
            )
            
            // تنفيذ النموذج
            val result = model.execute(input)
            
            // تحليل النتائج
            val documentation = result["documentation"] as? String ?: ""
            
            return@withContext AiResult.Success(documentation)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating documentation", e)
            return@withContext AiResult.Error("فشل توليد التوثيق: ${e.message}")
        }
    }
    
    /**
     * تحليل الكود وتقديم اقتراحات للتحسين
     */
    suspend fun analyzeCode(
        code: String,
        language: String
    ): AiResult<CodeAnalysis> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing code for language: $language")
            
            // الحصول على نموذج تحليل الكود
            val model = aiModelManager.getModel(AiModelType.CODE_ANALYSIS)
            if (model == null) {
                return@withContext AiResult.Error("نموذج تحليل الكود غير متوفر")
            }
            
            // إعداد المدخلات
            val input = mapOf(
                "code" to code,
                "language" to language
            )
            
            // تنفيذ النموذج
            val result = model.execute(input)
            
            // تحليل النتائج
            @Suppress("UNCHECKED_CAST")
            val issues = (result["issues"] as? List<Map<String, Any>> ?: emptyList()).map { issue ->
                CodeIssue(
                    message = issue["message"] as String,
                    severity = IssueSeverity.valueOf(issue["severity"] as String),
                    line = (issue["line"] as Double).toInt(),
                    column = (issue["column"] as Double).toInt(),
                    suggestion = issue["suggestion"] as String
                )
            }
            
            @Suppress("UNCHECKED_CAST")
            val suggestions = (result["suggestions"] as? List<Map<String, Any>> ?: emptyList()).map { suggestion ->
                CodeSuggestion(
                    message = suggestion["message"] as String,
                    type = SuggestionType.valueOf(suggestion["type"] as String),
                    code = suggestion["code"] as String
                )
            }
            
            val complexity = (result["complexity"] as? Double)?.toFloat() ?: 0f
            val quality = (result["quality"] as? Double)?.toFloat() ?: 0f
            
            val analysis = CodeAnalysis(
                issues = issues,
                suggestions = suggestions,
                complexity = complexity,
                quality = quality
            )
            
            return@withContext AiResult.Success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing code", e)
            return@withContext AiResult.Error("فشل تحليل الكود: ${e.message}")
        }
    }
    
    /**
     * توليد كود بناءً على وصف
     */
    suspend fun generateCode(
        description: String,
        language: String,
        context: String? = null
    ): AiResult<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating code for language: $language")
            
            // الحصول على نموذج توليد الكود
            val model = aiModelManager.getModel(AiModelType.CODE_GENERATION)
            if (model == null) {
                return@withContext AiResult.Error("نموذج توليد الكود غير متوفر")
            }
            
            // إعداد المدخلات
            val input = mapOf(
                "description" to description,
                "language" to language,
                "context" to (context ?: "")
            )
            
            // تنفيذ النموذج
            val result = model.execute(input)
            
            // تحليل النتائج
            val generatedCode = result["code"] as? String ?: ""
            
            return@withContext AiResult.Success(generatedCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating code", e)
            return@withContext AiResult.Error("فشل توليد الكود: ${e.message}")
        }
    }
    
    /**
     * إصلاح الأخطاء في الكود
     */
    suspend fun fixCode(
        code: String,
        language: String,
        errorMessage: String? = null
    ): AiResult<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fixing code for language: $language")
            
            // الحصول على نموذج إصلاح الكود
            val model = aiModelManager.getModel(AiModelType.CODE_FIXING)
            if (model == null) {
                return@withContext AiResult.Error("نموذج إصلاح الكود غير متوفر")
            }
            
            // إعداد المدخلات
            val input = mapOf(
                "code" to code,
                "language" to language,
                "error_message" to (errorMessage ?: "")
            )
            
            // تنفيذ النموذج
            val result = model.execute(input)
            
            // تحليل النتائج
            val fixedCode = result["fixed_code"] as? String ?: ""
            
            return@withContext AiResult.Success(fixedCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing code", e)
            return@withContext AiResult.Error("فشل إصلاح الكود: ${e.message}")
        }
    }
    
    /**
     * تحليل المشروع وتقديم اقتراحات للتحسين
     */
    suspend fun analyzeProject(project: Project): AiResult<ProjectAnalysis> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing project: ${project.name}")
            
            // الحصول على نموذج تحليل المشروع
            val model = aiModelManager.getModel(AiModelType.PROJECT_ANALYSIS)
            if (model == null) {
                return@withContext AiResult.Error("نموذج تحليل المشروع غير متوفر")
            }
            
            // جمع ملفات المشروع
            val projectFiles = collectProjectFiles(project)
            
            // إعداد المدخلات
            val input = mapOf(
                "project_name" to project.name,
                "project_files" to projectFiles
            )
            
            // تنفيذ النموذج
            val result = model.execute(input)
            
            // تحليل النتائج
            @Suppress("UNCHECKED_CAST")
            val issues = (result["issues"] as? List<Map<String, Any>> ?: emptyList()).map { issue ->
                ProjectIssue(
                    message = issue["message"] as String,
                    severity = IssueSeverity.valueOf(issue["severity"] as String),
                    file = issue["file"] as String,
                    line = (issue["line"] as Double).toInt(),
                    suggestion = issue["suggestion"] as String
                )
            }
            
            @Suppress("UNCHECKED_CAST")
            val suggestions = (result["suggestions"] as? List<Map<String, Any>> ?: emptyList()).map { suggestion ->
                ProjectSuggestion(
                    message = suggestion["message"] as String,
                    type = SuggestionType.valueOf(suggestion["type"] as String),
                    details = suggestion["details"] as String
                )
            }
            
            val analysis = ProjectAnalysis(
                issues = issues,
                suggestions = suggestions,
                overallQuality = (result["overall_quality"] as Double).toFloat()
            )
            
            return@withContext AiResult.Success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing project", e)
            return@withContext AiResult.Error("فشل تحليل المشروع: ${e.message}")
        }
    }
    
    /**
     * جمع ملفات المشروع
     */
    private fun collectProjectFiles(project: Project): List<Map<String, String>> {
        val files = mutableListOf<Map<String, String>>()
        val projectDir = File(project.path)
        
        fun scanDirectory(dir: File, relativePath: String = "") {
            dir.listFiles()?.forEach { file ->
                val path = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
                
                if (file.isDirectory) {
                    scanDirectory(file, path)
                } else {
                    // تجاهل الملفات الثنائية والملفات الكبيرة
                    if (isTextFile(file) && file.length() < 1024 * 1024) { // أقل من 1 ميجابايت
                        try {
                            val content = file.readText()
                            files.add(mapOf(
                                "path" to path,
                                "content" to content,
                                "language" to getLanguageFromExtension(file.extension)
                            ))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading file: ${file.absolutePath}", e)
                        }
                    }
                }
            }
        }
        
        scanDirectory(projectDir)
        return files
    }
    
    /**
     * التحقق مما إذا كان الملف نصيًا
     */
    private fun isTextFile(file: File): Boolean {
        val textExtensions = setOf(
            "java", "kt", "xml", "json", "txt", "md", "gradle", "properties",
            "html", "css", "js", "ts", "dart", "py", "c", "cpp", "h", "hpp"
        )
        return textExtensions.contains(file.extension.toLowerCase())
    }
    
    /**
     * الحصول على اللغة من امتداد الملف
     */
    private fun getLanguageFromExtension(extension: String): String {
        return when (extension.toLowerCase()) {
            "java" -> "java"
            "kt" -> "kotlin"
            "xml" -> "xml"
            "json" -> "json"
            "gradle" -> "gradle"
            "html" -> "html"
            "css" -> "css"
            "js" -> "javascript"
            "ts" -> "typescript"
            "dart" -> "dart"
            "py" -> "python"
            "c" -> "c"
            "cpp" -> "cpp"
            "h", "hpp" -> "cpp"
            else -> "text"
        }
    }
}

/**
 * نتيجة عملية الذكاء الاصطناعي
 */
sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Error(val message: String) : AiResult<Nothing>()
}