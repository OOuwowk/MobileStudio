package com.mobileide.aide.analyzer

import android.util.Log
import java.io.File
import java.util.regex.Pattern

/**
 * محلل كود Java
 */
class JavaAnalyzer : CodeAnalyzer {
    
    companion object {
        private const val TAG = "JavaAnalyzer"
    }
    
    override fun analyzeProject(projectDir: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // البحث عن جميع ملفات Java في المشروع
            val javaFiles = findJavaFiles(projectDir)
            
            // تحليل كل ملف
            javaFiles.forEach { file ->
                val fileIssues = analyzeFile(file)
                issues.addAll(fileIssues)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Java project", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Java file: ${file.absolutePath}", e)
        }
        
        return issues
    }
    
    override fun getName(): String = "Java Analyzer"
    
    override fun getDescription(): String = "محلل كود Java"
    
    override fun getSupportedFileExtensions(): List<String> = listOf("java")
    
    /**
     * البحث عن جميع ملفات Java في المشروع
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
     * تحليل الاستيرادات
     */
    private fun analyzeImports(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استيرادات غير مستخدمة
        val imports = mutableListOf<String>()
        val importPattern = Pattern.compile("import\\s+([\\w.]+);")
        
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
        
        // التحقق من استيرادات java.util.* و java.io.* معًا
        if (imports.any { it.startsWith("java.util.") } && imports.any { it.startsWith("java.io.") }) {
            // هذا مجرد مثال للتحليل، وليس بالضرورة مشكلة حقيقية
            issues.add(
                CodeIssue(
                    message = "استخدام java.util و java.io معًا",
                    description = "استخدام حزم java.util و java.io معًا قد يشير إلى مسؤوليات متعددة للفئة",
                    severity = IssueSeverity.INFO,
                    type = IssueType.MAINTAINABILITY,
                    file = file,
                    line = 1,
                    column = 1,
                    suggestion = "فكر في فصل المسؤوليات إلى فئات منفصلة"
                )
            )
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
                            description = "أسماء الفئات في Java يجب أن تبدأ بحرف كبير وفقًا لاتفاقيات التسمية",
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
            val variablePattern = Pattern.compile("(private|protected|public|)\\s+\\w+\\s+(\\w+)\\s*=")
            val variableMatcher = variablePattern.matcher(line)
            
            while (variableMatcher.find()) {
                val variableName = variableMatcher.group(2)
                
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
                    suggestion = "استخدم StringBuilder بدلاً من +"
                )
            )
        }
    }
    
    /**
     * تحليل الأمان
     */
    private fun analyzeSecurity(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استخدام System.out.println
        lines.forEachIndexed { index, line ->
            if (line.contains("System.out.println")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام System.out.println",
                        description = "استخدام System.out.println في التطبيقات الإنتاجية يمكن أن يؤدي إلى تسرب معلومات حساسة",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.SECURITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("System.out.println") + 1,
                        suggestion = "استخدم نظام تسجيل مناسب مثل SLF4J أو java.util.logging"
                    )
                )
            }
        }
        
        // البحث عن استخدام SQL خام
        lines.forEachIndexed { index, line ->
            if (line.contains("executeQuery(") && line.contains("SELECT") && line.contains("FROM")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام SQL خام",
                        description = "استخدام SQL خام يمكن أن يؤدي إلى ثغرات حقن SQL",
                        severity = IssueSeverity.ERROR,
                        type = IssueType.SECURITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("executeQuery") + 1,
                        suggestion = "استخدم PreparedStatement بدلاً من ذلك"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل التوثيق
     */
    private fun analyzeDocumentation(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن دوال عامة بدون توثيق JavaDoc
        val content = lines.joinToString("\n")
        val publicMethodPattern = Pattern.compile("public\\s+\\w+\\s+\\w+\\s*\\([^)]*\\)")
        val publicMethodMatcher = publicMethodPattern.matcher(content)
        
        while (publicMethodMatcher.find()) {
            val methodStart = publicMethodMatcher.start()
            val lineNumber = content.substring(0, methodStart).count { it == '\n' } + 1
            
            // التحقق من وجود توثيق JavaDoc قبل الدالة
            val prevLines = content.substring(0, methodStart).lines()
            val hasJavaDoc = prevLines.lastOrNull { it.trim().isNotEmpty() }?.trim()?.startsWith("/**") == true
            
            if (!hasJavaDoc) {
                issues.add(
                    CodeIssue(
                        message = "دالة عامة بدون توثيق JavaDoc",
                        description = "الدوال العامة يجب أن تكون موثقة باستخدام JavaDoc",
                        severity = IssueSeverity.INFO,
                        type = IssueType.DOCUMENTATION,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "أضف توثيق JavaDoc للدالة"
                    )
                )
            }
        }
    }
}