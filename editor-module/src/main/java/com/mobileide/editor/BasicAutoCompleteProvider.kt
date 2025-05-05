package com.mobileide.editor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides basic auto-completion functionality for the code editor
 */
@Singleton
class BasicAutoCompleteProvider @Inject constructor(
    private val context: Context
) {
    // Dictionaries for different languages
    private val javaKeywords = listOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
    )
    
    private val kotlinKeywords = listOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion", "const",
        "constructor", "continue", "crossinline", "data", "do", "dynamic", "else", "enum", "expect", "external",
        "false", "final", "finally", "for", "fun", "get", "if", "import", "in", "infix", "init", "inline",
        "inner", "interface", "internal", "is", "lateinit", "noinline", "null", "object", "open", "operator",
        "out", "override", "package", "private", "protected", "public", "reified", "return", "sealed", "set",
        "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "val", "var", "vararg", "when", "while"
    )
    
    private val androidClasses = listOf(
        "Activity", "AppCompatActivity", "Fragment", "View", "ViewGroup", "TextView", "Button", "EditText",
        "ImageView", "RecyclerView", "ListView", "LinearLayout", "RelativeLayout", "ConstraintLayout",
        "Intent", "Bundle", "Context", "SharedPreferences", "ContentProvider", "BroadcastReceiver"
    )
    
    private val androidLayouts = listOf(
        "LinearLayout", "RelativeLayout", "ConstraintLayout", "FrameLayout", "CoordinatorLayout",
        "DrawerLayout", "NestedScrollView", "ScrollView", "HorizontalScrollView", "CardView",
        "Toolbar", "AppBarLayout", "CollapsingToolbarLayout", "TabLayout", "ViewPager", "ViewPager2"
    )
    
    private val androidAttributes = listOf(
        "android:layout_width", "android:layout_height", "android:id", "android:text", "android:hint",
        "android:textSize", "android:textColor", "android:background", "android:padding", "android:margin",
        "android:layout_margin", "android:layout_gravity", "android:gravity", "android:orientation",
        "android:visibility", "android:onClick", "android:src", "android:contentDescription",
        "app:layout_constraintTop_toTopOf", "app:layout_constraintBottom_toBottomOf",
        "app:layout_constraintStart_toStartOf", "app:layout_constraintEnd_toEndOf"
    )
    
    /**
     * Gets auto-completion suggestions for the given prefix and language
     */
    fun getSuggestions(prefix: String, language: String): List<String> {
        if (prefix.isEmpty()) {
            return emptyList()
        }
        
        val keywords = when (language) {
            "java" -> javaKeywords + androidClasses
            "kotlin" -> kotlinKeywords + androidClasses
            "xml" -> androidLayouts + androidAttributes
            else -> emptyList()
        }
        
        return keywords.filter { it.startsWith(prefix) }
    }
    
    /**
     * Gets auto-completion suggestions as JSON array for use with Ace Editor
     */
    fun getSuggestionsAsJson(prefix: String, language: String): JSONArray {
        val suggestions = getSuggestions(prefix, language)
        val jsonArray = JSONArray()
        
        suggestions.forEach { suggestion ->
            val jsonObject = JSONObject()
            jsonObject.put("name", suggestion)
            jsonObject.put("value", suggestion)
            jsonObject.put("meta", getMetaForSuggestion(suggestion, language))
            jsonArray.put(jsonObject)
        }
        
        return jsonArray
    }
    
    /**
     * Gets the meta information for a suggestion
     */
    private fun getMetaForSuggestion(suggestion: String, language: String): String {
        return when {
            language == "java" && javaKeywords.contains(suggestion) -> "keyword"
            language == "kotlin" && kotlinKeywords.contains(suggestion) -> "keyword"
            androidClasses.contains(suggestion) -> "class"
            androidLayouts.contains(suggestion) -> "layout"
            androidAttributes.contains(suggestion) -> "attribute"
            else -> "snippet"
        }
    }
    
    /**
     * Sets up auto-completion in the editor
     */
    fun setupAutoComplete(editorView: CodeEditorView) {
        editorView.evaluateJavascript("""
            // Set up auto-completion
            var langTools = ace.require("ace/ext/language_tools");
            
            // Add custom completer
            var customCompleter = {
                getCompletions: function(editor, session, pos, prefix, callback) {
                    if (prefix.length === 0) {
                        callback(null, []);
                        return;
                    }
                    
                    // Get current language
                    var language = editor.session.getMode().$id.split('/').pop();
                    
                    // Request suggestions from Android
                    if (window.Android) {
                        window.Android.requestAutoComplete(prefix, language, function(suggestions) {
                            var completions = suggestions.map(function(suggestion) {
                                return {
                                    caption: suggestion.name,
                                    value: suggestion.value,
                                    meta: suggestion.meta
                                };
                            });
                            callback(null, completions);
                        });
                    }
                }
            };
            
            langTools.addCompleter(customCompleter);
        """, null)
    }
    
    /**
     * Creates code snippets for common patterns
     */
    fun createCodeSnippets() {
        try {
            // Create snippets directory
            val snippetsDir = File(context.filesDir, "snippets")
            if (!snippetsDir.exists()) {
                snippetsDir.mkdirs()
            }
            
            // Create Java snippets
            createJavaSnippets(snippetsDir)
            
            // Create Kotlin snippets
            createKotlinSnippets(snippetsDir)
            
            // Create XML snippets
            createXmlSnippets(snippetsDir)
            
            Timber.d("Code snippets created")
        } catch (e: Exception) {
            Timber.e(e, "Error creating code snippets")
        }
    }
    
    /**
     * Creates Java code snippets
     */
    private fun createJavaSnippets(snippetsDir: File) {
        val javaSnippetsFile = File(snippetsDir, "java_snippets.json")
        val javaSnippets = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "main")
                put("description", "Main method")
                put("code", """
                    public static void main(String[] args) {
                        // TODO: Add code here
                    }
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "sout")
                put("description", "System.out.println")
                put("code", """
                    System.out.println("${0:Hello World}");
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "class")
                put("description", "Class definition")
                put("code", """
                    public class ${1:ClassName} {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "for")
                put("description", "For loop")
                put("code", """
                    for (int ${1:i} = 0; ${1:i} < ${2:10}; ${1:i}++) {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "if")
                put("description", "If statement")
                put("code", """
                    if (${1:condition}) {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
        }
        
        javaSnippetsFile.writeText(javaSnippets.toString(2))
    }
    
    /**
     * Creates Kotlin code snippets
     */
    private fun createKotlinSnippets(snippetsDir: File) {
        val kotlinSnippetsFile = File(snippetsDir, "kotlin_snippets.json")
        val kotlinSnippets = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "main")
                put("description", "Main function")
                put("code", """
                    fun main(args: Array<String>) {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "println")
                put("description", "Print to console")
                put("code", """
                    println("${0:Hello World}")
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "class")
                put("description", "Class definition")
                put("code", """
                    class ${1:ClassName} {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "for")
                put("description", "For loop")
                put("code", """
                    for (${1:i} in 0 until ${2:10}) {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "if")
                put("description", "If expression")
                put("code", """
                    if (${1:condition}) {
                        ${0:// TODO: Add code here}
                    }
                """.trimIndent())
            })
        }
        
        kotlinSnippetsFile.writeText(kotlinSnippets.toString(2))
    }
    
    /**
     * Creates XML snippets
     */
    private fun createXmlSnippets(snippetsDir: File) {
        val xmlSnippetsFile = File(snippetsDir, "xml_snippets.json")
        val xmlSnippets = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "textview")
                put("description", "TextView element")
                put("code", """
                    <TextView
                        android:id="@+id/${1:textView}"
                        android:layout_width="${2:wrap_content}"
                        android:layout_height="${3:wrap_content}"
                        android:text="${0:Text}" />
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "button")
                put("description", "Button element")
                put("code", """
                    <Button
                        android:id="@+id/${1:button}"
                        android:layout_width="${2:wrap_content}"
                        android:layout_height="${3:wrap_content}"
                        android:text="${0:Button}" />
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "edittext")
                put("description", "EditText element")
                put("code", """
                    <EditText
                        android:id="@+id/${1:editText}"
                        android:layout_width="${2:match_parent}"
                        android:layout_height="${3:wrap_content}"
                        android:hint="${0:Hint}" />
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "linearlayout")
                put("description", "LinearLayout container")
                put("code", """
                    <LinearLayout
                        android:layout_width="${1:match_parent}"
                        android:layout_height="${2:wrap_content}"
                        android:orientation="${3:vertical}">
                        
                        ${0:<!-- Add views here -->}
                        
                    </LinearLayout>
                """.trimIndent())
            })
            
            put(JSONObject().apply {
                put("name", "constraintlayout")
                put("description", "ConstraintLayout container")
                put("code", """
                    <androidx.constraintlayout.widget.ConstraintLayout
                        android:layout_width="${1:match_parent}"
                        android:layout_height="${2:match_parent}">
                        
                        ${0:<!-- Add views here -->}
                        
                    </androidx.constraintlayout.widget.ConstraintLayout>
                """.trimIndent())
            })
        }
        
        xmlSnippetsFile.writeText(xmlSnippets.toString(2))
    }
}