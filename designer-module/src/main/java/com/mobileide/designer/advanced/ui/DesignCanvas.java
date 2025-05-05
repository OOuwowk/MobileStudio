package com.mobileide.designer.advanced.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.mobileide.designer.R;
import com.mobileide.designer.model.ComponentType;
import com.mobileide.designer.model.DesignerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * سطح التصميم حيث يتم وضع وتحرير العناصر
 */
public class DesignCanvas extends FrameLayout {
    
    private final List<DesignerComponent> components = new ArrayList<>();
    private DesignerComponent selectedComponent;
    private OnComponentSelectedListener selectionListener;
    private final Paint gridPaint;
    private final Paint selectionPaint;
    private ComponentType draggedComponentType;
    
    public DesignCanvas(Context context) {
        super(context);
        
        // إعداد طلاء الشبكة
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        
        // إعداد طلاء التحديد
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.RED);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(2);
        
        // تعيين خلفية
        setBackgroundResource(R.drawable.bg_design_surface);
        
        // إعداد مستمعي اللمس والسحب
        setupTouchListeners();
        setupDragListeners();
    }
    
    /**
     * إعداد مستمعي اللمس
     */
    private void setupTouchListeners() {
        setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // تحديد العنصر عند النقر
                    handleTouchDown(event.getX(), event.getY());
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    // تحريك العنصر المحدد
                    if (selectedComponent != null) {
                        moveSelectedComponent(event.getX(), event.getY());
                        return true;
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    // إنهاء التحريك
                    return true;
            }
            return false;
        });
    }
    
    /**
     * إعداد مستمعي السحب
     */
    private void setupDragListeners() {
        setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                    
                case DragEvent.ACTION_DRAG_ENTERED:
                    return true;
                    
                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;
                    
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                    
                case DragEvent.ACTION_DROP:
                    // إضافة عنصر جديد عند الإفلات
                    if (draggedComponentType != null) {
                        addComponent(draggedComponentType, event.getX(), event.getY());
                        draggedComponentType = null;
                        return true;
                    }
                    return false;
                    
                case DragEvent.ACTION_DRAG_ENDED:
                    draggedComponentType = null;
                    return true;
                    
                default:
                    return false;
            }
        });
    }
    
    /**
     * معالجة حدث النقر
     */
    private void handleTouchDown(float x, float y) {
        // البحث عن العنصر في النقطة المحددة
        DesignerComponent component = findComponentAt(x, y);
        
        // تحديد العنصر
        selectComponent(component);
    }
    
    /**
     * البحث عن عنصر في نقطة معينة
     */
    private DesignerComponent findComponentAt(float x, float y) {
        // البحث من الأعلى إلى الأسفل (العناصر الأحدث أولاً)
        for (int i = components.size() - 1; i >= 0; i--) {
            DesignerComponent component = components.get(i);
            
            // التحقق مما إذا كانت النقطة داخل العنصر
            if (isPointInComponent(x, y, component)) {
                return component;
            }
        }
        
        return null;
    }
    
    /**
     * التحقق مما إذا كانت نقطة داخل عنصر
     */
    private boolean isPointInComponent(float x, float y, DesignerComponent component) {
        return x >= component.getX() && 
               x <= component.getX() + component.getWidth() &&
               y >= component.getY() && 
               y <= component.getY() + component.getHeight();
    }
    
    /**
     * تحديد عنصر
     */
    private void selectComponent(DesignerComponent component) {
        selectedComponent = component;
        
        // إبلاغ المستمع بالتحديد
        if (selectionListener != null) {
            selectionListener.onComponentSelected(component);
        }
        
        // إعادة الرسم
        invalidate();
    }
    
    /**
     * تحريك العنصر المحدد
     */
    private void moveSelectedComponent(float x, float y) {
        if (selectedComponent != null) {
            // تحديث موقع العنصر
            selectedComponent.setX(x - selectedComponent.getWidth() / 2);
            selectedComponent.setY(y - selectedComponent.getHeight() / 2);
            
            // إعادة الرسم
            invalidate();
        }
    }
    
    /**
     * بدء قبول الإفلات لعنصر
     */
    public void startAcceptingDrop(ComponentType componentType) {
        this.draggedComponentType = componentType;
    }
    
    /**
     * إضافة عنصر جديد
     */
    private void addComponent(ComponentType type, float x, float y) {
        // إنشاء عنصر جديد
        DesignerComponent component = new DesignerComponent(
            UUID.randomUUID().toString(),
            type,
            x - getDefaultWidth(type) / 2,
            y - getDefaultHeight(type) / 2,
            getDefaultWidth(type),
            getDefaultHeight(type)
        );
        
        // إضافة الخصائص الافتراضية
        component.getProperties().put("id", "@+id/" + type.name().toLowerCase() + "_" + System.currentTimeMillis() % 10000);
        component.getProperties().put("layout_width", "wrap_content");
        component.getProperties().put("layout_height", "wrap_content");
        
        // إضافة خصائص إضافية حسب نوع العنصر
        switch (type) {
            case TEXT_VIEW:
                component.getProperties().put("text", "Text View");
                break;
            case BUTTON:
                component.getProperties().put("text", "Button");
                break;
            case EDIT_TEXT:
                component.getProperties().put("hint", "Enter text");
                break;
            case IMAGE_VIEW:
                component.getProperties().put("src", "@drawable/ic_image_placeholder");
                component.getProperties().put("contentDescription", "Image");
                break;
        }
        
        // إضافة العنصر إلى القائمة
        components.add(component);
        
        // تحديد العنصر الجديد
        selectComponent(component);
        
        // إعادة الرسم
        invalidate();
    }
    
    /**
     * الحصول على العرض الافتراضي للعنصر
     */
    private float getDefaultWidth(ComponentType type) {
        switch (type) {
            case LINEAR_LAYOUT:
            case CONSTRAINT_LAYOUT:
            case FRAME_LAYOUT:
            case RELATIVE_LAYOUT:
                return 300;
            case IMAGE_VIEW:
                return 100;
            default:
                return 150;
        }
    }
    
    /**
     * الحصول على الارتفاع الافتراضي للعنصر
     */
    private float getDefaultHeight(ComponentType type) {
        switch (type) {
            case LINEAR_LAYOUT:
            case CONSTRAINT_LAYOUT:
            case FRAME_LAYOUT:
            case RELATIVE_LAYOUT:
                return 300;
            case IMAGE_VIEW:
                return 100;
            default:
                return 50;
        }
    }
    
    /**
     * تحديث خصائص عنصر
     */
    public void updateComponent(DesignerComponent component, String property, Object value) {
        if (component != null) {
            // تحديث الخاصية
            component.getProperties().put(property, value);
            
            // تحديث الأبعاد إذا لزم الأمر
            if ("layout_width".equals(property)) {
                if ("match_parent".equals(value)) {
                    component.setWidth(getWidth() - 20);
                } else if (!"wrap_content".equals(value)) {
                    try {
                        component.setWidth(Float.parseFloat(value.toString()));
                    } catch (NumberFormatException e) {
                        // تجاهل
                    }
                }
            } else if ("layout_height".equals(property)) {
                if ("match_parent".equals(value)) {
                    component.setHeight(getHeight() - 20);
                } else if (!"wrap_content".equals(value)) {
                    try {
                        component.setHeight(Float.parseFloat(value.toString()));
                    } catch (NumberFormatException e) {
                        // تجاهل
                    }
                }
            }
            
            // إعادة الرسم
            invalidate();
        }
    }
    
    /**
     * حذف العنصر المحدد
     */
    public void deleteSelectedComponent() {
        if (selectedComponent != null) {
            components.remove(selectedComponent);
            selectComponent(null);
            invalidate();
        }
    }
    
    /**
     * توليد XML من التصميم
     */
    public String generateLayoutXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        
        // تحديد العنصر الجذر
        String rootTag = "FrameLayout";
        
        // بدء العنصر الجذر
        xml.append("<").append(rootTag).append(" xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        xml.append("    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n");
        xml.append("    xmlns:tools=\"http://schemas.android.com/tools\"\n");
        xml.append("    android:layout_width=\"match_parent\"\n");
        xml.append("    android:layout_height=\"match_parent\">\n\n");
        
        // إضافة العناصر
        for (DesignerComponent component : components) {
            // بدء العنصر
            xml.append("    <").append(component.getType().getXmlTag()).append("\n");
            
            // إضافة الخصائص
            for (String key : component.getProperties().keySet()) {
                Object value = component.getProperties().get(key);
                xml.append("        android:").append(key).append("=\"").append(value).append("\"\n");
            }
            
            // إضافة موقع العنصر
            xml.append("        android:layout_marginStart=\"").append((int)component.getX()).append("dp\"\n");
            xml.append("        android:layout_marginTop=\"").append((int)component.getY()).append("dp\"\n");
            
            // إغلاق العنصر
            xml.append("        />\n\n");
        }
        
        // إغلاق العنصر الجذر
        xml.append("</").append(rootTag).append(">");
        
        return xml.toString();
    }
    
    /**
     * تعيين مستمع تحديد العناصر
     */
    public void setOnComponentSelectedListener(OnComponentSelectedListener listener) {
        this.selectionListener = listener;
    }
    
    /**
     * رسم سطح التصميم
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // رسم الشبكة
        drawGrid(canvas);
        
        // رسم العناصر
        for (DesignerComponent component : components) {
            drawComponent(canvas, component);
        }
    }
    
    /**
     * رسم الشبكة
     */
    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // رسم الخطوط الأفقية
        for (int i = 0; i < height; i += 20) {
            canvas.drawLine(0, i, width, i, gridPaint);
        }
        
        // رسم الخطوط العمودية
        for (int i = 0; i < width; i += 20) {
            canvas.drawLine(i, 0, i, height, gridPaint);
        }
    }
    
    /**
     * رسم عنصر
     */
    private void drawComponent(Canvas canvas, DesignerComponent component) {
        // تحديد لون الرسم
        Paint paint = new Paint();
        if (component == selectedComponent) {
            paint.set(selectionPaint);
        } else {
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
        }
        
        // رسم مستطيل العنصر
        canvas.drawRect(
            component.getX(),
            component.getY(),
            component.getX() + component.getWidth(),
            component.getY() + component.getHeight(),
            paint
        );
        
        // رسم نوع العنصر
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(16);
        canvas.drawText(
            component.getType().name(),
            component.getX() + 5,
            component.getY() + 20,
            textPaint
        );
        
        // رسم نص العنصر إذا كان متاحًا
        Object text = component.getProperties().get("text");
        if (text != null) {
            canvas.drawText(
                text.toString(),
                component.getX() + 5,
                component.getY() + 40,
                textPaint
            );
        }
    }
    
    /**
     * واجهة مستمع تحديد العناصر
     */
    public interface OnComponentSelectedListener {
        void onComponentSelected(DesignerComponent component);
    }
}