package com.mobileide.presentation.projects

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mobileide.domain.model.Project
import com.mobileide.domain.repository.ProjectRepository
import com.mobileide.domain.service.ProjectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectManager: ProjectManager
) : ViewModel() {
    
    val projects: LiveData<List<Project>> = projectRepository.getAllProjects().asLiveData()
    
    fun createProject(name: String, packageName: String, templateId: Int) {
        viewModelScope.launch {
            projectManager.createProject(name, packageName, templateId)
        }
    }
    
    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectManager.deleteProject(projectId)
        }
    }
    
    fun openProject(project: Project) {
        // This will be handled by the MainActivity ViewModel
    }
}