package com.mobileide.aide

import com.mobileide.compiler.model.Project
import com.mobileide.editor.EditorService
import com.mobileide.editor.model.EditorFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

/**
 * اختبارات وحدة لخدمة المحرر
 */
class EditorServiceTest {
    
    private lateinit var editorService: EditorService
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
        
        // إنشاء خدمة المحرر
        editorService = EditorService(mock(android.content.Context::class.java))
    }
    
    @Test
    fun `test open project`() = runBlocking {
        // إنشاء ملف اختبار في الدليل المؤقت
        val testFile = File(tempDir, "test.java")
        testFile.writeText("public class Test {}")
        
        // فتح المشروع
        val result = editorService.openProject(mockProject)
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertTrue(result.message.isNotEmpty())
    }
    
    @Test
    fun `test open file`() = runBlocking {
        // إنشاء ملف اختبار في الدليل المؤقت
        val testFile = File(tempDir, "test.java")
        val testContent = "public class Test {}"
        testFile.writeText(testContent)
        
        // فتح الملف
        val result = editorService.openFile(testFile)
        
        // التحقق من النتيجة
        assertTrue(result.success)
        assertEquals(testContent, result.data?.content)
    }
    
    @Test
    fun `test save file`() = runBlocking {
        // إنشاء ملف اختبار في الدليل المؤقت
        val testFile = File(tempDir, "test.java")
        testFile.writeText("public class Test {}")
        
        // فتح الملف
        val openResult = editorService.openFile(testFile)
        val editorFile = openResult.data as EditorFile
        
        // تعديل المحتوى
        val newContent = "public class TestModified {}"
        val modifiedFile = editorFile.copy(content = newContent)
        
        // حفظ الملف
        val saveResult = editorService.saveFile(modifiedFile)
        
        // التحقق من النتيجة
        assertTrue(saveResult.success)
        assertEquals(newContent, testFile.readText())
    }
    
    @Test
    fun `test syntax highlighting`() = runBlocking {
        // إنشاء ملف اختبار في الدليل المؤقت
        val testFile = File(tempDir, "test.java")
        val testContent = "public class Test {}"
        testFile.writeText(testContent)
        
        // فتح الملف
        val openResult = editorService.openFile(testFile)
        val editorFile = openResult.data as EditorFile
        
        // تطبيق تمييز الصيغة
        val highlightResult = editorService.applySyntaxHighlighting(editorFile)
        
        // التحقق من النتيجة
        assertTrue(highlightResult.success)
        assertTrue((highlightResult.data as String).contains("public"))
    }
    
    @Test
    fun `test auto completion`() = runBlocking {
        // إنشاء ملف اختبار في الدليل المؤقت
        val testFile = File(tempDir, "test.java")
        val testContent = "public class Test { public void main() { Sys } }"
        testFile.writeText(testContent)
        
        // فتح الملف
        val openResult = editorService.openFile(testFile)
        val editorFile = openResult.data as EditorFile
        
        // طلب الإكمال التلقائي
        val completionResult = editorService.getAutoCompleteSuggestions(editorFile, "Sys", 42)
        
        // التحقق من النتيجة
        assertTrue(completionResult.success)
        assertTrue((completionResult.data as List<*>).any { it.toString().contains("System") })
    }
}