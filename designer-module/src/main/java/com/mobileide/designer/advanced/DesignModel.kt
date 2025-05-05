package com.mobileide.designer.advanced

/**
 * نموذج التصميم الذي يحتوي على جميع العناصر
 */
data class DesignModel(
    val components: MutableList<DesignComponent>
)

/**
 * نموذج عنصر التصميم
 */
data class DesignComponent(
    val id: String,
    val type: String,
    var x: Float,
    var y: Float,
    val properties: MutableMap<String, Any>
)