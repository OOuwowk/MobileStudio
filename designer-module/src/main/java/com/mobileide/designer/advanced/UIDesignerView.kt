package com.mobileide.designer.advanced

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mobileide.designer.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.StringReader
import java.util.UUID

/**
 * مصمم واجهات متقدم يسمح بالسحب والإفلات لتصميم واجهات أندرويد
 */
class UIDesignerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var designSurface: DesignSurface
    private lateinit var componentPalette: ComponentPalette
    private lateinit var propertyPanel: PropertyPanel
    
    private val viewModel: UIDesignerViewModel by lazy {
        if (context is ViewModelStoreOwner) {
            ViewModelProvider(context).get(UIDesignerViewModel::class.java)
        } else {
            throw IllegalStateException("Context must be a ViewModelStoreOwner")
        }
    }
    
    init {
        // تضمين التخطيط
        LayoutInflater.from(context).inflate(R.layout.view_ui_designer_advanced, this, true)
        
        // تهيئة المكونات
        designSurface = findViewById(R.id.design_surface)
        componentPalette = findViewById(R.id.component_palette)
        propertyPanel = findViewById(R.id.property_panel)
        
        // إعداد سطح التصميم
        setupDesignSurface()
        
        // إعداد لوحة المكونات
        setupComponentPalette()
        
        // إعداد لوحة الخصائص
        setupPropertyPanel()
    }
    
    private fun setupDesignSurface() {
        // إعداد مستمع السحب والإفلات
        designSurface.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    // الحصول على بيانات العنصر المسحوب
                    val componentType = event.clipData.getItemAt(0).text.toString()
                    
                    // إنشاء عنصر جديد
                    val component = createComponent(componentType)
                    
                    // تحديد موقع العنصر
                    component.x = event.x - (component.width / 2)
                    component.y = event.y - (component.height / 2)
                    
                    // إضافة العنصر إلى سطح التصميم
                    designSurface.addView(component)
                    
                    // تحديث نموذج التصميم
                    viewModel.addComponent(componentType, event.x, event.y)
                    
                    // تحديد العنصر المضاف
                    component.setOnClickListener {
                        val componentId = it.tag as? String
                        if (componentId != null) {
                            viewModel.selectComponent(componentId)
                        }
                    }
                    
                    true
                }
                else -> true
            }
        }
        
        // مراقبة تغييرات نموذج التصميم
        if (context is LifecycleOwner) {
            viewModel.designModel.observe(context as LifecycleOwner) { model ->
                updateDesignSurface(model)
            }
        }
    }
    
    private fun setupComponentPalette() {
        // إضافة مكونات واجهة المستخدم إلى اللوحة
        val components = listOf(
            ComponentInfo("TextView", R.drawable.ic_textview),
            ComponentInfo("Button", R.drawable.ic_button),
            ComponentInfo("EditText", R.drawable.ic_edittext),
            ComponentInfo("ImageView", R.drawable.ic_imageview),
            ComponentInfo("LinearLayout", R.drawable.ic_linearlayout),
            ComponentInfo("ConstraintLayout", R.drawable.ic_constraintlayout),
            ComponentInfo("RecyclerView", R.drawable.ic_recyclerview),
            ComponentInfo("CardView", R.drawable.ic_cardview)
        )
        
        componentPalette.setComponents(components)
        
        // إعداد مستمع السحب
        componentPalette.setOnComponentDragStartListener { componentType, view ->
            // إنشاء بيانات السحب
            val clipData = ClipData.newPlainText(componentType, componentType)
            val shadowBuilder = DragShadowBuilder(view)
            
            // بدء عملية السحب
            view.startDragAndDrop(clipData, shadowBuilder, view, 0)
            
            // إخفاء العنصر الأصلي أثناء السحب
            view.visibility = View.INVISIBLE
            
            true
        }
        
        // إعداد مستمع انتهاء السحب
        componentPalette.setOnComponentDragEndListener { view ->
            // إظهار العنصر الأصلي بعد انتهاء السحب
            view.visibility = View.VISIBLE
        }
    }
    
    private fun setupPropertyPanel() {
        // إعداد لوحة الخصائص
        propertyPanel.setOnPropertyChangeListener { property, value ->
            // تحديث خاصية العنصر المحدد
            viewModel.updateSelectedComponentProperty(property, value)
        }
        
        // مراقبة العنصر المحدد
        if (context is LifecycleOwner) {
            viewModel.selectedComponent.observe(context as LifecycleOwner) { component ->
                if (component != null) {
                    propertyPanel.setProperties(component.properties)
                } else {
                    propertyPanel.clearProperties()
                }
            }
        }
    }
    
    private fun createComponent(componentType: String): View {
        val componentId = UUID.randomUUID().toString()
        
        val view = when (componentType) {
            "TextView" -> TextView(context).apply {
                text = "Text View"
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            "Button" -> Button(context).apply {
                text = "Button"
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            "EditText" -> EditText(context).apply {
                hint = "Edit Text"
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            "ImageView" -> ImageView(context).apply {
                setImageResource(R.drawable.ic_image_placeholder)
                layoutParams = ViewGroup.LayoutParams(
                    100,
                    100
                )
            }
            "LinearLayout" -> LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    200,
                    200
                )
                setBackgroundColor(Color.LTGRAY)
            }
            "ConstraintLayout" -> ConstraintLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    300,
                    300
                )
                setBackgroundColor(Color.LTGRAY)
            }
            "RecyclerView" -> androidx.recyclerview.widget.RecyclerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
                )
                setBackgroundColor(Color.LTGRAY)
            }
            "CardView" -> androidx.cardview.widget.CardView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    200,
                    200
                )
                radius = 8f
                cardElevation = 4f
                setContentPadding(16, 16, 16, 16)
                setCardBackgroundColor(Color.WHITE)
            }
            else -> View(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    100,
                    100
                )
                setBackgroundColor(Color.GRAY)
            }
        }
        
        // تعيين معرف العنصر
        view.tag = componentId
        
        // إضافة مستمع النقر لتحديد العنصر
        view.setOnClickListener {
            viewModel.selectComponent(componentId)
        }
        
        // إضافة مستمع السحب لتحريك العنصر
        view.setOnLongClickListener {
            val clipData = ClipData.newPlainText("component_move", componentId)
            val shadowBuilder = DragShadowBuilder(it)
            it.startDragAndDrop(clipData, shadowBuilder, it, 0)
            true
        }
        
        return view
    }
    
    private fun updateDesignSurface(model: DesignModel) {
        // تحديث سطح التصميم بناءً على النموذج
        designSurface.removeAllViews()
        
        model.components.forEach { component ->
            val view = createComponent(component.type)
            view.x = component.x
            view.y = component.y
            
            // تطبيق الخصائص
            applyProperties(view, component.properties)
            
            designSurface.addView(view)
        }
    }
    
    private fun applyProperties(view: View, properties: Map<String, Any>) {
        properties.forEach { (property, value) ->
            when (property) {
                "text" -> if (view is TextView) view.text = value as String
                "hint" -> if (view is EditText) view.hint = value as String
                "textColor" -> if (view is TextView) view.setTextColor(value as Int)
                "backgroundColor" -> view.setBackgroundColor(value as Int)
                "width" -> {
                    val layoutParams = view.layoutParams ?: ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.width = when (value) {
                        "MATCH_PARENT" -> ViewGroup.LayoutParams.MATCH_PARENT
                        "WRAP_CONTENT" -> ViewGroup.LayoutParams.WRAP_CONTENT
                        is Int -> value
                        is String -> value.toIntOrNull() ?: ViewGroup.LayoutParams.WRAP_CONTENT
                        else -> ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    view.layoutParams = layoutParams
                }
                "height" -> {
                    val layoutParams = view.layoutParams ?: ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.height = when (value) {
                        "MATCH_PARENT" -> ViewGroup.LayoutParams.MATCH_PARENT
                        "WRAP_CONTENT" -> ViewGroup.LayoutParams.WRAP_CONTENT
                        is Int -> value
                        is String -> value.toIntOrNull() ?: ViewGroup.LayoutParams.WRAP_CONTENT
                        else -> ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    view.layoutParams = layoutParams
                }
                "padding" -> {
                    val padding = when (value) {
                        is Int -> value
                        is String -> value.toIntOrNull() ?: 0
                        else -> 0
                    }
                    view.setPadding(padding, padding, padding, padding)
                }
                "orientation" -> if (view is LinearLayout) {
                    view.orientation = when (value) {
                        "vertical", "VERTICAL" -> LinearLayout.VERTICAL
                        "horizontal", "HORIZONTAL" -> LinearLayout.HORIZONTAL
                        else -> LinearLayout.VERTICAL
                    }
                }
                "elevation" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    view.elevation = when (value) {
                        is Float -> value
                        is Int -> value.toFloat()
                        is String -> value.toFloatOrNull() ?: 0f
                        else -> 0f
                    }
                }
                "alpha" -> {
                    view.alpha = when (value) {
                        is Float -> value
                        is Double -> value.toFloat()
                        is Int -> value.toFloat() / 100f
                        is String -> value.toFloatOrNull() ?: 1f
                        else -> 1f
                    }
                }
                "visibility" -> {
                    view.visibility = when (value) {
                        "visible", "VISIBLE" -> View.VISIBLE
                        "invisible", "INVISIBLE" -> View.INVISIBLE
                        "gone", "GONE" -> View.GONE
                        else -> View.VISIBLE
                    }
                }
                "id" -> {
                    // لا نفعل شيئًا هنا، لأن المعرف يتم تعيينه بواسطة tag
                }
                // المزيد من الخصائص...
            }
        }
    }
    
    // توليد XML من التصميم
    fun generateXml(): String {
        return viewModel.generateXml()
    }
    
    // تحميل تصميم من XML
    fun loadFromXml(xml: String) {
        viewModel.loadFromXml(xml)
    }
}

