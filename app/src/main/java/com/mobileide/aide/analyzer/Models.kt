package com.mobileide.aide.analyzer

import java.io.File

/**
 * شدة المشكلة
 */
enum class IssueSeverity {
    /**
     * معلومات أو اقتراحات
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
    CRITICAL;
    
    /**
     * الحصول على اسم العرض للشدة
     */
    fun getDisplayName(): String {
        return when (this) {
            INFO -> "معلومات"
            WARNING -> "تحذير"
            ERROR -> "خطأ"
            CRITICAL -> "حرج"
        }
    }
    
    /**
     * الحصول على لون الشدة
     */
    fun getColor(): String {
        return when (this) {
            INFO -> "#2196F3" // أزرق
            WARNING -> "#FFC107" // أصفر
            ERROR -> "#F44336" // أحمر
            CRITICAL -> "#9C27B0" // بنفسجي
        }
    }
}

/**
 * نوع المشكلة
 */
enum class IssueType {
    /**
     * خطأ في الصيغة
     */
    SYNTAX,
    
    /**
     * خطأ في النمط
     */
    STYLE,
    
    /**
     * خطأ في الأداء
     */
    PERFORMANCE,
    
    /**
     * خطأ في الأمان
     */
    SECURITY,
    
    /**
     * خطأ في إمكانية الصيانة
     */
    MAINTAINABILITY,
    
    /**
     * خطأ في التوافق
     */
    COMPATIBILITY,
    
    /**
     * خطأ في الموارد
     */
    RESOURCES,
    
    /**
     * خطأ في الاختبار
     */
    TESTING,
    
    /**
     * خطأ في التوثيق
     */
    DOCUMENTATION,
    
    /**
     * خطأ آخر
     */
    OTHER;
    
    /**
     * الحصول على اسم العرض لنوع المشكلة
     */
    fun getDisplayName(): String {
        return when (this) {
            SYNTAX -> "صيغة"
            STYLE -> "نمط"
            PERFORMANCE -> "أداء"
            SECURITY -> "أمان"
            MAINTAINABILITY -> "قابلية الصيانة"
            COMPATIBILITY -> "توافق"
            RESOURCES -> "موارد"
            TESTING -> "اختبار"
            DOCUMENTATION -> "توثيق"
            OTHER -> "أخرى"
        }
    }
    
    /**
     * الحصول على وصف نوع المشكلة
     */
    fun getDescription(): String {
        return when (this) {
            SYNTAX -> "مشاكل في صيغة الكود"
            STYLE -> "مشاكل في نمط الكود"
            PERFORMANCE -> "مشاكل قد تؤثر على أداء التطبيق"
            SECURITY -> "مشاكل قد تؤثر على أمان التطبيق"
            MAINTAINABILITY -> "مشاكل قد تؤثر على قابلية صيانة الكود"
            COMPATIBILITY -> "مشاكل قد تؤثر على توافق التطبيق"
            RESOURCES -> "مشاكل في استخدام الموارد"
            TESTING -> "مشاكل في اختبار الكود"
            DOCUMENTATION -> "مشاكل في توثيق الكود"
            OTHER -> "مشاكل أخرى"
        }
    }
}

/**
 * مشكلة في الكود
 */
data class CodeIssue(
    /**
     * رسالة المشكلة
     */
    val message: String,
    
    /**
     * وصف المشكلة
     */
    val description: String,
    
    /**
     * شدة المشكلة
     */
    val severity: IssueSeverity,
    
    /**
     * نوع المشكلة
     */
    val type: IssueType,
    
    /**
     * الملف الذي توجد فيه المشكلة
     */
    val file: File,
    
    /**
     * رقم السطر الذي توجد فيه المشكلة
     */
    val line: Int,
    
    /**
     * رقم العمود الذي توجد فيه المشكلة
     */
    val column: Int,
    
    /**
     * اقتراح لإصلاح المشكلة
     */
    val suggestion: String? = null,
    
    /**
     * رابط للمزيد من المعلومات حول المشكلة
     */
    val infoUrl: String? = null
)

/**
 * نتيجة تحليل الكود
 */
data class AnalysisResult(
    /**
     * ما إذا كان التحليل ناجحًا
     */
    val success: Boolean,
    
    /**
     * قائمة المشاكل المكتشفة
     */
    val issues: List<CodeIssue>,
    
    /**
     * رسالة الخطأ (إذا فشل التحليل)
     */
    val errorMessage: String?
) {
    /**
     * الحصول على عدد المشاكل حسب الشدة
     */
    fun getIssueCountBySeverity(severity: IssueSeverity): Int {
        return issues.count { it.severity == severity }
    }
    
    /**
     * الحصول على عدد المشاكل حسب النوع
     */
    fun getIssueCountByType(type: IssueType): Int {
        return issues.count { it.type == type }
    }
    
    /**
     * الحصول على المشاكل حسب الملف
     */
    fun getIssuesByFile(file: File): List<CodeIssue> {
        return issues.filter { it.file.absolutePath == file.absolutePath }
    }
    
    /**
     * الحصول على المشاكل حسب الشدة
     */
    fun getIssuesBySeverity(severity: IssueSeverity): List<CodeIssue> {
        return issues.filter { it.severity == severity }
    }
    
    /**
     * الحصول على المشاكل حسب النوع
     */
    fun getIssuesByType(type: IssueType): List<CodeIssue> {
        return issues.filter { it.type == type }
    }
}