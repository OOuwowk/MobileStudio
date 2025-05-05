package com.mobileide.aide.frameworks

/**
 * أنواع أطر العمل المدعومة في التطبيق
 */
enum class FrameworkType {
    /**
     * تطوير أندرويد الأصلي (Java/Kotlin)
     */
    ANDROID_NATIVE,
    
    /**
     * إطار عمل Flutter
     */
    FLUTTER,
    
    /**
     * إطار عمل React Native
     */
    REACT_NATIVE,
    
    /**
     * إطار عمل Kotlin Multiplatform
     */
    KOTLIN_MULTIPLATFORM;
    
    /**
     * الحصول على اسم العرض لإطار العمل
     */
    fun getDisplayName(): String {
        return when (this) {
            ANDROID_NATIVE -> "Android Native (Java/Kotlin)"
            FLUTTER -> "Flutter"
            REACT_NATIVE -> "React Native"
            KOTLIN_MULTIPLATFORM -> "Kotlin Multiplatform"
        }
    }
    
    /**
     * الحصول على وصف إطار العمل
     */
    fun getDescription(): String {
        return when (this) {
            ANDROID_NATIVE -> "تطوير تطبيقات أندرويد الأصلية باستخدام Java أو Kotlin"
            FLUTTER -> "إطار عمل متعدد المنصات لبناء تطبيقات أصلية لأندرويد و iOS"
            REACT_NATIVE -> "إطار عمل متعدد المنصات لبناء تطبيقات أصلية باستخدام JavaScript"
            KOTLIN_MULTIPLATFORM -> "إطار عمل متعدد المنصات لمشاركة الكود بين أندرويد و iOS وويب"
        }
    }
    
    /**
     * الحصول على رابط الموقع الرسمي لإطار العمل
     */
    fun getWebsiteUrl(): String {
        return when (this) {
            ANDROID_NATIVE -> "https://developer.android.com"
            FLUTTER -> "https://flutter.dev"
            REACT_NATIVE -> "https://reactnative.dev"
            KOTLIN_MULTIPLATFORM -> "https://kotlinlang.org/docs/multiplatform.html"
        }
    }
    
    /**
     * الحصول على رابط التوثيق الرسمي لإطار العمل
     */
    fun getDocumentationUrl(): String {
        return when (this) {
            ANDROID_NATIVE -> "https://developer.android.com/docs"
            FLUTTER -> "https://docs.flutter.dev"
            REACT_NATIVE -> "https://reactnative.dev/docs/getting-started"
            KOTLIN_MULTIPLATFORM -> "https://kotlinlang.org/docs/multiplatform-get-started.html"
        }
    }
    
    /**
     * الحصول على امتدادات الملفات المرتبطة بإطار العمل
     */
    fun getFileExtensions(): List<String> {
        return when (this) {
            ANDROID_NATIVE -> listOf("java", "kt", "xml")
            FLUTTER -> listOf("dart", "yaml")
            REACT_NATIVE -> listOf("js", "jsx", "ts", "tsx", "json")
            KOTLIN_MULTIPLATFORM -> listOf("kt", "kts", "gradle")
        }
    }
    
    /**
     * الحصول على اللغات المستخدمة في إطار العمل
     */
    fun getLanguages(): List<String> {
        return when (this) {
            ANDROID_NATIVE -> listOf("Java", "Kotlin")
            FLUTTER -> listOf("Dart")
            REACT_NATIVE -> listOf("JavaScript", "TypeScript")
            KOTLIN_MULTIPLATFORM -> listOf("Kotlin")
        }
    }
    
    companion object {
        /**
         * الحصول على نوع إطار العمل من اسمه
         */
        fun fromString(name: String): FrameworkType? {
            return values().find { it.name.equals(name, ignoreCase = true) }
        }
    }
}