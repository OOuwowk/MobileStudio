package com.mobileide.editor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Provides auto-completion suggestions for code editing
 */
class AutoCompleteProvider(private val context: Context) {
    
    // Cache of language-specific keywords and APIs
    private val languageKeywords = mutableMapOf<String, List<String>>()
    private val languageApis = mutableMapOf<String, List<ApiItem>>()
    
    init {
        // Load language keywords and APIs from assets
        loadLanguageData()
    }
    
    /**
     * Gets auto-complete suggestions for the given prefix and cursor position
     */
    fun getSuggestions(prefix: String, position: Int, fileExtension: String? = null): JSONArray {
        val result = JSONArray()
        
        // Determine the current language based on file extension
        val currentLanguage = determineLanguage(fileExtension)
        
        // Get context-aware suggestions
        val contextSuggestions = getContextAwareSuggestions(prefix, position, currentLanguage)
        if (contextSuggestions.length() > 0) {
            return contextSuggestions
        }
        
        // Add language keywords
        val keywords = languageKeywords[currentLanguage] ?: emptyList()
        
        keywords.filter { it.startsWith(prefix, ignoreCase = true) }.forEach { keyword ->
            val suggestion = JSONObject().apply {
                put("value", keyword)
                put("caption", keyword)
                put("meta", "keyword")
                put("score", 100)
            }
            result.put(suggestion)
        }
        
        // Add API suggestions
        val apis = languageApis[currentLanguage] ?: emptyList()
        
        apis.filter { it.name.startsWith(prefix, ignoreCase = true) }.forEach { api ->
            val suggestion = JSONObject().apply {
                put("value", api.name)
                put("caption", api.name)
                put("meta", api.type)
                put("score", 90)
                
                // Add documentation if available
                if (api.documentation != null) {
                    put("docText", api.documentation)
                }
                
                // Add signature if available
                if (api.signature != null) {
                    put("snippet", api.signature)
                }
            }
            result.put(suggestion)
        }
        
        // Add common code snippets
        getSnippets(currentLanguage, prefix).forEach { snippet ->
            val suggestion = JSONObject().apply {
                put("value", snippet.name)
                put("caption", snippet.name)
                put("meta", "snippet")
                put("score", 80)
                put("snippet", snippet.code)
            }
            result.put(suggestion)
        }
        
        return result
    }
    
