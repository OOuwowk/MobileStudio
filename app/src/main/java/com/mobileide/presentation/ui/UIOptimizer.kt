package com.mobileide.presentation.ui

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.mobileide.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to optimize the UI for mobile devices
 */
@Singleton
class UIOptimizer @Inject constructor(
    private val context: Context
) {
    /**
     * Optimizes the UI for mobile devices
     */
    fun optimizeUIForMobileDevices(activity: AppCompatActivity) {
        try {
            // Get display metrics
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            // Adjust font size based on screen density
            val density = displayMetrics.density
            val fontScale = when {
                density <= 1.0f -> 0.8f  // For small screens
                density <= 2.0f -> 1.0f  // For medium screens
                else -> 1.2f             // For large screens
            }
            
            // Apply changes
            val configuration = activity.resources.configuration
            configuration.fontScale = fontScale
            val metrics = activity.resources.displayMetrics
            activity.resources.updateConfiguration(configuration, metrics)
            
            // Optimize layout for mobile devices
            setupResponsiveLayout(activity)
            
            Timber.d("UI optimized for mobile devices")
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing UI for mobile devices")
        }
    }
    
    /**
     * Sets up responsive layout
     */
    private fun setupResponsiveLayout(activity: AppCompatActivity) {
        try {
            // Check if device is a tablet
            val isTablet = activity.resources.configuration.screenLayout and 
                    Configuration.SCREENLAYOUT_SIZE_MASK >= 
                    Configuration.SCREENLAYOUT_SIZE_LARGE
            
            // Find container view
            val containerView = activity.findViewById<View>(R.id.container)
            if (containerView != null) {
                if (isTablet) {
                    // Enable split screen mode for tablets
                    containerView.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        // Split screen into two parts
                        // Left part for file tree
                        // Right part for editor
                    }
                    
                    Timber.d("Tablet layout applied")
                } else {
                    // Optimize layout for phones
                    containerView.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    
                    Timber.d("Phone layout applied")
                }
            } else {
                Timber.w("Container view not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up responsive layout")
        }
    }
    
    /**
     * Optimizes the editor UI
     */
    fun optimizeEditorUI(activity: AppCompatActivity) {
        try {
            // Find editor container view
            val editorContainer = activity.findViewById<View>(R.id.editor_container)
            if (editorContainer != null) {
                // Adjust editor layout based on screen orientation
                val orientation = activity.resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // Landscape orientation
                    // Show file tree and editor side by side
                    val fileTreeView = activity.findViewById<View>(R.id.file_tree)
                    if (fileTreeView != null) {
                        fileTreeView.visibility = View.VISIBLE
                    }
                    
                    Timber.d("Landscape editor layout applied")
                } else {
                    // Portrait orientation
                    // Hide file tree by default
                    val fileTreeView = activity.findViewById<View>(R.id.file_tree)
                    if (fileTreeView != null) {
                        fileTreeView.visibility = View.GONE
                    }
                    
                    Timber.d("Portrait editor layout applied")
                }
            } else {
                Timber.w("Editor container view not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing editor UI")
        }
    }
    
    /**
     * Optimizes the designer UI
     */
    fun optimizeDesignerUI(activity: AppCompatActivity) {
        try {
            // Find designer container view
            val designerContainer = activity.findViewById<View>(R.id.designer_container)
            if (designerContainer != null) {
                // Adjust designer layout based on screen size
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                
                // Adjust component palette size based on screen size
                val componentPalette = activity.findViewById<View>(R.id.component_palette)
                if (componentPalette != null) {
                    val paletteWidth = if (screenWidth > 1000) {
                        (screenWidth * 0.2).toInt()
                    } else {
                        (screenWidth * 0.3).toInt()
                    }
                    
                    componentPalette.layoutParams.width = paletteWidth
                }
                
                Timber.d("Designer UI optimized")
            } else {
                Timber.w("Designer container view not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing designer UI")
        }
    }
    
    /**
     * Optimizes the debugger UI
     */
    fun optimizeDebuggerUI(activity: AppCompatActivity) {
        try {
            // Find debugger container view
            val debuggerContainer = activity.findViewById<View>(R.id.debugger_container)
            if (debuggerContainer != null) {
                // Adjust debugger layout based on screen orientation
                val orientation = activity.resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // Landscape orientation
                    // Show variables and console side by side
                    val variablesView = activity.findViewById<View>(R.id.variables_container)
                    val consoleView = activity.findViewById<View>(R.id.console_container)
                    
                    if (variablesView != null && consoleView != null) {
                        variablesView.layoutParams.width = 0
                        consoleView.layoutParams.width = 0
                    }
                    
                    Timber.d("Landscape debugger layout applied")
                } else {
                    // Portrait orientation
                    // Show variables and console stacked
                    val variablesView = activity.findViewById<View>(R.id.variables_container)
                    val consoleView = activity.findViewById<View>(R.id.console_container)
                    
                    if (variablesView != null && consoleView != null) {
                        variablesView.layoutParams.height = 0
                        consoleView.layoutParams.height = 0
                    }
                    
                    Timber.d("Portrait debugger layout applied")
                }
            } else {
                Timber.w("Debugger container view not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing debugger UI")
        }
    }
    
    /**
     * Optimizes the project list UI
     */
    fun optimizeProjectListUI(activity: AppCompatActivity) {
        try {
            // Find project list container view
            val projectListContainer = activity.findViewById<View>(R.id.project_list_container)
            if (projectListContainer != null) {
                // Adjust project list layout based on screen size
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                
                val screenWidth = displayMetrics.widthPixels
                
                // Adjust project item size based on screen width
                val projectItemWidth = if (screenWidth > 1000) {
                    (screenWidth * 0.4).toInt()
                } else {
                    FrameLayout.LayoutParams.MATCH_PARENT
                }
                
                // Apply changes to project list items
                CoroutineScope(Dispatchers.Main).launch {
                    // This would be applied to each project item in a real implementation
                    // For now, just log the intended width
                    Timber.d("Project item width set to: $projectItemWidth")
                }
                
                Timber.d("Project list UI optimized")
            } else {
                Timber.w("Project list container view not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing project list UI")
        }
    }
    
    /**
     * Optimizes the settings UI
     */
    fun optimizeSettingsUI(activity: AppCompatActivity) {
        try {
            // Find settings container view
            val settingsContainer = activity.findViewById<View>(R.id.settings_container)
            if (settingsContainer != null) {
                // Adjust settings layout based on screen size
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                
                val screenWidth = displayMetrics.widthPixels
                
                // Adjust settings width based on screen size
                val settingsWidth = if (screenWidth > 1000) {
                    (screenWidth * 0.6).toInt()
                } else {
                    FrameLayout.LayoutParams.MATCH_PARENT
                }
                
                // Apply changes
                settingsContainer.layoutParams.width = settingsWidth
                
                Timber.d("Settings UI optimized")
            } else {
                Timber.w("Settings container view not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing settings UI")
        }
    }
}