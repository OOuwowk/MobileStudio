package com.mobileide.designer

/**
 * Represents the types of UI components that can be added to the designer
 */
enum class ComponentType(val displayName: String) {
    TEXT_VIEW("TextView"),
    BUTTON("Button"),
    EDIT_TEXT("EditText"),
    IMAGE_VIEW("ImageView"),
    LINEAR_LAYOUT("LinearLayout")
}