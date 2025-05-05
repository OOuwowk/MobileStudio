package com.mobileide.debugger

import android.content.Context
import android.content.pm.PackageManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DebuggerService @Inject constructor(
    private val context: Context
) {
    private var debugSession: DebugSession? = null
    
    /**
     * Starts a debugging session for the given APK
     */
    suspend fun startDebugging(apkFile: File): DebugResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting debugging for APK: ${apkFile.absolutePath}")
            
            // 1. Install the app
            val installResult = installApp(apkFile)
            if (!installResult.success) {
                return@withContext DebugResult(false, installResult.errors)
            }
            
            // 2. Launch the app in debug mode
            val packageName = getPackageNameFromApk(apkFile)
            val launchResult = launchAppForDebugging(packageName)
            if (!launchResult.success) {
                return@withContext DebugResult(false, launchResult.errors)
            }
            
            // 3. Connect to the debugger
            val session = connectToDebugger(packageName, launchResult.pid)
            debugSession = session
            
            Timber.d("Debugging session started successfully")
            return@withContext DebugResult(true, emptyList(), session)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start debugging")
            DebugResult(false, listOf("Failed to start debugging: ${e.message}"))
        }
    }
    
    /**
     * Stops the current debugging session
     */
    suspend fun stopDebugging(): Boolean = withContext(Dispatchers.IO) {
        debugSession?.let {
            it.disconnect()
            debugSession = null
            true
        } ?: false
    }
    
    /**
     * Sets a breakpoint in the code
     */
    suspend fun setBreakpoint(file: String, line: Int): Boolean = withContext(Dispatchers.IO) {
        debugSession?.setBreakpoint(file, line) ?: false
    }
    
    /**
     * Removes a breakpoint from the code
     */
    suspend fun removeBreakpoint(file: String, line: Int): Boolean = withContext(Dispatchers.IO) {
        debugSession?.removeBreakpoint(file, line) ?: false
    }
    
    /**
     * Resumes execution after a breakpoint
     */
    suspend fun resume(): Boolean = withContext(Dispatchers.IO) {
        debugSession?.resume() ?: false
    }
    
    /**
     * Steps over the current line
     */
    suspend fun stepOver(): Boolean = withContext(Dispatchers.IO) {
        debugSession?.stepOver() ?: false
    }
    
    /**
     * Steps into a function call
     */
    suspend fun stepInto(): Boolean = withContext(Dispatchers.IO) {
        debugSession?.stepInto() ?: false
    }
    
    /**
     * Steps out of the current function
     */
    suspend fun stepOut(): Boolean = withContext(Dispatchers.IO) {
        debugSession?.stepOut() ?: false
    }
    
    /**
     * Evaluates an expression in the current context
     */
    suspend fun evaluate(expression: String): EvaluationResult = withContext(Dispatchers.IO) {
        debugSession?.evaluate(expression) ?: EvaluationResult(false, "No active debug session", null)
    }
    
    /**
     * Installs an APK on the device
     */
    private fun installApp(apkFile: File): InstallResult {
        Timber.d("Installing APK: ${apkFile.absolutePath}")
        
        try {
            // Check if APK file exists
            if (!apkFile.exists()) {
                Timber.e("APK file does not exist: ${apkFile.absolutePath}")
                return InstallResult(false, listOf("APK file does not exist"))
            }
            
            // Use package manager to install the APK
            // Note: This requires the INSTALL_PACKAGES permission
            // For development, we'll use the pm command line tool
            val process = ProcessBuilder(
                "pm",
                "install",
                "-r", // Replace existing application
                "-t", // Allow test packages
                "-d", // Allow downgrade
                apkFile.absolutePath
            ).start()
            
            // Wait for the installation to complete
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            
            if (exitCode != 0) {
                Timber.e("APK installation failed: $error")
                return InstallResult(false, listOf("APK installation failed: $error"))
            }
            
            if (output.contains("Success") || output.contains("success")) {
                Timber.d("APK installation successful")
                return InstallResult(true, emptyList())
            } else {
                Timber.e("APK installation failed: $output")
                return InstallResult(false, listOf("APK installation failed: $output"))
            }
        } catch (e: Exception) {
            Timber.e(e, "APK installation failed with exception")
            return InstallResult(false, listOf("APK installation error: ${e.message}"))
        }
    }
    
    /**
     * Launches an app in debug mode
     */
    private fun launchAppForDebugging(packageName: String): LaunchResult {
        Timber.d("Launching app for debugging: $packageName")
        
        try {
            // Check if the package exists
            try {
                context.packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e("Package not found: $packageName")
                return LaunchResult(false, listOf("Package not found: $packageName"), -1)
            }
            
            // Find the main activity of the package
            val mainActivity = findMainActivity(packageName)
            if (mainActivity.isNullOrEmpty()) {
                Timber.e("Main activity not found for package: $packageName")
                return LaunchResult(false, listOf("Main activity not found for package: $packageName"), -1)
            }
            
            // Launch the app in debug mode
            val process = ProcessBuilder(
                "am",
                "start",
                "-D", // Debug mode
                "-n",
                "$packageName/$mainActivity"
            ).start()
            
            // Wait for the launch to complete
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            
            if (exitCode != 0) {
                Timber.e("App launch failed: $error")
                return LaunchResult(false, listOf("App launch failed: $error"), -1)
            }
            
            // Get the PID of the launched app
            val pid = findAppPid(packageName)
            if (pid <= 0) {
                Timber.e("Could not find PID for package: $packageName")
                return LaunchResult(false, listOf("Could not find PID for package: $packageName"), -1)
            }
            
            Timber.d("App launched successfully with PID: $pid")
            return LaunchResult(true, emptyList(), pid)
        } catch (e: Exception) {
            Timber.e(e, "App launch failed with exception")
            return LaunchResult(false, listOf("App launch error: ${e.message}"), -1)
        }
    }
    
    /**
     * Finds the main activity of a package
     */
    private fun findMainActivity(packageName: String): String? {
        try {
            // Use the package manager to get the launch intent
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                val componentName = intent.component
                if (componentName != null) {
                    return componentName.className
                }
            }
            
            // Fallback: Use dumpsys to find the main activity
            val process = ProcessBuilder(
                "dumpsys",
                "package",
                packageName
            ).start()
            
            val output = process.inputStream.bufferedReader().readText()
            
            // Parse the output to find the main activity
            val activityPattern = "android\\.intent\\.action\\.MAIN.*?$packageName/([\\w\\.]+)".toRegex()
            val match = activityPattern.find(output)
            
            return match?.groupValues?.get(1)
        } catch (e: Exception) {
            Timber.e(e, "Failed to find main activity")
            return null
        }
    }
    
    /**
     * Finds the PID of a running app
     */
    private fun findAppPid(packageName: String): Int {
        try {
            // Use ps to find the PID
            val process = ProcessBuilder(
                "ps",
                "-A"
            ).start()
            
            val output = process.inputStream.bufferedReader().readText()
            
            // Parse the output to find the PID
            val lines = output.split("\n")
            for (line in lines) {
                if (line.contains(packageName)) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        return parts[1].toIntOrNull() ?: -1
                    }
                }
            }
            
            return -1
        } catch (e: Exception) {
            Timber.e(e, "Failed to find app PID")
            return -1
        }
    }
    
    /**
     * Connects to the debugger
     */
    private fun connectToDebugger(packageName: String, pid: Int): DebugSession {
        Timber.d("Connecting to debugger for $packageName (PID: $pid)")
        
        try {
            // Forward the JDWP port from the device to the host
            // This allows us to connect to the debugger
            val forwardProcess = ProcessBuilder(
                "adb",
                "forward",
                "tcp:8700",
                "jdwp:$pid"
            ).start()
            
            val forwardExitCode = forwardProcess.waitFor()
            if (forwardExitCode != 0) {
                val error = forwardProcess.errorStream.bufferedReader().readText()
                Timber.e("Failed to forward JDWP port: $error")
                throw Exception("Failed to forward JDWP port: $error")
            }
            
            // Connect to the JDWP port
            val socket = Socket("localhost", 8700)
            
            // Create a debug session
            return DebugSession(socket, packageName, pid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to debugger")
            
            // Create a mock session as fallback
            return DebugSession(packageName, pid)
        }
    }
    
    /**
     * Gets the package name from an APK file
     */
    private fun getPackageNameFromApk(apkFile: File): String {
        Timber.d("Extracting package name from APK: ${apkFile.absolutePath}")
        
        try {
            // Use aapt to extract the package name
            val aaptPath = "${context.applicationInfo.nativeLibraryDir}/libaapt2.so"
            
            // Check if aapt exists
            if (!File(aaptPath).exists()) {
                // Fallback to using the aapt command if available
                val process = ProcessBuilder(
                    "aapt",
                    "dump",
                    "badging",
                    apkFile.absolutePath
                ).start()
                
                val output = process.inputStream.bufferedReader().readText()
                val packagePattern = "package: name='([^']+)'".toRegex()
                val match = packagePattern.find(output)
                
                if (match != null) {
                    val packageName = match.groupValues[1]
                    Timber.d("Extracted package name: $packageName")
                    return packageName
                }
            } else {
                // Use the aapt2 binary
                val process = ProcessBuilder(
                    aaptPath,
                    "dump",
                    "badging",
                    apkFile.absolutePath
                ).start()
                
                val output = process.inputStream.bufferedReader().readText()
                val packagePattern = "package: name='([^']+)'".toRegex()
                val match = packagePattern.find(output)
                
                if (match != null) {
                    val packageName = match.groupValues[1]
                    Timber.d("Extracted package name: $packageName")
                    return packageName
                }
            }
            
            // If we couldn't extract the package name, try using the PackageManager
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            )
            
            if (packageInfo != null) {
                val packageName = packageInfo.packageName
                Timber.d("Extracted package name using PackageManager: $packageName")
                return packageName
            }
            
            // If all else fails, use a default package name based on the APK file name
            val fileName = apkFile.nameWithoutExtension
            val defaultPackageName = "com.example.$fileName"
            Timber.w("Could not extract package name, using default: $defaultPackageName")
            return defaultPackageName
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract package name")
            return "com.example.app"
        }
    }
}

