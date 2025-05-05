package com.mobileide.aide.git.github

/**
 * نتيجة عملية GitHub
 */
data class GitHubResult<T>(
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
    val data: T?
)

/**
 * مستخدم GitHub
 */
data class GitHubUser(
    /**
     * معرف المستخدم
     */
    val id: Int,
    
    /**
     * اسم المستخدم
     */
    val login: String,
    
    /**
     * الاسم الكامل
     */
    val name: String,
    
    /**
     * رابط الصورة الرمزية
     */
    val avatarUrl: String,
    
    /**
     * البريد الإلكتروني
     */
    val email: String
)

/**
 * مستودع GitHub
 */
data class GitHubRepository(
    /**
     * معرف المستودع
     */
    val id: Int,
    
    /**
     * اسم المستودع
     */
    val name: String,
    
    /**
     * الاسم الكامل للمستودع (المالك/الاسم)
     */
    val fullName: String,
    
    /**
     * وصف المستودع
     */
    val description: String,
    
    /**
     * رابط المستودع
     */
    val url: String,
    
    /**
     * رابط استنساخ المستودع
     */
    val cloneUrl: String,
    
    /**
     * ما إذا كان المستودع خاصًا
     */
    val private: Boolean,
    
    /**
     * ما إذا كان المستودع مستنسخًا
     */
    val fork: Boolean,
    
    /**
     * مالك المستودع
     */
    val owner: String
)

/**
 * طلب سحب GitHub
 */
data class GitHubPullRequest(
    /**
     * معرف طلب السحب
     */
    val id: Int,
    
    /**
     * رقم طلب السحب
     */
    val number: Int,
    
    /**
     * عنوان طلب السحب
     */
    val title: String,
    
    /**
     * وصف طلب السحب
     */
    val body: String,
    
    /**
     * حالة طلب السحب
     */
    val state: String,
    
    /**
     * رابط طلب السحب
     */
    val url: String,
    
    /**
     * تاريخ إنشاء طلب السحب
     */
    val createdAt: String
)

/**
 * فرع GitHub
 */
data class GitHubBranch(
    /**
     * اسم الفرع
     */
    val name: String,
    
    /**
     * ما إذا كان الفرع محميًا
     */
    val protected: Boolean,
    
    /**
     * معرف آخر commit في الفرع
     */
    val commit: String
)