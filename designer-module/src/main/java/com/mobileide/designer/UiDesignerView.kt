package com.mobileide.designer

import android.content.ClipData
import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import timber.log.Timber

/**
 * A UI designer view that allows drag and drop of UI components
 */
class UiDesignerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val designerCanvas: FrameLayout
    private val componentPalette: LinearLayout
    private val propertyPanel: LinearLayout
    
    private var selectedView: View? = null
    private var onComponentAddedListener: ((ComponentType, Float, Float) -> Unit)? = null
    private var onComponentSelectedListener: ((View) -> Unit)? = null
    
    init {
        // Inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.view_ui_designer, this, true)
        
        // Get references to views
        designerCanvas = view.findViewById(R.id.designer_canvas)
        componentPalette = view.findViewById(R.id.component_palette)
        propertyPanel = view.findViewById(R.id.property_panel)
        
        // Setup component palette
        setupComponentPalette()
        
        // Setup designer canvas
        setupDesignerCanvas()
    }
    
    /**
     * Sets up the component palette with draggable UI components
     */
    private fun setupComponentPalette() {
        // Add components to palette
        ComponentType.values().forEach { componentType ->
            val componentView = createComponentView(componentType)
            componentPalette.addView(componentView)
        }
    }
    
    /**
     * Creates a draggable component view for the palette
     */
    private fun createComponentView(componentType: ComponentType): View {
        return TextView(context).apply {
            text = componentType.displayName
            setPadding(16, 16, 16, 16)
            
            setOnLongClickListener {
                val clipData = ClipData.newPlainText(componentType.name, componentType.name)
                val shadow = DragShadowBuilder(it)
                it.startDragAndDrop(clipData, shadow, componentType, 0)
                true
            }
        }
    }
    
    /**
     * Sets up the designer canvas as a drop target for components
     */
    private fun setupDesignerCanvas() {
        designerCanvas.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipData.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    true
                }
                DragEvent.ACTION_DROP -> {
                    // Get the component type from the drag event
                    val componentType = event.localState as ComponentType
                    
                    // Add the component to the canvas
                    addComponentToCanvas(componentType, event.x, event.y)
                    
                    // Notify listener
                    onComponentAddedListener?.invoke(componentType, event.x, event.y)
                    
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Adds a component to the canvas at the specified position
     */
    private fun addComponentToCanvas(componentType: ComponentType, x: Float, y: Float) {
        val component = createComponent(componentType)
        
        // Set layout parameters
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = x.toInt()
        params.topMargin = y.toInt()
        
        // Add component to canvas
        designerCanvas.addView(component, params)
        
        // Make component selectable
        component.setOnClickListener {
            selectComponent(it)
        }
        
        // Select the new component
        selectComponent(component)
        
        Timber.d("Added component $componentType at ($x, $y)")
    }
    
    /**
     * Creates a component view based on the component type
     */
    private fun createComponent(componentType: ComponentType): View {
        return when (componentType) {
            ComponentType.TEXT_VIEW -> TextView(context).apply {
                text = "TextView"
                setPadding(8, 8, 8, 8)
            }
            ComponentType.BUTTON -> TextView(context).apply {
                text = "Button"
                setPadding(16, 8, 16, 8)
                setBackgroundResource(android.R.drawable.btn_default)
            }
            ComponentType.EDIT_TEXT -> TextView(context).apply {
                text = "EditText"
                setPadding(8, 8, 8, 8)
                setBackgroundResource(android.R.drawable.edit_text)
            }
            ComponentType.IMAGE_VIEW -> TextView(context).apply {
                text = "ImageView"
                setPadding(8, 8, 8, 8)
                setBackgroundResource(android.R.drawable.gallery_thumb)
            }
            ComponentType.LINEAR_LAYOUT -> LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                layoutParams = ViewGroup.LayoutParams(200, 200)
            }
        }
    }
    
    /**
     * Selects a component and shows its properties
     */
    private fun selectComponent(view: View) {
        // Deselect previous component
        selectedView?.setBackgroundResource(0)
        
        // Select new component
        selectedView = view
        view.setBackgroundResource(android.R.drawable.picture_frame)
        
        // Show properties
        showComponentProperties(view)
        
        // Notify listener
        onComponentSelectedListener?.invoke(view)
    }
    
    /**
     * Shows the properties of the selected component
     */
    private fun showComponentProperties(view: View) {
        // Clear property panel
        propertyPanel.removeAllViews()
        
        // Add properties based on view type
        when (view) {
            is TextView -> {
                addPropertyField("Text", view.text.toString()) { newValue ->
                    view.text = newValue
                }
                addPropertyField("Text Size", view.textSize.toString()) { newValue ->
                    view.textSize = newValue.toFloatOrNull() ?: view.textSize
                }
            }
            is LinearLayout -> {
                addPropertyField("Orientation", if (view.orientation == LinearLayout.VERTICAL) "vertical" else "horizontal") { newValue ->
                    view.orientation = if (newValue.lowercase() == "vertical") LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                }
            }
        }
        
        // Add common properties
        addPropertyField("Width", (view.layoutParams.width).toString()) { newValue ->
            view.layoutParams.width = newValue.toIntOrNull() ?: view.layoutParams.width
            view.requestLayout()
        }
        addPropertyField("Height", (view.layoutParams.height).toString()) { newValue ->
            view.layoutParams.height = newValue.toIntOrNull() ?: view.layoutParams.height
            view.requestLayout()
        }
    }
    
    /**
     * Adds a property field to the property panel
     */
    private fun addPropertyField(name: String, value: String, onValueChanged: (String) -> Unit) {
        // TODO: Implement property field UI
        // For now, just add a TextView
        val propertyView = TextView(context).apply {
            text = "$name: $value"
            setPadding(8, 8, 8, 8)
        }
        propertyPanel.addView(propertyView)
    }
    
    /**
     * Sets a listener for component added events
     */
    fun setOnComponentAddedListener(listener: (ComponentType, Float, Float) -> Unit) {
        onComponentAddedListener = listener
    }
    
    /**
     * Sets a listener for component selected events
     */
    fun setOnComponentSelectedListener(listener: (View) -> Unit) {
        onComponentSelectedListener = listener
    }
    
    /**
     * Clears the designer canvas
     */
    fun clearCanvas() {
        designerCanvas.removeAllViews()
        selectedView = null
        propertyPanel.removeAllViews()
    }
    
    /**
     * Generates XML layout code for the current design
     */
    fun generateLayoutXml(): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        
        // Root layout
        sb.append("<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
        sb.append("    android:layout_width=\"match_parent\"\n")
        sb.append("    android:layout_height=\"match_parent\">\n")
        
        // Add all components
        for (i in 0 until designerCanvas.childCount) {
            val child = designerCanvas.getChildAt(i)
            generateXmlForView(child, sb, 1)
        }
        
        sb.append("</FrameLayout>")
        
        return sb.toString()
    }
    
    /**
     * Generates XML for a view and its children
     */
    private fun generateXmlForView(view: View, sb: StringBuilder, indent: Int) {
        val indentStr = "    ".repeat(indent)
        
        // Get the component type
        val componentType = when (view) {
            is TextView -> {
                if (view.background != null && view.background.constantState?.equals(
                    resources.getDrawable(android.R.drawable.btn_default).constantState
                ) == true) {
                    ComponentType.BUTTON
                } else if (view.background != null && view.background.constantState?.equals(
                    resources.getDrawable(android.R.drawable.edit_text).constantState
                ) == true) {
                    ComponentType.EDIT_TEXT
                } else if (view.background != null && view.background.constantState?.equals(
                    resources.getDrawable(android.R.drawable.gallery_thumb).constantState
                ) == true) {
                    ComponentType.IMAGE_VIEW
                } else {
                    ComponentType.TEXT_VIEW
                }
            }
            is LinearLayout -> ComponentType.LINEAR_LAYOUT
            else -> null
        }
        
        if (componentType != null) {
            // Get layout params
            val params = view.layoutParams as? FrameLayout.LayoutParams
            
            // Start tag
            sb.append("$indentStr<${componentType.displayName}\n")
            
            // Common attributes
            sb.append("$indentStr    android:layout_width=\"${getLayoutParamString(view.layoutParams.width)}\"\n")
            sb.append("$indentStr    android:layout_height=\"${getLayoutParamString(view.layoutParams.height)}\"\n")
            
            // Position attributes
            if (params != null) {
                sb.append("$indentStr    android:layout_marginStart=\"${params.leftMargin}dp\"\n")
                sb.append("$indentStr    android:layout_marginTop=\"${params.topMargin}dp\"\n")
            }
            
            // Component-specific attributes
            when (view) {
                is TextView -> {
                    sb.append("$indentStr    android:text=\"${view.text}\"\n")
                    sb.append("$indentStr    android:textSize=\"${view.textSize / resources.displayMetrics.density}sp\"\n")
                }
                is LinearLayout -> {
                    val orientation = if (view.orientation == LinearLayout.VERTICAL) "vertical" else "horizontal"
                    sb.append("$indentStr    android:orientation=\"$orientation\"\n")
                }
            }
            
            // Check if the view has children
            if (view is ViewGroup && view.childCount > 0) {
                // Close the start tag
                sb.append("$indentStr>\n")
                
                // Add children
                for (i in 0 until view.childCount) {
                    generateXmlForView(view.getChildAt(i), sb, indent + 1)
                }
                
                // End tag
                sb.append("$indentStr</${componentType.displayName}>\n")
            } else {
                // Self-closing tag
                sb.append("$indentStr/>\n")
            }
        }
    }
    
    /**
     * Converts a layout parameter value to a string
     */
    private fun getLayoutParamString(value: Int): String {
        return when (value) {
            ViewGroup.LayoutParams.MATCH_PARENT -> "match_parent"
            ViewGroup.LayoutParams.WRAP_CONTENT -> "wrap_content"
            else -> "${value}dp"
        }
    }
}