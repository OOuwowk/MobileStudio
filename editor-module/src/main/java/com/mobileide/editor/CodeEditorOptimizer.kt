package com.mobileide.editor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Utility class to optimize the code editor performance and user experience
 */
class CodeEditorOptimizer(private val context: Context) {
    
    /**
     * Optimizes the editor for better performance
     */
    fun optimizeEditor(editorView: CodeEditorView) {
        // Optimize editor settings for better performance
        editorView.evaluateJavascript("""
            editor.setOptions({
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true,
                fontSize: 14,
                showPrintMargin: false,
                showGutter: true,
                highlightActiveLine: true,
                enableSnippets: true,
                tabSize: 4,
                useSoftTabs: true
            });
            
            // Performance optimizations for large files
            editor.getSession().setUseWorker(false);
            editor.setShowFoldWidgets(false);
            
            // Mobile-specific optimizations
            editor.setOptions({
                scrollPastEnd: 0.5,
                dragEnabled: false
            });
        """, null)
    }
    
    /**
     * Sets up mobile-friendly keyboard helpers
     */
    fun setupMobileKeyboardHelpers(editorView: CodeEditorView) {
        // Add keyboard toolbar for commonly used symbols
        editorView.evaluateJavascript("""
            // Create keyboard toolbar for frequently used symbols
            if (!document.querySelector('.keyboard-toolbar')) {
                var keyboardToolbar = document.createElement('div');
                keyboardToolbar.className = 'keyboard-toolbar';
                keyboardToolbar.style.position = 'fixed';
                keyboardToolbar.style.bottom = '0';
                keyboardToolbar.style.left = '0';
                keyboardToolbar.style.right = '0';
                keyboardToolbar.style.backgroundColor = '#f0f0f0';
                keyboardToolbar.style.padding = '5px';
                keyboardToolbar.style.display = 'flex';
                keyboardToolbar.style.justifyContent = 'space-around';
                keyboardToolbar.style.zIndex = '1000';
                
                var buttons = [
                    { text: '{', display: '{' },
                    { text: '}', display: '}' },
                    { text: '(', display: '(' },
                    { text: ')', display: ')' },
                    { text: '[', display: '[' },
                    { text: ']', display: ']' },
                    { text: ';', display: ';' },
                    { text: ':', display: ':' },
                    { text: '"', display: '"' },
                    { text: "'", display: "'" },
                    { text: '=', display: '=' },
                    { text: '    ', display: 'Tab' }
                ];
                
                buttons.forEach(function(btn) {
                    var button = document.createElement('button');
                    button.setAttribute('data-text', btn.text);
                    button.textContent = btn.display;
                    button.style.padding = '8px';
                    button.style.margin = '2px';
                    button.style.backgroundColor = '#ffffff';
                    button.style.border = '1px solid #cccccc';
                    button.style.borderRadius = '4px';
                    keyboardToolbar.appendChild(button);
                });
                
                document.body.appendChild(keyboardToolbar);
                
                // Add event listeners
                keyboardToolbar.addEventListener('click', function(e) {
                    if (e.target.tagName === 'BUTTON') {
                        var text = e.target.getAttribute('data-text');
                        editor.insert(text);
                        editor.focus();
                    }
                });
            }
        """, null)
    }
    
