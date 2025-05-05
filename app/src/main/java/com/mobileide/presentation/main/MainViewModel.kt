package com.mobileide.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.domain.model.Project
import com.mobileide.domain.service.ProjectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val projectManager: ProjectManager
) : ViewModel() {
    
    private val _currentProject = MutableLiveData<Project?>()
    val currentProject: LiveData<Project?> = _currentProject
    
    private val _buildStatus = MutableLiveData<BuildStatus>()
    val buildStatus: LiveData<BuildStatus> = _buildStatus
    
    fun openProject(projectId: Long) {
        viewModelScope.launch {
            projectManager.openProject(projectId)
                .onSuccess { project ->
                    _currentProject.value = project
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to open project")
                    // TODO: Show error message
                }
        }
    }
    
    fun closeProject() {
        _currentProject.value = null
    }
    
    fun buildProject() {
        val project = _currentProject.value ?: return
        
        _buildStatus.value = BuildStatus.Building
        
        // TODO: Implement build process
        // This will be implemented in the compiler module
        
        // For now, just simulate a successful build
        _buildStatus.value = BuildStatus.Success("app-debug.apk")
    }
    
    fun runProject() {
        val project = _currentProject.value ?: return
        
        // First build the project
        buildProject()
        
        // TODO: Install and run the APK
        // This will be implemented in a separate service
    }
    
    fun debugProject() {
        val project = _currentProject.value ?: return
        
        // First build the project
        buildProject()
        
        // TODO: Start debugging session
        // This will be implemented in the debugger module
    }
}

sealed class BuildStatus {
    object Idle : BuildStatus()
    object Building : BuildStatus()
    data class Success(val apkPath: String) : BuildStatus()
    data class Error(val message: String) : BuildStatus()
}