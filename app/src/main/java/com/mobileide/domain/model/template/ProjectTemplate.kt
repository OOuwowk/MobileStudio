package com.mobileide.domain.model.template

data class ProjectTemplate(
    val id: Int,
    val name: String,
    val description: String,
    val files: List<TemplateFile>
)

data class TemplateFile(
    val path: String,
    val content: String
)