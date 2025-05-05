package com.mobileide.designer.model;

import java.util.HashMap;
import java.util.Map;

/**
 * نموذج عنصر في المصمم
 */
public class DesignerComponent {
    private final String id;
    private final ComponentType type;
    private float x;
    private float y;
    private float width;
    private float height;
    private final Map<String, Object> properties;
    
    /**
     * إنشاء عنصر جديد
     */
    public DesignerComponent(String id, ComponentType type, float x, float y, float width, float height) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.properties = new HashMap<>();
    }
    
    /**
     * الحصول على معرف العنصر
     */
    public String getId() {
        return id;
    }
    
    /**
     * الحصول على نوع العنصر
     */
    public ComponentType getType() {
        return type;
    }
    
    /**
     * الحصول على الموقع الأفقي
     */
    public float getX() {
        return x;
    }
    
    /**
     * تعيين الموقع الأفقي
     */
    public void setX(float x) {
        this.x = x;
    }
    
    /**
     * الحصول على الموقع الرأسي
     */
    public float getY() {
        return y;
    }
    
    /**
     * تعيين الموقع الرأسي
     */
    public void setY(float y) {
        this.y = y;
    }
    
    /**
     * الحصول على العرض
     */
    public float getWidth() {
        return width;
    }
    
    /**
     * تعيين العرض
     */
    public void setWidth(float width) {
        this.width = width;
    }
    
    /**
     * الحصول على الارتفاع
     */
    public float getHeight() {
        return height;
    }
    
    /**
     * تعيين الارتفاع
     */
    public void setHeight(float height) {
        this.height = height;
    }
    
    /**
     * الحصول على خصائص العنصر
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * الحصول على قيمة خاصية
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * تعيين قيمة خاصية
     */
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
}