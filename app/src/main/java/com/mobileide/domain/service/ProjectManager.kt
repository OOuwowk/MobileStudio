package com.mobileide.domain.service

import com.mobileide.compiler.CompilerService
import com.mobileide.debugger.DebuggerService
import com.mobileide.domain.model.Project
import com.mobileide.domain.model.template.ProjectTemplate
import com.mobileide.domain.repository.ProjectRepository
import com.mobileide.utils.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectManager @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val fileManager: FileManager,
    private val templateService: TemplateService,
    private val compilerService: CompilerService,
    private val debuggerService: DebuggerService
) {
    /**
     * Creates a new project from a template
     */
    suspend fun createProject(name: String, packageName: String, templateId: Int): Result<Project> {
        return try {
            val template = templateService.getTemplateById(templateId)
                ?: return Result.failure(IllegalArgumentException("Template not found"))
            
            // Create project directory
            val projectDir = fileManager.createProjectDirectory(name)
            
            // Create project files from template
            createProjectFiles(projectDir, template, name, packageName)
            
            // Save project to database
            val project = Project(
                name = name,
                packageName = packageName,
                path = projectDir.absolutePath
            )
            
            val id = projectRepository.insertProject(project)
            Result.success(project.copy(id = id))
        } catch (e: Exception) {
            Timber.e(e, "Failed to create project")
            Result.failure(e)
        }
    }
    
    /**
     * Opens an existing project
     */
    suspend fun openProject(projectId: Long): Result<Project> {
        return try {
            val project = projectRepository.getProject(projectId)
                ?: return Result.failure(IllegalArgumentException("Project not found"))
            
            // Verify project directory exists
            val projectDir = File(project.path)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return Result.failure(IllegalStateException("Project directory not found"))
            }
            
            Result.success(project)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open project")
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a project
     */
    suspend fun deleteProject(projectId: Long): Result<Boolean> {
        return try {
            val project = projectRepository.getProject(projectId)
                ?: return Result.failure(IllegalArgumentException("Project not found"))
            
            // Delete project directory
            val projectDir = File(project.path)
            if (projectDir.exists()) {
                fileManager.delete(projectDir)
            }
            
            // Delete from database
            projectRepository.deleteProject(projectId)
            
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete project")
            Result.failure(e)
        }
    }
    
    /**
     * Creates project files from a template
     */
    private fun createProjectFiles(
        projectDir: File,
        template: ProjectTemplate,
        projectName: String,
        packageName: String
    ) {
        template.files.forEach { templateFile ->
            // Replace placeholders in path and content
            val packagePath = packageName.replace('.', '/')
            val filePath = templateFile.path
                .replace("{{PACKAGE_PATH}}", packagePath)
            
            val content = templateFile.content
                .replace("{{PROJECT_NAME}}", projectName)
                .replace("{{PACKAGE_NAME}}", packageName)
                .replace("{{PACKAGE_PATH}}", packagePath)
            
            // Create the file
            val file = File(projectDir, filePath)
            fileManager.createFile(file, content)
        }
    }
    
    /**
     * Builds a project and returns the APK file
     */
    suspend fun buildProject(project: Project): Result<File> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Building project: ${project.name}")
            
            // Call the compiler service to build the project
            val result = compilerService.compileProject(project.path, project.name)
            
            return@withContext if (result.success) {
                val apkFile = result.apkFile
                if (apkFile != null && apkFile.exists()) {
                    Timber.d("Build successful: ${apkFile.absolutePath}")
                    Result.success(apkFile)
                } else {
                    Timber.e("Build failed: No APK file produced")
                    Result.failure(IllegalStateException("No APK file produced"))
                }
            } else {
                val errorMessage = result.errors.joinToString("\n")
                Timber.e("Build failed: $errorMessage")
                Result.failure(IllegalStateException("Build failed: $errorMessage"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during build")
            Result.failure(e)
        }
    }
    
    /**
     * Installs and runs an APK file
     */
    suspend fun installAndRunApp(apkFile: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Installing and running APK: ${apkFile.absolutePath}")
            
            // In a real implementation, this would use PackageManager or ADB to install and run the APK
            // For now, just simulate success
            
            return@withContext Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Exception during install and run")
            Result.failure(e)
        }
    }
    
    /**
     * Starts a debugging session for an APK file
     */
    suspend fun startDebugging(apkFile: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting debugging for APK: ${apkFile.absolutePath}")
            
            // Call the debugger service to start debugging
            val result = debuggerService.startDebugging(apkFile)
            
            return@withContext if (result.success) {
                Timber.d("Debugging started successfully")
                Result.success(true)
            } else {
                val errorMessage = result.errors.joinToString("\n")
                Timber.e("Failed to start debugging: $errorMessage")
                Result.failure(IllegalStateException("Failed to start debugging: $errorMessage"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during debugging")
            Result.failure(e)
        }
    }
}