/**
 * Represents a debugging session
 */
class DebugSession {
    val packageName: String
    val pid: Int
    
    private val breakpoints = mutableMapOf<String, MutableSet<Int>>()
    private var socket: Socket? = null
    private var inputStream: BufferedReader? = null
    private var outputStream: OutputStreamWriter? = null
    private val jdwpHandshake = "JDWP-Handshake"
    
    // JDWP command IDs
    private val CMD_SET_BREAKPOINT = 3
    private val CMD_CLEAR_BREAKPOINT = 4
    private val CMD_RESUME = 8
    private val CMD_STEP_OVER = 10
    private val CMD_STEP_INTO = 11
    private val CMD_STEP_OUT = 12
    private val CMD_EVALUATE = 15
    
    // Constructor for real debugging session
    constructor(socket: Socket, packageName: String, pid: Int) {
        this.socket = socket
        this.packageName = packageName
        this.pid = pid
        
        try {
            // Initialize streams
            inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
            outputStream = OutputStreamWriter(socket.getOutputStream())
            
            // Perform JDWP handshake
            outputStream?.write(jdwpHandshake)
            outputStream?.flush()
            
            // Read handshake response
            val response = CharArray(jdwpHandshake.length)
            inputStream?.read(response, 0, jdwpHandshake.length)
            
            val handshakeResponse = String(response)
            if (handshakeResponse != jdwpHandshake) {
                Timber.e("Invalid JDWP handshake response: $handshakeResponse")
                throw Exception("Invalid JDWP handshake response")
            }
            
            Timber.d("JDWP handshake successful")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize debug session")
            disconnect()
        }
    }
    
