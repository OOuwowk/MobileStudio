package com.mobileide.editor

/**
 * Provides syntax highlighting for different programming languages
 */
class SyntaxHighlighter {
    
    /**
     * Gets the language mode for a file extension
     */
    fun getLanguageMode(fileExtension: String): String {
        return when (fileExtension.lowercase()) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "xml" -> "xml"
            "json" -> "json"
            "js" -> "javascript"
            "html" -> "html"
            "css" -> "css"
            "md" -> "markdown"
            "gradle" -> "groovy"
            "properties" -> "properties"
            else -> "text"
        }
    }
    
    /**
     * Gets the language mode for a file path
     */
    fun getLanguageModeForFile(filePath: String): String {
        val extension = filePath.substringAfterLast('.', "")
        return getLanguageMode(extension)
    }
}