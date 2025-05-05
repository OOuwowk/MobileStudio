package com.mobileide.aide

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mobileide.aide.frameworks.FrameworkManager
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.aide.integration.ComponentIntegrator
import com.mobileide.compiler.CompilerService
import com.mobileide.compiler.model.Project
import com.mobileide.debugger.DebuggerService
import com.mobileide.designer.DesignerService
import com.mobileide.editor.EditorService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.io.File

/**
 * اختبارات تكامل للتطبيق
 */
@RunWith(AndroidJUnit4::class)
class IntegrationTest {
    
    private lateinit var context: Context
    private lateinit var componentIntegrator: ComponentIntegrator
    private lateinit var editorService: EditorService
    private lateinit var designerService: DesignerService
    private lateinit var compilerService: CompilerService
    private lateinit var debuggerService: DebuggerService
    private lateinit var frameworkManager: FrameworkManager
    private lateinit var testProject: Project
    private lateinit var tempDir: File
    
    @Before
    fun setup() {
        // الحصول على السياق
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // إنشاء دليل مؤقت للاختبارات
        tempDir = context.getDir("test_projects", Context.MODE_PRIVATE)
        
        // إنشاء الخدمات
        editorService = EditorService(context)
        designerService = DesignerService(context)
        compilerService = CompilerService(context)
        debuggerService = DebuggerService(context)
        frameworkManager = FrameworkManager(context)
        
        // إنشاء مكامل المكونات
        componentIntegrator = ComponentIntegrator(
            context,
            editorService,
            designerService,
            compilerService,
            debuggerService,
            frameworkManager,
            mock(com.mobileide.aide.analyzer.StaticCodeAnalyzer::class.java),
            mock(com.mobileide.aide.git.GitService::class.java)
        )
        
        // إنشاء مشروع اختبار
        val projectDir = File(tempDir, "TestProject")
        projectDir.mkdirs()
        
        testProject = Project(
            id = 1,
            name = "TestProject",
            packageName = "com.test",
            path = projectDir.absolutePath,
            createdAt = System.currentTimeMillis()
        )
        
        // إنشاء هيكل المشروع
        createTestProjectStructure(projectDir)
    }
    
    @Test
    fun testFullWorkflow() = runBlocking {
        // 1. فتح المشروع
        val openResult = componentIntegrator.openProject(testProject)
        assertTrue("فشل فتح المشروع: ${openResult.message}", openResult.success)
        
        // 2. تحليل المشروع
        val analysisResult = componentIntegrator.analyzeProject(testProject)
        assertTrue("فشل تحليل المشروع: ${analysisResult.message}", analysisResult.success)
        
        // 3. بناء المشروع
        val buildResult = componentIntegrator.buildProject(testProject)
        assertTrue("فشل بناء المشروع: ${buildResult.message}", buildResult.success)
        
        // 4. تصحيح المشروع
        val debugResult = componentIntegrator.debugProject(testProject)
        assertTrue("فشل تصحيح المشروع: ${debugResult.message}", debugResult.success)
        
        // 5. تشغيل المشروع
        val runResult = componentIntegrator.runProject(testProject)
        assertTrue("فشل تشغيل المشروع: ${runResult.message}", runResult.success)
    }
    
    @Test
    fun testEditorIntegration() = runBlocking {
        // فتح المشروع
        componentIntegrator.openProject(testProject)
        
        // فتح ملف في المحرر
        val mainActivityFile = File(testProject.path, "src/main/java/com/test/MainActivity.java")
        val openFileResult = editorService.openFile(mainActivityFile)
        assertTrue("فشل فتح الملف: ${openFileResult.message}", openFileResult.success)
        
        // تعديل الملف
        val editorFile = openFileResult.data!!
        val modifiedContent = editorFile.content.replace("Test App", "Modified App")
        val modifiedFile = editorFile.copy(content = modifiedContent)
        
        // حفظ الملف
        val saveResult = editorService.saveFile(modifiedFile)
        assertTrue("فشل حفظ الملف: ${saveResult.message}", saveResult.success)
        
        // التحقق من أن التغييرات تم حفظها
        val reopenResult = editorService.openFile(mainActivityFile)
        assertTrue(reopenResult.data!!.content.contains("Modified App"))
    }
    
