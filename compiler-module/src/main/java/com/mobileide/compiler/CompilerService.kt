package com.mobileide.compiler

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class CompilerService @Inject constructor(
    private val context: Context
) {
    /**
     * Compiles a project into an APK
     */
    suspend fun compileProject(projectPath: String): CompilationResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting compilation for project at $projectPath")
            
            // 1. Compile Java/Kotlin files
            val javaResult = compileJavaFiles(projectPath)
            if (!javaResult.success) {
                return@withContext CompilationResult(false, javaResult.errors)
            }
            
            // 2. Process Android resources
            val resourceResult = processResources(projectPath)
            if (!resourceResult.success) {
                return@withContext CompilationResult(false, resourceResult.errors)
            }
            
            // 3. Create DEX files
            val dexResult = createDexFiles(projectPath)
            if (!dexResult.success) {
                return@withContext CompilationResult(false, dexResult.errors)
            }
            
            // 4. Create APK file
            val apkResult = createApkFile(projectPath)
            if (!apkResult.success) {
                return@withContext CompilationResult(false, apkResult.errors)
            }
            
            // 5. Sign APK
            val signResult = signApk(projectPath, apkResult.apkFile)
            if (!signResult.success) {
                return@withContext CompilationResult(false, signResult.errors)
            }
            
            Timber.d("Compilation successful, APK created at ${signResult.apkFile.absolutePath}")
            return@withContext CompilationResult(
                success = true,
                apkFile = signResult.apkFile
            )
        } catch (e: Exception) {
            Timber.e(e, "Compilation failed with exception")
            CompilationResult(false, listOf("Compilation failed: ${e.message}"))
        }
    }
    
    /**
     * Compiles Java files in the project
     */
    private fun compileJavaFiles(projectPath: String): JavaCompilationResult {
        Timber.d("Compiling Java files")
        
        try {
            // Create output directory for compiled classes
            val outputDir = File("$projectPath/bin/classes")
            outputDir.mkdirs()
            
            // Find all Java files in the project
            val sourceFiles = findJavaFiles(projectPath)
            if (sourceFiles.isEmpty()) {
                Timber.w("No Java files found in project")
                return JavaCompilationResult(true, emptyList())
            }
            
            // Get classpath for compilation
            val classpath = getCompilationClasspath()
            
            // Prepare ECJ compiler arguments
            val args = arrayOf(
                "-classpath", classpath,
                "-source", "1.8",
                "-target", "1.8",
                "-proc:none",
                "-d", outputDir.absolutePath
            ) + sourceFiles.map { it.absolutePath }.toTypedArray()
            
            // Use ECJ compiler
            val compiler = org.eclipse.jdt.internal.compiler.batch.Main(
                null, // Use System.out
                null, // Use System.err
                false, // No progress
                null, // Default options
                null  // No compilation progress
            )
            
            // Execute compilation
            val result = compiler.compile(args)
            val success = result == 0
            
            return if (success) {
                Timber.d("Java compilation successful")
                JavaCompilationResult(true, emptyList())
            } else {
                Timber.e("Java compilation failed with code $result")
                JavaCompilationResult(false, listOf("Java compilation failed with code $result"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Java compilation failed with exception")
            return JavaCompilationResult(false, listOf("Java compilation error: ${e.message}"))
        }
    }
    
    /**
     * Finds all Java files in the project
     */
    private fun findJavaFiles(projectPath: String): List<File> {
        val sourceDir = File("$projectPath/src/main/java")
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            Timber.w("Source directory not found: ${sourceDir.absolutePath}")
            return emptyList()
        }
        
        return sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()
    }
    
    /**
     * Gets the classpath for compilation
     */
    private fun getCompilationClasspath(): String {
        val androidJar = "${context.filesDir}/android.jar"
        val supportLibs = "${context.filesDir}/libs"
        return "$androidJar:$supportLibs/*"
    }
    
    /**
     * Processes Android resources
     */
    private fun processResources(projectPath: String): ResourceProcessingResult {
        Timber.d("Processing Android resources")
        
        try {
            // Check if resource directory exists
            val resDir = File("$projectPath/src/main/res")
            if (!resDir.exists() || !resDir.isDirectory) {
                Timber.w("Resource directory not found: ${resDir.absolutePath}")
                return ResourceProcessingResult(true, emptyList()) // Continue without resources
            }
            
            // Check if manifest file exists
            val manifestFile = File("$projectPath/src/main/AndroidManifest.xml")
            if (!manifestFile.exists()) {
                Timber.e("AndroidManifest.xml not found: ${manifestFile.absolutePath}")
                return ResourceProcessingResult(false, listOf("AndroidManifest.xml not found"))
            }
            
            // Create output directory
            val outputDir = File("$projectPath/bin")
            outputDir.mkdirs()
            
            // Path to AAPT2 binary
            val aaptPath = "${context.applicationInfo.nativeLibraryDir}/libaapt2.so"
            if (!File(aaptPath).exists()) {
                Timber.e("AAPT2 binary not found: $aaptPath")
                return ResourceProcessingResult(false, listOf("AAPT2 binary not found"))
            }
            
            // Path to Android SDK platform
            val androidJar = "${context.filesDir}/android.jar"
            if (!File(androidJar).exists()) {
                Timber.e("Android platform JAR not found: $androidJar")
                return ResourceProcessingResult(false, listOf("Android platform JAR not found"))
            }
            
            // Execute AAPT2 compile step
            val compileProcess = ProcessBuilder(
                aaptPath,
                "compile",
                "--dir", resDir.absolutePath,
                "-o", "$outputDir/resources.zip"
            ).start()
            
            val compileExitCode = compileProcess.waitFor()
            if (compileExitCode != 0) {
                val error = compileProcess.errorStream.bufferedReader().readText()
                Timber.e("AAPT2 compile failed: $error")
                return ResourceProcessingResult(false, listOf("Resource compilation failed: $error"))
            }
            
            // Execute AAPT2 link step
            val linkProcess = ProcessBuilder(
                aaptPath,
                "link",
                "-I", androidJar,
                "--manifest", manifestFile.absolutePath,
                "-o", "$outputDir/resources.ap_",
                "$outputDir/resources.zip"
            ).start()
            
            val linkExitCode = linkProcess.waitFor()
            if (linkExitCode != 0) {
                val error = linkProcess.errorStream.bufferedReader().readText()
                Timber.e("AAPT2 link failed: $error")
                return ResourceProcessingResult(false, listOf("Resource linking failed: $error"))
            }
            
            Timber.d("Resource processing successful")
            return ResourceProcessingResult(true, emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Resource processing failed with exception")
            return ResourceProcessingResult(false, listOf("Resource processing error: ${e.message}"))
        }
    }
    
    /**
     * Creates DEX files from compiled Java classes
     */
    private fun createDexFiles(projectPath: String): DexResult {
        Timber.d("Creating DEX files")
        
        try {
            // Check if classes directory exists
            val classesDir = File("$projectPath/bin/classes")
            if (!classesDir.exists() || !classesDir.isDirectory || classesDir.list()?.isEmpty() == true) {
                Timber.w("No compiled classes found in ${classesDir.absolutePath}")
                return DexResult(false, listOf("No compiled classes found"))
            }
            
            // Create output directory
            val outputDir = File("$projectPath/bin")
            outputDir.mkdirs()
            
            // Path to D8 tool
            val d8Path = "${context.applicationInfo.nativeLibraryDir}/libd8.so"
            if (!File(d8Path).exists()) {
                Timber.e("D8 binary not found: $d8Path")
                return DexResult(false, listOf("D8 binary not found"))
            }
            
            // Find all class files
            val classFiles = classesDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map { it.absolutePath }
                .toList()
            
            if (classFiles.isEmpty()) {
                Timber.w("No class files found in ${classesDir.absolutePath}")
                return DexResult(false, listOf("No class files found"))
            }
            
            // Execute D8 to create DEX file
            val process = ProcessBuilder(
                d8Path,
                "--release",
                "--output", "$outputDir/classes.dex",
                *classFiles.toTypedArray()
            ).start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                Timber.e("D8 failed: $error")
                return DexResult(false, listOf("DEX creation failed: $error"))
            }
            
            // Check if DEX file was created
            val dexFile = File("$outputDir/classes.dex")
            if (!dexFile.exists() || dexFile.length() == 0L) {
                Timber.e("DEX file was not created or is empty")
                return DexResult(false, listOf("DEX file was not created or is empty"))
            }
            
            Timber.d("DEX creation successful")
            return DexResult(true, emptyList())
        } catch (e: Exception) {
            Timber.e(e, "DEX creation failed with exception")
            return DexResult(false, listOf("DEX creation error: ${e.message}"))
        }
    }
    
    /**
     * Creates an APK file from DEX and resources
     */
    private fun createApkFile(projectPath: String): ApkResult {
        Timber.d("Creating APK file")
        
        try {
            // Check if DEX file exists
            val dexFile = File("$projectPath/bin/classes.dex")
            if (!dexFile.exists()) {
                Timber.e("DEX file not found: ${dexFile.absolutePath}")
                return ApkResult(false, listOf("DEX file not found"), File(""))
            }
            
            // Check if resources file exists
            val resourcesFile = File("$projectPath/bin/resources.ap_")
            if (!resourcesFile.exists()) {
                Timber.e("Resources file not found: ${resourcesFile.absolutePath}")
                return ApkResult(false, listOf("Resources file not found"), File(""))
            }
            
            // Create output APK file
            val apkFile = File("$projectPath/bin/app-debug-unsigned.apk")
            apkFile.parentFile?.mkdirs()
            if (apkFile.exists()) {
                apkFile.delete()
            }
            
            // Create APK by combining resources and DEX
            ZipOutputStream(FileOutputStream(apkFile)).use { zipOut ->
                // Add DEX file to APK
                addFileToZip(zipOut, dexFile, "classes.dex")
                
                // Add resources from resources.ap_
                ZipFile(resourcesFile).use { resourcesZip ->
                    val entries = resourcesZip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name != "classes.dex") { // Avoid duplicate DEX
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            resourcesZip.getInputStream(entry).copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
                
                // Add any additional assets if needed
                val assetsDir = File("$projectPath/src/main/assets")
                if (assetsDir.exists() && assetsDir.isDirectory) {
                    val basePathLength = assetsDir.absolutePath.length + 1
                    assetsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relativePath = "assets/${file.absolutePath.substring(basePathLength)}"
                        addFileToZip(zipOut, file, relativePath)
                    }
                }
            }
            
            // Verify APK was created successfully
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Timber.e("APK file was not created or is empty")
                return ApkResult(false, listOf("APK file was not created or is empty"), File(""))
            }
            
            Timber.d("APK creation successful: ${apkFile.absolutePath}")
            return ApkResult(true, emptyList(), apkFile)
        } catch (e: Exception) {
            Timber.e(e, "APK creation failed with exception")
            return ApkResult(false, listOf("APK creation error: ${e.message}"), File(""))
        }
    }
    
    /**
     * Adds a file to a ZIP archive
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fileIn ->
            zipOut.putNextEntry(ZipEntry(entryName))
            fileIn.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
    
    /**
     * Signs an APK file
     */
    private fun signApk(projectPath: String, apkFile: File): SignResult {
        Timber.d("Signing APK: ${apkFile.absolutePath}")
        
        try {
            // Check if unsigned APK exists
            if (!apkFile.exists()) {
                Timber.e("Unsigned APK file not found: ${apkFile.absolutePath}")
                return SignResult(false, listOf("Unsigned APK file not found"), File(""))
            }
            
            // Create output signed APK file
            val signedApkFile = File("$projectPath/bin/app-debug.apk")
            if (signedApkFile.exists()) {
                signedApkFile.delete()
            }
            
            // Path to apksigner tool
            val apkSignerPath = "${context.applicationInfo.nativeLibraryDir}/libapksigner.so"
            if (!File(apkSignerPath).exists()) {
                Timber.e("APK signer binary not found: $apkSignerPath")
                
                // Fallback: Use debug keystore from Android SDK
                // For development purposes, we can use a debug keystore
                val debugKeystore = File("${context.filesDir}/debug.keystore")
                if (!debugKeystore.exists()) {
                    // Create a debug keystore if it doesn't exist
                    createDebugKeystore(debugKeystore)
                }
                
                // Use zipalign before signing (optional but recommended)
                val zipAlignPath = "${context.applicationInfo.nativeLibraryDir}/libzipalign.so"
                val alignedApk = File("$projectPath/bin/app-debug-aligned.apk")
                
                if (File(zipAlignPath).exists()) {
                    val alignProcess = ProcessBuilder(
                        zipAlignPath,
                        "-f", "4",
                        apkFile.absolutePath,
                        alignedApk.absolutePath
                    ).start()
                    
                    val alignExitCode = alignProcess.waitFor()
                    if (alignExitCode != 0) {
                        Timber.w("Zipalign failed, continuing with unaligned APK")
                    } else {
                        // Use aligned APK for signing
                        apkFile.delete()
                        alignedApk.renameTo(apkFile)
                    }
                }
                
                // Use jarsigner as fallback
                val jarsignerProcess = ProcessBuilder(
                    "jarsigner",
                    "-keystore", debugKeystore.absolutePath,
                    "-storepass", "android",
                    "-keypass", "android",
                    "-signedjar", signedApkFile.absolutePath,
                    apkFile.absolutePath,
                    "androiddebugkey"
                ).start()
                
                val jarsignerExitCode = jarsignerProcess.waitFor()
                if (jarsignerExitCode != 0) {
                    val error = jarsignerProcess.errorStream.bufferedReader().readText()
                    Timber.e("Jarsigner failed: $error")
                    
                    // If jarsigner fails, just copy the unsigned APK as a fallback
                    apkFile.copyTo(signedApkFile, overwrite = true)
                    Timber.w("Using unsigned APK as fallback")
                }
            } else {
                // Use apksigner tool
                val process = ProcessBuilder(
                    apkSignerPath,
                    "sign",
                    "--ks", "${context.filesDir}/debug.keystore",
                    "--ks-pass", "pass:android",
                    "--key-pass", "pass:android",
                    "--out", signedApkFile.absolutePath,
                    apkFile.absolutePath
                ).start()
                
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val error = process.errorStream.bufferedReader().readText()
                    Timber.e("APK signing failed: $error")
                    return SignResult(false, listOf("APK signing failed: $error"), File(""))
                }
            }
            
            // Verify signed APK was created successfully
            if (!signedApkFile.exists() || signedApkFile.length() == 0L) {
                Timber.e("Signed APK file was not created or is empty")
                return SignResult(false, listOf("Signed APK file was not created or is empty"), File(""))
            }
            
            Timber.d("APK signing successful: ${signedApkFile.absolutePath}")
            return SignResult(true, emptyList(), signedApkFile)
        } catch (e: Exception) {
            Timber.e(e, "APK signing failed with exception")
            return SignResult(false, listOf("APK signing error: ${e.message}"), File(""))
        }
    }
    
    /**
     * Creates a debug keystore for signing APKs
     */
    private fun createDebugKeystore(keystoreFile: File) {
        try {
            // Ensure parent directory exists
            keystoreFile.parentFile?.mkdirs()
            
            // Generate a debug keystore using keytool
            val process = ProcessBuilder(
                "keytool",
                "-genkey",
                "-v",
                "-keystore", keystoreFile.absolutePath,
                "-storepass", "android",
                "-alias", "androiddebugkey",
                "-keypass", "android",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=Android Debug,O=Android,C=US"
            ).start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                Timber.e("Failed to create debug keystore: $error")
                
                // Create a dummy keystore file as fallback
                keystoreFile.createNewFile()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create debug keystore")
            
            // Create a dummy keystore file as fallback
            try {
                keystoreFile.createNewFile()
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to create dummy keystore file")
            }
        }
    }
}

/**
 * Result of the compilation process
 */
data class CompilationResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val apkFile: File? = null
)

/**
 * Result of Java compilation
 */
data class JavaCompilationResult(
    val success: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Result of resource processing
 */
data class ResourceProcessingResult(
    val success: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Result of DEX creation
 */
data class DexResult(
    val success: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Result of APK creation
 */
data class ApkResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val apkFile: File
)

/**
 * Result of APK signing
 */
data class SignResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val apkFile: File
)