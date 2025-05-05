package com.mobileide.domain.repository

import com.mobileide.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    
    fun getAllProjects(): Flow<List<Project>>
    
    suspend fun getProject(id: Long): Project?
    
    suspend fun insertProject(project: Project): Long
    
    suspend fun updateProject(project: Project)
    
    suspend fun deleteProject(project: Project)
    
    suspend fun deleteProject(id: Long)
}