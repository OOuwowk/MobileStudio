package com.mobileide.debugger

import android.content.Context
import android.content.pm.PackageManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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

// DebugSession has been moved to its own file: DebugSession.kt

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

// EvaluationResult has been moved to its own file: EvaluationResult.kt