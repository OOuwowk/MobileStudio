package com.mobileide.aide.integration

import com.mobileide.aide.analyzer.AnalysisResult
import com.mobileide.aide.frameworks.Framework
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.compiler.model.Project

/**
 * نتيجة عملية التكامل
 */
data class IntegrationResult(
    /**
     * ما إذا كانت العملية ناجحة
     */
    val success: Boolean,
    
    /**
     * رسالة توضح نتيجة العملية
     */
    val message: String,
    
    /**
     * البيانات الناتجة عن العملية
     */
    val data: Any?
)

/**
 * بيانات التكامل
 */
data class IntegrationData(
    /**
     * المشروع
     */
    val project: Project,
    
    /**
     * نوع إطار العمل
     */
    val frameworkType: FrameworkType,
    
    /**
     * إطار العمل
     */
    val framework: Framework,
    
    /**
     * بيانات المحرر
     */
    val editorData: Any? = null,
    
    /**
     * بيانات المصمم
     */
    val designerData: Any? = null,
    
    /**
     * بيانات المترجم
     */
    val compilerData: Any? = null,
    
    /**
     * بيانات المصحح
     */
    val debuggerData: Any? = null,
    
    /**
     * نتيجة التحليل
     */
    val analysisResult: AnalysisResult? = null
)