/**
 * سطح التصميم حيث يتم وضع العناصر
 */
class DesignSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    init {
        // تعيين خلفية شبكية
        setBackgroundResource(R.drawable.bg_design_surface)
    }
}

/**
 * لوحة المكونات التي تحتوي على العناصر القابلة للسحب
 */
class ComponentPalette @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onComponentDragStartListener: ((String, View) -> Boolean)? = null
    private var onComponentDragEndListener: ((View) -> Unit)? = null
    
    init {
        orientation = VERTICAL
    }
    
    // تعيين المكونات
    fun setComponents(components: List<ComponentInfo>) {
        removeAllViews()
        
        components.forEach { component ->
            val componentView = LayoutInflater.from(context).inflate(
                R.layout.item_component,
                this,
                false
            )
            
            val iconView = componentView.findViewById<ImageView>(R.id.component_icon)
            val nameView = componentView.findViewById<TextView>(R.id.component_name)
            
            iconView.setImageResource(component.iconResId)
            nameView.text = component.type
            
            componentView.tag = component.type
            
            componentView.setOnLongClickListener { view ->
                val result = onComponentDragStartListener?.invoke(component.type, view) ?: false
                if (result) {
                    view.postDelayed({
                        onComponentDragEndListener?.invoke(view)
                    }, 300)
                }
                result
            }
            
            addView(componentView)
        }
    }
    
    // تعيين مستمع بدء السحب
    fun setOnComponentDragStartListener(listener: (String, View) -> Boolean) {
        onComponentDragStartListener = listener
    }
    
    // تعيين مستمع انتهاء السحب
    fun setOnComponentDragEndListener(listener: (View) -> Unit) {
        onComponentDragEndListener = listener
    }
}

