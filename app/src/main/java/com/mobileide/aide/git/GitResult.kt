package com.mobileide.aide.git

/**
 * نتيجة عملية Git
 */
data class GitResult(
    /**
     * ما إذا كانت العملية ناجحة
     */
    val success: Boolean,
    
    /**
     * رسالة توضح نتيجة العملية
     */
    val message: String,
    
    /**
     * إخراج العملية
     */
    val output: String
)