package com.mobileide.designer.advanced

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.StringReader
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel لمصمم الواجهات المتقدم
 */
class UIDesignerViewModel @Inject constructor() : ViewModel() {
    
    private val _designModel = MutableLiveData<DesignModel>()
    val designModel: LiveData<DesignModel> = _designModel
    
    private val _selectedComponent = MutableLiveData<DesignComponent?>()
    val selectedComponent: LiveData<DesignComponent?> = _selectedComponent
    
    init {
        _designModel.value = DesignModel(mutableListOf())
    }
    
    /**
     * إضافة عنصر جديد إلى التصميم
     */
    fun addComponent(type: String, x: Float, y: Float) {
        val model = _designModel.value ?: DesignModel(mutableListOf())
        val component = DesignComponent(
            id = UUID.randomUUID().toString(),
            type = type,
            x = x,
            y = y,
            properties = getDefaultProperties(type)
        )
        
        model.components.add(component)
        _designModel.value = model
        _selectedComponent.value = component
    }
    
    /**
     * تحديد عنصر
     */
    fun selectComponent(id: String) {
        val model = _designModel.value ?: return
        _selectedComponent.value = model.components.find { it.id == id }
    }
    
    /**
     * تحديث خاصية العنصر المحدد
     */
    fun updateSelectedComponentProperty(property: String, value: Any) {
        val component = _selectedComponent.value ?: return
        val model = _designModel.value ?: return
        
        // تحديث الخاصية
        component.properties[property] = value
        
        // تحديث النموذج
        _designModel.value = model
    }
    
    /**
     * حذف العنصر المحدد
     */
    fun deleteSelectedComponent() {
        val component = _selectedComponent.value ?: return
        val model = _designModel.value ?: return
        
        // حذف العنصر
        model.components.removeIf { it.id == component.id }
        
        // تحديث النموذج
        _designModel.value = model
        _selectedComponent.value = null
    }
    
    /**
     * نقل العنصر المحدد
     */
    fun moveSelectedComponent(x: Float, y: Float) {
        val component = _selectedComponent.value ?: return
        val model = _designModel.value ?: return
        
        // تحديث الموقع
        component.x = x
        component.y = y
        
        // تحديث النموذج
        _designModel.value = model
    }
    
    /**
     * توليد XML من التصميم
     */
    fun generateXml(): String {
        val model = _designModel.value ?: return ""
        val xmlBuilder = StringBuilder()
        
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        
        // تحديد العنصر الجذر
        val rootComponent = model.components.find { it.type.endsWith("Layout") }
            ?: DesignComponent(
                id = "root",
                type = "androidx.constraintlayout.widget.ConstraintLayout",
                x = 0f,
                y = 0f,
                properties = getDefaultProperties("ConstraintLayout")
            )
        
        // توليد XML للعنصر الجذر
        generateComponentXml(xmlBuilder, rootComponent, model.components.filter { it.id != rootComponent.id }, 0)
        
        return xmlBuilder.toString()
    }
    
    /**
     * توليد XML لعنصر
     */
    private fun generateComponentXml(
        builder: StringBuilder,
        component: DesignComponent,
        children: List<DesignComponent>,
        indent: Int
    ) {
        val indentStr = " ".repeat(indent)
        
        // تحويل نوع العنصر إلى اسم XML
        val xmlTag = when (component.type) {
            "TextView" -> "TextView"
            "Button" -> "Button"
            "EditText" -> "EditText"
            "ImageView" -> "ImageView"
            "LinearLayout" -> "LinearLayout"
            "ConstraintLayout" -> "androidx.constraintlayout.widget.ConstraintLayout"
            "RecyclerView" -> "androidx.recyclerview.widget.RecyclerView"
            "CardView" -> "androidx.cardview.widget.CardView"
            else -> component.type
        }
        
        // فتح العنصر
        builder.append("$indentStr<$xmlTag\n")
        
        // إضافة الخصائص
        component.properties.forEach { (property, value) ->
            val xmlProperty = when (property) {
                "text" -> "android:text"
                "hint" -> "android:hint"
                "textColor" -> "android:textColor"
                "backgroundColor" -> "android:background"
                "width" -> "android:layout_width"
                "height" -> "android:layout_height"
                "padding" -> "android:padding"
                "orientation" -> "android:orientation"
                "elevation" -> "android:elevation"
                "alpha" -> "android:alpha"
                "visibility" -> "android:visibility"
                else -> "android:$property"
            }
            
            val xmlValue = when (value) {
                is Int -> value.toString()
                is Float -> value.toString()
                is Boolean -> value.toString()
                "MATCH_PARENT" -> "match_parent"
                "WRAP_CONTENT" -> "wrap_content"
                "VERTICAL" -> "vertical"
                "HORIZONTAL" -> "horizontal"
                "VISIBLE" -> "visible"
                "INVISIBLE" -> "invisible"
                "GONE" -> "gone"
                is String -> "\"$value\""
                else -> value.toString()
            }
            
            builder.append("$indentStr    $xmlProperty=$xmlValue\n")
        }
        
        // إضافة العناصر الفرعية أو إغلاق العنصر
        if (children.isEmpty()) {
            builder.append("$indentStr/>\n")
        } else {
            builder.append("$indentStr>\n")
            
            // إضافة العناصر الفرعية
            children.forEach { child ->
                generateComponentXml(builder, child, emptyList(), indent + 4)
            }
            
            // إغلاق العنصر
            builder.append("$indentStr</$xmlTag>\n")
        }
    }
    
