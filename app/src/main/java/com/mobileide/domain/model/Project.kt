package com.mobileide.domain.model

data class Project(
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val path: String,
    val createdAt: Long = System.currentTimeMillis()
)