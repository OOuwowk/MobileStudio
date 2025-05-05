package com.mobileide.presentation.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.domain.model.File
import com.mobileide.domain.model.Project
import com.mobileide.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {
    
    private val _currentFile = MutableLiveData<File?>()
    val currentFile: LiveData<File?> = _currentFile
    
    private val _projectFiles = MutableLiveData<List<File>>()
    val projectFiles: LiveData<List<File>> = _projectFiles
    
    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> = _isSaving
    
    /**
     * Loads all files for a project
     */
    fun loadProjectFiles(project: Project) {
        viewModelScope.launch {
            try {
                val files = fileRepository.getFilesForProject(project.id)
                _projectFiles.value = files
                
                // Select the first file if available and no file is currently selected
                if (_currentFile.value == null && files.isNotEmpty()) {
                    selectFile(files.first())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading project files")
            }
        }
    }
    
    /**
     * Selects a file for editing
     */
    fun selectFile(file: File) {
        viewModelScope.launch {
            try {
                // Load the file content if it's not already loaded
                val fileWithContent = if (file.content.isNullOrEmpty()) {
                    fileRepository.getFileContent(file)
                } else {
                    file
                }
                _currentFile.value = fileWithContent
            } catch (e: Exception) {
                Timber.e(e, "Error selecting file: ${file.path}")
            }
        }
    }
    
    /**
     * Updates the content of the current file
     */
    fun updateCurrentFile(content: String) {
        _currentFile.value?.let { file ->
            _currentFile.value = file.copy(content = content, isModified = true)
        }
    }
    
    /**
     * Saves the current file
     */
    fun saveCurrentFile() {
        _currentFile.value?.let { file ->
            if (file.isModified) {
                viewModelScope.launch {
                    _isSaving.value = true
                    try {
                        val savedFile = fileRepository.saveFile(file)
                        _currentFile.value = savedFile.copy(isModified = false)
                        
                        // Update the file in the project files list
                        _projectFiles.value = _projectFiles.value?.map {
                            if (it.id == savedFile.id) savedFile.copy(isModified = false) else it
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error saving file: ${file.path}")
                    } finally {
                        _isSaving.value = false
                    }
                }
            }
        }
    }
    
    /**
     * Creates a new file in the current project
     */
    fun createNewFile(name: String, path: String, projectId: Long) {
        viewModelScope.launch {
            try {
                val newFile = fileRepository.createFile(name, path, projectId)
                
                // Add the new file to the project files list
                val currentFiles = _projectFiles.value ?: emptyList()
                _projectFiles.value = currentFiles + newFile
                
                // Select the new file
                selectFile(newFile)
            } catch (e: Exception) {
                Timber.e(e, "Error creating file: $path")
            }
        }
    }
    
    /**
     * Deletes a file
     */
    fun deleteFile(file: File) {
        viewModelScope.launch {
            try {
                fileRepository.deleteFile(file)
                
                // Remove the file from the project files list
                _projectFiles.value = _projectFiles.value?.filter { it.id != file.id }
                
                // If the current file is deleted, select another file
                if (_currentFile.value?.id == file.id) {
                    _currentFile.value = null
                    _projectFiles.value?.firstOrNull()?.let { selectFile(it) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting file: ${file.path}")
            }
        }
    }
}