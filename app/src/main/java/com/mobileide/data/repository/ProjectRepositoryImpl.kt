package com.mobileide.data.repository

import com.mobileide.data.local.ProjectDao
import com.mobileide.data.model.ProjectEntity
import com.mobileide.domain.model.Project
import com.mobileide.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {
    
    override fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toProject() }
        }
    }
    
    override suspend fun getProject(id: Long): Project? {
        return projectDao.getProjectById(id)?.toProject()
    }
    
    override suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(ProjectEntity.fromProject(project))
    }
    
    override suspend fun updateProject(project: Project) {
        projectDao.updateProject(ProjectEntity.fromProject(project))
    }
    
    override suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(ProjectEntity.fromProject(project))
    }
    
    override suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }
}