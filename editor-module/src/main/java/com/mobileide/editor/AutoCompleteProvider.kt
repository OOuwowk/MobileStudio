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
    fun getSuggestions(prefix: String, position: Int): JSONArray {
        val result = JSONArray()
        
        // TODO: Implement more sophisticated auto-complete based on context
        // For now, just return basic suggestions
        
        // Add language keywords
        val currentLanguage = "java" // This should be determined based on the current file
        val keywords = languageKeywords[currentLanguage] ?: emptyList()
        
        keywords.filter { it.startsWith(prefix) }.forEach { keyword ->
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
        
        apis.filter { it.name.startsWith(prefix) }.forEach { api ->
            val suggestion = JSONObject().apply {
                put("value", api.name)
                put("caption", api.name)
                put("meta", api.type)
                put("score", 90)
            }
            result.put(suggestion)
        }
        
        return result
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
            
            // TODO: Load API data from JSON files
            
        } catch (e: Exception) {
            // If assets are not available, use hardcoded fallbacks
            languageKeywords["java"] = JAVA_KEYWORDS
            languageKeywords["kotlin"] = KOTLIN_KEYWORDS
            languageKeywords["xml"] = XML_KEYWORDS
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
    }
}