    @Test
    fun testDesignerIntegration() = runBlocking {
        // فتح المشروع
        componentIntegrator.openProject(testProject)
        
        // فتح ملف تخطيط في المصمم
        val layoutFile = File(testProject.path, "src/main/res/layout/activity_main.xml")
        val openLayoutResult = designerService.openLayout(layoutFile)
        assertTrue("فشل فتح ملف التخطيط: ${openLayoutResult.message}", openLayoutResult.success)
        
        // إضافة عنصر إلى التخطيط
        val designerLayout = openLayoutResult.data!!
        val modifiedLayout = designerService.addComponent(designerLayout, "TextView", 100, 100)
        assertTrue("فشل إضافة عنصر إلى التخطيط", modifiedLayout.success)
        
        // حفظ التخطيط
        val saveResult = designerService.saveLayout(modifiedLayout.data!!)
        assertTrue("فشل حفظ التخطيط: ${saveResult.message}", saveResult.success)
        
        // التحقق من أن التغييرات تم حفظها
        val reopenResult = designerService.openLayout(layoutFile)
        assertTrue(reopenResult.data!!.content.contains("TextView"))
    }
    
    @Test
    fun testCompilerIntegration() = runBlocking {
        // فتح المشروع
        componentIntegrator.openProject(testProject)
        
        // تهيئة المترجم للمشروع
        val initResult = compilerService.initializeForProject(testProject, FrameworkType.ANDROID_NATIVE)
        assertTrue("فشل تهيئة المترجم: ${initResult.message}", initResult.success)
        
        // بناء المشروع
        val buildResult = compilerService.buildProject(testProject)
        assertTrue("فشل بناء المشروع: ${buildResult.message}", buildResult.success)
        
        // التحقق من وجود ملف APK
        assertTrue("لم يتم إنشاء ملف APK", buildResult.outputFile != null && buildResult.outputFile!!.exists())
    }
    
    @Test
    fun testDebuggerIntegration() = runBlocking {
        // فتح المشروع
        componentIntegrator.openProject(testProject)
        
        // تهيئة المصحح للمشروع
        val initResult = debuggerService.initializeForProject(testProject, FrameworkType.ANDROID_NATIVE)
        assertTrue("فشل تهيئة المصحح: ${initResult.message}", initResult.success)
        
        // وضع نقطة توقف
        val mainActivityFile = File(testProject.path, "src/main/java/com/test/MainActivity.java")
        val breakpointResult = debuggerService.setBreakpoint(mainActivityFile.absolutePath, 8)
        assertTrue("فشل وضع نقطة توقف: ${breakpointResult.message}", breakpointResult.success)
        
        // إزالة نقطة التوقف
        val removeResult = debuggerService.removeBreakpoint(mainActivityFile.absolutePath, 8)
        assertTrue("فشل إزالة نقطة التوقف: ${removeResult.message}", removeResult.success)
    }
    
    /**
     * إنشاء هيكل مشروع اختبار
     */
    private fun createTestProjectStructure(projectDir: File) {
        // إنشاء هيكل الدلائل
        val srcDir = File(projectDir, "src/main/java/com/test")
        srcDir.mkdirs()
        
        val resDir = File(projectDir, "src/main/res/layout")
        resDir.mkdirs()
        
        // إنشاء ملف AndroidManifest.xml
        val manifestFile = File(projectDir, "src/main/AndroidManifest.xml")
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
        
        // إنشاء ملف MainActivity.java
        val mainActivityFile = File(srcDir, "MainActivity.java")
        mainActivityFile.writeText("""
            package com.test;
            
            import android.app.Activity;
            import android.os.Bundle;
            
            public class MainActivity extends Activity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_main);
                }
            }
        """.trimIndent())
        
        // إنشاء ملف activity_main.xml
        val layoutFile = File(resDir, "activity_main.xml")
        layoutFile.writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical">
            </LinearLayout>
        """.trimIndent())
    }
}