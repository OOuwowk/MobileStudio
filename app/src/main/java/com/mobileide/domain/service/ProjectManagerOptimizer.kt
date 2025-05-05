package com.mobileide.domain.service

import android.content.Context
import com.mobileide.domain.model.Project
import com.mobileide.domain.repository.ProjectRepository
import com.mobileide.utils.FileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to optimize project management operations
 */
@Singleton
class ProjectManagerOptimizer @Inject constructor(
    private val context: Context,
    private val projectRepository: ProjectRepository,
    private val fileManager: FileManager
) {
    /**
     * Optimizes project scanning for better performance
     */
    fun optimizeProjectScanning() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Implement incremental file scanning
                val projectsFlow = projectRepository.getAllProjects()
                projectsFlow.collect { projects ->
                    projects.forEach { project ->
                        // Scan project files asynchronously
                        launch {
                            scanProjectFiles(project)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error optimizing project scanning")
            }
        }
    }
    
    /**
     * Scans project files incrementally
     */
    private suspend fun scanProjectFiles(project: Project) = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return@withContext
            }
            
            // Create a cache file to track last scan time
            val cacheDir = File(context.cacheDir, "project_scan_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val cacheFile = File(cacheDir, "${project.id}_scan.txt")
            val lastScanTime = if (cacheFile.exists()) {
                cacheFile.readText().toLongOrNull() ?: 0L
            } else {
                0L
            }
            
            val currentTime = System.currentTimeMillis()
            
            // Only scan files modified since last scan
            val modifiedFiles = mutableListOf<File>()
            scanForModifiedFiles(projectDir, lastScanTime, modifiedFiles)
            
            Timber.d("Found ${modifiedFiles.size} modified files in project ${project.name}")
            
            // Update cache file with current scan time
            cacheFile.writeText(currentTime.toString())
            
        } catch (e: Exception) {
            Timber.e(e, "Error scanning project files")
        }
    }
    
    /**
     * Recursively scans for modified files
     */
    private fun scanForModifiedFiles(dir: File, lastScanTime: Long, modifiedFiles: MutableList<File>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Skip common build and cache directories
                if (!file.name.startsWith(".") && 
                    file.name != "build" && 
                    file.name != ".gradle" && 
                    file.name != "generated") {
                    scanForModifiedFiles(file, lastScanTime, modifiedFiles)
                }
            } else {
                // Check if file was modified since last scan
                if (file.lastModified() > lastScanTime) {
                    modifiedFiles.add(file)
                }
            }
        }
    }
    
    /**
     * Sets up additional project templates
     */
    fun setupProjectTemplates() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val templatesDir = File(context.filesDir, "templates")
                if (!templatesDir.exists()) {
                    templatesDir.mkdirs()
                    
                    // Create basic Android app template
                    createBasicAndroidAppTemplate(templatesDir)
                    
                    // Create Kotlin Android app template
                    createKotlinAndroidAppTemplate(templatesDir)
                    
                    // Create Android library template
                    createAndroidLibraryTemplate(templatesDir)
                    
                    Timber.d("Created additional project templates")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error setting up project templates")
            }
        }
    }
    
    /**
     * Creates a basic Android app template
     */
    private fun createBasicAndroidAppTemplate(templatesDir: File) {
        val basicAppTemplate = File(templatesDir, "basic_android_app")
        basicAppTemplate.mkdirs()
        
        // Create template files
        File(basicAppTemplate, "MainActivity.java").writeText("""
            package {{PACKAGE_NAME}};
            
            import android.os.Bundle;
            import androidx.appcompat.app.AppCompatActivity;
            
            public class MainActivity extends AppCompatActivity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_main);
                }
            }
        """.trimIndent())
        
        File(basicAppTemplate, "activity_main.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <androidx.constraintlayout.widget.ConstraintLayout 
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Hello, {{PROJECT_NAME}}!"
                    app:layout_constraintBottom_toBottomOf="parent"
                    app:layout_constraintEnd_toEndOf="parent"
                    app:layout_constraintStart_toStartOf="parent"
                    app:layout_constraintTop_toTopOf="parent" />
                    
            </androidx.constraintlayout.widget.ConstraintLayout>
        """.trimIndent())
        
        File(basicAppTemplate, "AndroidManifest.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="{{PACKAGE_NAME}}">
                
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="{{PROJECT_NAME}}"
                    android:roundIcon="@mipmap/ic_launcher_round"
                    android:supportsRtl="true"
                    android:theme="@style/AppTheme">
                    <activity android:name=".MainActivity">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
                
            </manifest>
        """.trimIndent())
        
        File(basicAppTemplate, "build.gradle").writeText("""
            apply plugin: 'com.android.application'
            
            android {
                compileSdkVersion 30
                
                defaultConfig {
                    applicationId "{{PACKAGE_NAME}}"
                    minSdkVersion 21
                    targetSdkVersion 30
                    versionCode 1
                    versionName "1.0"
                }
                
                buildTypes {
                    release {
                        minifyEnabled false
                        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                    }
                }
            }
            
            dependencies {
                implementation 'androidx.appcompat:appcompat:1.3.0'
                implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
            }
        """.trimIndent())
    }
    
    /**
     * Creates a Kotlin Android app template
     */
    private fun createKotlinAndroidAppTemplate(templatesDir: File) {
        val kotlinAppTemplate = File(templatesDir, "kotlin_android_app")
        kotlinAppTemplate.mkdirs()
        
        // Create template files
        File(kotlinAppTemplate, "MainActivity.kt").writeText("""
            package {{PACKAGE_NAME}}
            
            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity
            
            class MainActivity : AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContentView(R.layout.activity_main)
                }
            }
        """.trimIndent())
        
        File(kotlinAppTemplate, "activity_main.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <androidx.constraintlayout.widget.ConstraintLayout 
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Hello, {{PROJECT_NAME}}!"
                    app:layout_constraintBottom_toBottomOf="parent"
                    app:layout_constraintEnd_toEndOf="parent"
                    app:layout_constraintStart_toStartOf="parent"
                    app:layout_constraintTop_toTopOf="parent" />
                    
            </androidx.constraintlayout.widget.ConstraintLayout>
        """.trimIndent())
        
        File(kotlinAppTemplate, "AndroidManifest.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="{{PACKAGE_NAME}}">
                
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="{{PROJECT_NAME}}"
                    android:roundIcon="@mipmap/ic_launcher_round"
                    android:supportsRtl="true"
                    android:theme="@style/AppTheme">
                    <activity android:name=".MainActivity">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
                
            </manifest>
        """.trimIndent())
        
        File(kotlinAppTemplate, "build.gradle").writeText("""
            apply plugin: 'com.android.application'
            apply plugin: 'kotlin-android'
            
            android {
                compileSdkVersion 30
                
                defaultConfig {
                    applicationId "{{PACKAGE_NAME}}"
                    minSdkVersion 21
                    targetSdkVersion 30
                    versionCode 1
                    versionName "1.0"
                }
                
                buildTypes {
                    release {
                        minifyEnabled false
                        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                    }
                }
                
                kotlinOptions {
                    jvmTarget = '1.8'
                }
            }
            
            dependencies {
                implementation 'androidx.core:core-ktx:1.6.0'
                implementation 'androidx.appcompat:appcompat:1.3.0'
                implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
            }
        """.trimIndent())
    }
    
    /**
     * Creates an Android library template
     */
    private fun createAndroidLibraryTemplate(templatesDir: File) {
        val libraryTemplate = File(templatesDir, "android_library")
        libraryTemplate.mkdirs()
        
        // Create template files
        File(libraryTemplate, "LibraryClass.java").writeText("""
            package {{PACKAGE_NAME}};
            
            /**
             * Main library class for {{PROJECT_NAME}}
             */
            public class LibraryClass {
                
                /**
                 * Example method
                 * @return A welcome message
                 */
                public String getWelcomeMessage() {
                    return "Welcome to {{PROJECT_NAME}} library!";
                }
            }
        """.trimIndent())
        
        File(libraryTemplate, "AndroidManifest.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="{{PACKAGE_NAME}}">
                
            </manifest>
        """.trimIndent())
        
        File(libraryTemplate, "build.gradle").writeText("""
            apply plugin: 'com.android.library'
            
            android {
                compileSdkVersion 30
                
                defaultConfig {
                    minSdkVersion 21
                    targetSdkVersion 30
                    versionCode 1
                    versionName "1.0"
                    
                    testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
                    consumerProguardFiles "consumer-rules.pro"
                }
                
                buildTypes {
                    release {
                        minifyEnabled false
                        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                    }
                }
            }
            
            dependencies {
                implementation 'androidx.appcompat:appcompat:1.3.0'
                testImplementation 'junit:junit:4.13.2'
                androidTestImplementation 'androidx.test.ext:junit:1.1.3'
            }
        """.trimIndent())
    }
    
    /**
     * Optimizes project file indexing
     */
    fun optimizeProjectIndexing() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create index directory
                val indexDir = File(context.filesDir, "project_index")
                if (!indexDir.exists()) {
                    indexDir.mkdirs()
                }
                
                // Index all projects
                val projects = projectRepository.getAllProjects().collect { projects ->
                    projects.forEach { project ->
                        indexProject(project, indexDir)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error optimizing project indexing")
            }
        }
    }
    
    /**
     * Indexes a project for faster searching
     */
    private suspend fun indexProject(project: Project, indexDir: File) = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return@withContext
            }
            
            val projectIndexDir = File(indexDir, project.id.toString())
            if (!projectIndexDir.exists()) {
                projectIndexDir.mkdirs()
            }
            
            // Create index files for different file types
            val javaFiles = mutableListOf<String>()
            val kotlinFiles = mutableListOf<String>()
            val xmlFiles = mutableListOf<String>()
            val gradleFiles = mutableListOf<String>()
            
            // Scan project files
            scanProjectForIndexing(projectDir, projectDir, javaFiles, kotlinFiles, xmlFiles, gradleFiles)
            
            // Write index files
            File(projectIndexDir, "java_files.idx").writeText(javaFiles.joinToString("\n"))
            File(projectIndexDir, "kotlin_files.idx").writeText(kotlinFiles.joinToString("\n"))
            File(projectIndexDir, "xml_files.idx").writeText(xmlFiles.joinToString("\n"))
            File(projectIndexDir, "gradle_files.idx").writeText(gradleFiles.joinToString("\n"))
            
            Timber.d("Indexed project ${project.name}: ${javaFiles.size} Java files, ${kotlinFiles.size} Kotlin files, ${xmlFiles.size} XML files, ${gradleFiles.size} Gradle files")
            
        } catch (e: Exception) {
            Timber.e(e, "Error indexing project")
        }
    }
    
    /**
     * Scans project files for indexing
     */
    private fun scanProjectForIndexing(
        baseDir: File,
        dir: File,
        javaFiles: MutableList<String>,
        kotlinFiles: MutableList<String>,
        xmlFiles: MutableList<String>,
        gradleFiles: MutableList<String>
    ) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Skip common build and cache directories
                if (!file.name.startsWith(".") && 
                    file.name != "build" && 
                    file.name != ".gradle" && 
                    file.name != "generated") {
                    scanProjectForIndexing(baseDir, file, javaFiles, kotlinFiles, xmlFiles, gradleFiles)
                }
            } else {
                // Get relative path
                val relativePath = file.absolutePath.substring(baseDir.absolutePath.length + 1)
                
                // Categorize file by extension
                when {
                    file.name.endsWith(".java") -> javaFiles.add(relativePath)
                    file.name.endsWith(".kt") || file.name.endsWith(".kts") -> kotlinFiles.add(relativePath)
                    file.name.endsWith(".xml") -> xmlFiles.add(relativePath)
                    file.name.endsWith(".gradle") -> gradleFiles.add(relativePath)
                }
            }
        }
    }
}