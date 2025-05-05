package com.mobileide.aide

import android.content.Context
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.compiler.CompilerService
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

/**
 * اختبارات وحدة لخدمة المترجم
 */
class CompilerServiceTest {
    
    private lateinit var compilerService: CompilerService
    private lateinit var mockContext: Context
    private lateinit var mockProject: Project
    private lateinit var tempDir: File
    
    @Before
    fun setup() {
        // إنشاء دليل مؤقت للاختبارات
        tempDir = createTempDir()
        
        // إنشاء مشروع وهمي
        mockProject = mock(Project::class.java)
        `when`(mockProject.path).thenReturn(tempDir.absolutePath)
        `when`(mockProject.name).thenReturn("TestProject")
        
        // إنشاء سياق وهمي
        mockContext = mock(Context::class.java)
        
        // إنشاء خدمة المترجم
        compilerService = CompilerService(mockContext)
    }
    
    @Test
    fun `test initialize for project`() = runBlocking {
        // إنشاء هيكل المشروع
        val srcDir = File(tempDir, "src/main/java")
        srcDir.mkdirs()
        
        val javaFile = File(srcDir, "MainActivity.java")
        javaFile.writeText("""
            package com.test;
            
            import android.app.Activity;
            import android.os.Bundle;
            
            public class MainActivity extends Activity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                }
            }
        """.trimIndent())
        
        // تهيئة المترجم للمشروع
        val result = compilerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // التحقق من النتيجة
        assertTrue(result.success)
    }
    
    @Test
    fun `test build project`() = runBlocking {
        // إنشاء هيكل المشروع
        val srcDir = File(tempDir, "src/main/java")
        srcDir.mkdirs()
        
        val resDir = File(tempDir, "src/main/res/layout")
        resDir.mkdirs()
        
        val manifestFile = File(tempDir, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.test">
                <application
                    android:label="Test App">
                    <activity android:name=".MainActivity">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())
        
        val javaFile = File(srcDir, "MainActivity.java")
        javaFile.writeText("""
            package com.test;
            
            import android.app.Activity;
            import android.os.Bundle;
            
            public class MainActivity extends Activity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                }
            }
        """.trimIndent())
        
        val layoutFile = File(resDir, "activity_main.xml")
        layoutFile.writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical">
            </LinearLayout>
        """.trimIndent())
        
        // بناء المشروع
        val result = compilerService.buildProject(mockProject)
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertNotNull(result.outputFile)
    }
    
    @Test
    fun `test build project for debugging`() = runBlocking {
        // إنشاء هيكل المشروع
        val srcDir = File(tempDir, "src/main/java")
        srcDir.mkdirs()
        
        val resDir = File(tempDir, "src/main/res/layout")
        resDir.mkdirs()
        
        val manifestFile = File(tempDir, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.test">
                <application
                    android:label="Test App">
                    <activity android:name=".MainActivity">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())
        
        val javaFile = File(srcDir, "MainActivity.java")
        javaFile.writeText("""
            package com.test;
            
            import android.app.Activity;
            import android.os.Bundle;
            
            public class MainActivity extends Activity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                }
            }
        """.trimIndent())
        
        // بناء المشروع للتصحيح
        val result = compilerService.buildProjectForDebugging(mockProject)
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertNotNull(result.outputFile)
    }
    
    @Test
    fun `test detect framework type`() = runBlocking {
        // إنشاء ملف pubspec.yaml لمشروع Flutter
        val pubspecFile = File(tempDir, "pubspec.yaml")
        pubspecFile.writeText("""
            name: flutter_app
            description: A new Flutter project.
            version: 1.0.0+1
            
            environment:
              sdk: ">=2.12.0 <3.0.0"
            
            dependencies:
              flutter:
                sdk: flutter
        """.trimIndent())
        
        // بناء المشروع
        val result = compilerService.buildProject(mockProject)
        
        // التحقق من أن المترجم اكتشف أن المشروع هو مشروع Flutter
        assertEquals(FrameworkType.FLUTTER, result.frameworkType)
    }
}