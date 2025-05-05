package com.mobileide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mobileide.domain.model.Project

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val path: String,
    val createdAt: Long
) {
    fun toProject(): Project {
        return Project(
            id = id,
            name = name,
            packageName = packageName,
            path = path,
            createdAt = createdAt
        )
    }
    
    companion object {
        fun fromProject(project: Project): ProjectEntity {
            return ProjectEntity(
                id = project.id,
                name = project.name,
                packageName = project.packageName,
                path = project.path,
                createdAt = project.createdAt
            )
        }
    }
}