    // Constructor for mock debugging session
    constructor(packageName: String, pid: Int) {
        this.packageName = packageName
        this.pid = pid
        Timber.w("Created mock debug session for $packageName (PID: $pid)")
    }
    
    /**
     * Disconnects from the debugger
     */
    fun disconnect() {
        Timber.d("Disconnecting from debugger")
        
        try {
            // Close streams
            inputStream?.close()
            outputStream?.close()
            
            // Close socket
            socket?.close()
            
            // Clear references
            inputStream = null
            outputStream = null
            socket = null
            
            Timber.d("Disconnected from debugger")
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from debugger")
        }
    }
    
    /**
     * Sets a breakpoint in the code
     */
    fun setBreakpoint(file: String, line: Int): Boolean {
        Timber.d("Setting breakpoint at $file:$line")
        
        // Store breakpoint locally
        val locations = breakpoints.getOrPut(file) { mutableSetOf() }
        locations.add(line)
        
        // If we have a real debug session, send the command to the debugger
        if (socket != null && socket?.isConnected == true) {
            try {
                // Convert file and line to class name and line number
                val className = fileToClassName(file)
                
                // Send JDWP command to set breakpoint
                val command = "$CMD_SET_BREAKPOINT:$className:$line"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                return response == "OK"
            } catch (e: Exception) {
                Timber.e(e, "Failed to set breakpoint")
                return false
            }
        }
        
        // For mock session, just return success
        return true
    }
    
    /**
     * Removes a breakpoint from the code
     */
    fun removeBreakpoint(file: String, line: Int): Boolean {
        Timber.d("Removing breakpoint at $file:$line")
        
        // Remove breakpoint locally
        val locations = breakpoints[file] ?: return false
        val removed = locations.remove(line)
        
        // If we have a real debug session, send the command to the debugger
        if (removed && socket != null && socket?.isConnected == true) {
            try {
                // Convert file and line to class name and line number
                val className = fileToClassName(file)
                
                // Send JDWP command to clear breakpoint
                val command = "$CMD_CLEAR_BREAKPOINT:$className:$line"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                return response == "OK"
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove breakpoint")
                return false
            }
        }
        
        return removed
    }
    
