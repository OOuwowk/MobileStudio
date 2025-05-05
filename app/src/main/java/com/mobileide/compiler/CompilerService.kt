package com.mobileide.compiler

import android.content.Context
import android.util.Log
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.compiler.model.BuildResult
import com.mobileide.compiler.model.CompilationResult
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة المترجم المسؤولة عن بناء المشاريع
 */
@Singleton
class CompilerService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "CompilerService"
    }
    
    /**
     * تهيئة المترجم لمشروع معين
     */
    suspend fun initializeForProject(project: Project, frameworkType: FrameworkType): CompilationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing compiler for project: ${project.name} with framework: $frameworkType")
            
            // إنشاء دليل البناء
            val buildDir = File(project.path, "build")
            if (!buildDir.exists()) {
                buildDir.mkdirs()
            }
            
            // تهيئة المترجم حسب نوع إطار العمل
            when (frameworkType) {
                FrameworkType.ANDROID_NATIVE -> initializeAndroidCompiler(project)
                FrameworkType.FLUTTER -> initializeFlutterCompiler(project)
                FrameworkType.REACT_NATIVE -> initializeReactNativeCompiler(project)
                FrameworkType.KOTLIN_MULTIPLATFORM -> initializeKMPCompiler(project)
            }
            
            return@withContext CompilationResult(
                success = true,
                message = "تم تهيئة المترجم بنجاح",
                outputFile = null,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing compiler", e)
            return@withContext CompilationResult(
                success = false,
                message = "فشل تهيئة المترجم: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع
     */
    suspend fun buildProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building project: ${project.name}")
            
            // تحديد نوع إطار العمل
            val frameworkType = detectFrameworkType(project)
            
            // بناء المشروع حسب نوع إطار العمل
            val result = when (frameworkType) {
                FrameworkType.ANDROID_NATIVE -> buildAndroidProject(project)
                FrameworkType.FLUTTER -> buildFlutterProject(project)
                FrameworkType.REACT_NATIVE -> buildReactNativeProject(project)
                FrameworkType.KOTLIN_MULTIPLATFORM -> buildKMPProject(project)
            }
            
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error building project", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء المشروع: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع للتصحيح
     */
    suspend fun buildProjectForDebugging(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building project for debugging: ${project.name}")
            
            // تحديد نوع إطار العمل
            val frameworkType = detectFrameworkType(project)
            
            // بناء المشروع للتصحيح حسب نوع إطار العمل
            val result = when (frameworkType) {
                FrameworkType.ANDROID_NATIVE -> buildAndroidProjectForDebugging(project)
                FrameworkType.FLUTTER -> buildFlutterProjectForDebugging(project)
                FrameworkType.REACT_NATIVE -> buildReactNativeProjectForDebugging(project)
                FrameworkType.KOTLIN_MULTIPLATFORM -> buildKMPProjectForDebugging(project)
            }
            
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error building project for debugging", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء المشروع للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * تهيئة مترجم Android
     */
    private suspend fun initializeAndroidCompiler(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // إنشاء دليل البناء
            val buildDir = File(project.path, "build")
            val classesDir = File(buildDir, "classes")
            val outputDir = File(buildDir, "outputs")
            
            classesDir.mkdirs()
            outputDir.mkdirs()
            
            // نسخ ملفات Android SDK
            val androidJarDir = File(context.filesDir, "android-sdk")
            if (!androidJarDir.exists()) {
                androidJarDir.mkdirs()
                
                // نسخ ملف android.jar من الموارد
                context.assets.open("android.jar").use { input ->
                    File(androidJarDir, "android.jar").outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Android compiler", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مترجم Flutter
     */
    private suspend fun initializeFlutterCompiler(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود Flutter SDK
            val flutterSdkDir = File(context.filesDir, "flutter-sdk")
            if (!flutterSdkDir.exists()) {
                // في التطبيق الحقيقي، يجب تنزيل Flutter SDK أو طلب من المستخدم تحديد مساره
                flutterSdkDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Flutter compiler", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مترجم React Native
     */
    private suspend fun initializeReactNativeCompiler(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود Node.js
            val nodeDir = File(context.filesDir, "node")
            if (!nodeDir.exists()) {
                // في التطبيق الحقيقي، يجب تنزيل Node.js أو طلب من المستخدم تحديد مساره
                nodeDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing React Native compiler", e)
            return@withContext false
        }
    }
    
    /**
     * تهيئة مترجم Kotlin Multiplatform
     */
    private suspend fun initializeKMPCompiler(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            // التحقق من وجود Kotlin
            val kotlinDir = File(context.filesDir, "kotlin")
            if (!kotlinDir.exists()) {
                // في التطبيق الحقيقي، يجب تنزيل Kotlin أو طلب من المستخدم تحديد مساره
                kotlinDir.mkdirs()
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing KMP compiler", e)
            return@withContext false
        }
    }
    
    /**
     * بناء مشروع Android
     */
    private suspend fun buildAndroidProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building Android project: ${project.name}")
            
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val outputDir = File(buildDir, "outputs")
            
            // 1. تجميع ملفات Java/Kotlin
            val javaResult = compileJavaFiles(project)
            if (!javaResult.success) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل تجميع ملفات Java: ${javaResult.message}",
                    outputFile = null,
                    errors = javaResult.errors
                )
            }
            
            // 2. معالجة موارد أندرويد
            val resourceResult = processAndroidResources(project)
            if (!resourceResult.success) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل معالجة موارد أندرويد: ${resourceResult.message}",
                    outputFile = null,
                    errors = resourceResult.errors
                )
            }
            
            // 3. إنشاء ملف DEX
            val dexResult = createDexFiles(project)
            if (!dexResult.success) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل إنشاء ملفات DEX: ${dexResult.message}",
                    outputFile = null,
                    errors = dexResult.errors
                )
            }
            
            // 4. إنشاء ملف APK
            val apkFile = createApkFile(project)
            if (apkFile == null) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل إنشاء ملف APK",
                    outputFile = null,
                    errors = listOf("فشل إنشاء ملف APK")
                )
            }
            
            // 5. توقيع ملف APK
            val signedApk = signApk(project, apkFile)
            if (signedApk == null) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل توقيع ملف APK",
                    outputFile = null,
                    errors = listOf("فشل توقيع ملف APK")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء المشروع بنجاح",
                outputFile = signedApk,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building Android project", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء المشروع: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع Flutter
     */
    private suspend fun buildFlutterProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building Flutter project: ${project.name}")
            
            val projectDir = File(project.path)
            
            // استخدام أمر Flutter لبناء المشروع
            val process = ProcessBuilder(
                "flutter",
                "build",
                "apk",
                "--release"
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل بناء مشروع Flutter",
                    outputFile = null,
                    errors = listOf(output)
                )
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(projectDir, "build/app/outputs/flutter-apk/app-release.apk")
            if (!apkFile.exists()) {
                return@withContext BuildResult(
                    success = false,
                    message = "لم يتم العثور على ملف APK الناتج",
                    outputFile = null,
                    errors = listOf("لم يتم العثور على ملف APK الناتج")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء مشروع Flutter بنجاح",
                outputFile = apkFile,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building Flutter project", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء مشروع Flutter: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع React Native
     */
    private suspend fun buildReactNativeProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building React Native project: ${project.name}")
            
            val projectDir = File(project.path)
            val androidDir = File(projectDir, "android")
            
            // استخدام Gradle لبناء المشروع
            val process = ProcessBuilder(
                "./gradlew",
                "assembleRelease"
            )
                .directory(androidDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل بناء مشروع React Native",
                    outputFile = null,
                    errors = listOf(output)
                )
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(androidDir, "app/build/outputs/apk/release/app-release.apk")
            if (!apkFile.exists()) {
                return@withContext BuildResult(
                    success = false,
                    message = "لم يتم العثور على ملف APK الناتج",
                    outputFile = null,
                    errors = listOf("لم يتم العثور على ملف APK الناتج")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء مشروع React Native بنجاح",
                outputFile = apkFile,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building React Native project", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء مشروع React Native: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع Kotlin Multiplatform
     */
    private suspend fun buildKMPProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building KMP project: ${project.name}")
            
            val projectDir = File(project.path)
            
            // استخدام Gradle لبناء المشروع
            val process = ProcessBuilder(
                "./gradlew",
                "androidApp:assembleRelease"
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل بناء مشروع Kotlin Multiplatform",
                    outputFile = null,
                    errors = listOf(output)
                )
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(projectDir, "androidApp/build/outputs/apk/release/androidApp-release.apk")
            if (!apkFile.exists()) {
                return@withContext BuildResult(
                    success = false,
                    message = "لم يتم العثور على ملف APK الناتج",
                    outputFile = null,
                    errors = listOf("لم يتم العثور على ملف APK الناتج")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء مشروع Kotlin Multiplatform بنجاح",
                outputFile = apkFile,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building KMP project", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء مشروع Kotlin Multiplatform: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع Android للتصحيح
     */
    private suspend fun buildAndroidProjectForDebugging(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building Android project for debugging: ${project.name}")
            
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val outputDir = File(buildDir, "outputs")
            
            // 1. تجميع ملفات Java/Kotlin مع معلومات التصحيح
            val javaResult = compileJavaFilesForDebugging(project)
            if (!javaResult.success) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل تجميع ملفات Java للتصحيح: ${javaResult.message}",
                    outputFile = null,
                    errors = javaResult.errors
                )
            }
            
            // 2. معالجة موارد أندرويد
            val resourceResult = processAndroidResources(project)
            if (!resourceResult.success) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل معالجة موارد أندرويد: ${resourceResult.message}",
                    outputFile = null,
                    errors = resourceResult.errors
                )
            }
            
            // 3. إنشاء ملف DEX مع معلومات التصحيح
            val dexResult = createDexFilesForDebugging(project)
            if (!dexResult.success) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل إنشاء ملفات DEX للتصحيح: ${dexResult.message}",
                    outputFile = null,
                    errors = dexResult.errors
                )
            }
            
            // 4. إنشاء ملف APK للتصحيح
            val apkFile = createApkFileForDebugging(project)
            if (apkFile == null) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل إنشاء ملف APK للتصحيح",
                    outputFile = null,
                    errors = listOf("فشل إنشاء ملف APK للتصحيح")
                )
            }
            
            // 5. توقيع ملف APK
            val signedApk = signApk(project, apkFile)
            if (signedApk == null) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل توقيع ملف APK للتصحيح",
                    outputFile = null,
                    errors = listOf("فشل توقيع ملف APK للتصحيح")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء المشروع للتصحيح بنجاح",
                outputFile = signedApk,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building Android project for debugging", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء المشروع للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع Flutter للتصحيح
     */
    private suspend fun buildFlutterProjectForDebugging(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building Flutter project for debugging: ${project.name}")
            
            val projectDir = File(project.path)
            
            // استخدام أمر Flutter لبناء المشروع في وضع التصحيح
            val process = ProcessBuilder(
                "flutter",
                "build",
                "apk",
                "--debug"
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل بناء مشروع Flutter للتصحيح",
                    outputFile = null,
                    errors = listOf(output)
                )
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(projectDir, "build/app/outputs/flutter-apk/app-debug.apk")
            if (!apkFile.exists()) {
                return@withContext BuildResult(
                    success = false,
                    message = "لم يتم العثور على ملف APK الناتج للتصحيح",
                    outputFile = null,
                    errors = listOf("لم يتم العثور على ملف APK الناتج للتصحيح")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء مشروع Flutter للتصحيح بنجاح",
                outputFile = apkFile,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building Flutter project for debugging", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء مشروع Flutter للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع React Native للتصحيح
     */
    private suspend fun buildReactNativeProjectForDebugging(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building React Native project for debugging: ${project.name}")
            
            val projectDir = File(project.path)
            val androidDir = File(projectDir, "android")
            
            // استخدام Gradle لبناء المشروع في وضع التصحيح
            val process = ProcessBuilder(
                "./gradlew",
                "assembleDebug"
            )
                .directory(androidDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل بناء مشروع React Native للتصحيح",
                    outputFile = null,
                    errors = listOf(output)
                )
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(androidDir, "app/build/outputs/apk/debug/app-debug.apk")
            if (!apkFile.exists()) {
                return@withContext BuildResult(
                    success = false,
                    message = "لم يتم العثور على ملف APK الناتج للتصحيح",
                    outputFile = null,
                    errors = listOf("لم يتم العثور على ملف APK الناتج للتصحيح")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء مشروع React Native للتصحيح بنجاح",
                outputFile = apkFile,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building React Native project for debugging", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء مشروع React Native للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * بناء مشروع Kotlin Multiplatform للتصحيح
     */
    private suspend fun buildKMPProjectForDebugging(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building KMP project for debugging: ${project.name}")
            
            val projectDir = File(project.path)
            
            // استخدام Gradle لبناء المشروع في وضع التصحيح
            val process = ProcessBuilder(
                "./gradlew",
                "androidApp:assembleDebug"
            )
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return@withContext BuildResult(
                    success = false,
                    message = "فشل بناء مشروع Kotlin Multiplatform للتصحيح",
                    outputFile = null,
                    errors = listOf(output)
                )
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(projectDir, "androidApp/build/outputs/apk/debug/androidApp-debug.apk")
            if (!apkFile.exists()) {
                return@withContext BuildResult(
                    success = false,
                    message = "لم يتم العثور على ملف APK الناتج للتصحيح",
                    outputFile = null,
                    errors = listOf("لم يتم العثور على ملف APK الناتج للتصحيح")
                )
            }
            
            return@withContext BuildResult(
                success = true,
                message = "تم بناء مشروع Kotlin Multiplatform للتصحيح بنجاح",
                outputFile = apkFile,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building KMP project for debugging", e)
            return@withContext BuildResult(
                success = false,
                message = "فشل بناء مشروع Kotlin Multiplatform للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * تجميع ملفات Java
     */
    private fun compileJavaFiles(project: Project): CompilationResult {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val classesDir = File(buildDir, "classes")
            
            // إنشاء دليل الفئات
            classesDir.mkdirs()
            
            // استخدام ECJ لتجميع ملفات Java
            val compiler = org.eclipse.jdt.internal.compiler.batch.Main(
                PrintWriter(System.out),
                PrintWriter(System.err),
                false,
                null,
                null
            )
            
            // البحث عن ملفات Java
            val javaFiles = findJavaFiles(projectDir)
            if (javaFiles.isEmpty()) {
                return CompilationResult(
                    success = true,
                    message = "لا توجد ملفات Java للتجميع",
                    outputFile = null,
                    errors = emptyList()
                )
            }
            
            // تجميع ملفات Java
            val classpath = getCompilationClasspath()
            val args = arrayOf(
                "-classpath", classpath,
                "-source", "1.8",
                "-target", "1.8",
                "-proc:none",
                "-d", classesDir.absolutePath
            ) + javaFiles.map { it.absolutePath }.toTypedArray()
            
            val result = compiler.compile(args)
            
            return if (result == 0) {
                CompilationResult(
                    success = true,
                    message = "تم تجميع ملفات Java بنجاح",
                    outputFile = classesDir,
                    errors = emptyList()
                )
            } else {
                CompilationResult(
                    success = false,
                    message = "فشل تجميع ملفات Java",
                    outputFile = null,
                    errors = listOf("فشل تجميع ملفات Java")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling Java files", e)
            return CompilationResult(
                success = false,
                message = "فشل تجميع ملفات Java: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * تجميع ملفات Java للتصحيح
     */
    private fun compileJavaFilesForDebugging(project: Project): CompilationResult {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val classesDir = File(buildDir, "classes")
            
            // إنشاء دليل الفئات
            classesDir.mkdirs()
            
            // استخدام ECJ لتجميع ملفات Java مع معلومات التصحيح
            val compiler = org.eclipse.jdt.internal.compiler.batch.Main(
                PrintWriter(System.out),
                PrintWriter(System.err),
                false,
                null,
                null
            )
            
            // البحث عن ملفات Java
            val javaFiles = findJavaFiles(projectDir)
            if (javaFiles.isEmpty()) {
                return CompilationResult(
                    success = true,
                    message = "لا توجد ملفات Java للتجميع",
                    outputFile = null,
                    errors = emptyList()
                )
            }
            
            // تجميع ملفات Java مع معلومات التصحيح
            val classpath = getCompilationClasspath()
            val args = arrayOf(
                "-classpath", classpath,
                "-source", "1.8",
                "-target", "1.8",
                "-proc:none",
                "-g", // إضافة معلومات التصحيح
                "-d", classesDir.absolutePath
            ) + javaFiles.map { it.absolutePath }.toTypedArray()
            
            val result = compiler.compile(args)
            
            return if (result == 0) {
                CompilationResult(
                    success = true,
                    message = "تم تجميع ملفات Java للتصحيح بنجاح",
                    outputFile = classesDir,
                    errors = emptyList()
                )
            } else {
                CompilationResult(
                    success = false,
                    message = "فشل تجميع ملفات Java للتصحيح",
                    outputFile = null,
                    errors = listOf("فشل تجميع ملفات Java للتصحيح")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling Java files for debugging", e)
            return CompilationResult(
                success = false,
                message = "فشل تجميع ملفات Java للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * معالجة موارد أندرويد
     */
    private fun processAndroidResources(project: Project): CompilationResult {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val outputDir = File(buildDir, "outputs")
            
            // إنشاء دليل الإخراج
            outputDir.mkdirs()
            
            // استخدام AAPT2 لمعالجة الموارد
            val aaptPath = "${context.applicationInfo.nativeLibraryDir}/libaapt2.so"
            val resPath = "${projectDir}/src/main/res"
            val manifestPath = "${projectDir}/src/main/AndroidManifest.xml"
            
            val process = ProcessBuilder(
                aaptPath,
                "package",
                "-f",
                "-m",
                "-M", manifestPath,
                "-S", resPath,
                "-I", "${context.filesDir}/android-sdk/android.jar",
                "-F", "${outputDir}/resources.ap_"
            ).start()
            
            val exitCode = process.waitFor()
            
            return if (exitCode == 0) {
                CompilationResult(
                    success = true,
                    message = "تم معالجة موارد أندرويد بنجاح",
                    outputFile = File(outputDir, "resources.ap_"),
                    errors = emptyList()
                )
            } else {
                val error = process.errorStream.bufferedReader().readText()
                CompilationResult(
                    success = false,
                    message = "فشل معالجة موارد أندرويد",
                    outputFile = null,
                    errors = listOf(error)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Android resources", e)
            return CompilationResult(
                success = false,
                message = "فشل معالجة موارد أندرويد: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * إنشاء ملفات DEX
     */
    private fun createDexFiles(project: Project): CompilationResult {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val classesDir = File(buildDir, "classes")
            val outputDir = File(buildDir, "outputs")
            
            // إنشاء دليل الإخراج
            outputDir.mkdirs()
            
            // استخدام D8 لتحويل bytecode إلى DEX
            val d8Path = "${context.applicationInfo.nativeLibraryDir}/libd8.so"
            
            val process = ProcessBuilder(
                d8Path,
                "--release",
                "--output", "${outputDir}/classes.dex",
                classesDir.absolutePath
            ).start()
            
            val exitCode = process.waitFor()
            
            return if (exitCode == 0) {
                CompilationResult(
                    success = true,
                    message = "تم إنشاء ملفات DEX بنجاح",
                    outputFile = File(outputDir, "classes.dex"),
                    errors = emptyList()
                )
            } else {
                val error = process.errorStream.bufferedReader().readText()
                CompilationResult(
                    success = false,
                    message = "فشل إنشاء ملفات DEX",
                    outputFile = null,
                    errors = listOf(error)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating DEX files", e)
            return CompilationResult(
                success = false,
                message = "فشل إنشاء ملفات DEX: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * إنشاء ملفات DEX للتصحيح
     */
    private fun createDexFilesForDebugging(project: Project): CompilationResult {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val classesDir = File(buildDir, "classes")
            val outputDir = File(buildDir, "outputs")
            
            // إنشاء دليل الإخراج
            outputDir.mkdirs()
            
            // استخدام D8 لتحويل bytecode إلى DEX مع معلومات التصحيح
            val d8Path = "${context.applicationInfo.nativeLibraryDir}/libd8.so"
            
            val process = ProcessBuilder(
                d8Path,
                "--debug",
                "--output", "${outputDir}/classes.dex",
                classesDir.absolutePath
            ).start()
            
            val exitCode = process.waitFor()
            
            return if (exitCode == 0) {
                CompilationResult(
                    success = true,
                    message = "تم إنشاء ملفات DEX للتصحيح بنجاح",
                    outputFile = File(outputDir, "classes.dex"),
                    errors = emptyList()
                )
            } else {
                val error = process.errorStream.bufferedReader().readText()
                CompilationResult(
                    success = false,
                    message = "فشل إنشاء ملفات DEX للتصحيح",
                    outputFile = null,
                    errors = listOf(error)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating DEX files for debugging", e)
            return CompilationResult(
                success = false,
                message = "فشل إنشاء ملفات DEX للتصحيح: ${e.message}",
                outputFile = null,
                errors = listOf(e.message ?: "خطأ غير معروف")
            )
        }
    }
    
    /**
     * إنشاء ملف APK
     */
    private fun createApkFile(project: Project): File? {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val outputDir = File(buildDir, "outputs")
            
            val apkFile = File(outputDir, "${project.name}.apk")
            
            // دمج ملف DEX مع موارد لإنشاء APK
            ZipOutputStream(FileOutputStream(apkFile)).use { zipOut ->
                // إضافة ملف classes.dex
                val dexFile = File(outputDir, "classes.dex")
                addFileToZip(zipOut, dexFile, "classes.dex")
                
                // إضافة الموارد من resources.ap_
                val resourcesApk = File(outputDir, "resources.ap_")
                ZipFile(resourcesApk).use { resourcesZip ->
                    val entries = resourcesZip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name != "classes.dex") {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            resourcesZip.getInputStream(entry).copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            
            return apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating APK file", e)
            return null
        }
    }
    
    /**
     * إنشاء ملف APK للتصحيح
     */
    private fun createApkFileForDebugging(project: Project): File? {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val outputDir = File(buildDir, "outputs")
            
            val apkFile = File(outputDir, "${project.name}-debug.apk")
            
            // دمج ملف DEX مع موارد لإنشاء APK
            ZipOutputStream(FileOutputStream(apkFile)).use { zipOut ->
                // إضافة ملف classes.dex
                val dexFile = File(outputDir, "classes.dex")
                addFileToZip(zipOut, dexFile, "classes.dex")
                
                // إضافة الموارد من resources.ap_
                val resourcesApk = File(outputDir, "resources.ap_")
                ZipFile(resourcesApk).use { resourcesZip ->
                    val entries = resourcesZip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name != "classes.dex") {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            resourcesZip.getInputStream(entry).copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            
            return apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating APK file for debugging", e)
            return null
        }
    }
    
    /**
     * توقيع ملف APK
     */
    private fun signApk(project: Project, apkFile: File): File? {
        try {
            val projectDir = File(project.path)
            val buildDir = File(projectDir, "build")
            val outputDir = File(buildDir, "outputs")
            
            val signedApk = File(outputDir, "${apkFile.nameWithoutExtension}-signed.apk")
            
            // استخدام apksigner لتوقيع التطبيق
            val apkSignerPath = "${context.applicationInfo.nativeLibraryDir}/libapksigner.so"
            val keyStorePath = "${context.filesDir}/debug.keystore"
            
            val process = ProcessBuilder(
                apkSignerPath,
                "sign",
                "--ks", keyStorePath,
                "--ks-pass", "pass:android",
                "--out", signedApk.absolutePath,
                apkFile.absolutePath
            ).start()
            
            val exitCode = process.waitFor()
            
            return if (exitCode == 0) {
                signedApk
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing APK", e)
            return null
        }
    }
    
    /**
     * إضافة ملف إلى أرشيف ZIP
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        file.inputStream().use { fileIn ->
            zipOut.putNextEntry(ZipEntry(entryName))
            fileIn.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
    
    /**
     * البحث عن ملفات Java
     */
    private fun findJavaFiles(dir: File): List<File> {
        val javaFiles = mutableListOf<File>()
        
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    javaFiles.addAll(findJavaFiles(file))
                } else if (file.name.endsWith(".java")) {
                    javaFiles.add(file)
                }
            }
        }
        
        return javaFiles
    }
    
    /**
     * الحصول على مسار الفصول (classpath) للترجمة
     */
    private fun getCompilationClasspath(): String {
        val androidJar = "${context.filesDir}/android-sdk/android.jar"
        val supportLibs = "${context.filesDir}/android-sdk/libs"
        return "$androidJar:$supportLibs/*"
    }
    
    /**
     * تحديد نوع إطار العمل المستخدم في المشروع
     */
    private fun detectFrameworkType(project: Project): FrameworkType {
        val projectDir = File(project.path)
        
        // التحقق من وجود ملفات Flutter
        if (File(projectDir, "pubspec.yaml").exists()) {
            return FrameworkType.FLUTTER
        }
        
        // التحقق من وجود ملفات React Native
        if (File(projectDir, "package.json").exists() && 
            File(projectDir, "node_modules").exists() && 
            File(projectDir, "android").exists()) {
            return FrameworkType.REACT_NATIVE
        }
        
        // التحقق من وجود ملفات Kotlin Multiplatform
        if (File(projectDir, "shared").exists() && 
            File(projectDir, "androidApp").exists() && 
            File(projectDir, "iosApp").exists()) {
            return FrameworkType.KOTLIN_MULTIPLATFORM
        }
        
        // افتراضيًا، استخدم Android Native
        return FrameworkType.ANDROID_NATIVE
    }
}