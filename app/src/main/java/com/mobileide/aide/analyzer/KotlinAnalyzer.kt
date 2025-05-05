package com.mobileide.aide.analyzer

import android.util.Log
import java.io.File
import java.util.regex.Pattern

/**
 * محلل كود Kotlin
 */
class KotlinAnalyzer : CodeAnalyzer {
    
    companion object {
        private const val TAG = "KotlinAnalyzer"
    }
    
    override fun analyzeProject(projectDir: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // البحث عن جميع ملفات Kotlin في المشروع
            val kotlinFiles = findKotlinFiles(projectDir)
            
            // تحليل كل ملف
            kotlinFiles.forEach { file ->
                val fileIssues = analyzeFile(file)
                issues.addAll(fileIssues)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Kotlin project", e)
        }
        
        return issues
    }
    
    override fun analyzeFile(file: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // قراءة محتوى الملف
            val content = file.readText()
            val lines = content.lines()
            
            // تحليل الملف
            analyzeImports(file, lines, issues)
            analyzeNaming(file, lines, issues)
            analyzeCodeStyle(file, lines, issues)
            analyzePerformance(file, lines, issues)
            analyzeSecurity(file, lines, issues)
            analyzeDocumentation(file, lines, issues)
            analyzeKotlinSpecific(file, lines, issues)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Kotlin file: ${file.absolutePath}", e)
        }
        
