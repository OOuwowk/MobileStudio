package com.mobileide.designer.advanced.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileide.designer.R;
import com.mobileide.designer.model.DesignerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * محرر خصائص العناصر
 */
public class PropertyEditor extends LinearLayout {
    
    private final RecyclerView recyclerView;
    private final PropertyAdapter adapter;
    private DesignerComponent currentComponent;
    private OnPropertyChangedListener propertyChangedListener;
    
    public PropertyEditor(Context context) {
        super(context);
        
        // إعداد التخطيط
        setOrientation(VERTICAL);
        setPadding(8, 8, 8, 8);
        
        // إضافة عنوان
        TextView titleView = new TextView(context);
        titleView.setText(R.string.properties);
        titleView.setTextSize(16);
        titleView.setPadding(8, 8, 8, 16);
        addView(titleView);
        
        // إعداد قائمة الخصائص
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        
        // إعداد محول القائمة
        adapter = new PropertyAdapter((property, value) -> {
            if (currentComponent != null && propertyChangedListener != null) {
                propertyChangedListener.onPropertyChanged(currentComponent, property, value);
            }
        });
        recyclerView.setAdapter(adapter);
        
        addView(recyclerView);
    }
    
    /**
     * تعيين العنصر الحالي
     */
    public void setComponent(DesignerComponent component) {
        this.currentComponent = component;
        
        if (component != null) {
            // تحويل خصائص العنصر إلى قائمة
            List<PropertyItem> properties = new ArrayList<>();
            for (Map.Entry<String, Object> entry : component.getProperties().entrySet()) {
                properties.add(new PropertyItem(entry.getKey(), entry.getValue().toString()));
            }
            
            // إضافة خصائص الموقع والأبعاد
            properties.add(new PropertyItem("x", String.valueOf((int)component.getX())));
            properties.add(new PropertyItem("y", String.valueOf((int)component.getY())));
            properties.add(new PropertyItem("width", String.valueOf((int)component.getWidth())));
            properties.add(new PropertyItem("height", String.valueOf((int)component.getHeight())));
            
            // تحديث القائمة
            adapter.setProperties(properties);
        } else {
            // مسح القائمة
            adapter.setProperties(new ArrayList<>());
        }
    }
    
    /**
     * تعيين مستمع تغيير الخصائص
     */
    public void setOnPropertyChangedListener(OnPropertyChangedListener listener) {
        this.propertyChangedListener = listener;
    }
    
    /**
     * واجهة مستمع تغيير الخصائص
     */
    public interface OnPropertyChangedListener {
        void onPropertyChanged(DesignerComponent component, String property, Object value);
    }
    
    /**
     * عنصر خاصية
     */
    private static class PropertyItem {
        private final String name;
        private String value;
        
        public PropertyItem(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    /**
     * محول قائمة الخصائص
     */
    private static class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.ViewHolder> {
        
        private List<PropertyItem> properties = new ArrayList<>();
        private final OnPropertyValueChangedListener valueChangedListener;
        
        public PropertyAdapter(OnPropertyValueChangedListener listener) {
            this.valueChangedListener = listener;
        }
        
        public void setProperties(List<PropertyItem> properties) {
            this.properties = properties;
            notifyDataSetChanged();
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PropertyItem property = properties.get(position);
            holder.bind(property, valueChangedListener);
        }
        
        @Override
        public int getItemCount() {
            return properties.size();
        }
        
        /**
         * حامل العرض للخاصية
         */
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameView;
            private final TextView valueView;
            
            public ViewHolder(View itemView) {
                super(itemView);
                nameView = itemView.findViewById(android.R.id.text1);
                valueView = itemView.findViewById(android.R.id.text2);
            }
            
            public void bind(PropertyItem property, OnPropertyValueChangedListener listener) {
                nameView.setText(property.getName());
                valueView.setText(property.getValue());
                
                // إعداد مستمع النقر لتعديل القيمة
                itemView.setOnClickListener(v -> {
                    showEditDialog(property, listener);
                });
            }
            
            /**
             * عرض مربع حوار تعديل القيمة
             */
            private void showEditDialog(PropertyItem property, OnPropertyValueChangedListener listener) {
                Context context = itemView.getContext();
                
                // إنشاء حقل الإدخال
                EditText editText = new EditText(context);
                editText.setText(property.getValue());
                
                // إنشاء مربع الحوار
                new AlertDialog.Builder(context)
                    .setTitle(property.getName())
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        String newValue = editText.getText().toString();
                        property.setValue(newValue);
                        valueView.setText(newValue);
                        listener.onPropertyValueChanged(property.getName(), newValue);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            }
        }
        
        /**
         * واجهة مستمع تغيير قيمة الخاصية
         */
        interface OnPropertyValueChangedListener {
            void onPropertyValueChanged(String property, String value);
        }
    }
}