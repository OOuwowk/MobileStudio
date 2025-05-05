package com.mobileide.designer.advanced.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileide.designer.R;
import com.mobileide.designer.model.ComponentType;

import java.util.ArrayList;
import java.util.List;

/**
 * لوحة المكونات التي تحتوي على العناصر القابلة للسحب
 */
public class ComponentPalette extends LinearLayout {
    
    private final RecyclerView recyclerView;
    private final ComponentAdapter adapter;
    private OnComponentDragListener dragListener;
    
    public ComponentPalette(Context context) {
        super(context);
        
        // إعداد التخطيط
        setOrientation(VERTICAL);
        setPadding(8, 8, 8, 8);
        
        // إضافة عنوان
        TextView titleView = new TextView(context);
        titleView.setText(R.string.components);
        titleView.setTextSize(16);
        titleView.setPadding(8, 8, 8, 16);
        addView(titleView);
        
        // إعداد قائمة المكونات
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        
        // إعداد محول القائمة
        adapter = new ComponentAdapter(component -> {
            if (dragListener != null) {
                dragListener.onComponentDrag(component);
            }
        });
        recyclerView.setAdapter(adapter);
        
        addView(recyclerView);
        
        // تحميل المكونات المتاحة
        loadComponents();
    }
    
    /**
     * تحميل المكونات المتاحة
     */
    private void loadComponents() {
        List<ComponentType> components = new ArrayList<>();
        components.add(ComponentType.TEXT_VIEW);
        components.add(ComponentType.BUTTON);
        components.add(ComponentType.EDIT_TEXT);
        components.add(ComponentType.IMAGE_VIEW);
        components.add(ComponentType.LINEAR_LAYOUT);
        components.add(ComponentType.CONSTRAINT_LAYOUT);
        components.add(ComponentType.FRAME_LAYOUT);
        components.add(ComponentType.RELATIVE_LAYOUT);
        
        adapter.setComponents(components);
    }
    
    /**
     * تعيين مستمع سحب المكونات
     */
    public void setOnComponentDragListener(OnComponentDragListener listener) {
        this.dragListener = listener;
    }
    
    /**
     * واجهة مستمع سحب المكونات
     */
    public interface OnComponentDragListener {
        void onComponentDrag(ComponentType component);
    }
    
    /**
     * محول قائمة المكونات
     */
    private static class ComponentAdapter extends RecyclerView.Adapter<ComponentAdapter.ViewHolder> {
        
        private List<ComponentType> components = new ArrayList<>();
        private final OnComponentClickListener clickListener;
        
        public ComponentAdapter(OnComponentClickListener clickListener) {
            this.clickListener = clickListener;
        }
        
        public void setComponents(List<ComponentType> components) {
            this.components = components;
            notifyDataSetChanged();
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_component, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ComponentType component = components.get(position);
            holder.bind(component, clickListener);
        }
        
        @Override
        public int getItemCount() {
            return components.size();
        }
        
        /**
         * حامل العرض للمكون
         */
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView iconView;
            private final TextView nameView;
            
            public ViewHolder(View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.component_icon);
                nameView = itemView.findViewById(R.id.component_name);
            }
            
            public void bind(ComponentType component, OnComponentClickListener listener) {
                // تعيين أيقونة المكون
                int iconResId = getIconForComponent(component);
                iconView.setImageResource(iconResId);
                
                // تعيين اسم المكون
                nameView.setText(component.getDisplayName());
                
                // إعداد مستمع النقر المطول لبدء السحب
                itemView.setOnLongClickListener(v -> {
                    listener.onComponentClick(component);
                    return true;
                });
                
                // إعداد مستمع النقر العادي
                itemView.setOnClickListener(v -> {
                    listener.onComponentClick(component);
                });
            }
            
            /**
             * الحصول على أيقونة للمكون
             */
            private int getIconForComponent(ComponentType component) {
                switch (component) {
                    case TEXT_VIEW:
                        return R.drawable.ic_textview;
                    case BUTTON:
                        return R.drawable.ic_button;
                    case EDIT_TEXT:
                        return R.drawable.ic_edittext;
                    case IMAGE_VIEW:
                        return R.drawable.ic_imageview;
                    case LINEAR_LAYOUT:
                    case CONSTRAINT_LAYOUT:
                    case FRAME_LAYOUT:
                    case RELATIVE_LAYOUT:
                        return R.drawable.ic_layout;
                    default:
                        return R.drawable.ic_textview;
                }
            }
        }
        
        /**
         * واجهة مستمع النقر على المكون
         */
        interface OnComponentClickListener {
            void onComponentClick(ComponentType component);
        }
    }
}