package com.mobileide.designer.model;

/**
 * أنواع العناصر المدعومة في المصمم
 */
public enum ComponentType {
    TEXT_VIEW("TextView", "android.widget.TextView"),
    BUTTON("Button", "android.widget.Button"),
    EDIT_TEXT("EditText", "android.widget.EditText"),
    IMAGE_VIEW("ImageView", "android.widget.ImageView"),
    LINEAR_LAYOUT("LinearLayout", "android.widget.LinearLayout"),
    CONSTRAINT_LAYOUT("ConstraintLayout", "androidx.constraintlayout.widget.ConstraintLayout"),
    FRAME_LAYOUT("FrameLayout", "android.widget.FrameLayout"),
    RELATIVE_LAYOUT("RelativeLayout", "android.widget.RelativeLayout");
    
    private final String xmlTag;
    private final String fullClassName;
    
    ComponentType(String xmlTag, String fullClassName) {
        this.xmlTag = xmlTag;
        this.fullClassName = fullClassName;
    }
    
    /**
     * الحصول على اسم العرض
     */
    public String getDisplayName() {
        switch (this) {
            case TEXT_VIEW:
                return "Text View";
            case EDIT_TEXT:
                return "Edit Text";
            case IMAGE_VIEW:
                return "Image View";
            case LINEAR_LAYOUT:
                return "Linear Layout";
            case CONSTRAINT_LAYOUT:
                return "Constraint Layout";
            case FRAME_LAYOUT:
                return "Frame Layout";
            case RELATIVE_LAYOUT:
                return "Relative Layout";
            default:
                return name();
        }
    }
    
    /**
     * الحصول على اسم العنصر في XML
     */
    public String getXmlTag() {
        return xmlTag;
    }
    
    /**
     * الحصول على اسم الفئة الكامل
     */
    public String getFullClassName() {
        return fullClassName;
    }
    
    /**
     * التحقق مما إذا كان العنصر حاوية
     */
    public boolean isContainer() {
        return this == LINEAR_LAYOUT || 
               this == CONSTRAINT_LAYOUT || 
               this == FRAME_LAYOUT || 
               this == RELATIVE_LAYOUT;
    }
    
    /**
     * الحصول على الخصائص الافتراضية للعنصر
     */
    public String[] getDefaultProperties() {
        switch (this) {
            case TEXT_VIEW:
                return new String[] {"id", "layout_width", "layout_height", "text", "textSize", "textColor"};
            case BUTTON:
                return new String[] {"id", "layout_width", "layout_height", "text", "textSize", "textColor", "background"};
            case EDIT_TEXT:
                return new String[] {"id", "layout_width", "layout_height", "hint", "inputType", "textSize", "textColor"};
            case IMAGE_VIEW:
                return new String[] {"id", "layout_width", "layout_height", "src", "scaleType", "contentDescription"};
            case LINEAR_LAYOUT:
                return new String[] {"id", "layout_width", "layout_height", "orientation", "gravity", "padding"};
            case CONSTRAINT_LAYOUT:
                return new String[] {"id", "layout_width", "layout_height"};
            case FRAME_LAYOUT:
                return new String[] {"id", "layout_width", "layout_height", "foreground", "foregroundGravity"};
            case RELATIVE_LAYOUT:
                return new String[] {"id", "layout_width", "layout_height", "gravity", "padding"};
            default:
                return new String[] {"id", "layout_width", "layout_height"};
        }
    }
}