package com.mobileide.designer.advanced.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

/**
 * مصمم واجهات متقدم مع معاينة مباشرة
 */
public class AdvancedUIDesigner extends FrameLayout {
    private final ComponentPalette componentPalette;
    private final DesignCanvas designCanvas;
    private final PropertyEditor propertyEditor;
    private final LivePreview livePreview;
    
    public AdvancedUIDesigner(Context context) {
        super(context);
        
        // إنشاء مكونات المصمم
        componentPalette = new ComponentPalette(context);
        designCanvas = new DesignCanvas(context);
        propertyEditor = new PropertyEditor(context);
        livePreview = new LivePreview(context);
        
        // إعداد التخطيط
        setupLayout();
        
        // ربط المكونات
        setupComponentInteractions();
    }
    
    /**
     * إعداد تخطيط المصمم
     */
    private void setupLayout() {
        // تقسيم الشاشة إلى مناطق
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        
        // منطقة لوحة المكونات (يسار)
        addView(componentPalette, new LayoutParams(
            dpToPx(200), 
            LayoutParams.MATCH_PARENT
        ));
        
        // منطقة القماش (وسط)
        LayoutParams canvasParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        );
        canvasParams.leftMargin = dpToPx(200);
        canvasParams.rightMargin = dpToPx(250);
        addView(designCanvas, canvasParams);
        
        // منطقة محرر الخصائص (يمين)
        LayoutParams propParams = new LayoutParams(
            dpToPx(250),
            LayoutParams.MATCH_PARENT
        );
        propParams.gravity = Gravity.END;
        addView(propertyEditor, propParams);
        
        // منطقة المعاينة المباشرة (أسفل)
        LayoutParams previewParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dpToPx(300)
        );
        previewParams.gravity = Gravity.BOTTOM;
        addView(livePreview, previewParams);
        
        // إخفاء المعاينة افتراضيًا
        livePreview.setVisibility(GONE);
    }
    
    /**
     * إعداد التفاعلات بين المكونات
     */
    private void setupComponentInteractions() {
        // إعداد السحب والإفلات من لوحة المكونات إلى القماش
        componentPalette.setOnComponentDragListener(component -> {
            designCanvas.startAcceptingDrop(component);
        });
        
        // عند تحديد عنصر في القماش، تحديث محرر الخصائص
        designCanvas.setOnComponentSelectedListener(component -> {
            propertyEditor.setComponent(component);
        });
        
        // عند تغيير خاصية، تحديث العنصر في القماش والمعاينة
        propertyEditor.setOnPropertyChangedListener((component, property, value) -> {
            designCanvas.updateComponent(component, property, value);
            updateLivePreview();
        });
    }
    
    /**
     * تحديث المعاينة المباشرة
     */
    private void updateLivePreview() {
        // توليد XML من التصميم الحالي
        String layoutXml = designCanvas.generateLayoutXml();
        
        // تحديث المعاينة المباشرة
        livePreview.updatePreview(layoutXml);
    }
    
    /**
     * عرض/إخفاء المعاينة المباشرة
     */
    public void toggleLivePreview() {
        if (livePreview.getVisibility() == VISIBLE) {
            livePreview.setVisibility(GONE);
        } else {
            updateLivePreview();
            livePreview.setVisibility(VISIBLE);
        }
    }
    
    /**
     * تصدير التصميم كملف XML
     */
    public String exportLayoutXml() {
        return designCanvas.generateLayoutXml();
    }
    
    /**
     * تحويل وحدات dp إلى بكسل
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}