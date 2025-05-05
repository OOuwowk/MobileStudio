package com.mobileide.aide.frameworks

import java.io.File

/**
 * حالة تثبيت إطار العمل
 */
enum class InstallationStatus {
    /**
     * إطار العمل غير مثبت
     */
    NOT_INSTALLED,
    
    /**
     * إطار العمل مثبت جزئيًا
     */
    PARTIALLY_INSTALLED,
    
    /**
     * إطار العمل مثبت بالكامل
     */
    INSTALLED,
    
    /**
     * إطار العمل يحتاج إلى تحديث
     */
    NEEDS_UPDATE
}

/**
 * نتيجة تثبيت إطار العمل
 */
data class InstallationResult(
    /**
     * ما إذا كان التثبيت ناجحًا
     */
    val success: Boolean,
    
    /**
     * رسالة توضح نتيجة التثبيت
     */
    val message: String,
    
    /**
     * الإصدار المثبت (إذا كان التثبيت ناجحًا)
     */
    val version: String? = null
)

/**
 * نتيجة بناء المشروع
 */
data class BuildResult(
    /**
     * ما إذا كان البناء ناجحًا
     */
    val success: Boolean,
    
    /**
     * ملف APK الناتج (إذا كان البناء ناجحًا)
     */
    val outputFile: File? = null,
    
    /**
     * رسالة توضح نتيجة البناء
     */
    val message: String = if (success) "تم بناء المشروع بنجاح" else "فشل بناء المشروع"
)

/**
 * نتيجة تشغيل المشروع
 */
data class RunResult(
    /**
     * ما إذا كان التشغيل ناجحًا
     */
    val success: Boolean,
    
    /**
     * رسالة توضح نتيجة التشغيل
     */
    val message: String = if (success) "تم تشغيل المشروع بنجاح" else "فشل تشغيل المشروع"
)

/**
 * نتيجة تصحيح المشروع
 */
data class DebugResult(
    /**
     * ما إذا كان بدء التصحيح ناجحًا
     */
    val success: Boolean,
    
    /**
     * رسالة توضح نتيجة بدء التصحيح
     */
    val message: String = if (success) "تم بدء تصحيح المشروع بنجاح" else "فشل بدء تصحيح المشروع"
)

/**
 * نتيجة تنفيذ أمر
 */
data class CommandResult(
    /**
     * ما إذا كان تنفيذ الأمر ناجحًا
     */
    val success: Boolean,
    
    /**
     * إخراج الأمر
     */
    val output: String,
    
    /**
     * رمز الخروج للأمر
     */
    val exitCode: Int
)

/**
 * معلومات التحديث
 */
data class UpdateInfo(
    /**
     * ما إذا كان هناك تحديث متاح
     */
    val updateAvailable: Boolean,
    
    /**
     * الإصدار الحالي
     */
    val currentVersion: String?,
    
    /**
     * أحدث إصدار متاح
     */
    val latestVersion: String?,
    
    /**
     * رابط التحديث
     */
    val updateUrl: String? = null
)

/**
 * قالب مشروع
 */
data class ProjectTemplate(
    /**
     * معرف القالب
     */
    val id: String,
    
    /**
     * اسم القالب
     */
    val name: String,
    
    /**
     * وصف القالب
     */
    val description: String,
    
    /**
     * نوع إطار العمل
     */
    val frameworkType: FrameworkType,
    
    /**
     * مسار القالب
     */
    val path: String,
    
    /**
     * صورة مصغرة للقالب
     */
    val thumbnailPath: String? = null,
    
    /**
     * الإصدار الأدنى المدعوم من إطار العمل
     */
    val minFrameworkVersion: String? = null,
    
    /**
     * الإصدار الأدنى المدعوم من Android SDK
     */
    val minSdkVersion: Int = 21,
    
    /**
     * الإصدار المستهدف من Android SDK
     */
    val targetSdkVersion: Int = 33,
    
    /**
     * اللغات المدعومة في القالب
     */
    val supportedLanguages: List<String> = listOf()
)