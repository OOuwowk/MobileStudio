package com.mobileide.editor

import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import timber.log.Timber

/**
 * A code editor view that uses a WebView with a JavaScript editor (Ace or Monaco)
 */
class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val editorWebView: WebView
    private val syntaxHighlighter: SyntaxHighlighter
    private val autoCompleteProvider: AutoCompleteProvider
    
    private var onTextChangedListener: ((String) -> Unit)? = null
    
    init {
        // Setup WebView with JavaScript editor
        editorWebView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(EditorJsInterface(), "Android")
            loadUrl("file:///android_asset/editor/index.html")
        }
        
        // Setup syntax highlighter
        syntaxHighlighter = SyntaxHighlighter()
        
        // Setup auto-complete provider
        autoCompleteProvider = AutoCompleteProvider(context)
        
        // Add WebView to layout
        addView(editorWebView)
    }
    
    /**
     * JavaScript interface for communication between WebView and Android
     */
    private inner class EditorJsInterface {
        @JavascriptInterface
        fun onTextChanged(text: String) {
            Timber.d("Text changed: ${text.take(20)}...")
            onTextChangedListener?.invoke(text)
        }
        
        @JavascriptInterface
        fun requestAutoComplete(prefix: String, position: Int): String {
            return autoCompleteProvider.getSuggestions(prefix, position).toString()
        }
        
        @JavascriptInterface
        fun log(message: String) {
            Timber.d("Editor log: $message")
        }
    }
    
    /**
     * Sets the text in the editor
     */
    fun setText(text: String) {
        val escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        editorWebView.evaluateJavascript("editor.setValue(\"$escapedText\");", null)
    }
    
    /**
     * Gets the text from the editor
     */
    fun getText(callback: (String) -> Unit) {
        editorWebView.evaluateJavascript("editor.getValue();") { value ->
            // Remove quotes from the returned JSON string
            val unquoted = value.substring(1, value.length - 1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
            callback(unquoted)
        }
    }
    
    /**
     * Sets the language for syntax highlighting
     */
    fun setLanguage(language: String) {
        editorWebView.evaluateJavascript("editor.session.setMode(\"ace/mode/$language\");", null)
    }
    
    /**
     * Sets a listener for text changes
     */
    fun setOnTextChangedListener(listener: (String) -> Unit) {
        onTextChangedListener = listener
    }
    
    /**
     * Sets the theme of the editor
     */
    fun setTheme(theme: String) {
        editorWebView.evaluateJavascript("editor.setTheme(\"ace/theme/$theme\");", null)
    }
    
    /**
     * Sets the font size
     */
    fun setFontSize(size: Int) {
        editorWebView.evaluateJavascript("document.getElementById('editor').style.fontSize='${size}px';", null)
    }
    
    /**
     * Undo the last edit
     */
    fun undo() {
        editorWebView.evaluateJavascript("editor.undo();", null)
    }
    
    /**
     * Redo the last undone edit
     */
    fun redo() {
        editorWebView.evaluateJavascript("editor.redo();", null)
    }
    
    /**
     * Find text in the editor
     */
    fun find(text: String, caseSensitive: Boolean = false, wholeWord: Boolean = false, regex: Boolean = false) {
        val options = "{" +
            "caseSensitive: $caseSensitive, " +
            "wholeWord: $wholeWord, " +
            "regExp: $regex" +
            "}"
        editorWebView.evaluateJavascript("editor.find('$text', $options);", null)
    }
    
    /**
     * Replace text in the editor
     */
    fun replace(text: String, replacement: String) {
        editorWebView.evaluateJavascript("editor.replace('$replacement');", null)
    }
    
    /**
     * Replace all occurrences of text in the editor
     */
    fun replaceAll(text: String, replacement: String) {
        editorWebView.evaluateJavascript("editor.replaceAll('$replacement');", null)
    }
}