    /**
     * Preloads syntax highlighting definitions for better performance
     */
    fun preloadSyntaxDefinitions() {
        CoroutineScope(Dispatchers.IO).launch {
            // Create cache directory if it doesn't exist
            val cacheDir = File(context.cacheDir, "editor_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Preload syntax highlighting definitions
            val syntaxModes = listOf("java", "kotlin", "xml", "json", "gradle")
            
            // Create a configuration file to track preloaded modes
            val configFile = File(cacheDir, "preloaded_modes.txt")
            configFile.writeText(syntaxModes.joinToString("\n"))
        }
    }
    
    /**
     * Sets up code snippets for faster development
     */
    fun setupCodeSnippets(editorView: CodeEditorView) {
        editorView.evaluateJavascript("""
            // Define common code snippets
            var snippetManager = ace.require("ace/snippets").snippetManager;
            
            // Java snippets
            var javaSnippets = [
                {
                    name: "main",
                    content: "public static void main(String[] args) {\n\t${1:// TODO: Add code here}\n}"
                },
                {
                    name: "sout",
                    content: "System.out.println(${1:\"Hello World\"});"
                },
                {
                    name: "class",
                    content: "public class ${1:ClassName} {\n\t${2:// TODO: Add code here}\n}"
                },
                {
                    name: "for",
                    content: "for (int ${1:i} = 0; ${1:i} < ${2:10}; ${1:i}++) {\n\t${3:// TODO: Add code here}\n}"
                }
            ];
            
            // Kotlin snippets
            var kotlinSnippets = [
                {
                    name: "main",
                    content: "fun main(args: Array<String>) {\n\t${1:// TODO: Add code here}\n}"
                },
                {
                    name: "println",
                    content: "println(${1:\"Hello World\"})"
                },
                {
                    name: "class",
                    content: "class ${1:ClassName} {\n\t${2:// TODO: Add code here}\n}"
                },
                {
                    name: "for",
                    content: "for (${1:i} in 0 until ${2:10}) {\n\t${3:// TODO: Add code here}\n}"
                }
            ];
            
            // XML snippets
            var xmlSnippets = [
                {
                    name: "textview",
                    content: "<TextView\n\tandroid:id=\"@+id/${1:textView}\"\n\tandroid:layout_width=\"${2:wrap_content}\"\n\tandroid:layout_height=\"${3:wrap_content}\"\n\tandroid:text=\"${4:Text}\" />"
                },
                {
                    name: "button",
                    content: "<Button\n\tandroid:id=\"@+id/${1:button}\"\n\tandroid:layout_width=\"${2:wrap_content}\"\n\tandroid:layout_height=\"${3:wrap_content}\"\n\tandroid:text=\"${4:Button}\" />"
                },
                {
                    name: "edittext",
                    content: "<EditText\n\tandroid:id=\"@+id/${1:editText}\"\n\tandroid:layout_width=\"${2:match_parent}\"\n\tandroid:layout_height=\"${3:wrap_content}\"\n\tandroid:hint=\"${4:Hint}\" />"
                }
            ];
            
            // Register snippets
            snippetManager.register(javaSnippets, "java");
            snippetManager.register(kotlinSnippets, "kotlin");
            snippetManager.register(xmlSnippets, "xml");
        """, null)
    }
    
    /**
     * Enables offline mode for the editor
     */
    fun enableOfflineMode(editorView: CodeEditorView) {
        // Create a service worker to cache editor resources
        val serviceWorkerJs = """
            // Service worker for offline editor support
            self.addEventListener('install', function(event) {
                event.waitUntil(
                    caches.open('editor-cache-v1').then(function(cache) {
                        return cache.addAll([
                            '/editor/ace.js',
                            '/editor/mode-java.js',
                            '/editor/mode-kotlin.js',
                            '/editor/mode-xml.js',
                            '/editor/theme-monokai.js',
                            '/editor/ext-language_tools.js'
                        ]);
                    })
                );
            });
            
            self.addEventListener('fetch', function(event) {
                event.respondWith(
                    caches.match(event.request).then(function(response) {
                        return response || fetch(event.request);
                    })
                );
            });
        """.trimIndent()
        
        // Save service worker to assets
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serviceWorkerFile = File(context.cacheDir, "editor-sw.js")
                serviceWorkerFile.writeText(serviceWorkerJs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Register service worker
        editorView.evaluateJavascript("""
            // Register service worker for offline support
            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.register('/editor-sw.js')
                    .then(function(registration) {
                        console.log('Service Worker registered with scope:', registration.scope);
                    })
                    .catch(function(error) {
                        console.log('Service Worker registration failed:', error);
                    });
            }
        """, null)
    }
}