    /**
     * Determines the language based on file extension
     */
    private fun determineLanguage(fileExtension: String?): String {
        return when (fileExtension?.toLowerCase()) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "xml" -> "xml"
            "dart" -> "dart"
            "js" -> "javascript"
            "ts" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "gradle" -> "gradle"
            else -> "java" // Default to Java
        }
    }
    
    /**
     * Gets context-aware suggestions based on the current code context
     */
    private fun getContextAwareSuggestions(prefix: String, position: Int, language: String): JSONArray {
        val result = JSONArray()
        
        // This is a more advanced implementation that would analyze the code context
        // For example, if we detect we're inside a class, we might suggest methods
        // or if we're after a dot operator, we might suggest members of the object
        
        // For now, this is a simplified implementation
        return result
    }
    
    /**
     * Gets code snippets for the given language and prefix
     */
    private fun getSnippets(language: String, prefix: String): List<CodeSnippet> {
        val snippets = when (language) {
            "java" -> JAVA_SNIPPETS
            "kotlin" -> KOTLIN_SNIPPETS
            "xml" -> XML_SNIPPETS
            else -> emptyList()
        }
        
        return snippets.filter { it.name.startsWith(prefix, ignoreCase = true) }
    }
    
    /**
     * Loads language data from assets
     */
    private fun loadLanguageData() {
        try {
            // Load Java keywords
            val javaKeywords = loadAssetFile("editor/keywords/java.txt")
            languageKeywords["java"] = javaKeywords.split("\n").filter { it.isNotBlank() }
            
            // Load Kotlin keywords
            val kotlinKeywords = loadAssetFile("editor/keywords/kotlin.txt")
            languageKeywords["kotlin"] = kotlinKeywords.split("\n").filter { it.isNotBlank() }
            
            // Load XML keywords
            val xmlKeywords = loadAssetFile("editor/keywords/xml.txt")
            languageKeywords["xml"] = xmlKeywords.split("\n").filter { it.isNotBlank() }
            
            // Load Dart keywords
            val dartKeywords = loadAssetFile("editor/keywords/dart.txt")
            languageKeywords["dart"] = dartKeywords.split("\n").filter { it.isNotBlank() }
            
            // Load JavaScript keywords
            val jsKeywords = loadAssetFile("editor/keywords/javascript.txt")
            languageKeywords["javascript"] = jsKeywords.split("\n").filter { it.isNotBlank() }
            
            // Load API data from JSON files
            loadApiData("java")
            loadApiData("kotlin")
            loadApiData("android")
            
        } catch (e: Exception) {
            // If assets are not available, use hardcoded fallbacks
            languageKeywords["java"] = JAVA_KEYWORDS
            languageKeywords["kotlin"] = KOTLIN_KEYWORDS
            languageKeywords["xml"] = XML_KEYWORDS
            languageKeywords["dart"] = DART_KEYWORDS
            languageKeywords["javascript"] = JS_KEYWORDS
            
            // Add some basic Android APIs
            languageApis["java"] = ANDROID_APIS
        }
    }
    
    /**
     * Loads API data from JSON files
     */
    private fun loadApiData(language: String) {
        try {
            val apiJson = loadAssetFile("editor/apis/$language.json")
            if (apiJson.isNotBlank()) {
                val jsonArray = JSONArray(apiJson)
                val apis = mutableListOf<ApiItem>()
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    apis.add(
                        ApiItem(
                            name = item.getString("name"),
                            type = item.getString("type"),
                            signature = if (item.has("signature")) item.getString("signature") else null,
                            documentation = if (item.has("documentation")) item.getString("documentation") else null
                        )
                    )
                }
                
                languageApis[language] = apis
            }
        } catch (e: Exception) {
            // If API data loading fails, log the error but continue
            android.util.Log.e("AutoCompleteProvider", "Failed to load API data for $language", e)
        }
    }
    
    /**
     * Loads a file from assets as a string
     */
    private fun loadAssetFile(fileName: String): String {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append('\n')
            }
            
            reader.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Represents an API item for auto-completion
     */
    data class ApiItem(
        val name: String,
        val type: String, // "class", "method", "field", etc.
        val signature: String? = null,
        val documentation: String? = null
    )
    
    /**
     * Represents a code snippet for auto-completion
     */
    data class CodeSnippet(
        val name: String,
        val code: String,
        val description: String? = null
    )
    
    companion object {
        // Fallback keywords if assets are not available
        private val JAVA_KEYWORDS = listOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
        )
        
        private val KOTLIN_KEYWORDS = listOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
            "in", "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "typeof", "val", "var", "when", "while", "by", "catch",
            "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import", "init", "param",
            "property", "receiver", "set", "setparam", "where", "actual", "abstract", "annotation", "companion",
            "const", "crossinline", "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
            "internal", "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected",
            "public", "reified", "sealed", "suspend", "tailrec", "vararg"
        )
        
        private val XML_KEYWORDS = listOf(
            "android", "app", "layout", "tools", "xmlns", "android:layout_width", "android:layout_height",
            "android:text", "android:id", "android:background", "android:padding", "android:layout_margin",
            "android:layout_gravity", "android:gravity", "android:orientation", "android:visibility",
            "android:textSize", "android:textColor", "android:onClick", "android:src", "android:contentDescription",
            "app:layout_constraintTop_toTopOf", "app:layout_constraintBottom_toBottomOf",
            "app:layout_constraintStart_toStartOf", "app:layout_constraintEnd_toEndOf"
        )
        
        private val DART_KEYWORDS = listOf(
            "abstract", "as", "assert", "async", "await", "break", "case", "catch", "class", "const",
            "continue", "covariant", "default", "deferred", "do", "dynamic", "else", "enum", "export",
            "extends", "extension", "external", "factory", "false", "final", "finally", "for", "Function",
            "get", "hide", "if", "implements", "import", "in", "interface", "is", "library", "mixin",
            "new", "null", "on", "operator", "part", "rethrow", "return", "set", "show", "static",
            "super", "switch", "sync", "this", "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield"
        )
        
        private val JS_KEYWORDS = listOf(
            "abstract", "arguments", "await", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "debugger", "default", "delete", "do", "double", "else", "enum", "eval",
            "export", "extends", "false", "final", "finally", "float", "for", "function", "goto", "if",
            "implements", "import", "in", "instanceof", "int", "interface", "let", "long", "native", "new",
            "null", "package", "private", "protected", "public", "return", "short", "static", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void",
            "volatile", "while", "with", "yield"
        )
        
        // Basic Android APIs for fallback
        private val ANDROID_APIS = listOf(
            ApiItem("Activity", "class", "Activity()", "Base class for activities that want to use the support library action bar features."),
            ApiItem("Context", "class", "Context", "Interface to global information about an application environment."),
            ApiItem("Intent", "class", "Intent()", "An intent is an abstract description of an operation to be performed."),
            ApiItem("Bundle", "class", "Bundle()", "A mapping from String keys to various Parcelable values."),
            ApiItem("View", "class", "View(Context context)", "This class represents the basic building block for user interface components."),
            ApiItem("TextView", "class", "TextView(Context context)", "Displays text to the user and optionally allows them to edit it."),
            ApiItem("Button", "class", "Button(Context context)", "Push-button widget that can be pressed or clicked by the user."),
            ApiItem("EditText", "class", "EditText(Context context)", "A user interface element for entering and modifying text."),
            ApiItem("RecyclerView", "class", "RecyclerView(Context context)", "A flexible view for providing a limited window into a large data set."),
            ApiItem("Fragment", "class", "Fragment()", "A Fragment is a piece of an application's user interface or behavior that can be placed in an Activity.")
        )
        
        // Code snippets for Java
        private val JAVA_SNIPPETS = listOf(
            CodeSnippet(
                "main",
                "public static void main(String[] args) {\n\t${0}\n}",
                "Main method"
            ),
            CodeSnippet(
                "sout",
                "System.out.println(${0});",
                "Print to standard output"
            ),
            CodeSnippet(
                "fori",
                "for (int ${1:i} = 0; ${1:i} < ${2:10}; ${1:i}++) {\n\t${0}\n}",
                "For loop with index"
            ),
            CodeSnippet(
                "foreach",
                "for (${1:Object} ${2:item} : ${3:collection}) {\n\t${0}\n}",
                "For-each loop"
            ),
            CodeSnippet(
                "if",
                "if (${1:condition}) {\n\t${0}\n}",
                "If statement"
            ),
            CodeSnippet(
                "ifelse",
                "if (${1:condition}) {\n\t${2}\n} else {\n\t${0}\n}",
                "If-else statement"
            ),
            CodeSnippet(
                "try",
                "try {\n\t${1}\n} catch (${2:Exception} e) {\n\t${0}\n}",
                "Try-catch block"
            ),
            CodeSnippet(
                "class",
                "public class ${1:ClassName} {\n\t${0}\n}",
                "Class definition"
            )
        )
        
        // Code snippets for Kotlin
        private val KOTLIN_SNIPPETS = listOf(
            CodeSnippet(
                "main",
                "fun main(args: Array<String>) {\n\t${0}\n}",
                "Main function"
            ),
            CodeSnippet(
                "println",
                "println(${0})",
                "Print to standard output"
            ),
            CodeSnippet(
                "fun",
                "fun ${1:functionName}(${2:params}): ${3:Unit} {\n\t${0}\n}",
                "Function definition"
            ),
            CodeSnippet(
                "class",
                "class ${1:ClassName}(${2:params}) {\n\t${0}\n}",
                "Class definition"
            ),
            CodeSnippet(
                "for",
                "for (${1:item} in ${2:collection}) {\n\t${0}\n}",
                "For loop"
            ),
            CodeSnippet(
                "if",
                "if (${1:condition}) {\n\t${0}\n}",
                "If statement"
            ),
            CodeSnippet(
                "when",
                "when (${1:expression}) {\n\t${2:value} -> ${3}\n\telse -> ${0}\n}",
                "When expression"
            ),
            CodeSnippet(
                "try",
                "try {\n\t${1}\n} catch (e: ${2:Exception}) {\n\t${0}\n}",
                "Try-catch block"
            )
        )
        
        // Code snippets for XML
        private val XML_SNIPPETS = listOf(
            CodeSnippet(
                "textview",
                "<TextView\n\tandroid:layout_width=\"wrap_content\"\n\tandroid:layout_height=\"wrap_content\"\n\tandroid:text=\"${1:Text}\" />${0}",
                "TextView element"
            ),
            CodeSnippet(
                "button",
                "<Button\n\tandroid:layout_width=\"wrap_content\"\n\tandroid:layout_height=\"wrap_content\"\n\tandroid:text=\"${1:Button}\" />${0}",
                "Button element"
            ),
            CodeSnippet(
                "edittext",
                "<EditText\n\tandroid:layout_width=\"match_parent\"\n\tandroid:layout_height=\"wrap_content\"\n\tandroid:hint=\"${1:Hint}\" />${0}",
                "EditText element"
            ),
            CodeSnippet(
                "linearlayout",
                "<LinearLayout\n\tandroid:layout_width=\"match_parent\"\n\tandroid:layout_height=\"wrap_content\"\n\tandroid:orientation=\"${1:vertical}\">\n\t${0}\n</LinearLayout>",
                "LinearLayout container"
            ),
            CodeSnippet(
                "constraintlayout",
                "<androidx.constraintlayout.widget.ConstraintLayout\n\tandroid:layout_width=\"match_parent\"\n\tandroid:layout_height=\"match_parent\">\n\t${0}\n</androidx.constraintlayout.widget.ConstraintLayout>",
                "ConstraintLayout container"
            )
        )
    }
}