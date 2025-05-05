package com.mobileide.aide.ai

/**
 * نوع نموذج الذكاء الاصطناعي
 */
enum class AiModelType {
    /**
     * الإكمال التلقائي للكود
     */
    CODE_COMPLETION,
    
    /**
     * توليد التوثيق
     */
    DOCUMENTATION,
    
    /**
     * تحليل الكود
     */
    CODE_ANALYSIS,
    
    /**
     * توليد الكود
     */
    CODE_GENERATION,
    
    /**
     * إصلاح الكود
     */
    CODE_FIXING,
    
    /**
     * تحليل المشروع
     */
    PROJECT_ANALYSIS
}

/**
 * اقتراح إكمال الكود
 */
data class CodeCompletion(
    /**
     * نص الاقتراح
     */
    val text: String,
    
    /**
     * النص المعروض
     */
    val displayText: String,
    
    /**
     * نوع الاقتراح
     */
    val type: String,
    
    /**
     * درجة الثقة
     */
    val score: Float
)

/**
 * تحليل الكود
 */
data class CodeAnalysis(
    /**
     * المشاكل في الكود
     */
    val issues: List<CodeIssue>,
    
    /**
     * الاقتراحات لتحسين الكود
     */
    val suggestions: List<CodeSuggestion>,
    
    /**
     * تعقيد الكود
     */
    val complexity: Float,
    
    /**
     * جودة الكود
     */
    val quality: Float
)

/**
 * مشكلة في الكود
 */
data class CodeIssue(
    /**
     * رسالة المشكلة
     */
    val message: String,
    
    /**
     * خطورة المشكلة
     */
    val severity: IssueSeverity,
    
    /**
     * رقم السطر
     */
    val line: Int,
    
    /**
     * رقم العمود
     */
    val column: Int,
    
    /**
     * اقتراح لحل المشكلة
     */
    val suggestion: String
)

/**
 * اقتراح لتحسين الكود
 */
data class CodeSuggestion(
    /**
     * رسالة الاقتراح
     */
    val message: String,
    
    /**
     * نوع الاقتراح
     */
    val type: SuggestionType,
    
    /**
     * الكود المقترح
     */
    val code: String
)

/**
 * تحليل المشروع
 */
data class ProjectAnalysis(
    /**
     * المشاكل في المشروع
     */
    val issues: List<ProjectIssue>,
    
    /**
     * الاقتراحات لتحسين المشروع
     */
    val suggestions: List<ProjectSuggestion>,
    
    /**
     * الجودة الإجمالية للمشروع
     */
    val overallQuality: Float
)

/**
 * مشكلة في المشروع
 */
data class ProjectIssue(
    /**
     * رسالة المشكلة
     */
    val message: String,
    
    /**
     * خطورة المشكلة
     */
    val severity: IssueSeverity,
    
    /**
     * الملف الذي يحتوي على المشكلة
     */
    val file: String,
    
    /**
     * رقم السطر
     */
    val line: Int,
    
    /**
     * اقتراح لحل المشكلة
     */
    val suggestion: String
)

/**
 * اقتراح لتحسين المشروع
 */
data class ProjectSuggestion(
    /**
     * رسالة الاقتراح
     */
    val message: String,
    
    /**
     * نوع الاقتراح
     */
    val type: SuggestionType,
    
    /**
     * تفاصيل الاقتراح
     */
    val details: String
)

/**
 * خطورة المشكلة
 */
enum class IssueSeverity {
    /**
     * معلومات
     */
    INFO,
    
    /**
     * تحذير
     */
    WARNING,
    
    /**
     * خطأ
     */
    ERROR,
    
    /**
     * خطأ حرج
     */
    CRITICAL
}

/**
 * نوع الاقتراح
 */
enum class SuggestionType {
    /**
     * تحسين الأداء
     */
    PERFORMANCE,
    
    /**
     * تحسين الأمان
     */
    SECURITY,
    
    /**
     * تحسين قابلية الصيانة
     */
    MAINTAINABILITY,
    
    /**
     * تحسين قابلية القراءة
     */
    READABILITY,
    
    /**
     * تحسين الهيكل
     */
    STRUCTURE,
    
    /**
     * تحسين الوظائف
     */
    FUNCTIONALITY
}