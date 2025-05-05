package com.mobileide.designer.advanced

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobileide.designer.R

/**
 * لوحة الخصائص التي تعرض وتسمح بتعديل خصائص العنصر المحدد
 */
class PropertyPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val recyclerView: RecyclerView
    private val adapter: PropertyAdapter
    private var onPropertyChangeListener: ((String, Any) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        
        // إضافة عنوان
        val titleView = TextView(context).apply {
            text = context.getString(R.string.properties)
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        addView(titleView)
        
        // إضافة قائمة الخصائص
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(context)
        }
        addView(recyclerView)
        
        // إعداد محول القائمة
        adapter = PropertyAdapter { property, value ->
            onPropertyChangeListener?.invoke(property, value)
        }
        recyclerView.adapter = adapter
    }
    
    /**
     * تعيين الخصائص للعرض
     */
    fun setProperties(properties: Map<String, Any>) {
        val propertyList = properties.map { (key, value) ->
            PropertyItem(key, value)
        }
        adapter.submitList(propertyList)
    }
    
    /**
     * مسح الخصائص
     */
    fun clearProperties() {
        adapter.submitList(emptyList())
    }
    
    /**
     * تعيين مستمع تغيير الخصائص
     */
    fun setOnPropertyChangeListener(listener: (String, Any) -> Unit) {
        onPropertyChangeListener = listener
    }
    
    /**
     * محول قائمة الخصائص
     */
    private inner class PropertyAdapter(
        private val onPropertyChange: (String, Any) -> Unit
    ) : RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {
        
        private var properties = listOf<PropertyItem>()
        
        fun submitList(list: List<PropertyItem>) {
            properties = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val property = properties[position]
            holder.bind(property)
        }
        
        override fun getItemCount() = properties.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameView: TextView = itemView.findViewById(android.R.id.text1)
            private val valueView: TextView = itemView.findViewById(android.R.id.text2)
            
            fun bind(property: PropertyItem) {
                nameView.text = property.name
                valueView.text = property.value.toString()
                
                // تعيين مستمع النقر لتعديل القيمة
                itemView.setOnClickListener {
                    showEditDialog(property)
                }
            }
            
            private fun showEditDialog(property: PropertyItem) {
                val editText = EditText(itemView.context).apply {
                    setText(property.value.toString())
                }
                
                AlertDialog.Builder(itemView.context)
                    .setTitle(property.name)
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val newValue = editText.text.toString()
                        
                        // تحويل القيمة إلى النوع المناسب
                        val typedValue = when (property.value) {
                            is Int -> newValue.toIntOrNull() ?: 0
                            is Float -> newValue.toFloatOrNull() ?: 0f
                            is Boolean -> newValue.toBoolean()
                            else -> newValue
                        }
                        
                        onPropertyChange(property.name, typedValue)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }
    
    /**
     * عنصر خاصية
     */
    data class PropertyItem(
        val name: String,
        val value: Any
    )
}