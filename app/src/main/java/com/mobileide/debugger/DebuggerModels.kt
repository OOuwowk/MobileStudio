package com.mobileide.debugger

/**
 * نتيجة عملية التصحيح
 */
data class DebugResult(
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
 * نتيجة تقييم تعبير
 */
data class EvaluationResult(
    /**
     * ما إذا كان التقييم ناجحًا
     */
    val success: Boolean,
    
    /**
     * رسالة الخطأ (إذا فشل التقييم)
     */
    val error: String?,
    
    /**
     * نوع النتيجة
     */
    val type: String?,
    
    /**
     * قيمة النتيجة
     */
    val value: String?
)

/**
 * متغير في جلسة التصحيح
 */
data class Variable(
    /**
     * اسم المتغير
     */
    val name: String,
    
    /**
     * نوع المتغير
     */
    val type: String,
    
    /**
     * قيمة المتغير
     */
    val value: String
)

/**
 * نتيجة الحصول على المتغيرات
 */
data class VariablesResult(
    /**
     * ما إذا كانت العملية ناجحة
     */
    val success: Boolean,
    
    /**
     * رسالة الخطأ (إذا فشلت العملية)
     */
    val error: String?,
    
    /**
     * قائمة المتغيرات
     */
    val variables: List<Variable>
)

/**
 * إطار في مكدس الاستدعاء
 */
data class StackFrame(
    /**
     * اسم الدالة
     */
    val method: String,
    
    /**
     * اسم الملف
     */
    val file: String,
    
    /**
     * رقم السطر
     */
    val line: Int
)

/**
 * نتيجة الحصول على مكدس الاستدعاء
 */
data class CallStackResult(
    /**
     * ما إذا كانت العملية ناجحة
     */
    val success: Boolean,
    
    /**
     * رسالة الخطأ (إذا فشلت العملية)
     */
    val error: String?,
    
    /**
     * قائمة إطارات مكدس الاستدعاء
     */
    val frames: List<StackFrame>
)