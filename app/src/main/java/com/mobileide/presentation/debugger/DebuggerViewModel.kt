package com.mobileide.presentation.debugger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileide.debugger.DebuggerService
import com.mobileide.debugger.EvaluationResult
import com.mobileide.domain.model.Project
import com.mobileide.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DebuggerViewModel @Inject constructor(
    private val debuggerService: DebuggerService,
    private val fileRepository: FileRepository
) : ViewModel() {
    
    private val _currentProject = MutableLiveData<Project>()
    val currentProject: LiveData<Project> = _currentProject
    
    private val _isDebugging = MutableLiveData<Boolean>()
    val isDebugging: LiveData<Boolean> = _isDebugging
    
    private val _variables = MutableLiveData<List<Variable>>()
    val variables: LiveData<List<Variable>> = _variables
    
    private val _logs = MutableLiveData<List<String>>()
    val logs: LiveData<List<String>> = _logs
    
    private val _breakpoints = MutableLiveData<List<Breakpoint>>()
    val breakpoints: LiveData<List<Breakpoint>> = _breakpoints
    
    private val _evaluationResult = MutableLiveData<String>()
    val evaluationResult: LiveData<String> = _evaluationResult
    
    private val _currentLine = MutableLiveData<Int>()
    val currentLine: LiveData<Int> = _currentLine
    
    private val _currentFile = MutableLiveData<String>()
    val currentFile: LiveData<String> = _currentFile
    
    // Initialize with empty lists
    init {
        _variables.value = emptyList()
        _logs.value = emptyList()
        _breakpoints.value = emptyList()
        _isDebugging.value = false
    }
    
    /**
     * Sets the current project
     */
    fun setCurrentProject(project: Project) {
        _currentProject.value = project
    }
    
    /**
     * Starts debugging the current project
     */
    fun startDebugging(project: Project) {
        viewModelScope.launch {
            try {
                addLog("Starting debugging for project: ${project.name}")
                
                // Get the APK file
                val apkFile = File("${project.path}/bin/${project.name}-debug.apk")
                
                // Start debugging
                val result = debuggerService.startDebugging(apkFile)
                
                if (result.success) {
                    _isDebugging.value = true
                    addLog("Debugging started successfully")
                    
                    // Add some mock variables for demonstration
                    addMockVariables()
                } else {
                    addLog("Failed to start debugging: ${result.errors.joinToString()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting debugging")
                addLog("Error starting debugging: ${e.message}")
            }
        }
    }
    
    /**
     * Stops debugging
     */
    fun stopDebugging() {
        viewModelScope.launch {
            try {
                if (_isDebugging.value == true) {
                    val result = debuggerService.stopDebugging()
                    
                    if (result) {
                        _isDebugging.value = false
                        addLog("Debugging stopped")
                        
                        // Clear variables
                        _variables.value = emptyList()
                    } else {
                        addLog("Failed to stop debugging")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stopping debugging")
                addLog("Error stopping debugging: ${e.message}")
            }
        }
    }
    
    /**
     * Adds a breakpoint
     */
    fun addBreakpoint(file: String, line: Int) {
        viewModelScope.launch {
            try {
                val result = debuggerService.setBreakpoint(file, line)
                
                if (result) {
                    addLog("Breakpoint added at $file:$line")
                    
                    // Add to breakpoints list
                    val currentBreakpoints = _breakpoints.value ?: emptyList()
                    _breakpoints.value = currentBreakpoints + Breakpoint(file, line)
                } else {
                    addLog("Failed to add breakpoint at $file:$line")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding breakpoint")
                addLog("Error adding breakpoint: ${e.message}")
            }
        }
    }
    
    /**
     * Removes a breakpoint
     */
    fun removeBreakpoint(file: String, line: Int) {
        viewModelScope.launch {
            try {
                val result = debuggerService.removeBreakpoint(file, line)
                
                if (result) {
                    addLog("Breakpoint removed at $file:$line")
                    
                    // Remove from breakpoints list
                    val currentBreakpoints = _breakpoints.value ?: emptyList()
                    _breakpoints.value = currentBreakpoints.filter { 
                        it.file != file || it.line != line 
                    }
                } else {
                    addLog("Failed to remove breakpoint at $file:$line")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing breakpoint")
                addLog("Error removing breakpoint: ${e.message}")
            }
        }
    }
    
    /**
     * Resumes execution
     */
    fun resumeExecution() {
        viewModelScope.launch {
            try {
                val result = debuggerService.resume()
                
                if (result) {
                    addLog("Execution resumed")
                    
                    // Update variables
                    updateVariables()
                } else {
                    addLog("Failed to resume execution")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error resuming execution")
                addLog("Error resuming execution: ${e.message}")
            }
        }
    }
    
    /**
     * Steps over the current line
     */
    fun stepOver() {
        viewModelScope.launch {
            try {
                val result = debuggerService.stepOver()
                
                if (result) {
                    addLog("Stepped over")
                    
                    // Update variables
                    updateVariables()
                    
                    // Update current line
                    _currentLine.value = (_currentLine.value ?: 0) + 1
                } else {
                    addLog("Failed to step over")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stepping over")
                addLog("Error stepping over: ${e.message}")
            }
        }
    }
    
    /**
     * Steps into a function
     */
    fun stepInto() {
        viewModelScope.launch {
            try {
                val result = debuggerService.stepInto()
                
                if (result) {
                    addLog("Stepped into")
                    
                    // Update variables
                    updateVariables()
                    
                    // Update current line and file (simulated)
                    _currentLine.value = 1
                    _currentFile.value = "SomeOtherClass.java"
                } else {
                    addLog("Failed to step into")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stepping into")
                addLog("Error stepping into: ${e.message}")
            }
        }
    }
    
    /**
     * Steps out of the current function
     */
    fun stepOut() {
        viewModelScope.launch {
            try {
                val result = debuggerService.stepOut()
                
                if (result) {
                    addLog("Stepped out")
                    
                    // Update variables
                    updateVariables()
                    
                    // Update current line and file (simulated)
                    _currentLine.value = 43
                    _currentFile.value = "MainActivity.java"
                } else {
                    addLog("Failed to step out")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stepping out")
                addLog("Error stepping out: ${e.message}")
            }
        }
    }
    
    /**
     * Evaluates an expression
     */
    fun evaluateExpression(expression: String) {
        viewModelScope.launch {
            try {
                addLog("Evaluating expression: $expression")
                
                val result = debuggerService.evaluate(expression)
                
                if (result.success) {
                    _evaluationResult.value = result.value ?: "null"
                    addLog("Evaluation result: ${result.value}")
                } else {
                    _evaluationResult.value = "Error: ${result.message}"
                    addLog("Evaluation failed: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error evaluating expression")
                addLog("Error evaluating expression: ${e.message}")
                _evaluationResult.value = "Error: ${e.message}"
            }
        }
    }
    
    /**
     * Updates the variables list
     */
    private fun updateVariables() {
        // In a real implementation, this would fetch variables from the debugger
        // For now, just add some mock variables
        addMockVariables()
    }
    
    /**
     * Adds a log message
     */
    private fun addLog(message: String) {
        Timber.d("Debug log: $message")
        
        val currentLogs = _logs.value ?: emptyList()
        _logs.value = currentLogs + message
    }
    
    /**
     * Adds mock variables for demonstration
     */
    private fun addMockVariables() {
        val mockVariables = listOf(
            Variable("count", "42", "int"),
            Variable("name", "\"John Doe\"", "String"),
            Variable("isActive", "true", "boolean"),
            Variable("items", "ArrayList (size=3)", "ArrayList<String>")
        )
        
        _variables.value = mockVariables
    }
    
    /**
     * Represents a variable in the debugger
     */
    data class Variable(
        val name: String,
        val value: String,
        val type: String
    )
    
    /**
     * Represents a breakpoint in the debugger
     */
    data class Breakpoint(
        val file: String,
        val line: Int
    )
}