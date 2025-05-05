package com.mobileide.aide.analyzer

import android.util.Log
import java.io.File
import java.util.regex.Pattern

/**
 * محلل كود Dart
 */
class DartAnalyzer : CodeAnalyzer {
    
    companion object {
        private const val TAG = "DartAnalyzer"
    }
    
    override fun analyzeProject(projectDir: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // البحث عن جميع ملفات Dart في المشروع
            val dartFiles = findDartFiles(projectDir)
            
            // تحليل كل ملف
            dartFiles.forEach { file ->
                val fileIssues = analyzeFile(file)
                issues.addAll(fileIssues)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Dart project", e)
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
            analyzeDartSpecific(file, lines, issues)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Dart file: ${file.absolutePath}", e)
        }
        
        return issues
    }
    
    override fun getName(): String = "Dart Analyzer"
    
    override fun getDescription(): String = "محلل كود Dart"
    
    override fun getSupportedFileExtensions(): List<String> = listOf("dart")
    
    /**
     * البحث عن جميع ملفات Dart في المشروع
     */
    private fun findDartFiles(dir: File): List<File> {
        val dartFiles = mutableListOf<File>()
        
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    dartFiles.addAll(findDartFiles(file))
                } else if (file.name.endsWith(".dart")) {
                    dartFiles.add(file)
                }
            }
        }
        
        return dartFiles
    }
    
    /**
     * تحليل الاستيرادات
     */
    private fun analyzeImports(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استيرادات غير مستخدمة
        val imports = mutableListOf<String>()
        val importPattern = Pattern.compile("import\\s+'([^']+)'")
        
        lines.forEachIndexed { index, line ->
            val matcher = importPattern.matcher(line)
            if (matcher.find()) {
                val importPath = matcher.group(1)
                imports.add(importPath)
                
                // التحقق من استيرادات مطلقة
                if (importPath.startsWith("/")) {
                    issues.add(
                        CodeIssue(
                            message = "استخدام مسار استيراد مطلق: $importPath",
                            description = "استخدام مسارات استيراد مطلقة يمكن أن يؤدي إلى مشاكل في التوافق",
                            severity = IssueSeverity.WARNING,
                            type = IssueType.STYLE,
                            file = file,
                            line = index + 1,
                            column = line.indexOf("import") + 1,
                            suggestion = "استخدم مسارات استيراد نسبية بدلاً من المطلقة"
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
                            description = "أسماء الفئات في Dart يجب أن تبدأ بحرف كبير وفقًا لاتفاقيات التسمية",
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
            val variablePattern = Pattern.compile("(var|final|const)\\s+(\\w+)")
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
                
                // التحقق من أسماء الثوابت
                if (line.contains("const") && !variableName.matches(Regex("[A-Z][A-Z0-9_]*"))) {
                    issues.add(
                        CodeIssue(
                            message = "اسم ثابت لا يتبع اتفاقية التسمية: $variableName",
                            description = "أسماء الثوابت في Dart يجب أن تكون بأحرف كبيرة مع فصل الكلمات بشرطة سفلية",
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
    
    /**
     * تحليل نمط الكود
     */
    private fun analyzeCodeStyle(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن أسطر طويلة جدًا
        lines.forEachIndexed { index, line ->
            if (line.length > 80) {
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
                    suggestion = "استخدم StringBuffer أو join بدلاً من +"
                )
            )
        }
    }
    
    /**
     * تحليل الأمان
     */
    private fun analyzeSecurity(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استخدام print
        lines.forEachIndexed { index, line ->
            if (line.contains("print(")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام print",
                        description = "استخدام print في التطبيقات الإنتاجية يمكن أن يؤدي إلى تسرب معلومات حساسة",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.SECURITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("print") + 1,
                        suggestion = "استخدم نظام تسجيل مناسب مثل logger"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل التوثيق
     */
    private fun analyzeDocumentation(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن دوال عامة بدون توثيق
        val content = lines.joinToString("\n")
        val publicFunctionPattern = Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{")
        val publicFunctionMatcher = publicFunctionPattern.matcher(content)
        
        while (publicFunctionMatcher.find()) {
            val returnType = publicFunctionMatcher.group(1)
            val functionName = publicFunctionMatcher.group(2)
            
            // تجاهل الدوال الخاصة
            if (functionName.startsWith("_")) {
                continue
            }
            
            val functionStart = publicFunctionMatcher.start()
            val lineNumber = content.substring(0, functionStart).count { it == '\n' } + 1
            
            // التحقق من وجود توثيق قبل الدالة
            val prevLines = content.substring(0, functionStart).lines()
            val hasDocumentation = prevLines.lastOrNull { it.trim().isNotEmpty() }?.trim()?.startsWith("///") == true
            
            if (!hasDocumentation) {
                issues.add(
                    CodeIssue(
                        message = "دالة عامة بدون توثيق: $functionName",
                        description = "الدوال العامة يجب أن تكون موثقة باستخدام تعليقات التوثيق ///",
                        severity = IssueSeverity.INFO,
                        type = IssueType.DOCUMENTATION,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "أضف توثيقًا للدالة باستخدام ///"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل ميزات خاصة بـ Dart
     */
    private fun analyzeDartSpecific(file: File, lines: List<String>, issues: MutableList<CodeIssue>) {
        // البحث عن استخدام setState في build
        val content = lines.joinToString("\n")
        val buildMethodPattern = Pattern.compile("Widget\\s+build\\s*\\([^)]*\\)\\s*\\{[^}]*setState\\([^}]*\\}")
        val buildMethodMatcher = buildMethodPattern.matcher(content)
        
        if (buildMethodMatcher.find()) {
            val lineNumber = content.substring(0, buildMethodMatcher.start()).count { it == '\n' } + 1
            
            issues.add(
                CodeIssue(
                    message = "استخدام setState في دالة build",
                    description = "استخدام setState في دالة build يمكن أن يؤدي إلى حلقة لا نهائية من إعادة البناء",
                    severity = IssueSeverity.ERROR,
                    type = IssueType.MAINTAINABILITY,
                    file = file,
                    line = lineNumber,
                    column = 1,
                    suggestion = "انقل استدعاء setState خارج دالة build"
                )
            )
        }
        
        // البحث عن استخدام async بدون await
        lines.forEachIndexed { index, line ->
            if (line.contains("async") && !content.contains("await")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام async بدون await",
                        description = "استخدام async بدون await لا فائدة منه",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.MAINTAINABILITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("async") + 1,
                        suggestion = "إما استخدم await داخل الدالة أو أزل كلمة async"
                    )
                )
            }
        }
        
        // البحث عن استخدام late بدون تهيئة
        lines.forEachIndexed { index, line ->
            if (line.contains("late") && !line.contains("=")) {
                issues.add(
                    CodeIssue(
                        message = "استخدام late بدون تهيئة",
                        description = "استخدام late بدون تهيئة يمكن أن يؤدي إلى استثناءات في وقت التشغيل",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.MAINTAINABILITY,
                        file = file,
                        line = index + 1,
                        column = line.indexOf("late") + 1,
                        suggestion = "قم بتهيئة المتغير أو استخدم نوع قابل للإسناد إلى null"
                    )
                )
            }
        }
    }
}