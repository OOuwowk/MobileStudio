package com.mobileide.aide.collaboration

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير WebSocket - مسؤول عن إدارة اتصالات WebSocket
 */
@Singleton
class WebSocketManager @Inject constructor() {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_DELAY = 5000L // 5 ثوانٍ
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var serverUrl: String? = null
    private var headers: Map<String, String>? = null
    private var messageListener: ((CollaborationMessage) -> Unit)? = null
    private var connectionListener: ((Boolean) -> Unit)? = null
    
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var isConnected = false
    
    /**
     * الاتصال بخادم WebSocket
     */
    fun connect(serverUrl: String, headers: Map<String, String>): Boolean {
        try {
            Log.d(TAG, "Connecting to WebSocket server: $serverUrl")
            
            this.serverUrl = serverUrl
            this.headers = headers
            
            val request = buildRequest(serverUrl, headers)
            webSocket = client.newWebSocket(request, createWebSocketListener())
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to WebSocket server", e)
            return false
        }
    }
    
    /**
     * قطع الاتصال بخادم WebSocket
     */
    fun disconnect() {
        try {
            Log.d(TAG, "Disconnecting from WebSocket server")
            
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectAttempts = 0
            
            webSocket?.close(1000, "Disconnected by user")
            webSocket = null
            
            isConnected = false
            connectionListener?.invoke(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from WebSocket server", e)
        }
    }
    
    /**
     * إرسال رسالة عبر WebSocket
     */
    fun sendMessage(message: CollaborationMessage): Boolean {
        try {
            if (webSocket == null || !isConnected) {
                Log.w(TAG, "Cannot send message, WebSocket is not connected")
                return false
            }
            
            val json = gson.toJson(message)
            val result = webSocket?.send(json) ?: false
            
            if (!result) {
                Log.w(TAG, "Failed to send message")
            }
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            return false
        }
    }
    
    /**
     * تعيين مستمع الرسائل
     */
    fun setMessageListener(listener: (CollaborationMessage) -> Unit) {
        messageListener = listener
    }
    
    /**
     * تعيين مستمع الاتصال
     */
    fun setConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }
    
    /**
     * إنشاء طلب WebSocket
     */
    private fun buildRequest(serverUrl: String, headers: Map<String, String>): Request {
        val requestBuilder = Request.Builder()
            .url(serverUrl)
        
        // إضافة الرؤوس
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * إنشاء مستمع WebSocket
     */
    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened")
                isConnected = true
                reconnectAttempts = 0
                connectionListener?.invoke(true)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, CollaborationMessage::class.java)
                    messageListener?.invoke(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket connection closing: $code - $reason")
                webSocket.close(code, reason)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket connection closed: $code - $reason")
                isConnected = false
                connectionListener?.invoke(false)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failure", t)
                isConnected = false
                connectionListener?.invoke(false)
                
                // محاولة إعادة الاتصال
                attemptReconnect()
            }
        }
    }
    
    /**
     * محاولة إعادة الاتصال
     */
    private fun attemptReconnect() {
        if (reconnectJob != null || serverUrl == null || headers == null) {
            return
        }
        
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts++
                
                Log.d(TAG, "Attempting to reconnect (attempt $reconnectAttempts of $MAX_RECONNECT_ATTEMPTS)")
                
                // انتظار قبل إعادة الاتصال
                delay(RECONNECT_DELAY)
                
                // محاولة إعادة الاتصال
                val request = buildRequest(serverUrl!!, headers!!)
                webSocket = client.newWebSocket(request, createWebSocketListener())
            } else {
                Log.w(TAG, "Max reconnect attempts reached")
            }
            
            reconnectJob = null
        }
    }
}