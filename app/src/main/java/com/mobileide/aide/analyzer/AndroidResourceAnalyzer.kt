package com.mobileide.aide.analyzer

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * محلل موارد أندرويد (ملفات XML)
 */
class AndroidResourceAnalyzer : CodeAnalyzer {
    
    companion object {
        private const val TAG = "AndroidResourceAnalyzer"
    }
    
    override fun analyzeProject(projectDir: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // البحث عن دليل الموارد
            val resDir = findResourceDirectory(projectDir)
            
            if (resDir != null && resDir.exists()) {
                // تحليل ملفات التخطيط
                val layoutDir = File(resDir, "layout")
                if (layoutDir.exists()) {
                    layoutDir.listFiles()?.filter { it.isFile && it.extension == "xml" }?.forEach { file ->
                        val fileIssues = analyzeFile(file)
                        issues.addAll(fileIssues)
                    }
                }
                
                // تحليل ملفات القيم
                val valuesDir = File(resDir, "values")
                if (valuesDir.exists()) {
                    valuesDir.listFiles()?.filter { it.isFile && it.extension == "xml" }?.forEach { file ->
                        val fileIssues = analyzeFile(file)
                        issues.addAll(fileIssues)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Android resources", e)
        }
        
        return issues
    }
    
    override fun analyzeFile(file: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // التحقق من أن الملف هو XML
            if (file.extension != "xml") {
                return issues
            }
            
            // قراءة ملف XML
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(file)
            
            // تحديد نوع ملف الموارد
            val rootElement = document.documentElement
            
            when (rootElement.tagName) {
                "LinearLayout", "RelativeLayout", "FrameLayout", "ConstraintLayout", "androidx.constraintlayout.widget.ConstraintLayout" -> {
                    analyzeLayoutFile(file, document, issues)
                }
                "resources" -> {
                    analyzeValuesFile(file, document, issues)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Android resource file: ${file.absolutePath}", e)
        }
        
        return issues
    }
    
    override fun getName(): String = "Android Resource Analyzer"
    
    override fun getDescription(): String = "محلل موارد أندرويد (ملفات XML)"
    
    override fun getSupportedFileExtensions(): List<String> = listOf("xml")
    
    /**
     * البحث عن دليل الموارد في المشروع
     */
    private fun findResourceDirectory(dir: File): File? {
        if (dir.isDirectory) {
            // البحث المباشر عن دليل res
            val resDir = File(dir, "res")
            if (resDir.exists() && resDir.isDirectory) {
                return resDir
            }
            
            // البحث في الدلائل الفرعية
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val resourceDir = findResourceDirectory(file)
                    if (resourceDir != null) {
                        return resourceDir
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * تحليل ملف تخطيط
     */
    private fun analyzeLayoutFile(file: File, document: Document, issues: MutableList<CodeIssue>) {
        // تحليل استخدام النصوص الثابتة
        analyzeHardcodedStrings(file, document, issues)
        
        // تحليل استخدام الأبعاد الثابتة
        analyzeHardcodedDimensions(file, document, issues)
        
        // تحليل استخدام الألوان الثابتة
        analyzeHardcodedColors(file, document, issues)
        
        // تحليل استخدام معرفات غير صالحة
        analyzeInvalidIds(file, document, issues)
        
        // تحليل تداخل التخطيطات
        analyzeNestedLayouts(file, document, issues)
        
        // تحليل إمكانية الوصول
        analyzeAccessibility(file, document, issues)
    }
    
    /**
     * تحليل ملف قيم
     */
    private fun analyzeValuesFile(file: File, document: Document, issues: MutableList<CodeIssue>) {
        // تحليل تكرار المعرفات
        analyzeDuplicateIds(file, document, issues)
        
        // تحليل تسمية الموارد
        analyzeResourceNaming(file, document, issues)
    }
    
    /**
     * تحليل استخدام النصوص الثابتة
     */
    private fun analyzeHardcodedStrings(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val elements = document.getElementsByTagName("*")
        
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            
            // التحقق من سمات النص
            val textAttributes = listOf("android:text", "android:hint", "android:contentDescription")
            
            textAttributes.forEach { attribute ->
                if (element.hasAttribute(attribute)) {
                    val value = element.getAttribute(attribute)
                    
                    // التحقق مما إذا كانت القيمة نصًا ثابتًا (ليست مرجعًا)
                    if (value.isNotEmpty() && !value.startsWith("@string/")) {
                        val lineNumber = getLineNumber(file, "$attribute=\"$value\"")
                        
                        issues.add(
                            CodeIssue(
                                message = "نص ثابت في التخطيط: $value",
                                description = "استخدام النصوص الثابتة في ملفات التخطيط يجعل الترجمة صعبة",
                                severity = IssueSeverity.WARNING,
                                type = IssueType.RESOURCES,
                                file = file,
                                line = lineNumber,
                                column = 1,
                                suggestion = "استخدم مرجع سلسلة (@string/resource_name) بدلاً من النص الثابت"
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * تحليل استخدام الأبعاد الثابتة
     */
    private fun analyzeHardcodedDimensions(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val elements = document.getElementsByTagName("*")
        
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            
            // التحقق من سمات الأبعاد
            val dimensionAttributes = listOf(
                "android:layout_width", "android:layout_height",
                "android:padding", "android:paddingLeft", "android:paddingRight",
                "android:paddingTop", "android:paddingBottom",
                "android:margin", "android:marginLeft", "android:marginRight",
                "android:marginTop", "android:marginBottom"
            )
            
            dimensionAttributes.forEach { attribute ->
                if (element.hasAttribute(attribute)) {
                    val value = element.getAttribute(attribute)
                    
                    // التحقق مما إذا كانت القيمة بعدًا ثابتًا (ليست مرجعًا أو "match_parent" أو "wrap_content")
                    if (value.isNotEmpty() && 
                        !value.startsWith("@dimen/") && 
                        value != "match_parent" && 
                        value != "wrap_content" && 
                        value != "0dp" &&
                        value.endsWith("dp") || value.endsWith("px")) {
                        
                        val lineNumber = getLineNumber(file, "$attribute=\"$value\"")
                        
                        issues.add(
                            CodeIssue(
                                message = "بعد ثابت في التخطيط: $value",
                                description = "استخدام الأبعاد الثابتة في ملفات التخطيط يجعل التكيف مع أحجام الشاشات المختلفة صعبًا",
                                severity = IssueSeverity.INFO,
                                type = IssueType.RESOURCES,
                                file = file,
                                line = lineNumber,
                                column = 1,
                                suggestion = "استخدم مرجع بعد (@dimen/resource_name) بدلاً من البعد الثابت"
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * تحليل استخدام الألوان الثابتة
     */
    private fun analyzeHardcodedColors(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val elements = document.getElementsByTagName("*")
        
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            
            // التحقق من سمات الألوان
            val colorAttributes = listOf(
                "android:textColor", "android:background", "android:tint",
                "app:backgroundTint", "app:tint"
            )
            
            colorAttributes.forEach { attribute ->
                if (element.hasAttribute(attribute)) {
                    val value = element.getAttribute(attribute)
                    
                    // التحقق مما إذا كانت القيمة لونًا ثابتًا (ليست مرجعًا)
                    if (value.isNotEmpty() && 
                        !value.startsWith("@color/") && 
                        !value.startsWith("@android:color/") &&
                        (value.startsWith("#") || value == "black" || value == "white" || value == "red" || value == "green" || value == "blue")) {
                        
                        val lineNumber = getLineNumber(file, "$attribute=\"$value\"")
                        
                        issues.add(
                            CodeIssue(
                                message = "لون ثابت في التخطيط: $value",
                                description = "استخدام الألوان الثابتة في ملفات التخطيط يجعل تغيير السمات صعبًا",
                                severity = IssueSeverity.INFO,
                                type = IssueType.RESOURCES,
                                file = file,
                                line = lineNumber,
                                column = 1,
                                suggestion = "استخدم مرجع لون (@color/resource_name) بدلاً من اللون الثابت"
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * تحليل استخدام معرفات غير صالحة
     */
    private fun analyzeInvalidIds(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val elements = document.getElementsByTagName("*")
        
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            
            // التحقق من سمة المعرف
            if (element.hasAttribute("android:id")) {
                val id = element.getAttribute("android:id")
                
                // التحقق من صيغة المعرف
                if (!id.startsWith("@+id/") && !id.startsWith("@id/")) {
                    val lineNumber = getLineNumber(file, "android:id=\"$id\"")
                    
                    issues.add(
                        CodeIssue(
                            message = "معرف غير صالح: $id",
                            description = "معرفات العناصر يجب أن تبدأ بـ @+id/ أو @id/",
                            severity = IssueSeverity.ERROR,
                            type = IssueType.RESOURCES,
                            file = file,
                            line = lineNumber,
                            column = 1,
                            suggestion = "استخدم الصيغة الصحيحة للمعرف: @+id/name"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * تحليل تداخل التخطيطات
     */
    private fun analyzeNestedLayouts(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val rootElement = document.documentElement
        
        // التحقق من تداخل التخطيطات
        checkNestedLayouts(file, rootElement, 0, issues)
    }
    
    /**
     * التحقق من تداخل التخطيطات بشكل متكرر
     */
    private fun checkNestedLayouts(file: File, element: Element, depth: Int, issues: MutableList<CodeIssue>) {
        // التحقق من عمق التداخل
        if (depth > 2 && isLayoutElement(element)) {
            val id = element.getAttribute("android:id")
            val lineNumber = getLineNumber(file, element.tagName)
            
            issues.add(
                CodeIssue(
                    message = "تداخل عميق للتخطيطات: ${element.tagName} $id",
                    description = "تداخل التخطيطات بعمق أكبر من 2 يمكن أن يؤدي إلى مشاكل في الأداء",
                    severity = IssueSeverity.WARNING,
                    type = IssueType.PERFORMANCE,
                    file = file,
                    line = lineNumber,
                    column = 1,
                    suggestion = "استخدم ConstraintLayout أو أعد هيكلة التخطيط لتقليل التداخل"
                )
            )
        }
        
        // التحقق من العناصر الفرعية
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node is Element) {
                checkNestedLayouts(file, node, if (isLayoutElement(element)) depth + 1 else depth, issues)
            }
        }
    }
    
    /**
     * التحقق مما إذا كان العنصر هو تخطيط
     */
    private fun isLayoutElement(element: Element): Boolean {
        val layoutTags = listOf(
            "LinearLayout", "RelativeLayout", "FrameLayout", "ConstraintLayout",
            "androidx.constraintlayout.widget.ConstraintLayout",
            "GridLayout", "TableLayout", "CoordinatorLayout",
            "androidx.coordinatorlayout.widget.CoordinatorLayout"
        )
        
        return layoutTags.contains(element.tagName)
    }
    
    /**
     * تحليل إمكانية الوصول
     */
    private fun analyzeAccessibility(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val elements = document.getElementsByTagName("*")
        
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            
            // التحقق من العناصر التي تحتاج إلى وصف محتوى
            val needsContentDescription = listOf(
                "ImageView", "ImageButton", "android.widget.ImageView", "android.widget.ImageButton"
            )
            
            if (needsContentDescription.contains(element.tagName) && 
                !element.hasAttribute("android:contentDescription") && 
                !element.hasAttribute("android:importantForAccessibility")) {
                
                val id = element.getAttribute("android:id")
                val lineNumber = getLineNumber(file, element.tagName)
                
                issues.add(
                    CodeIssue(
                        message = "عنصر بدون وصف محتوى: ${element.tagName} $id",
                        description = "العناصر المرئية مثل ImageView و ImageButton تحتاج إلى وصف محتوى لإمكانية الوصول",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.RESOURCES,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "أضف android:contentDescription أو اضبط android:importantForAccessibility=\"no\" إذا كان العنصر زخرفيًا فقط"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل تكرار المعرفات
     */
    private fun analyzeDuplicateIds(file: File, document: Document, issues: MutableList<CodeIssue>) {
        // التحقق من نوع ملف القيم
        if (document.documentElement.tagName != "resources") {
            return
        }
        
        // جمع جميع المعرفات
        val idElements = document.getElementsByTagName("item")
        val ids = mutableMapOf<String, Int>()
        
        for (i in 0 until idElements.length) {
            val element = idElements.item(i) as Element
            
            if (element.hasAttribute("name") && element.hasAttribute("type") && element.getAttribute("type") == "id") {
                val name = element.getAttribute("name")
                ids[name] = (ids[name] ?: 0) + 1
            }
        }
        
        // التحقق من المعرفات المكررة
        ids.filter { it.value > 1 }.forEach { (name, count) ->
            val lineNumber = getLineNumber(file, "name=\"$name\"")
            
            issues.add(
                CodeIssue(
                    message = "معرف مكرر: $name ($count مرات)",
                    description = "المعرفات المكررة يمكن أن تؤدي إلى سلوك غير متوقع",
                    severity = IssueSeverity.ERROR,
                    type = IssueType.RESOURCES,
                    file = file,
                    line = lineNumber,
                    column = 1,
                    suggestion = "استخدم اسمًا فريدًا لكل معرف"
                )
            )
        }
    }
    
    /**
     * تحليل تسمية الموارد
     */
    private fun analyzeResourceNaming(file: File, document: Document, issues: MutableList<CodeIssue>) {
        // التحقق من نوع ملف القيم
        if (document.documentElement.tagName != "resources") {
            return
        }
        
        // التحقق من تسمية السلاسل
        val stringElements = document.getElementsByTagName("string")
        for (i in 0 until stringElements.length) {
            val element = stringElements.item(i) as Element
            
            if (element.hasAttribute("name")) {
                val name = element.getAttribute("name")
                
                // التحقق من اتفاقية التسمية
                if (!name.matches(Regex("[a-z][a-z0-9_]*"))) {
                    val lineNumber = getLineNumber(file, "name=\"$name\"")
                    
                    issues.add(
                        CodeIssue(
                            message = "اسم سلسلة لا يتبع اتفاقية التسمية: $name",
                            description = "أسماء الموارد يجب أن تبدأ بحرف صغير وتحتوي فقط على أحرف صغيرة وأرقام وشرطات سفلية",
                            severity = IssueSeverity.INFO,
                            type = IssueType.STYLE,
                            file = file,
                            line = lineNumber,
                            column = 1,
                            suggestion = "استخدم اتفاقية التسمية الصحيحة: أحرف صغيرة وأرقام وشرطات سفلية"
                        )
                    )
                }
            }
        }
        
        // التحقق من تسمية الألوان
        val colorElements = document.getElementsByTagName("color")
        for (i in 0 until colorElements.length) {
            val element = colorElements.item(i) as Element
            
            if (element.hasAttribute("name")) {
                val name = element.getAttribute("name")
                
                // التحقق من اتفاقية التسمية
                if (!name.matches(Regex("[a-z][a-z0-9_]*"))) {
                    val lineNumber = getLineNumber(file, "name=\"$name\"")
                    
                    issues.add(
                        CodeIssue(
                            message = "اسم لون لا يتبع اتفاقية التسمية: $name",
                            description = "أسماء الموارد يجب أن تبدأ بحرف صغير وتحتوي فقط على أحرف صغيرة وأرقام وشرطات سفلية",
                            severity = IssueSeverity.INFO,
                            type = IssueType.STYLE,
                            file = file,
                            line = lineNumber,
                            column = 1,
                            suggestion = "استخدم اتفاقية التسمية الصحيحة: أحرف صغيرة وأرقام وشرطات سفلية"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * الحصول على رقم السطر لنص محدد في الملف
     */
    private fun getLineNumber(file: File, text: String): Int {
        try {
            val lines = file.readLines()
            
            for (i in lines.indices) {
                if (lines[i].contains(text)) {
                    return i + 1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting line number", e)
        }
        
        return 1
    }
}