    /**
     * تحميل تصميم من XML
     */
    fun loadFromXml(xml: String) {
        viewModelScope.launch {
            try {
                val components = parseXml(xml)
                _designModel.value = DesignModel(components)
                _selectedComponent.value = null
            } catch (e: Exception) {
                Timber.e(e, "Error loading XML")
            }
        }
    }
    
    /**
     * تحليل XML
     */
    private suspend fun parseXml(xml: String): MutableList<DesignComponent> = withContext(Dispatchers.IO) {
        val components = mutableListOf<DesignComponent>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            var eventType = parser.eventType
            var currentComponent: DesignComponent? = null
            var x = 0f
            var y = 0f
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        
                        // تحويل اسم XML إلى نوع العنصر
                        val componentType = when (tagName) {
                            "androidx.constraintlayout.widget.ConstraintLayout" -> "ConstraintLayout"
                            "androidx.recyclerview.widget.RecyclerView" -> "RecyclerView"
                            "androidx.cardview.widget.CardView" -> "CardView"
                            else -> tagName
                        }
                        
                        // إنشاء مكون جديد
                        currentComponent = DesignComponent(
                            id = UUID.randomUUID().toString(),
                            type = componentType,
                            x = x,
                            y = y,
                            properties = mutableMapOf()
                        )
                        
                        // قراءة الخصائص
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrValue = parser.getAttributeValue(i)
                            
                            // تحويل اسم الخاصية من android:xxx إلى xxx
                            val propertyName = when {
                                attrName.startsWith("android:") -> attrName.substring(8)
                                else -> attrName
                            }
                            
                            // تحويل قيمة الخاصية
                            val propertyValue = when (propertyName) {
                                "layout_width" -> when (attrValue) {
                                    "match_parent" -> "MATCH_PARENT"
                                    "wrap_content" -> "WRAP_CONTENT"
                                    else -> attrValue
                                }
                                "layout_height" -> when (attrValue) {
                                    "match_parent" -> "MATCH_PARENT"
                                    "wrap_content" -> "WRAP_CONTENT"
                                    else -> attrValue
                                }
                                "orientation" -> when (attrValue) {
                                    "vertical" -> "VERTICAL"
                                    "horizontal" -> "HORIZONTAL"
                                    else -> attrValue
                                }
                                "visibility" -> when (attrValue) {
                                    "visible" -> "VISIBLE"
                                    "invisible" -> "INVISIBLE"
                                    "gone" -> "GONE"
                                    else -> attrValue
                                }
                                else -> attrValue
                            }
                            
                            // إضافة الخاصية إلى المكون
                            currentComponent.properties[propertyName] = propertyValue
                        }
                        
                        // إضافة المكون إلى القائمة
                        components.add(currentComponent)
                        
                        // تحديث الموقع للمكون التالي
                        x += 50f
                        y += 50f
                    }
                }
                
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing XML")
        }
        
        return@withContext components
    }
    
    /**
     * الحصول على الخصائص الافتراضية لنوع العنصر
     */
    private fun getDefaultProperties(type: String): MutableMap<String, Any> {
        return when (type) {
            "TextView" -> mutableMapOf(
                "text" to "Text View",
                "textColor" to android.graphics.Color.BLACK,
                "width" to "WRAP_CONTENT",
                "height" to "WRAP_CONTENT"
            )
            "Button" -> mutableMapOf(
                "text" to "Button",
                "width" to "WRAP_CONTENT",
                "height" to "WRAP_CONTENT"
            )
            "EditText" -> mutableMapOf(
                "hint" to "Edit Text",
                "width" to "WRAP_CONTENT",
                "height" to "WRAP_CONTENT"
            )
            "ImageView" -> mutableMapOf(
                "width" to 100,
                "height" to 100
            )
            "LinearLayout" -> mutableMapOf(
                "orientation" to "VERTICAL",
                "width" to "MATCH_PARENT",
                "height" to "MATCH_PARENT",
                "backgroundColor" to android.graphics.Color.LTGRAY
            )
            "ConstraintLayout" -> mutableMapOf(
                "width" to "MATCH_PARENT",
                "height" to "MATCH_PARENT",
                "backgroundColor" to android.graphics.Color.LTGRAY
            )
            "RecyclerView" -> mutableMapOf(
                "width" to "MATCH_PARENT",
                "height" to 200
            )
            "CardView" -> mutableMapOf(
                "width" to 200,
                "height" to 200,
                "elevation" to 4f
            )
            else -> mutableMapOf(
                "width" to "WRAP_CONTENT",
                "height" to "WRAP_CONTENT"
            )
        }
    }
}