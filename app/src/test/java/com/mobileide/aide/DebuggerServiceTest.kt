package com.mobileide.aide

import android.content.Context
import com.mobileide.aide.frameworks.FrameworkType
import com.mobileide.compiler.model.Project
import com.mobileide.debugger.DebuggerService
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
 * اختبارات وحدة لخدمة المصحح
 */
class DebuggerServiceTest {
    
    private lateinit var debuggerService: DebuggerService
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
        
        // إنشاء خدمة المصحح
        debuggerService = DebuggerService(mockContext)
    }
    
    @Test
    fun `test initialize for project`() = runBlocking {
        // تهيئة المصحح للمشروع
        val result = debuggerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // التحقق من النتيجة
        assertTrue(result.success)
    }
    
    @Test
    fun `test set breakpoint`() = runBlocking {
        // تهيئة المصحح للمشروع
        debuggerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // إنشاء ملف اختبار
        val testFile = File(tempDir, "MainActivity.java")
        testFile.writeText("""
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
        
        // وضع نقطة توقف
        val result = debuggerService.setBreakpoint(testFile.absolutePath, 8)
        
        // التحقق من النتيجة
        assertTrue(result.success)
    }
    
    @Test
    fun `test remove breakpoint`() = runBlocking {
        // تهيئة المصحح للمشروع
        debuggerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // إنشاء ملف اختبار
        val testFile = File(tempDir, "MainActivity.java")
        testFile.writeText("""
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
        
        // وضع نقطة توقف
        debuggerService.setBreakpoint(testFile.absolutePath, 8)
        
        // إزالة نقطة التوقف
        val result = debuggerService.removeBreakpoint(testFile.absolutePath, 8)
        
        // التحقق من النتيجة
        assertTrue(result.success)
    }
    
    @Test
    fun `test evaluate expression`() = runBlocking {
        // تهيئة المصحح للمشروع
        debuggerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // تقييم تعبير
        val result = debuggerService.evaluate("1 + 1")
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertEquals("2", result.data?.value)
    }
    
    @Test
    fun `test get variables`() = runBlocking {
        // تهيئة المصحح للمشروع
        debuggerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // الحصول على المتغيرات
        val result = debuggerService.getVariables()
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertNotNull(result.data)
    }
    
    @Test
    fun `test get call stack`() = runBlocking {
        // تهيئة المصحح للمشروع
        debuggerService.initializeForProject(mockProject, FrameworkType.ANDROID_NATIVE)
        
        // الحصول على مكدس الاستدعاء
        val result = debuggerService.getCallStack()
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertNotNull(result.data)
    }
}