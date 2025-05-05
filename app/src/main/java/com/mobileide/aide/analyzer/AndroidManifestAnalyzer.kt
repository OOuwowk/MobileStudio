package com.mobileide.aide.analyzer

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * محلل ملف AndroidManifest.xml
 */
class AndroidManifestAnalyzer : CodeAnalyzer {
    
    companion object {
        private const val TAG = "AndroidManifestAnalyzer"
    }
    
    override fun analyzeProject(projectDir: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // البحث عن ملف AndroidManifest.xml
            val manifestFile = findManifestFile(projectDir)
            
            if (manifestFile != null) {
                val fileIssues = analyzeFile(manifestFile)
                issues.addAll(fileIssues)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Android project manifest", e)
        }
        
        return issues
    }
    
    override fun analyzeFile(file: File): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        try {
            // التحقق من أن الملف هو AndroidManifest.xml
            if (!file.name.equals("AndroidManifest.xml", ignoreCase = true)) {
                return issues
            }
            
            // قراءة ملف XML
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(file)
            
            // تحليل الملف
            analyzePermissions(file, document, issues)
            analyzeMinSdkVersion(file, document, issues)
            analyzeExportedComponents(file, document, issues)
            analyzeDebuggableFlag(file, document, issues)
            analyzeBackupFlag(file, document, issues)
            analyzeIntentFilters(file, document, issues)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing AndroidManifest.xml: ${file.absolutePath}", e)
        }
        