/**
 * لوحة الخصائص لتعديل خصائص العنصر المحدد
 */
class PropertyPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onPropertyChangeListener: ((String, Any) -> Unit)? = null
    private val propertyViews = mutableMapOf<String, View>()
    
    init {
        orientation = VERTICAL
    }
    
    // تعيين الخصائص
    fun setProperties(properties: Map<String, Any>) {
        removeAllViews()
        propertyViews.clear()
        
        // إضافة عنوان
        val titleView = TextView(context).apply {
            text = "الخصائص"
            textSize = 18f
            setPadding(16, 16, 16, 16)
        }
        addView(titleView)
        
        // إضافة خصائص العنصر
        properties.forEach { (property, value) ->
            val propertyView = createPropertyView(property, value)
            addView(propertyView)
            propertyViews[property] = propertyView
        }
    }
    
    // إنشاء عرض الخاصية
    private fun createPropertyView(property: String, value: Any): View {
        val propertyLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }
        
        // إضافة اسم الخاصية
        val nameView = TextView(context).apply {
            text = property
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        propertyLayout.addView(nameView)
        
        // إضافة قيمة الخاصية
        val valueView = when (value) {
            is String -> EditText(context).apply {
                setText(value)
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        onPropertyChangeListener?.invoke(property, text.toString())
                    }
                }
            }
            is Int -> EditText(context).apply {
                setText(value.toString())
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val intValue = text.toString().toIntOrNull() ?: value
                        onPropertyChangeListener?.invoke(property, intValue)
                    }
                }
            }
            is Float -> EditText(context).apply {
                setText(value.toString())
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val floatValue = text.toString().toFloatOrNull() ?: value
                        onPropertyChangeListener?.invoke(property, floatValue)
                    }
                }
            }
            is Boolean -> {
                val switch = androidx.appcompat.widget.SwitchCompat(context).apply {
                    isChecked = value
                    layoutParams = LayoutParams(
                        0,
                        LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    setOnCheckedChangeListener { _, isChecked ->
                        onPropertyChangeListener?.invoke(property, isChecked)
                    }
                }
                switch
            }
            else -> TextView(context).apply {
                text = value.toString()
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
        }
        propertyLayout.addView(valueView)
        
        return propertyLayout
    }
    
    // مسح الخصائص
    fun clearProperties() {
        removeAllViews()
        propertyViews.clear()
        
        // إضافة رسالة
        val messageView = TextView(context).apply {
            text = "لم يتم تحديد أي عنصر"
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        addView(messageView)
    }
    
    // تعيين مستمع تغيير الخاصية
    fun setOnPropertyChangeListener(listener: (String, Any) -> Unit) {
        onPropertyChangeListener = listener
    }
}

/**
 * نموذج البيانات للتصميم
 */
data class DesignModel(
    val components: MutableList<DesignComponent>
)

/**
 * نموذج البيانات للعنصر
 */
data class DesignComponent(
    val id: String,
    val type: String,
    var x: Float,
    var y: Float,
    val properties: MutableMap<String, Any>
)

/**
 * معلومات المكون
 */
data class ComponentInfo(
    val type: String,
    val iconResId: Int
)