package com.mobileide.debugger

import android.content.Context
import android.util.Log
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة المصحح المسؤولة عن تصحيح المشاريع
 */
@Singleton
class DebuggerService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DebuggerService"
        private const val DEBUG_PORT = 8700
    }
    
    private var debugSession: DebugSession? = null
    
    /**
     * تهيئة المصحح لمشروع معين
     */
    suspend fun initializeForProject(project: Project, frameworkType: FrameworkType): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing debugger for project: ${project.name} with framework: $frameworkType")
            
            // تهيئة المصحح حسب نوع إطار العمل
            when (frameworkType) {
                FrameworkType.ANDROID_NATIVE -> initializeAndroidDebugger(project)
                FrameworkType.FLUTTER -> initializeFlutterDebugger(project)
                FrameworkType.REACT_NATIVE -> initializeReactNativeDebugger(project)
                FrameworkType.KOTLIN_MULTIPLATFORM -> initializeKMPDebugger(project)
            }
            
            return@withContext DebugResult(
                success = true,
                message = "تم تهيئة المصحح بنجاح",
                data = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing debugger", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل تهيئة المصحح: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * بدء جلسة تصحيح
     */
    suspend fun startDebugging(project: Project, apkFile: File): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting debugging for project: ${project.name}")
            
            // 1. تثبيت التطبيق على الجهاز
            val installResult = installApp(apkFile)
            if (!installResult) {
                return@withContext DebugResult(
                    success = false,
                    message = "فشل تثبيت التطبيق",
                    data = null
                )
            }
            
            // 2. بدء التطبيق في وضع التصحيح
            val packageName = getPackageNameFromApk(apkFile)
            val launchResult = launchAppForDebugging(packageName)
            if (launchResult == -1) {
                return@withContext DebugResult(
                    success = false,
                    message = "فشل بدء التطبيق للتصحيح",
                    data = null
                )
            }
            
            // 3. الاتصال بالتطبيق للتصحيح
            val session = connectToDebugger(packageName, launchResult)
            if (session == null) {
                return@withContext DebugResult(
                    success = false,
                    message = "فشل الاتصال بالمصحح",
                    data = null
                )
            }
            
            debugSession = session
            
            return@withContext DebugResult(
                success = true,
                message = "تم بدء التصحيح بنجاح",
                data = session
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting debugging", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل بدء التصحيح: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * إيقاف جلسة التصحيح
     */
    suspend fun stopDebugging(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping debugging")
            
            debugSession?.let {
                it.disconnect()
                debugSession = null
                
                return@withContext DebugResult(
                    success = true,
                    message = "تم إيقاف التصحيح بنجاح",
                    data = null
                )
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping debugging", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل إيقاف التصحيح: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * وضع نقطة توقف
     */
    suspend fun setBreakpoint(file: String, line: Int): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Setting breakpoint at $file:$line")
            
            debugSession?.let {
                val result = it.setBreakpoint(file, line)
                
                return@withContext if (result) {
                    DebugResult(
                        success = true,
                        message = "تم وضع نقطة التوقف بنجاح",
                        data = null
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل وضع نقطة التوقف",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting breakpoint", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل وضع نقطة التوقف: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * إزالة نقطة توقف
     */
    suspend fun removeBreakpoint(file: String, line: Int): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Removing breakpoint at $file:$line")
            
            debugSession?.let {
                val result = it.removeBreakpoint(file, line)
                
                return@withContext if (result) {
                    DebugResult(
                        success = true,
                        message = "تم إزالة نقطة التوقف بنجاح",
                        data = null
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل إزالة نقطة التوقف",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing breakpoint", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل إزالة نقطة التوقف: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * استئناف التنفيذ
     */
    suspend fun resume(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Resuming execution")
            
            debugSession?.let {
                val result = it.resume()
                
                return@withContext if (result) {
                    DebugResult(
                        success = true,
                        message = "تم استئناف التنفيذ بنجاح",
                        data = null
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل استئناف التنفيذ",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming execution", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل استئناف التنفيذ: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تنفيذ خطوة واحدة
     */
    suspend fun stepOver(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stepping over")
            
            debugSession?.let {
                val result = it.stepOver()
                
                return@withContext if (result) {
                    DebugResult(
                        success = true,
                        message = "تم تنفيذ خطوة واحدة بنجاح",
                        data = null
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل تنفيذ خطوة واحدة",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stepping over", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل تنفيذ خطوة واحدة: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الدخول إلى دالة
     */
    suspend fun stepInto(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stepping into")
            
            debugSession?.let {
                val result = it.stepInto()
                
                return@withContext if (result) {
                    DebugResult(
                        success = true,
                        message = "تم الدخول إلى الدالة بنجاح",
                        data = null
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل الدخول إلى الدالة",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stepping into", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل الدخول إلى الدالة: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الخروج من دالة
     */
    suspend fun stepOut(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stepping out")
            
            debugSession?.let {
                val result = it.stepOut()
                
                return@withContext if (result) {
                    DebugResult(
                        success = true,
                        message = "تم الخروج من الدالة بنجاح",
                        data = null
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل الخروج من الدالة",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stepping out", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل الخروج من الدالة: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تقييم تعبير
     */
    suspend fun evaluate(expression: String): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Evaluating expression: $expression")
            
            debugSession?.let {
                val result = it.evaluate(expression)
                
                return@withContext if (result.success) {
                    DebugResult(
                        success = true,
                        message = "تم تقييم التعبير بنجاح",
                        data = result
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل تقييم التعبير: ${result.error}",
                        data = result
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating expression", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل تقييم التعبير: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الحصول على قائمة المتغيرات
     */
    suspend fun getVariables(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting variables")
            
            debugSession?.let {
                val result = it.getVariables()
                
                return@withContext if (result.success) {
                    DebugResult(
                        success = true,
                        message = "تم الحصول على المتغيرات بنجاح",
                        data = result.variables
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل الحصول على المتغيرات: ${result.error}",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting variables", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل الحصول على المتغيرات: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الحصول على مكدس الاستدعاء
     */
    suspend fun getCallStack(): DebugResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting call stack")
            
            debugSession?.let {
                val result = it.getCallStack()
                
                return@withContext if (result.success) {
                    DebugResult(
                        success = true,
                        message = "تم الحصول على مكدس الاستدعاء بنجاح",
                        data = result.frames
                    )
                } else {
                    DebugResult(
                        success = false,
                        message = "فشل الحصول على مكدس الاستدعاء: ${result.error}",
                        data = null
                    )
                }
            } ?: run {
                return@withContext DebugResult(
                    success = false,
                    message = "لا توجد جلسة تصحيح نشطة",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting call stack", e)
            return@withContext DebugResult(
                success = false,
                message = "فشل الحصول على مكدس الاستدعاء: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * تهيئة مصحح Android
     */
    private suspend fun initializeAndroidDebugger(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود أدوات التصحيح
            val debugToolsDir = File(context.filesDir, "debug-tools")
            if (!debugToolsDir.exists()) {
                debugToolsDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Android debugger", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مصحح Flutter
     */
    private suspend fun initializeFlutterDebugger(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود أدوات تصحيح Flutter
            val flutterDebugToolsDir = File(context.filesDir, "flutter-debug-tools")
            if (!flutterDebugToolsDir.exists()) {
                flutterDebugToolsDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Flutter debugger", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مصحح React Native
     */
    private suspend fun initializeReactNativeDebugger(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود أدوات تصحيح React Native
            val reactNativeDebugToolsDir = File(context.filesDir, "react-native-debug-tools")
            if (!reactNativeDebugToolsDir.exists()) {
                reactNativeDebugToolsDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing React Native debugger", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مصحح Kotlin Multiplatform
     */
    private suspend fun initializeKMPDebugger(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود أدوات تصحيح Kotlin Multiplatform
            val kmpDebugToolsDir = File(context.filesDir, "kmp-debug-tools")
            if (!kmpDebugToolsDir.exists()) {
                kmpDebugToolsDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing KMP debugger", e)
            return@withContext false
        }
    }
    
    /**
     * تثبيت التطبيق على الجهاز
     */
    private fun installApp(apkFile: File): Boolean {
        try {
            val process = ProcessBuilder(
                "pm",
                "install",
                "-r",
                "-t",
                apkFile.absolutePath
            ).start()
            
            val exitCode = process.waitFor()
            return exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error installing app", e)
            return false
        }
    }
    
    /**
     * بدء التطبيق في وضع التصحيح
     */
    private fun launchAppForDebugging(packageName: String): Int {
        try {
            val process = ProcessBuilder(
                "am",
                "start",
                "-D",
                "-n",
                "$packageName/.MainActivity"
            ).start()
            
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            
            // استخراج PID من الإخراج
            val pidRegex = "PID: (\\d+)".toRegex()
            val pidMatch = pidRegex.find(output)
            val pid = pidMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
            
            return if (exitCode == 0) pid else -1
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app for debugging", e)
            return -1
        }
    }
    
    /**
     * الاتصال بالمصحح
     */
    private fun connectToDebugger(packageName: String, pid: Int): DebugSession? {
        try {
            // استخدام JDWP للاتصال بالتطبيق للتصحيح
            val socket = Socket("localhost", DEBUG_PORT)
            return DebugSession(socket, packageName, pid)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to debugger", e)
            return null
        }
    }
    
    /**
     * استخراج اسم الحزمة من ملف APK
     */
    private fun getPackageNameFromApk(apkFile: File): String {
        try {
            val aapt = "${context.applicationInfo.nativeLibraryDir}/libaapt2.so"
            val process = ProcessBuilder(
                aapt,
                "dump",
                "badging",
                apkFile.absolutePath
            ).start()
            
            val output = process.inputStream.bufferedReader().readText()
            val packageRegex = "package: name='([^']+)'".toRegex()
            val packageMatch = packageRegex.find(output)
            
            return packageMatch?.groupValues?.get(1) ?: throw IllegalStateException("لا يمكن استخراج اسم الحزمة من APK")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting package name from APK", e)
            throw e
        }
    }
}

/**
 * جلسة التصحيح
 */
class DebugSession(
    private val socket: Socket,
    val packageName: String,
    val pid: Int
) {
    private val inputStream = socket.getInputStream()
    private val outputStream = socket.getOutputStream()
    private val breakpoints = mutableMapOf<String, MutableSet<Int>>()
    
    /**
     * قطع الاتصال بالمصحح
     */
    fun disconnect() {
        try {
            socket.close()
        } catch (e: Exception) {
            // تجاهل الأخطاء عند الإغلاق
        }
    }
    
    /**
     * وضع نقطة توقف
     */
    fun setBreakpoint(file: String, line: Int): Boolean {
        val locations = breakpoints.getOrPut(file) { mutableSetOf() }
        locations.add(line)
        return sendBreakpointCommand(file, line, true)
    }
    
    /**
     * إزالة نقطة توقف
     */
    fun removeBreakpoint(file: String, line: Int): Boolean {
        val locations = breakpoints[file] ?: return false
        val removed = locations.remove(line)
        if (removed) {
            return sendBreakpointCommand(file, line, false)
        }
        return false
    }
    
    /**
     * إرسال أمر نقطة توقف
     */
    private fun sendBreakpointCommand(file: String, line: Int, set: Boolean): Boolean {
        try {
            // تنفيذ بروتوكول JDWP لإضافة/إزالة نقاط التوقف
            val command = if (set) "set_breakpoint" else "clear_breakpoint"
            val message = "$command:$file:$line"
            
            outputStream.write(message.toByteArray())
            outputStream.flush()
            
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            val response = String(buffer, 0, bytesRead)
            
            return response == "OK"
        } catch (e: IOException) {
            return false
        }
    }
    
    /**
     * استئناف التنفيذ
     */
    fun resume(): Boolean {
        return sendCommand("resume")
    }
    
    /**
     * تنفيذ خطوة واحدة
     */
    fun stepOver(): Boolean {
        return sendCommand("step_over")
    }
    
    /**
     * الدخول إلى دالة
     */
    fun stepInto(): Boolean {
        return sendCommand("step_into")
    }
    
    /**
     * الخروج من دالة
     */
    fun stepOut(): Boolean {
        return sendCommand("step_out")
    }
    
    /**
     * تقييم تعبير
     */
    fun evaluate(expression: String): EvaluationResult {
        try {
            val command = "evaluate:$expression"
            outputStream.write(command.toByteArray())
            outputStream.flush()
            
            val buffer = ByteArray(4096)
            val bytesRead = inputStream.read(buffer)
            val response = String(buffer, 0, bytesRead)
            
            val parts = response.split(":", limit = 3)
            return if (parts.size >= 3 && parts[0] == "OK") {
                EvaluationResult(true, null, parts[1], parts[2])
            } else {
                EvaluationResult(false, response, null, null)
            }
        } catch (e: IOException) {
            return EvaluationResult(false, e.message, null, null)
        }
    }
    
    /**
     * الحصول على قائمة المتغيرات
     */
    fun getVariables(): VariablesResult {
        try {
            val command = "get_variables"
            outputStream.write(command.toByteArray())
            outputStream.flush()
            
            val buffer = ByteArray(4096)
            val bytesRead = inputStream.read(buffer)
            val response = String(buffer, 0, bytesRead)
            
            val parts = response.split(":", limit = 2)
            return if (parts.size >= 2 && parts[0] == "OK") {
                val variablesJson = parts[1]
                val variables = parseVariables(variablesJson)
                VariablesResult(true, null, variables)
            } else {
                VariablesResult(false, response, emptyList())
            }
        } catch (e: IOException) {
            return VariablesResult(false, e.message, emptyList())
        }
    }
    
    /**
     * الحصول على مكدس الاستدعاء
     */
    fun getCallStack(): CallStackResult {
        try {
            val command = "get_call_stack"
            outputStream.write(command.toByteArray())
            outputStream.flush()
            
            val buffer = ByteArray(4096)
            val bytesRead = inputStream.read(buffer)
            val response = String(buffer, 0, bytesRead)
            
            val parts = response.split(":", limit = 2)
            return if (parts.size >= 2 && parts[0] == "OK") {
                val callStackJson = parts[1]
                val frames = parseCallStack(callStackJson)
                CallStackResult(true, null, frames)
            } else {
                CallStackResult(false, response, emptyList())
            }
        } catch (e: IOException) {
            return CallStackResult(false, e.message, emptyList())
        }
    }
    
    /**
     * إرسال أمر بسيط
     */
    private fun sendCommand(command: String): Boolean {
        try {
            outputStream.write(command.toByteArray())
            outputStream.flush()
            
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            val response = String(buffer, 0, bytesRead)
            
            return response == "OK"
        } catch (e: IOException) {
            return false
        }
    }
    
    /**
     * تحليل المتغيرات من JSON
     */
    private fun parseVariables(json: String): List<Variable> {
        // في التطبيق الحقيقي، يجب تحليل JSON باستخدام مكتبة مناسبة
        // هذا مجرد مثال بسيط
        val variables = mutableListOf<Variable>()
        
        // تقسيم JSON إلى متغيرات
        val variableEntries = json.split("},")
        for (entry in variableEntries) {
            val nameMatch = "\"name\":\"([^\"]+)\"".toRegex().find(entry)
            val typeMatch = "\"type\":\"([^\"]+)\"".toRegex().find(entry)
            val valueMatch = "\"value\":\"([^\"]+)\"".toRegex().find(entry)
            
            if (nameMatch != null && typeMatch != null && valueMatch != null) {
                val name = nameMatch.groupValues[1]
                val type = typeMatch.groupValues[1]
                val value = valueMatch.groupValues[1]
                
                variables.add(Variable(name, type, value))
            }
        }
        
        return variables
    }
    
    /**
     * تحليل مكدس الاستدعاء من JSON
     */
    private fun parseCallStack(json: String): List<StackFrame> {
        // في التطبيق الحقيقي، يجب تحليل JSON باستخدام مكتبة مناسبة
        // هذا مجرد مثال بسيط
        val frames = mutableListOf<StackFrame>()
        
        // تقسيم JSON إلى إطارات
        val frameEntries = json.split("},")
        for (entry in frameEntries) {
            val methodMatch = "\"method\":\"([^\"]+)\"".toRegex().find(entry)
            val fileMatch = "\"file\":\"([^\"]+)\"".toRegex().find(entry)
            val lineMatch = "\"line\":([0-9]+)".toRegex().find(entry)
            
            if (methodMatch != null && fileMatch != null && lineMatch != null) {
                val method = methodMatch.groupValues[1]
                val file = fileMatch.groupValues[1]
                val line = lineMatch.groupValues[1].toInt()
                
                frames.add(StackFrame(method, file, line))
            }
        }
        
        return frames
    }
}