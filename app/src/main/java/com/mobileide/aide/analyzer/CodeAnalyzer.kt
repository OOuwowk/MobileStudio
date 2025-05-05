package com.mobileide.aide.analyzer

import java.io.File

/**
 * واجهة لمحلل الكود
 */
interface CodeAnalyzer {
    /**
     * تحليل مشروع كامل
     */
    fun analyzeProject(projectDir: File): List<CodeIssue>
    
    /**
     * تحليل ملف محدد
     */
    fun analyzeFile(file: File): List<CodeIssue>
    
    /**
     * الحصول على اسم المحلل
     */
    fun getName(): String
    
    /**
     * الحصول على وصف المحلل
     */
    fun getDescription(): String
    
    /**
     * الحصول على قائمة امتدادات الملفات المدعومة
     */
    fun getSupportedFileExtensions(): List<String>
}