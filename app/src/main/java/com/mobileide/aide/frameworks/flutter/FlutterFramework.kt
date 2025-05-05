package com.mobileide.aide.frameworks.flutter

import android.content.Context
import android.util.Log
import com.mobileide.aide.frameworks.*
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.inject.Inject

/**
 * إطار عمل Flutter
 */
class FlutterFramework @Inject constructor(
    private val context: Context,
    private val frameworkInstaller: FrameworkInstaller
) : Framework {
    
    companion object {
        private const val TAG = "FlutterFramework"
        private const val FLUTTER_SDK_URL = "https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_3.10.6-stable.tar.xz"
        private const val FLUTTER_VERSION = "3.10.6"
    }
    
    override fun getType(): FrameworkType = FrameworkType.FLUTTER
    
    override fun getName(): String = "Flutter"
    
    override fun getDescription(): String = "إطار عمل متعدد المنصات لبناء تطبيقات أصلية لأندرويد و iOS"
    
    override fun getWebsiteUrl(): String = "https://flutter.dev"
    
    override fun getDocumentationUrl(): String = "https://docs.flutter.dev"
    
    /**
     * الحصول على دليل تثبيت Flutter
     */
    private fun getFlutterDirectory(): File {
        return frameworkInstaller.getInstallationDirectory(FrameworkType.FLUTTER)
    }
    
    /**
     * الحصول على مسار أداة Flutter
     */
    private fun getFlutterExecutable(): File {
        return File(getFlutterDirectory(), "bin/flutter")
    }
    
    /**
     * التحقق من حالة تثبيت إطار العمل
     */
    override suspend fun checkInstallation(): InstallationStatus = withContext(Dispatchers.IO) {
        val flutterDir = getFlutterDirectory()
        val flutterExecutable = getFlutterExecutable()
        
        if (!flutterDir.exists() || !flutterExecutable.exists()) {
            return@withContext InstallationStatus.NOT_INSTALLED
        }
        
        // التحقق من إصدار Flutter
        val versionResult = frameworkInstaller.executeCommand(
            "${flutterExecutable.absolutePath} --version",
            flutterDir
        )
        
        if (!versionResult.success) {
            return@withContext InstallationStatus.PARTIALLY_INSTALLED
        }
        
        // التحقق من تهيئة Flutter
        val doctorResult = frameworkInstaller.executeCommand(
            "${flutterExecutable.absolutePath} doctor",
            flutterDir
        )
        
        if (!doctorResult.success) {
            return@withContext InstallationStatus.PARTIALLY_INSTALLED
        }
        
        // التحقق من وجود تحديثات
        val updateInfo = checkForUpdates()
        if (updateInfo.updateAvailable) {
            return@withContext InstallationStatus.NEEDS_UPDATE
        }
        
        return@withContext InstallationStatus.INSTALLED
    }
    
    /**
     * تثبيت إطار العمل
     */
    override suspend fun install(): InstallationResult = withContext(Dispatchers.IO) {
        try {
            val flutterDir = getFlutterDirectory()
            val flutterExecutable = getFlutterExecutable()
            
            // إنشاء دليل التثبيت
            flutterDir.mkdirs()
            
            // تنزيل Flutter SDK
            val downloadFile = File(context.cacheDir, "flutter_sdk.tar.xz")
            val downloadSuccess = frameworkInstaller.downloadFile(FLUTTER_SDK_URL, downloadFile) { progress ->
                Log.d(TAG, "Download progress: ${progress * 100}%")
            }
            
            if (!downloadSuccess) {
                return@withContext InstallationResult(false, "فشل تنزيل Flutter SDK")
            }
            
            // فك ضغط Flutter SDK
            val extractCommand = "tar xf ${downloadFile.absolutePath} -C ${flutterDir.parent.absolutePath}"
            val extractResult = frameworkInstaller.executeCommand(extractCommand, flutterDir.parentFile!!)
            
            if (!extractResult.success) {
                return@withContext InstallationResult(false, "فشل فك ضغط Flutter SDK: ${extractResult.output}")
            }
            
            // تعيين أذونات التنفيذ
            frameworkInstaller.setExecutablePermissions(flutterExecutable)
            
            // تهيئة Flutter
            val configResult = frameworkInstaller.executeCommand(
                "${flutterExecutable.absolutePath} config --no-analytics",
                flutterDir
            )
            
            if (!configResult.success) {
                return@withContext InstallationResult(false, "فشل تهيئة Flutter: ${configResult.output}")
            }
            
            // تنزيل مكونات Flutter
            val precacheResult = frameworkInstaller.executeCommand(
                "${flutterExecutable.absolutePath} precache",
                flutterDir
            )
            
            if (!precacheResult.success) {
                return@withContext InstallationResult(false, "فشل تنزيل مكونات Flutter: ${precacheResult.output}")
            }
            
            // التحقق من تثبيت Flutter
            val doctorResult = frameworkInstaller.executeCommand(
                "${flutterExecutable.absolutePath} doctor",
                flutterDir
            )
            
            if (!doctorResult.success) {
                return@withContext InstallationResult(false, "فشل التحقق من تثبيت Flutter: ${doctorResult.output}")
            }
            
            // حذف ملف التنزيل
            downloadFile.delete()
            
            return@withContext InstallationResult(true, "تم تثبيت Flutter بنجاح", FLUTTER_VERSION)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing Flutter", e)
            return@withContext InstallationResult(false, "فشل تثبيت Flutter: ${e.message}")
        }
    }
    
    /**
     * إلغاء تثبيت إطار العمل
     */
    override suspend fun uninstall(): Boolean = withContext(Dispatchers.IO) {
        val flutterDir = getFlutterDirectory()
        
        if (flutterDir.exists()) {
            return@withContext flutterDir.deleteRecursively()
        }
        
        return@withContext true
    }
    
    /**
     * الحصول على قوالب المشاريع المتاحة
     */
    override suspend fun getTemplates(): List<ProjectTemplate> = withContext(Dispatchers.IO) {
        val templates = mutableListOf<ProjectTemplate>()
        
        // قالب تطبيق Flutter فارغ
        templates.add(
            ProjectTemplate(
                id = "flutter_empty",
                name = "تطبيق Flutter فارغ",
                description = "تطبيق Flutter فارغ مع صفحة واحدة",
                frameworkType = FrameworkType.FLUTTER,
                path = "templates/flutter/empty",
                minFrameworkVersion = "2.0.0",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                supportedLanguages = listOf("Dart")
            )
        )
        
        // قالب تطبيق Flutter مع التنقل
        templates.add(
            ProjectTemplate(
                id = "flutter_navigation",
                name = "تطبيق Flutter مع تنقل",
                description = "تطبيق Flutter مع تنقل بين عدة صفحات",
                frameworkType = FrameworkType.FLUTTER,
                path = "templates/flutter/navigation",
                minFrameworkVersion = "2.0.0",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                supportedLanguages = listOf("Dart")
            )
        )
        
        // قالب تطبيق Flutter مع قائمة
        templates.add(
            ProjectTemplate(
                id = "flutter_list",
                name = "تطبيق Flutter مع قائمة",
                description = "تطبيق Flutter مع قائمة وتفاصيل العناصر",
                frameworkType = FrameworkType.FLUTTER,
                path = "templates/flutter/list",
                minFrameworkVersion = "2.0.0",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                supportedLanguages = listOf("Dart")
            )
        )
        
        return@withContext templates
    }
    
    /**
     * إنشاء مشروع جديد
     */
    override suspend fun createProject(
        name: String,
        packageName: String,
        templateId: String,
        location: File
    ): Project = withContext(Dispatchers.IO) {
        try {
            // التحقق من تثبيت Flutter
            val installationStatus = checkInstallation()
            if (installationStatus != InstallationStatus.INSTALLED) {
                throw IOException("Flutter غير مثبت أو يحتاج إلى تحديث")
            }
            
            val flutterExecutable = getFlutterExecutable()
            val projectDir = File(location, name)
            
            // إنشاء مشروع Flutter جديد
            val createCommand = "${flutterExecutable.absolutePath} create --org $packageName --project-name ${name.toLowerCase().replace(" ", "_")} ${projectDir.absolutePath}"
            val createResult = frameworkInstaller.executeCommand(createCommand, location)
            
            if (!createResult.success) {
                throw IOException("فشل إنشاء مشروع Flutter: ${createResult.output}")
            }
            
            // تخصيص المشروع حسب القالب المحدد
            customizeFlutterProject(projectDir, name, packageName, templateId)
            
            // إنشاء كائن المشروع
            val project = Project(
                id = 0,
                name = name,
                packageName = packageName,
                path = projectDir.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            
            // تعيين نوع إطار العمل
            project.frameworkType = FrameworkType.FLUTTER
            
            return@withContext project
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Flutter project", e)
            throw IOException("فشل إنشاء مشروع Flutter: ${e.message}")
        }
    }
    
    /**
     * بناء مشروع
     */
    override suspend fun buildProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            // التحقق من تثبيت Flutter
            val installationStatus = checkInstallation()
            if (installationStatus != InstallationStatus.INSTALLED) {
                return@withContext BuildResult(false, null, "Flutter غير مثبت أو يحتاج إلى تحديث")
            }
            
            val flutterExecutable = getFlutterExecutable()
            val projectDir = File(project.path)
            
            // بناء تطبيق أندرويد
            val buildCommand = "${flutterExecutable.absolutePath} build apk --release"
            val buildResult = frameworkInstaller.executeCommand(buildCommand, projectDir)
            
            if (!buildResult.success) {
                return@withContext BuildResult(false, null, "فشل بناء مشروع Flutter: ${buildResult.output}")
            }
            
            // البحث عن ملف APK الناتج
            val apkFile = File(projectDir, "build/app/outputs/flutter-apk/app-release.apk")
            
            if (apkFile.exists()) {
                return@withContext BuildResult(true, apkFile, "تم بناء مشروع Flutter بنجاح")
            } else {
                return@withContext BuildResult(false, null, "تم بناء المشروع ولكن لم يتم العثور على ملف APK")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building Flutter project", e)
            return@withContext BuildResult(false, null, "فشل بناء مشروع Flutter: ${e.message}")
        }
    }
    
    /**
     * تشغيل مشروع
     */
    override suspend fun runProject(project: Project): RunResult = withContext(Dispatchers.IO) {
        try {
            // التحقق من تثبيت Flutter
            val installationStatus = checkInstallation()
            if (installationStatus != InstallationStatus.INSTALLED) {
                return@withContext RunResult(false, "Flutter غير مثبت أو يحتاج إلى تحديث")
            }
            
            val flutterExecutable = getFlutterExecutable()
            val projectDir = File(project.path)
            
            // التحقق من وجود أجهزة متصلة
            val devicesCommand = "${flutterExecutable.absolutePath} devices"
            val devicesResult = frameworkInstaller.executeCommand(devicesCommand, projectDir)
            
            if (!devicesResult.success || !devicesResult.output.contains("android")) {
                return@withContext RunResult(false, "لم يتم العثور على أجهزة أندرويد متصلة")
            }
            
            // تشغيل التطبيق
            val runCommand = "${flutterExecutable.absolutePath} run"
            val runResult = frameworkInstaller.executeCommand(runCommand, projectDir)
            
            return@withContext if (runResult.success) {
                RunResult(true, "تم تشغيل مشروع Flutter بنجاح")
            } else {
                RunResult(false, "فشل تشغيل مشروع Flutter: ${runResult.output}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running Flutter project", e)
            return@withContext RunResult(false, "فشل تشغيل مشروع Flutter: ${e.message}")
        }
    }
    
    /**
     * تصحيح مشروع
     */
    override suspend fun debugProject(project: Project): DebugResult = withContext(Dispatchers.IO) {
        try {
            // التحقق من تثبيت Flutter
            val installationStatus = checkInstallation()
            if (installationStatus != InstallationStatus.INSTALLED) {
                return@withContext DebugResult(false, "Flutter غير مثبت أو يحتاج إلى تحديث")
            }
            
            val flutterExecutable = getFlutterExecutable()
            val projectDir = File(project.path)
            
            // التحقق من وجود أجهزة متصلة
            val devicesCommand = "${flutterExecutable.absolutePath} devices"
            val devicesResult = frameworkInstaller.executeCommand(devicesCommand, projectDir)
            
            if (!devicesResult.success || !devicesResult.output.contains("android")) {
                return@withContext DebugResult(false, "لم يتم العثور على أجهزة أندرويد متصلة")
            }
            
            // تشغيل التطبيق في وضع التصحيح
            val debugCommand = "${flutterExecutable.absolutePath} run --debug"
            val debugResult = frameworkInstaller.executeCommand(debugCommand, projectDir)
            
            return@withContext if (debugResult.success) {
                DebugResult(true, "تم بدء تصحيح مشروع Flutter بنجاح")
            } else {
                DebugResult(false, "فشل بدء تصحيح مشروع Flutter: ${debugResult.output}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error debugging Flutter project", e)
            return@withContext DebugResult(false, "فشل بدء تصحيح مشروع Flutter: ${e.message}")
        }
    }
    
    /**
     * الحصول على إصدار إطار العمل المثبت
     */
    override suspend fun getInstalledVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val flutterDir = getFlutterDirectory()
            val flutterExecutable = getFlutterExecutable()
            
            if (!flutterDir.exists() || !flutterExecutable.exists()) {
                return@withContext null
            }
            
            val versionCommand = "${flutterExecutable.absolutePath} --version"
            val versionResult = frameworkInstaller.executeCommand(versionCommand, flutterDir)
            
            if (!versionResult.success) {
                return@withContext null
            }
            
            // استخراج الإصدار من الإخراج
            val versionRegex = "Flutter (\\d+\\.\\d+\\.\\d+)".toRegex()
            val matchResult = versionRegex.find(versionResult.output)
            
            return@withContext matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Flutter version", e)
            return@withContext null
        }
    }
    
    /**
     * الحصول على أحدث إصدار متاح لإطار العمل
     */
    override suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
        try {
            // في التنفيذ الفعلي، يمكن استخدام API لاستعلام أحدث إصدار
            // هنا نستخدم قيمة ثابتة للتبسيط
            return@withContext "3.10.6"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest Flutter version", e)
            return@withContext null
        }
    }
    
    /**
     * التحقق من وجود تحديثات لإطار العمل
     */
    override suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getInstalledVersion()
            val latestVersion = getLatestVersion()
            
            if (currentVersion == null || latestVersion == null) {
                return@withContext UpdateInfo(false, currentVersion, latestVersion)
            }
            
            // مقارنة الإصدارات
            val updateAvailable = compareVersions(currentVersion, latestVersion) < 0
            
            return@withContext UpdateInfo(
                updateAvailable,
                currentVersion,
                latestVersion,
                if (updateAvailable) FLUTTER_SDK_URL else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for Flutter updates", e)
            return@withContext UpdateInfo(false, null, null)
        }
    }
    
    /**
     * مقارنة إصدارين
     * @return قيمة سالبة إذا كان الإصدار الأول أقدم، وقيمة موجبة إذا كان أحدث، وصفر إذا كانا متساويين
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toInt() }
        val v2Parts = version2.split(".").map { it.toInt() }
        
        for (i in 0 until minOf(v1Parts.size, v2Parts.size)) {
            val diff = v1Parts[i] - v2Parts[i]
            if (diff != 0) {
                return diff
            }
        }
        
        return v1Parts.size - v2Parts.size
    }
    
    /**
     * تخصيص مشروع Flutter حسب القالب المحدد
     */
    private fun customizeFlutterProject(projectDir: File, name: String, packageName: String, templateId: String) {
        try {
            when (templateId) {
                "flutter_empty" -> {
                    // لا يلزم تخصيص إضافي للقالب الفارغ
                }
                "flutter_navigation" -> {
                    // تطبيق قالب التنقل
                    applyNavigationTemplate(projectDir, name, packageName)
                }
                "flutter_list" -> {
                    // تطبيق قالب القائمة
                    applyListTemplate(projectDir, name, packageName)
                }
                else -> {
                    // استخدام القالب الافتراضي
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error customizing Flutter project", e)
            throw IOException("فشل تخصيص مشروع Flutter: ${e.message}")
        }
    }
    
    /**
     * تطبيق قالب التنقل على مشروع Flutter
     */
    private fun applyNavigationTemplate(projectDir: File, name: String, packageName: String) {
        try {
            // استخراج قالب التنقل من الأصول
            val templateDir = File(context.filesDir, "templates/flutter/navigation")
            if (!templateDir.exists()) {
                // استخراج القالب من الأصول
                extractTemplateFromAssets("navigation", templateDir)
            }
            
            // نسخ ملفات القالب إلى المشروع
            val libDir = File(projectDir, "lib")
            copyDirectory(File(templateDir, "lib"), libDir)
            
            // تحديث ملف pubspec.yaml لإضافة التبعيات اللازمة
            val pubspecFile = File(projectDir, "pubspec.yaml")
            if (pubspecFile.exists()) {
                var content = pubspecFile.readText()
                
                // إضافة تبعية التنقل
                if (!content.contains("flutter_localizations")) {
                    val dependenciesIndex = content.indexOf("dependencies:")
                    if (dependenciesIndex != -1) {
                        val insertIndex = content.indexOf("\n", dependenciesIndex) + 1
                        val newDependencies = """
                          flutter_localizations:
                            sdk: flutter
                          provider: ^6.0.0
                        """.trimIndent()
                        
                        content = content.substring(0, insertIndex) + newDependencies + "\n" + content.substring(insertIndex)
                    }
                }
                
                pubspecFile.writeText(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying navigation template", e)
            throw IOException("فشل تطبيق قالب التنقل: ${e.message}")
        }
    }
    
    /**
     * تطبيق قالب القائمة على مشروع Flutter
     */
    private fun applyListTemplate(projectDir: File, name: String, packageName: String) {
        try {
            // استخراج قالب القائمة من الأصول
            val templateDir = File(context.filesDir, "templates/flutter/list")
            if (!templateDir.exists()) {
                // استخراج القالب من الأصول
                extractTemplateFromAssets("list", templateDir)
            }
            
            // نسخ ملفات القالب إلى المشروع
            val libDir = File(projectDir, "lib")
            copyDirectory(File(templateDir, "lib"), libDir)
            
            // نسخ ملفات الموارد
            val assetsDir = File(projectDir, "assets")
            assetsDir.mkdirs()
            copyDirectory(File(templateDir, "assets"), assetsDir)
            
            // تحديث ملف pubspec.yaml لإضافة التبعيات والموارد
            val pubspecFile = File(projectDir, "pubspec.yaml")
            if (pubspecFile.exists()) {
                var content = pubspecFile.readText()
                
                // إضافة تبعيات القائمة
                if (!content.contains("http:")) {
                    val dependenciesIndex = content.indexOf("dependencies:")
                    if (dependenciesIndex != -1) {
                        val insertIndex = content.indexOf("\n", dependenciesIndex) + 1
                        val newDependencies = """
                          http: ^0.13.5
                          cached_network_image: ^3.2.3
                        """.trimIndent()
                        
                        content = content.substring(0, insertIndex) + newDependencies + "\n" + content.substring(insertIndex)
                    }
                }
                
                // إضافة الموارد
                if (!content.contains("assets:")) {
                    val flutterIndex = content.indexOf("flutter:")
                    if (flutterIndex != -1) {
                        val insertIndex = content.indexOf("\n", flutterIndex) + 1
                        val assetsSection = """
                          assets:
                            - assets/
                        """.trimIndent()
                        
                        content = content.substring(0, insertIndex) + assetsSection + "\n" + content.substring(insertIndex)
                    }
                }
                
                pubspecFile.writeText(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying list template", e)
            throw IOException("فشل تطبيق قالب القائمة: ${e.message}")
        }
    }
    
    /**
     * استخراج قالب من الأصول
     */
    private fun extractTemplateFromAssets(templateId: String, targetDir: File) {
        try {
            // إنشاء الدليل الهدف
            targetDir.mkdirs()
            
            // استخراج ملفات القالب من الأصول
            val assetManager = context.assets
            val templatePath = "frameworks/flutter/$templateId"
            val templateFiles = assetManager.list(templatePath) ?: return
            
            for (fileName in templateFiles) {
                val assetFile = "$templatePath/$fileName"
                val targetFile = File(targetDir, fileName)
                
                // التحقق مما إذا كان الملف دليلًا
                val subFiles = assetManager.list(assetFile)
                if (subFiles != null && subFiles.isNotEmpty()) {
                    // إذا كان دليلًا، استخراج محتوياته بشكل متكرر
                    extractTemplateFromAssets("$templateId/$fileName", targetFile)
                } else {
                    // إذا كان ملفًا، نسخه إلى الدليل الهدف
                    assetManager.open(assetFile).use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting template from assets", e)
            throw IOException("فشل استخراج القالب: ${e.message}")
        }
    }
    
    /**
     * نسخ دليل بالكامل
     */
    private fun copyDirectory(source: File, target: File) {
        if (!target.exists()) {
            target.mkdirs()
        }
        
        source.listFiles()?.forEach { file ->
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                copyDirectory(file, targetFile)
            } else {
                file.inputStream().use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}