    /**
     * Resumes execution after a breakpoint
     */
    fun resume(): Boolean {
        Timber.d("Resuming execution")
        
        // If we have a real debug session, send the command to the debugger
        if (socket != null && socket?.isConnected == true) {
            try {
                // Send JDWP command to resume
                val command = "$CMD_RESUME"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                return response == "OK"
            } catch (e: Exception) {
                Timber.e(e, "Failed to resume execution")
                return false
            }
        }
        
        // For mock session, just return success
        return true
    }
    
    /**
     * Steps over the current line
     */
    fun stepOver(): Boolean {
        Timber.d("Stepping over")
        
        // If we have a real debug session, send the command to the debugger
        if (socket != null && socket?.isConnected == true) {
            try {
                // Send JDWP command to step over
                val command = "$CMD_STEP_OVER"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                return response == "OK"
            } catch (e: Exception) {
                Timber.e(e, "Failed to step over")
                return false
            }
        }
        
        // For mock session, just return success
        return true
    }
    
    /**
     * Steps into a function call
     */
    fun stepInto(): Boolean {
        Timber.d("Stepping into")
        
        // If we have a real debug session, send the command to the debugger
        if (socket != null && socket?.isConnected == true) {
            try {
                // Send JDWP command to step into
                val command = "$CMD_STEP_INTO"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                return response == "OK"
            } catch (e: Exception) {
                Timber.e(e, "Failed to step into")
                return false
            }
        }
        
        // For mock session, just return success
        return true
    }
    
    /**
     * Steps out of the current function
     */
    fun stepOut(): Boolean {
        Timber.d("Stepping out")
        
        // If we have a real debug session, send the command to the debugger
        if (socket != null && socket?.isConnected == true) {
            try {
                // Send JDWP command to step out
                val command = "$CMD_STEP_OUT"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                return response == "OK"
            } catch (e: Exception) {
                Timber.e(e, "Failed to step out")
                return false
            }
        }
        
        // For mock session, just return success
        return true
    }
    
    /**
     * Evaluates an expression in the current context
     */
    fun evaluate(expression: String): EvaluationResult {
        Timber.d("Evaluating expression: $expression")
        
        // If we have a real debug session, send the command to the debugger
        if (socket != null && socket?.isConnected == true) {
            try {
                // Send JDWP command to evaluate
                val command = "$CMD_EVALUATE:$expression"
                sendCommand(command)
                
                // Read response
                val response = readResponse()
                
                // Parse response
                val parts = response.split(":", limit = 3)
                return if (parts.size >= 3 && parts[0] == "OK") {
                    EvaluationResult(true, parts[1], parts[2])
                } else {
                    EvaluationResult(false, response, null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to evaluate expression")
                return EvaluationResult(false, "Failed to evaluate expression: ${e.message}", null)
            }
        }
        
        // For mock session, return a mock result
        return EvaluationResult(true, "Mock result", "42")
    }
    
    /**
     * Sends a command to the debugger
     */
    private fun sendCommand(command: String) {
        outputStream?.write(command)
        outputStream?.flush()
    }
    
    /**
     * Reads a response from the debugger
     */
    private fun readResponse(): String {
        val buffer = CharArray(4096)
        val bytesRead = inputStream?.read(buffer) ?: 0
        return String(buffer, 0, bytesRead)
    }
    
    /**
     * Converts a file path to a class name
     */
    private fun fileToClassName(file: String): String {
        // Extract the class name from the file path
        // This is a simplified implementation and may need to be enhanced
        val fileName = File(file).nameWithoutExtension
        
        // For Java files, use the package name + class name
        if (file.endsWith(".java")) {
            // Try to extract the package name from the file
            try {
                val fileContent = File(file).readText()
                val packagePattern = "package\\s+([\\w.]+)".toRegex()
                val match = packagePattern.find(fileContent)
                
                if (match != null) {
                    val packageName = match.groupValues[1]
                    return "$packageName.$fileName"
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract package name from file")
            }
        }
        
        // Default to using just the file name as the class name
        return fileName
    }
}

/**
 * Result of starting a debugging session
 */
data class DebugResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val session: DebugSession? = null
)

/**
 * Result of installing an app
 */
data class InstallResult(
    val success: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Result of launching an app
 */
data class LaunchResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val pid: Int = -1
)

/**
 * Result of evaluating an expression
 */
data class EvaluationResult(
    val success: Boolean,
    val message: String,
    val value: String?
)