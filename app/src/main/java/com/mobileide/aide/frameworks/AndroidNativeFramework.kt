package com.mobileide.aide.frameworks

import android.content.Context
import android.util.Log
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Properties

/**
 * إطار عمل أندرويد الأصلي (Java/Kotlin)
 */
class AndroidNativeFramework(
    private val context: Context
) : Framework {
    
    companion object {
        private const val TAG = "AndroidNativeFramework"
    }
    
    override fun getType(): FrameworkType = FrameworkType.ANDROID_NATIVE
    
    override fun getName(): String = "Android Native"
    
    override fun getDescription(): String = "تطوير تطبيقات أندرويد الأصلية باستخدام Java أو Kotlin"
    
    override fun getWebsiteUrl(): String = "https://developer.android.com"
    
    override fun getDocumentationUrl(): String = "https://developer.android.com/docs"
    
    /**
     * التحقق من حالة تثبيت إطار العمل
     * إطار عمل أندرويد الأصلي متوفر دائمًا في التطبيق
     */
    override suspend fun checkInstallation(): InstallationStatus = withContext(Dispatchers.IO) {
        return@withContext InstallationStatus.INSTALLED
    }
    
    /**
     * تثبيت إطار العمل
     * إطار عمل أندرويد الأصلي متوفر دائمًا في التطبيق
     */
    override suspend fun install(): InstallationResult = withContext(Dispatchers.IO) {
        return@withContext InstallationResult(true, "إطار عمل أندرويد الأصلي متوفر بالفعل", "1.0.0")
    }
    
    /**
     * إلغاء تثبيت إطار العمل
     * لا يمكن إلغاء تثبيت إطار عمل أندرويد الأصلي
     */
    override suspend fun uninstall(): Boolean = withContext(Dispatchers.IO) {
        return@withContext false
    }
    
    /**
     * الحصول على قوالب المشاريع المتاحة
     */
    override suspend fun getTemplates(): List<ProjectTemplate> = withContext(Dispatchers.IO) {
        val templates = mutableListOf<ProjectTemplate>()
        
        // قالب تطبيق أندرويد فارغ
        templates.add(
            ProjectTemplate(
                id = "android_empty",
                name = "تطبيق أندرويد فارغ",
                description = "تطبيق أندرويد فارغ مع نشاط واحد",
                frameworkType = FrameworkType.ANDROID_NATIVE,
                path = "templates/android/empty",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                supportedLanguages = listOf("Java", "Kotlin")
            )
        )
        
        // قالب تطبيق أندرويد مع التنقل السفلي
        templates.add(
            ProjectTemplate(
                id = "android_bottom_navigation",
                name = "تطبيق مع تنقل سفلي",
                description = "تطبيق أندرويد مع شريط تنقل سفلي وعدة شاشات",
                frameworkType = FrameworkType.ANDROID_NATIVE,
                path = "templates/android/bottom_navigation",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                supportedLanguages = listOf("Java", "Kotlin")
            )
        )
        
        // قالب تطبيق أندرويد مع قائمة جانبية
        templates.add(
            ProjectTemplate(
                id = "android_navigation_drawer",
                name = "تطبيق مع قائمة جانبية",
                description = "تطبيق أندرويد مع قائمة تنقل جانبية",
                frameworkType = FrameworkType.ANDROID_NATIVE,
                path = "templates/android/navigation_drawer",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                supportedLanguages = listOf("Java", "Kotlin")
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
            // إنشاء دليل المشروع
            val projectDir = File(location, name)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }
            
            // نسخ ملفات القالب
            val templateDir = File(context.filesDir, "templates/android/$templateId")
            if (!templateDir.exists()) {
                // استخراج القالب من الأصول
                extractTemplateFromAssets(templateId, templateDir)
            }
            
            // نسخ ملفات القالب إلى دليل المشروع
            copyDirectory(templateDir, projectDir)
            
            // تخصيص ملفات المشروع
            customizeProjectFiles(projectDir, name, packageName)
            
            // إنشاء كائن المشروع
            val project = Project(
                id = 0,
                name = name,
                packageName = packageName,
                path = projectDir.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            
            // تعيين نوع إطار العمل
            project.frameworkType = FrameworkType.ANDROID_NATIVE
            
            return@withContext project
        } catch (e: Exception) {
            Log.e(TAG, "Error creating project", e)
            throw IOException("فشل إنشاء المشروع: ${e.message}")
        }
    }
    
    /**
     * بناء مشروع
     */
    override suspend fun buildProject(project: Project): BuildResult = withContext(Dispatchers.IO) {
        try {
            // تنفيذ أمر Gradle لبناء المشروع
            val projectDir = File(project.path)
            val gradlewFile = File(projectDir, "gradlew")
            
            // التأكد من أن ملف gradlew قابل للتنفيذ
            if (!gradlewFile.canExecute()) {
                gradlewFile.setExecutable(true)
            }
            
            // تنفيذ أمر البناء
            val process = ProcessBuilder()
                .command("./gradlew", "assembleDebug")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            // قراءة الإخراج
            val output = process.inputStream.bufferedReader().use { it.readText() }
            
            // انتظار انتهاء العملية
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // البحث عن ملف APK الناتج
                val apkDir = File(projectDir, "app/build/outputs/apk/debug")
                val apkFile = apkDir.listFiles()?.firstOrNull { it.name.endsWith(".apk") }
                
                if (apkFile != null) {
                    return@withContext BuildResult(true, apkFile, "تم بناء المشروع بنجاح")
                } else {
                    return@withContext BuildResult(false, null, "تم بناء المشروع ولكن لم يتم العثور على ملف APK")
                }
            } else {
                return@withContext BuildResult(false, null, "فشل بناء المشروع: $output")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building project", e)
            return@withContext BuildResult(false, null, "فشل بناء المشروع: ${e.message}")
        }
    }
    
    /**
     * تشغيل مشروع
     */
    override suspend fun runProject(project: Project): RunResult = withContext(Dispatchers.IO) {
        try {
            // بناء المشروع أولاً
            val buildResult = buildProject(project)
            if (!buildResult.success || buildResult.outputFile == null) {
                return@withContext RunResult(false, buildResult.message)
            }
            
            // تثبيت التطبيق على الجهاز
            val apkFile = buildResult.outputFile
            val installProcess = ProcessBuilder()
                .command("adb", "install", "-r", apkFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            // قراءة الإخراج
            val installOutput = installProcess.inputStream.bufferedReader().use { it.readText() }
            
            // انتظار انتهاء العملية
            val installExitCode = installProcess.waitFor()
            
            if (installExitCode != 0) {
                return@withContext RunResult(false, "فشل تثبيت التطبيق: $installOutput")
            }
            
            // تشغيل التطبيق
            val runProcess = ProcessBuilder()
                .command("adb", "shell", "am", "start", "-n", "${project.packageName}/.MainActivity")
                .redirectErrorStream(true)
                .start()
            
            // قراءة الإخراج
            val runOutput = runProcess.inputStream.bufferedReader().use { it.readText() }
            
            // انتظار انتهاء العملية
            val runExitCode = runProcess.waitFor()
            
            return@withContext if (runExitCode == 0) {
                RunResult(true, "تم تشغيل التطبيق بنجاح")
            } else {
                RunResult(false, "فشل تشغيل التطبيق: $runOutput")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running project", e)
            return@withContext RunResult(false, "فشل تشغيل المشروع: ${e.message}")
        }
    }
    
    /**
     * تصحيح مشروع
     */
    override suspend fun debugProject(project: Project): DebugResult = withContext(Dispatchers.IO) {
        try {
            // بناء المشروع أولاً
            val buildResult = buildProject(project)
            if (!buildResult.success || buildResult.outputFile == null) {
                return@withContext DebugResult(false, buildResult.message)
            }
            
            // تثبيت التطبيق على الجهاز
            val apkFile = buildResult.outputFile
            val installProcess = ProcessBuilder()
                .command("adb", "install", "-r", apkFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            // قراءة الإخراج
            val installOutput = installProcess.inputStream.bufferedReader().use { it.readText() }
            
            // انتظار انتهاء العملية
            val installExitCode = installProcess.waitFor()
            
            if (installExitCode != 0) {
                return@withContext DebugResult(false, "فشل تثبيت التطبيق: $installOutput")
            }
            
            // تشغيل التطبيق في وضع التصحيح
            val debugProcess = ProcessBuilder()
                .command("adb", "shell", "am", "start", "-D", "-n", "${project.packageName}/.MainActivity")
                .redirectErrorStream(true)
                .start()
            
            // قراءة الإخراج
            val debugOutput = debugProcess.inputStream.bufferedReader().use { it.readText() }
            
            // انتظار انتهاء العملية
            val debugExitCode = debugProcess.waitFor()
            
            return@withContext if (debugExitCode == 0) {
                DebugResult(true, "تم بدء تصحيح التطبيق بنجاح")
            } else {
                DebugResult(false, "فشل بدء تصحيح التطبيق: $debugOutput")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error debugging project", e)
            return@withContext DebugResult(false, "فشل بدء تصحيح المشروع: ${e.message}")
        }
    }
    
    /**
     * الحصول على إصدار إطار العمل المثبت
     */
    override suspend fun getInstalledVersion(): String? = withContext(Dispatchers.IO) {
        return@withContext "1.0.0" // إطار عمل أندرويد الأصلي متوفر دائمًا
    }
    
    /**
     * الحصول على أحدث إصدار متاح لإطار العمل
     */
    override suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
        return@withContext "1.0.0" // إطار عمل أندرويد الأصلي متوفر دائمًا
    }
    
    /**
     * التحقق من وجود تحديثات لإطار العمل
     */
    override suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        return@withContext UpdateInfo(false, "1.0.0", "1.0.0")
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
            val templatePath = "frameworks/android/$templateId"
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
    
    /**
     * تخصيص ملفات المشروع
     */
    private fun customizeProjectFiles(projectDir: File, projectName: String, packageName: String) {
        try {
            // تحديث ملف settings.gradle
            val settingsGradleFile = File(projectDir, "settings.gradle")
            if (settingsGradleFile.exists()) {
                var content = settingsGradleFile.readText()
                content = content.replace("{{PROJECT_NAME}}", projectName)
                settingsGradleFile.writeText(content)
            }
            
            // تحديث ملف build.gradle
            val buildGradleFile = File(projectDir, "app/build.gradle")
            if (buildGradleFile.exists()) {
                var content = buildGradleFile.readText()
                content = content.replace("{{PACKAGE_NAME}}", packageName)
                buildGradleFile.writeText(content)
            }
            
            // تحديث ملف AndroidManifest.xml
            val manifestFile = File(projectDir, "app/src/main/AndroidManifest.xml")
            if (manifestFile.exists()) {
                var content = manifestFile.readText()
                content = content.replace("{{PACKAGE_NAME}}", packageName)
                manifestFile.writeText(content)
            }
            
            // تحديث ملف strings.xml
            val stringsFile = File(projectDir, "app/src/main/res/values/strings.xml")
            if (stringsFile.exists()) {
                var content = stringsFile.readText()
                content = content.replace("{{PROJECT_NAME}}", projectName)
                stringsFile.writeText(content)
            }
            
            // إنشاء هيكل الحزمة
            val packagePath = packageName.replace(".", "/")
            val srcDir = File(projectDir, "app/src/main/java")
            val packageDir = File(srcDir, packagePath)
            packageDir.mkdirs()
            
            // نقل ملفات Java/Kotlin إلى الحزمة الصحيحة
            val defaultPackageDir = File(srcDir, "com/example/app")
            if (defaultPackageDir.exists()) {
                defaultPackageDir.listFiles()?.forEach { file ->
                    val targetFile = File(packageDir, file.name)
                    if (file.isFile) {
                        // تحديث محتوى الملف لاستخدام الحزمة الصحيحة
                        var content = file.readText()
                        content = content.replace("package com.example.app", "package $packageName")
                        targetFile.writeText(content)
                    } else {
                        copyDirectory(file, targetFile)
                    }
                }
                
                // حذف الحزمة الافتراضية
                defaultPackageDir.deleteRecursively()
            }
            
            // تحديث ملف gradle.properties
            val gradlePropertiesFile = File(projectDir, "gradle.properties")
            if (gradlePropertiesFile.exists()) {
                val properties = Properties()
                gradlePropertiesFile.inputStream().use { properties.load(it) }
                properties.setProperty("android.useAndroidX", "true")
                properties.setProperty("android.enableJetifier", "true")
                FileOutputStream(gradlePropertiesFile).use { properties.store(it, null) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error customizing project files", e)
            throw IOException("فشل تخصيص ملفات المشروع: ${e.message}")
        }
    }
}