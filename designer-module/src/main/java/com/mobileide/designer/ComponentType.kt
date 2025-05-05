package com.mobileide.designer

/**
 * Enum representing the different types of UI components that can be added to the designer.
 */
enum class ComponentType(val displayName: String, val xmlTag: String) {
    TEXT_VIEW("TextView", "TextView"),
    BUTTON("Button", "Button"),
    EDIT_TEXT("EditText", "EditText"),
    IMAGE_VIEW("ImageView", "ImageView"),
    LINEAR_LAYOUT("LinearLayout", "LinearLayout"),
    CONSTRAINT_LAYOUT("ConstraintLayout", "androidx.constraintlayout.widget.ConstraintLayout"),
    FRAME_LAYOUT("FrameLayout", "FrameLayout"),
    RELATIVE_LAYOUT("RelativeLayout", "RelativeLayout"),
    RECYCLER_VIEW("RecyclerView", "androidx.recyclerview.widget.RecyclerView"),
    CARD_VIEW("CardView", "androidx.cardview.widget.CardView"),
    CHECKBOX("CheckBox", "CheckBox"),
    RADIO_BUTTON("RadioButton", "RadioButton"),
    SWITCH("Switch", "Switch"),
    PROGRESS_BAR("ProgressBar", "ProgressBar"),
    SEEK_BAR("SeekBar", "SeekBar"),
    SPINNER("Spinner", "Spinner");
    
    companion object {
        /**
         * Get a ComponentType from its display name.
         */
        fun fromDisplayName(displayName: String): ComponentType? {
            return values().find { it.displayName == displayName }
        }
        
        /**
         * Get a ComponentType from its XML tag.
         */
        fun fromXmlTag(xmlTag: String): ComponentType? {
            return values().find { it.xmlTag == xmlTag }
        }
    }
}