        return issues
    }
    
    override fun getName(): String = "Android Manifest Analyzer"
    
    override fun getDescription(): String = "محلل ملف AndroidManifest.xml"
    
    override fun getSupportedFileExtensions(): List<String> = listOf("xml")
    
    /**
     * البحث عن ملف AndroidManifest.xml في المشروع
     */
    private fun findManifestFile(dir: File): File? {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val manifestFile = findManifestFile(file)
                    if (manifestFile != null) {
                        return manifestFile
                    }
                } else if (file.name.equals("AndroidManifest.xml", ignoreCase = true)) {
                    return file
                }
            }
        }
        
        return null
    }
    
    /**
     * تحليل الأذونات
     */
    private fun analyzePermissions(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val permissions = document.getElementsByTagName("uses-permission")
        val dangerousPermissions = listOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.ANSWER_PHONE_CALLS",
            "android.permission.BODY_SENSORS"
        )
        
        for (i in 0 until permissions.length) {
            val permission = permissions.item(i) as Element
            val permissionName = permission.getAttribute("android:name")
            
            // التحقق من الأذونات الخطرة
            if (dangerousPermissions.any { permissionName.endsWith(it) }) {
                val lineNumber = getLineNumber(file, permissionName)
                
                issues.add(
                    CodeIssue(
                        message = "استخدام إذن خطر: $permissionName",
                        description = "هذا الإذن يتطلب موافقة المستخدم في وقت التشغيل على أندرويد 6.0 وما فوق",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.SECURITY,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "تأكد من طلب الإذن في وقت التشغيل باستخدام واجهة برمجة تطبيقات الأذونات"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل الإصدار الأدنى من SDK
     */
    private fun analyzeMinSdkVersion(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val usesSdk = document.getElementsByTagName("uses-sdk")
        
        if (usesSdk.length > 0) {
            val sdkElement = usesSdk.item(0) as Element
            val minSdkVersion = sdkElement.getAttribute("android:minSdkVersion")
            
            if (minSdkVersion.isNotEmpty()) {
                val minSdk = minSdkVersion.toIntOrNull()
                
                if (minSdk != null && minSdk < 21) {
                    val lineNumber = getLineNumber(file, "android:minSdkVersion")
                    
                    issues.add(
                        CodeIssue(
                            message = "الإصدار الأدنى من SDK منخفض جدًا: $minSdk",
                            description = "استخدام إصدار أقل من API 21 (Android 5.0) يمكن أن يؤدي إلى مشاكل في الأمان والأداء",
                            severity = IssueSeverity.WARNING,
                            type = IssueType.COMPATIBILITY,
                            file = file,
                            line = lineNumber,
                            column = 1,
                            suggestion = "زيادة الإصدار الأدنى من SDK إلى 21 أو أعلى"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * تحليل المكونات المصدرة
     */
    private fun analyzeExportedComponents(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val components = listOf("activity", "service", "receiver", "provider")
        
        components.forEach { componentType ->
            val elements = document.getElementsByTagName(componentType)
            
            for (i in 0 until elements.length) {
                val element = elements.item(i) as Element
                val exported = element.getAttribute("android:exported")
                val hasIntentFilter = element.getElementsByTagName("intent-filter").length > 0
                
                // التحقق من المكونات المصدرة بدون أذونات
                if ((exported == "true" || (exported.isEmpty() && hasIntentFilter)) && 
                    element.getAttribute("android:permission").isEmpty()) {
                    
                    val name = element.getAttribute("android:name")
                    val lineNumber = getLineNumber(file, name)
                    
                    issues.add(
                        CodeIssue(
                            message = "مكون مصدر بدون إذن: $name",
                            description = "المكونات المصدرة بدون أذونات يمكن أن تكون عرضة للوصول غير المصرح به",
                            severity = IssueSeverity.ERROR,
                            type = IssueType.SECURITY,
                            file = file,
                            line = lineNumber,
                            column = 1,
                            suggestion = "أضف android:permission أو اضبط android:exported=\"false\" إذا لم يكن المكون بحاجة إلى الوصول من تطبيقات أخرى"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * تحليل علامة التصحيح
     */
    private fun analyzeDebuggableFlag(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val application = document.getElementsByTagName("application")
        
        if (application.length > 0) {
            val appElement = application.item(0) as Element
            val debuggable = appElement.getAttribute("android:debuggable")
            
            if (debuggable == "true") {
                val lineNumber = getLineNumber(file, "android:debuggable")
                
                issues.add(
                    CodeIssue(
                        message = "التطبيق قابل للتصحيح",
                        description = "ضبط android:debuggable=\"true\" في الإصدارات الإنتاجية يمكن أن يؤدي إلى مخاطر أمنية",
                        severity = IssueSeverity.ERROR,
                        type = IssueType.SECURITY,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "إزالة android:debuggable=\"true\" أو استخدام BuildConfig.DEBUG للتحكم في هذه العلامة"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل علامة النسخ الاحتياطي
     */
    private fun analyzeBackupFlag(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val application = document.getElementsByTagName("application")
        
        if (application.length > 0) {
            val appElement = application.item(0) as Element
            val allowBackup = appElement.getAttribute("android:allowBackup")
            
            if (allowBackup.isEmpty() || allowBackup == "true") {
                val lineNumber = getLineNumber(file, "<application")
                
                issues.add(
                    CodeIssue(
                        message = "النسخ الاحتياطي مسموح به",
                        description = "السماح بالنسخ الاحتياطي يمكن أن يؤدي إلى تسرب البيانات الحساسة",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.SECURITY,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "ضبط android:allowBackup=\"false\" إذا كان التطبيق يتعامل مع بيانات حساسة"
                    )
                )
            }
        }
    }
    
    /**
     * تحليل مرشحات النوايا
     */
    private fun analyzeIntentFilters(file: File, document: Document, issues: MutableList<CodeIssue>) {
        val intentFilters = document.getElementsByTagName("intent-filter")
        
        for (i in 0 until intentFilters.length) {
            val intentFilter = intentFilters.item(i) as Element
            val parent = intentFilter.parentNode as Element
            
            // التحقق من وجود android:exported
            if (!parent.hasAttribute("android:exported")) {
                val lineNumber = getLineNumber(file, "<intent-filter")
                
                issues.add(
                    CodeIssue(
                        message = "مكون مع مرشح نية بدون تحديد android:exported",
                        description = "المكونات مع مرشحات النوايا تكون مصدرة افتراضيًا، مما قد يؤدي إلى مخاطر أمنية",
                        severity = IssueSeverity.WARNING,
                        type = IssueType.SECURITY,
                        file = file,
                        line = lineNumber,
                        column = 1,
                        suggestion = "أضف android:exported=\"true\" وتأكد من حماية المكون بشكل مناسب"
                    )
                )
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