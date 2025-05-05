package com.mobileide.aide.collaboration

import android.content.Context
import android.util.Log
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة التطوير التعاوني - مسؤولة عن إدارة جلسات التعاون
 */
@Singleton
class CollaborationService @Inject constructor(
    private val context: Context,
    private val webSocketManager: WebSocketManager
) {
    companion object {
        private const val TAG = "CollaborationService"
    }
    
    // حالة الاتصال
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    // المستخدمون المتصلون
    private val _connectedUsers = MutableStateFlow<List<CollaborationUser>>(emptyList())
    val connectedUsers: StateFlow<List<CollaborationUser>> = _connectedUsers
    
    // التغييرات المعلقة
    private val _pendingChanges = MutableStateFlow<List<CollaborationChange>>(emptyList())
    val pendingChanges: StateFlow<List<CollaborationChange>> = _pendingChanges
    
    // المشروع الحالي
    private var currentProject: Project? = null
    
    // معرف الجلسة
    private var sessionId: String? = null
    
    // وظيفة مزامنة التغييرات
    private var syncJob: Job? = null
    
    init {
        // إعداد مستمع WebSocket
        setupWebSocketListener()
    }
    
    /**
     * إنشاء جلسة تعاون جديدة
     */
    suspend fun createSession(project: Project, username: String): CollaborationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating collaboration session for project: ${project.name}")
            
            // إنشاء معرف جلسة جديد
            val newSessionId = generateSessionId(project)
            
            // الاتصال بخادم WebSocket
            val connectResult = webSocketManager.connect(
                serverUrl = "wss://collaboration.mobileide.com/session/$newSessionId",
                headers = mapOf(
                    "X-Username" to username,
                    "X-Project-Name" to project.name,
                    "X-Project-Id" to project.id.toString()
                )
            )
            
            if (!connectResult) {
                return@withContext CollaborationResult(
                    success = false,
                    message = "فشل الاتصال بخادم التعاون",
                    sessionId = null
                )
            }
            
            // تحديث الحالة
            currentProject = project
            sessionId = newSessionId
            _connectionState.value = ConnectionState.Connected(newSessionId)
            
            // بدء مزامنة التغييرات
            startSyncJob()
            
            return@withContext CollaborationResult(
                success = true,
                message = "تم إنشاء جلسة التعاون بنجاح",
                sessionId = newSessionId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating collaboration session", e)
            return@withContext CollaborationResult(
                success = false,
                message = "فشل إنشاء جلسة التعاون: ${e.message}",
                sessionId = null
            )
        }
    }
    
    /**
     * الانضمام إلى جلسة تعاون موجودة
     */
    suspend fun joinSession(sessionId: String, username: String): CollaborationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Joining collaboration session: $sessionId")
            
            // الاتصال بخادم WebSocket
            val connectResult = webSocketManager.connect(
                serverUrl = "wss://collaboration.mobileide.com/session/$sessionId",
                headers = mapOf(
                    "X-Username" to username,
                    "X-Join" to "true"
                )
            )
            
            if (!connectResult) {
                return@withContext CollaborationResult(
                    success = false,
                    message = "فشل الاتصال بخادم التعاون",
                    sessionId = null
                )
            }
            
            // تحديث الحالة
            this@CollaborationService.sessionId = sessionId
            _connectionState.value = ConnectionState.Connected(sessionId)
            
            // طلب معلومات المشروع
            webSocketManager.sendMessage(CollaborationMessage(
                type = MessageType.PROJECT_INFO_REQUEST,
                data = null,
                sender = username,
                timestamp = System.currentTimeMillis()
            ))
            
            // بدء مزامنة التغييرات
            startSyncJob()
            
            return@withContext CollaborationResult(
                success = true,
                message = "تم الانضمام إلى جلسة التعاون بنجاح",
                sessionId = sessionId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error joining collaboration session", e)
            return@withContext CollaborationResult(
                success = false,
                message = "فشل الانضمام إلى جلسة التعاون: ${e.message}",
                sessionId = null
            )
        }
    }
    
    /**
     * مغادرة جلسة التعاون
     */
    suspend fun leaveSession(): CollaborationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Leaving collaboration session")
            
            // إيقاف مزامنة التغييرات
            stopSyncJob()
            
            // قطع الاتصال بخادم WebSocket
            webSocketManager.disconnect()
            
            // تحديث الحالة
            sessionId = null
            currentProject = null
            _connectionState.value = ConnectionState.Disconnected
            _connectedUsers.value = emptyList()
            _pendingChanges.value = emptyList()
            
            return@withContext CollaborationResult(
                success = true,
                message = "تم مغادرة جلسة التعاون بنجاح",
                sessionId = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving collaboration session", e)
            return@withContext CollaborationResult(
                success = false,
                message = "فشل مغادرة جلسة التعاون: ${e.message}",
                sessionId = null
            )
        }
    }
    
    /**
     * إرسال تغيير إلى المتعاونين
     */
    suspend fun sendChange(change: CollaborationChange): CollaborationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending change: ${change.type} for file: ${change.filePath}")
            
            // التحقق من الاتصال
            if (_connectionState.value !is ConnectionState.Connected) {
                return@withContext CollaborationResult(
                    success = false,
                    message = "غير متصل بجلسة تعاون",
                    sessionId = null
                )
            }
            
            // إرسال التغيير عبر WebSocket
            val message = CollaborationMessage(
                type = MessageType.FILE_CHANGE,
                data = change,
                sender = change.author,
                timestamp = System.currentTimeMillis()
            )
            
            val sendResult = webSocketManager.sendMessage(message)
            
            return@withContext if (sendResult) {
                CollaborationResult(
                    success = true,
                    message = "تم إرسال التغيير بنجاح",
                    sessionId = sessionId
                )
            } else {
                // إضافة التغيير إلى قائمة التغييرات المعلقة
                val updatedChanges = _pendingChanges.value.toMutableList()
                updatedChanges.add(change)
                _pendingChanges.value = updatedChanges
                
                CollaborationResult(
                    success = false,
                    message = "فشل إرسال التغيير، تمت إضافته إلى قائمة التغييرات المعلقة",
                    sessionId = sessionId
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending change", e)
            return@withContext CollaborationResult(
                success = false,
                message = "فشل إرسال التغيير: ${e.message}",
                sessionId = sessionId
            )
        }
    }
    
    /**
     * إرسال رسالة دردشة إلى المتعاونين
     */
    suspend fun sendChatMessage(message: String, username: String): CollaborationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending chat message from $username: $message")
            
            // التحقق من الاتصال
            if (_connectionState.value !is ConnectionState.Connected) {
                return@withContext CollaborationResult(
                    success = false,
                    message = "غير متصل بجلسة تعاون",
                    sessionId = null
                )
            }
            
            // إرسال رسالة الدردشة عبر WebSocket
            val chatMessage = CollaborationMessage(
                type = MessageType.CHAT_MESSAGE,
                data = ChatMessage(message, username),
                sender = username,
                timestamp = System.currentTimeMillis()
            )
            
            val sendResult = webSocketManager.sendMessage(chatMessage)
            
            return@withContext if (sendResult) {
                CollaborationResult(
                    success = true,
                    message = "تم إرسال رسالة الدردشة بنجاح",
                    sessionId = sessionId
                )
            } else {
                CollaborationResult(
                    success = false,
                    message = "فشل إرسال رسالة الدردشة",
                    sessionId = sessionId
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chat message", e)
            return@withContext CollaborationResult(
                success = false,
                message = "فشل إرسال رسالة الدردشة: ${e.message}",
                sessionId = sessionId
            )
        }
    }
    
    /**
     * إعداد مستمع WebSocket
     */
    private fun setupWebSocketListener() {
        webSocketManager.setMessageListener { message ->
            CoroutineScope(Dispatchers.Main).launch {
                handleWebSocketMessage(message)
            }
        }
        
        webSocketManager.setConnectionListener { connected ->
            CoroutineScope(Dispatchers.Main).launch {
                if (connected) {
                    if (sessionId != null) {
                        _connectionState.value = ConnectionState.Connected(sessionId!!)
                    }
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }
    
    /**
     * معالجة رسالة WebSocket
     */
    private suspend fun handleWebSocketMessage(message: CollaborationMessage) {
        when (message.type) {
            MessageType.USER_JOINED -> {
                // إضافة المستخدم إلى قائمة المستخدمين المتصلين
                val user = message.data as CollaborationUser
                val updatedUsers = _connectedUsers.value.toMutableList()
                updatedUsers.add(user)
                _connectedUsers.value = updatedUsers
                
                Log.d(TAG, "User joined: ${user.username}")
            }
            
            MessageType.USER_LEFT -> {
                // إزالة المستخدم من قائمة المستخدمين المتصلين
                val username = message.data as String
                val updatedUsers = _connectedUsers.value.filter { it.username != username }
                _connectedUsers.value = updatedUsers
                
                Log.d(TAG, "User left: $username")
            }
            
            MessageType.FILE_CHANGE -> {
                // معالجة تغيير الملف
                val change = message.data as CollaborationChange
                handleFileChange(change)
                
                Log.d(TAG, "Received file change: ${change.type} for file: ${change.filePath}")
            }
            
            MessageType.PROJECT_INFO -> {
                // معالجة معلومات المشروع
                val projectInfo = message.data as ProjectInfo
                handleProjectInfo(projectInfo)
                
                Log.d(TAG, "Received project info: ${projectInfo.projectName}")
            }
            
            MessageType.PROJECT_INFO_REQUEST -> {
                // إرسال معلومات المشروع
                currentProject?.let { project ->
                    val projectInfo = ProjectInfo(
                        projectId = project.id,
                        projectName = project.name,
                        projectPath = project.path,
                        files = getProjectFiles(project)
                    )
                    
                    webSocketManager.sendMessage(CollaborationMessage(
                        type = MessageType.PROJECT_INFO,
                        data = projectInfo,
                        sender = "system",
                        timestamp = System.currentTimeMillis()
                    ))
                    
                    Log.d(TAG, "Sent project info for: ${project.name}")
                }
            }
            
            MessageType.CHAT_MESSAGE -> {
                // معالجة رسالة الدردشة
                val chatMessage = message.data as ChatMessage
                
                Log.d(TAG, "Received chat message from ${chatMessage.sender}: ${chatMessage.message}")
            }
            
            else -> {
                Log.d(TAG, "Received unknown message type: ${message.type}")
            }
        }
    }
    
    /**
     * معالجة تغيير الملف
     */
    private suspend fun handleFileChange(change: CollaborationChange) = withContext(Dispatchers.IO) {
        try {
            currentProject?.let { project ->
                val file = File(project.path, change.filePath)
                
                when (change.type) {
                    ChangeType.CREATE -> {
                        // إنشاء الملف
                        file.parentFile?.mkdirs()
                        file.writeText(change.content ?: "")
                    }
                    
                    ChangeType.MODIFY -> {
                        // تعديل الملف
                        if (file.exists() && change.content != null) {
                            file.writeText(change.content)
                        }
                    }
                    
                    ChangeType.DELETE -> {
                        // حذف الملف
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    
                    ChangeType.RENAME -> {
                        // إعادة تسمية الملف
                        if (file.exists() && change.newPath != null) {
                            val newFile = File(project.path, change.newPath)
                            newFile.parentFile?.mkdirs()
                            file.renameTo(newFile)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling file change", e)
        }
    }
    
    /**
     * معالجة معلومات المشروع
     */
    private suspend fun handleProjectInfo(projectInfo: ProjectInfo) = withContext(Dispatchers.IO) {
        try {
            // إنشاء مشروع محلي
            val projectDir = File(context.filesDir, "collaboration/${projectInfo.projectId}")
            projectDir.mkdirs()
            
            val project = Project(
                id = projectInfo.projectId,
                name = projectInfo.projectName,
                packageName = "com.collaboration.${projectInfo.projectName.toLowerCase().replace(" ", "")}",
                path = projectDir.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            
            // إنشاء ملفات المشروع
            projectInfo.files.forEach { fileInfo ->
                val file = File(projectDir, fileInfo.path)
                file.parentFile?.mkdirs()
                file.writeText(fileInfo.content)
            }
            
            // تحديث المشروع الحالي
            currentProject = project
            
            Log.d(TAG, "Created local project from project info: ${project.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling project info", e)
        }
    }
    
    /**
     * الحصول على ملفات المشروع
     */
    private fun getProjectFiles(project: Project): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val projectDir = File(project.path)
        
        fun scanDirectory(dir: File, relativePath: String = "") {
            dir.listFiles()?.forEach { file ->
                val path = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
                
                if (file.isDirectory) {
                    scanDirectory(file, path)
                } else {
                    val content = file.readText()
                    files.add(FileInfo(path, content))
                }
            }
        }
        
        scanDirectory(projectDir)
        return files
    }
    
    /**
     * بدء مزامنة التغييرات
     */
    private fun startSyncJob() {
        syncJob?.cancel()
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    // مزامنة التغييرات المعلقة
                    val pendingChanges = _pendingChanges.value
                    if (pendingChanges.isNotEmpty()) {
                        val change = pendingChanges.first()
                        
                        // محاولة إرسال التغيير
                        val message = CollaborationMessage(
                            type = MessageType.FILE_CHANGE,
                            data = change,
                            sender = change.author,
                            timestamp = System.currentTimeMillis()
                        )
                        
                        val sendResult = webSocketManager.sendMessage(message)
                        
                        if (sendResult) {
                            // إزالة التغيير من قائمة التغييرات المعلقة
                            val updatedChanges = _pendingChanges.value.toMutableList()
                            updatedChanges.removeAt(0)
                            _pendingChanges.value = updatedChanges
                            
                            Log.d(TAG, "Successfully synced pending change: ${change.type} for file: ${change.filePath}")
                        }
                    }
                    
                    // انتظار قبل المحاولة التالية
                    kotlinx.coroutines.delay(5000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in sync job", e)
                    kotlinx.coroutines.delay(10000)
                }
            }
        }
    }
    
    /**
     * إيقاف مزامنة التغييرات
     */
    private fun stopSyncJob() {
        syncJob?.cancel()
        syncJob = null
    }
    
    /**
     * إنشاء معرف جلسة
     */
    private fun generateSessionId(project: Project): String {
        val timestamp = System.currentTimeMillis()
        val random = (0..9999).random()
        return "session_${project.id}_${timestamp}_$random"
    }
}

/**
 * نتيجة عملية التعاون
 */
data class CollaborationResult(
    val success: Boolean,
    val message: String,
    val sessionId: String?
)

/**
 * حالة الاتصال
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connected(val sessionId: String) : ConnectionState()
    object Connecting : ConnectionState()
}