        return issues
    }
    
    override fun getName(): String = "Kotlin Analyzer"
    
    override fun getDescription(): String = "محلل كود Kotlin"
    
    override fun getSupportedFileExtensions(): List<String> = listOf("kt", "kts")
    
    /**
     * البحث عن جميع ملفات Kotlin في المشروع
     */
    private fun findKotlinFiles(dir: File): List<File> {
        val kotlinFiles = mutableListOf<File>()
        
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    kotlinFiles.addAll(findKotlinFiles(file))
                } else if (file.name.endsWith(".kt") || file.name.endsWith(".kts")) {
                    kotlinFiles.add(file)
                }
            }
        }
        
        return kotlinFiles
    }
    
    /**
     * تحليل الاستيرادات
     */
    private fun analyzeImports(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استيرادات غير مستخدمة
        val imports = mutableListOf<String>()
        val importPattern = Pattern.compile("import\\s+([\\w.]+)")
        
        lines.forEachIndexed { index, line ->
            val matcher = importPattern.matcher(line)
            if (matcher.find()) {
                val importName = matcher.group(1)
                imports.add(importName)
                
                // التحقق من استيرادات النجمة
                if (importName.endsWith(".*")) {
                    issues.add(
                        CodeIssue(
                            message = "استخدام استيراد النجمة",
                            description = "استيراد النجمة يمكن أن يؤدي إلى تضارب الأسماء وصعوبة قراءة الكود",
                            severity = IssueSeverity.WARNING,
                            type = IssueType.STYLE,
                            file = file,
                            line = index + 1,
                            column = line.indexOf("import") + 1,
                            suggestion = "استورد الفئات المحددة بدلاً من استخدام النجمة"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * تحليل تسمية المتغيرات والفئات والدوال
     */
    private fun analyzeNaming(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن أسماء الفئات
        val classPattern = Pattern.compile("class\\s+(\\w+)")
        
        lines.forEachIndexed { index, line ->
            val classMatcher = classPattern.matcher(line)
            if (classMatcher.find()) {
                val className = classMatcher.group(1)
                
                // التحقق من أن اسم الفئة يبدأ بحرف كبير
                if (className.isNotEmpty() && !Character.isUpperCase(className[0])) {
                    issues.add(
                        CodeIssue(
                            message = "اسم الفئة لا يبدأ بحرف كبير: $className",
                            description = "أسماء الفئات في Kotlin يجب أن تبدأ بحرف كبير وفقًا لاتفاقيات التسمية",
                            severity = IssueSeverity.WARNING,
                            type = IssueType.STYLE,
                            file = file,
                            line = index + 1,
                            column = line.indexOf(className) + 1,
                            suggestion = "غير اسم الفئة إلى ${className.capitalize()}"
                        )
                    )
                }
            }
            
            // البحث عن أسماء المتغيرات
            val variablePattern = Pattern.compile("val\\s+(\\w+)|var\\s+(\\w+)")
            val variableMatcher = variablePattern.matcher(line)
            
            while (variableMatcher.find()) {
                val variableName = variableMatcher.group(1) ?: variableMatcher.group(2)
                
                // التحقق من أسماء المتغيرات القصيرة جدًا
                if (variableName.length == 1 && variableName != "i" && variableName != "j" && variableName != "k") {
                    issues.add(
                        CodeIssue(
                            message = "اسم متغير قصير جدًا: $variableName",
                            description = "أسماء المتغيرات القصيرة جدًا تجعل الكود أقل قابلية للقراءة",
                            severity = IssueSeverity.INFO,
                            type = IssueType.STYLE,
                            file = file,
                            line = index + 1,
                            column = line.indexOf(variableName) + 1,
                            suggestion = "استخدم اسمًا أكثر وصفًا للمتغير"
                        )
                    )
                }
                
                // التحقق من أسماء الثوابت
                if (line.contains("val") && variableName.matches(Regex("[a-z].*"))) {
                    // التحقق مما إذا كان المتغير ثابتًا على مستوى الفئة
                    val isTopLevel = !line.contains("fun") && !line.contains("init") && !line.contains("{")
                    if (isTopLevel) {
                        issues.add(
                            CodeIssue(
                                message = "اسم ثابت لا يتبع اتفاقية التسمية: $variableName",
                                description = "أسماء الثوابت على مستوى الفئة في Kotlin يجب أن تكون بأحرف كبيرة مع فصل الكلمات بشرطة سفلية",
                                severity = IssueSeverity.INFO,
                                type = IssueType.STYLE,
                                file = file,
                                line = index + 1,
                                column = line.indexOf(variableName) + 1,
                                suggestion = "غير اسم الثابت إلى ${variableName.toUpperCase().replace(".", "_")}"
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * تحليل نمط الكود
     */
    private fun analyzeCodeStyle(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن أسطر طويلة جدًا
        lines.forEachIndexed { index, line ->
            if (line.length > 100) {
                issues.add(
                    CodeIssue(
                        message = "سطر طويل جدًا: ${line.length} حرف",
                        description = "الأسطر الطويلة جدًا تجعل الكود أقل قابلية للقراءة",
                        severity = IssueSeverity.INFO,
                        type = IssueType.STYLE,
                        file = file,
                        line = index + 1,
                        column = 1,
                        suggestion = "قسم السطر إلى أسطر متعددة"
                    )
                )
            }
        }
        
        // البحث عن كتل try-catch فارغة
        val content = lines.joinToString("\n")
        val emptyCatchPattern = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}")
        val emptyCatchMatcher = emptyCatchPattern.matcher(content)
        
        while (emptyCatchMatcher.find()) {
            val lineNumber = content.substring(0, emptyCatchMatcher.start()).count { it == '\n' } + 1
            
            issues.add(
                CodeIssue(
                    message = "كتلة catch فارغة",
                    description = "كتل catch الفارغة تخفي الأخطاء وتجعل تصحيح الأخطاء أكثر صعوبة",
                    severity = IssueSeverity.WARNING,
                    type = IssueType.MAINTAINABILITY,
                    file = file,
                    line = lineNumber,
                    column = 1,
                    suggestion = "سجل الخطأ أو أعد رميه بدلاً من تجاهله"
                )
            )
        }
    }
    
    /**
     * تحليل الأداء
     */
    private fun analyzePerformance(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استخدام سلاسل + في حلقات
        val content = lines.joinToString("\n")
        val stringConcatPattern = Pattern.compile("for\\s*\\([^)]+\\)\\s*\\{[^}]*\\+=[^}]*\\}")
        val stringConcatMatcher = stringConcatPattern.matcher(content)
        
        while (stringConcatMatcher.find()) {
            val lineNumber = content.substring(0, stringConcatMatcher.start()).count { it == '\n' } + 1
            
            issues.add(
                CodeIssue(
                    message = "استخدام سلاسل + في حلقة",
                    description = "استخدام عملية + لسلاسل في حلقة يمكن أن يؤدي إلى مشاكل في الأداء",
                    severity = IssueSeverity.WARNING,
                    type = IssueType.PERFORMANCE,
                    file = file,
                    line = lineNumber,
                    column = 1,
                    suggestion = "استخدم StringBuilder أو joinToString بدلاً من +"
                )
            )
        }
    }
    
    /**
     * تحليل الأمان
     */
    private fun analyzeSecurity(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استخدام println
        lines.forEachIndexed { index, line ->
            if (line.contains("println(")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام println",
                        description = "استخدام println في التطبيقات الإنتاجية يمكن أن يؤدي إلى تسرب معلومات حساسة",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.SECURITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("println") + 1,
                        suggestion = "استخدم نظام تسجيل مناسب مثل SLF4J أو Timber"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل التوثيق
     */
    private fun analyzeDocumentation(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن دوال عامة بدون توثيق KDoc
        val content = lines.joinToString("\n")
        val publicFunctionPattern = Pattern.compile("fun\\s+(\\w+)\\s*\\([^)]*\\)")
        val publicFunctionMatcher = publicFunctionPattern.matcher(content)
        
        while (publicFunctionMatcher.find()) {
            val functionName = publicFunctionMatcher.group(1)
            val functionStart = publicFunctionMatcher.start()
            val lineNumber = content.substring(0, functionStart).count { it == '\n' } + 1
            
            // التحقق من وجود توثيق KDoc قبل الدالة
            val prevLines = content.substring(0, functionStart).lines()
            val hasKDoc = prevLines.lastOrNull { it.trim().isNotEmpty() }?.trim()?.startsWith("/**") == true
            
            // التحقق مما إذا كانت الدالة عامة (ليست خاصة أو محمية)
            val isPrivate = prevLines.lastOrNull { it.trim().isNotEmpty() && !it.trim().startsWith("/**") }?.contains("private") == true
            val isProtected = prevLines.lastOrNull { it.trim().isNotEmpty() && !it.trim().startsWith("/**") }?.contains("protected") == true
            
            if (!hasKDoc && !isPrivate && !isProtected) {
                issues.add(
                    CodeIssue(
                        message = "دالة عامة بدون توثيق KDoc: $functionName",
                        description = "الدوال العامة يجب أن تكون موثقة باستخدام KDoc",
                        severity = IssueSeverity.INFO,
                        type = IssueType.DOCUMENTATION,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "أضف توثيق KDoc للدالة"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل ميزات خاصة بـ Kotlin
     */
    private fun analyzeKotlinSpecific(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استخدام !!
        lines.forEachIndexed { index, line ->
            if (line.contains("!!")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام !! للتحويل غير الآمن",
                        description = "استخدام !! يمكن أن يؤدي إلى استثناءات NullPointerException",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.MAINTAINABILITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("!!") + 1,
                        suggestion = "استخدم ?. أو let أو ?: بدلاً من !!"
                    )
                )
            }
        }
        
        // البحث عن استخدام when بدون else
        val content = lines.joinToString("\n")
        val whenPattern = Pattern.compile("when\\s*\\([^)]+\\)\\s*\\{[^}]*\\}")
        val whenMatcher = whenPattern.matcher(content)
        
        while (whenMatcher.find()) {
            val whenBlock = whenMatcher.group(0)
            val lineNumber = content.substring(0, whenMatcher.start()).count { it == '\n' } + 1
            
            if (!whenBlock.contains("else ->")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام when بدون else",
                        description = "استخدام when بدون else يمكن أن يؤدي إلى سلوك غير متوقع إذا تم إضافة حالات جديدة",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.MAINTAINABILITY,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "أضف حالة else للتعامل مع الحالات غير المتوقعة"
                    )
                )
            }
        }
        
        // البحث عن استخدام var بدلاً من val
        lines.forEachIndexed { index, line ->
            if (line.contains("var ") && !line.contains("private var") && !line.contains("protected var")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام var بدلاً من val",
                        description = "استخدام var يجعل الكود أقل أمانًا وأكثر عرضة للأخطاء",
                        severity = IssueSeverity.INFO,
                        type = IssueType.STYLE,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("var") + 1,
                        suggestion = "استخدم val إذا كان المتغير لا يحتاج إلى التغيير"
                    )
                )
            }
        }
    }
}