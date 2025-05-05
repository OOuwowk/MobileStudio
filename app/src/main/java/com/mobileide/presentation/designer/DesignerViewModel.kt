package com.mobileide.presentation.designer

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.designer.ComponentType
import com.mobileide.domain.model.File
import com.mobileide.domain.model.Project
import com.mobileide.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DesignerViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {
    
    private val _currentProject = MutableLiveData<Project>()
    val currentProject: LiveData<Project> = _currentProject
    
    private val _currentLayoutFile = MutableLiveData<File>()
    val currentLayoutFile: LiveData<File> = _currentLayoutFile
    
    private val _layoutFiles = MutableLiveData<List<File>>()
    val layoutFiles: LiveData<List<File>> = _layoutFiles
    
    private val _generatedXml = MutableLiveData<String>()
    val generatedXml: LiveData<String> = _generatedXml
    
    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> = _isSaving
    
    /**
     * Sets the current project
     */
    fun setCurrentProject(project: Project) {
        _currentProject.value = project
        loadLayoutFiles(project)
    }
    
    /**
     * Loads layout files for the current project
     */
    private fun loadLayoutFiles(project: Project) {
        viewModelScope.launch {
            try {
                val files = fileRepository.getFilesForProject(project.id)
                    .filter { it.path.endsWith(".xml") && it.path.contains("/layout/") }
                
                _layoutFiles.value = files
                
                // Select the first layout file if available
                if (files.isNotEmpty() && _currentLayoutFile.value == null) {
                    selectLayoutFile(files.first())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading layout files")
            }
        }
    }
    
    /**
     * Selects a layout file for editing
     */
    fun selectLayoutFile(file: File) {
        viewModelScope.launch {
            try {
                // Load the file content if it's not already loaded
                val fileWithContent = if (file.content.isNullOrEmpty()) {
                    fileRepository.getFileContent(file)
                } else {
                    file
                }
                _currentLayoutFile.value = fileWithContent
            } catch (e: Exception) {
                Timber.e(e, "Error selecting layout file: ${file.path}")
            }
        }
    }
    
    /**
     * Creates a new layout file
     */
    fun createNewLayoutFile(name: String) {
        _currentProject.value?.let { project ->
            viewModelScope.launch {
                try {
                    // Create a new layout file with basic XML content
                    val path = "${project.path}/src/main/res/layout/$name.xml"
                    val content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent">
                            
                        </FrameLayout>
                    """.trimIndent()
                    
                    val newFile = fileRepository.createFile(name, path, project.id, content)
                    
                    // Add the new file to the layout files list
                    val currentFiles = _layoutFiles.value ?: emptyList()
                    _layoutFiles.value = currentFiles + newFile
                    
                    // Select the new file
                    selectLayoutFile(newFile)
                } catch (e: Exception) {
                    Timber.e(e, "Error creating layout file: $name")
                }
            }
        }
    }
    
    /**
     * Saves the current layout file with the generated XML
     */
    fun saveLayoutFile(xml: String) {
        _currentLayoutFile.value?.let { file ->
            viewModelScope.launch {
                _isSaving.value = true
                try {
                    val updatedFile = file.copy(content = xml, isModified = true)
                    val savedFile = fileRepository.saveFile(updatedFile)
                    
                    _currentLayoutFile.value = savedFile.copy(isModified = false)
                    
                    // Update the file in the layout files list
                    _layoutFiles.value = _layoutFiles.value?.map {
                        if (it.id == savedFile.id) savedFile.copy(isModified = false) else it
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error saving layout file: ${file.path}")
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }
    
    /**
     * Sets the generated XML
     */
    fun setGeneratedXml(xml: String) {
        _generatedXml.value = xml
    }
    
    /**
     * Adds a component to the design
     */
    fun addComponent(componentType: ComponentType, x: Float, y: Float) {
        // This is just for tracking components in the ViewModel
        // The actual component is added to the canvas in the UI
        Timber.d("Component added to design: $componentType at ($x, $y)")
    }
    
    /**
     * Selects a component in the design
     */
    fun selectComponent(view: View) {
        // This is just for tracking the selected component in the ViewModel
        // The actual selection is handled in the UI
        Timber.d("Component selected in design: $view")
    }
}