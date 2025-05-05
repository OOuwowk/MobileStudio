package com.mobileide.aide.collaboration

/**
 * رسالة التعاون
 */
data class CollaborationMessage(
    /**
     * نوع الرسالة
     */
    val type: MessageType,
    
    /**
     * بيانات الرسالة
     */
    val data: Any?,
    
    /**
     * مرسل الرسالة
     */
    val sender: String,
    
    /**
     * الطابع الزمني للرسالة
     */
    val timestamp: Long
)

/**
 * نوع الرسالة
 */
enum class MessageType {
    /**
     * انضمام مستخدم
     */
    USER_JOINED,
    
    /**
     * مغادرة مستخدم
     */
    USER_LEFT,
    
    /**
     * تغيير ملف
     */
    FILE_CHANGE,
    
    /**
     * معلومات المشروع
     */
    PROJECT_INFO,
    
    /**
     * طلب معلومات المشروع
     */
    PROJECT_INFO_REQUEST,
    
    /**
     * رسالة دردشة
     */
    CHAT_MESSAGE
}

/**
 * مستخدم التعاون
 */
data class CollaborationUser(
    /**
     * اسم المستخدم
     */
    val username: String,
    
    /**
     * معرف المستخدم
     */
    val userId: String,
    
    /**
     * الصورة الرمزية للمستخدم
     */
    val avatar: String?,
    
    /**
     * حالة المستخدم
     */
    val status: UserStatus
)

/**
 * حالة المستخدم
 */
enum class UserStatus {
    /**
     * متصل
     */
    ONLINE,
    
    /**
     * غير نشط
     */
    IDLE,
    
    /**
     * مشغول
     */
    BUSY,
    
    /**
     * غير متصل
     */
    OFFLINE
}

/**
 * تغيير التعاون
 */
data class CollaborationChange(
    /**
     * نوع التغيير
     */
    val type: ChangeType,
    
    /**
     * مسار الملف
     */
    val filePath: String,
    
    /**
     * محتوى الملف
     */
    val content: String?,
    
    /**
     * المسار الجديد (للتغييرات من نوع RENAME)
     */
    val newPath: String?,
    
    /**
     * مؤلف التغيير
     */
    val author: String,
    
    /**
     * الطابع الزمني للتغيير
     */
    val timestamp: Long
)

/**
 * نوع التغيير
 */
enum class ChangeType {
    /**
     * إنشاء
     */
    CREATE,
    
    /**
     * تعديل
     */
    MODIFY,
    
    /**
     * حذف
     */
    DELETE,
    
    /**
     * إعادة تسمية
     */
    RENAME
}

/**
 * معلومات المشروع
 */
data class ProjectInfo(
    /**
     * معرف المشروع
     */
    val projectId: Long,
    
    /**
     * اسم المشروع
     */
    val projectName: String,
    
    /**
     * مسار المشروع
     */
    val projectPath: String,
    
    /**
     * ملفات المشروع
     */
    val files: List<FileInfo>
)

/**
 * معلومات الملف
 */
data class FileInfo(
    /**
     * مسار الملف
     */
    val path: String,
    
    /**
     * محتوى الملف
     */
    val content: String
)

/**
 * رسالة الدردشة
 */
data class ChatMessage(
    /**
     * نص الرسالة
     */
    val message: String,
    
    /**
     * مرسل الرسالة
     */
    val sender: String
)