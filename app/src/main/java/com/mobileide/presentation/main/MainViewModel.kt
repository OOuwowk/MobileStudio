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
        
        viewModelScope.launch {
            try {
                // Call the compiler service to build the project
                val result = projectManager.buildProject(project)
                
                if (result.isSuccess) {
                    val apkPath = result.getOrNull()?.absolutePath ?: ""
                    _buildStatus.postValue(BuildStatus.Success(apkPath))
                    Timber.d("Build successful: $apkPath")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _buildStatus.postValue(BuildStatus.Error(error))
                    Timber.e("Build failed: $error")
                }
            } catch (e: Exception) {
                _buildStatus.postValue(BuildStatus.Error(e.message ?: "Unknown error"))
                Timber.e(e, "Exception during build")
            }
        }
    }
    
    fun runProject() {
        val project = _currentProject.value ?: return
        
        viewModelScope.launch {
            try {
                // First build the project
                _buildStatus.postValue(BuildStatus.Building)
                
                val buildResult = projectManager.buildProject(project)
                
                if (buildResult.isSuccess) {
                    val apkFile = buildResult.getOrNull()
                    if (apkFile != null) {
                        // Install and run the APK
                        val installResult = projectManager.installAndRunApp(apkFile)
                        
                        if (installResult.isSuccess) {
                            _buildStatus.postValue(BuildStatus.Success(apkFile.absolutePath))
                            Timber.d("App installed and running")
                        } else {
                            val error = installResult.exceptionOrNull()?.message ?: "Failed to install app"
                            _buildStatus.postValue(BuildStatus.Error(error))
                            Timber.e("Install failed: $error")
                        }
                    } else {
                        _buildStatus.postValue(BuildStatus.Error("Build succeeded but no APK was produced"))
                    }
                } else {
                    val error = buildResult.exceptionOrNull()?.message ?: "Build failed"
                    _buildStatus.postValue(BuildStatus.Error(error))
                    Timber.e("Build failed: $error")
                }
            } catch (e: Exception) {
                _buildStatus.postValue(BuildStatus.Error(e.message ?: "Unknown error"))
                Timber.e(e, "Exception during run")
            }
        }
    }
    
    fun debugProject() {
        val project = _currentProject.value ?: return
        
        viewModelScope.launch {
            try {
                // First build the project
                _buildStatus.postValue(BuildStatus.Building)
                
                val buildResult = projectManager.buildProject(project)
                
                if (buildResult.isSuccess) {
                    val apkFile = buildResult.getOrNull()
                    if (apkFile != null) {
                        // Start debugging session
                        val debugResult = projectManager.startDebugging(apkFile)
                        
                        if (debugResult.isSuccess) {
                            _buildStatus.postValue(BuildStatus.Success(apkFile.absolutePath))
                            Timber.d("Debugging session started")
                        } else {
                            val error = debugResult.exceptionOrNull()?.message ?: "Failed to start debugging"
                            _buildStatus.postValue(BuildStatus.Error(error))
                            Timber.e("Debug failed: $error")
                        }
                    } else {
                        _buildStatus.postValue(BuildStatus.Error("Build succeeded but no APK was produced"))
                    }
                } else {
                    val error = buildResult.exceptionOrNull()?.message ?: "Build failed"
                    _buildStatus.postValue(BuildStatus.Error(error))
                    Timber.e("Build failed: $error")
                }
            } catch (e: Exception) {
                _buildStatus.postValue(BuildStatus.Error(e.message ?: "Unknown error"))
                Timber.e(e, "Exception during debug")
            }
        }
    }
}

sealed class BuildStatus {
    object Idle : BuildStatus()
    object Building : BuildStatus()
    data class Success(val apkPath: String) : BuildStatus()
    data class Error(val message: String) : BuildStatus()
}