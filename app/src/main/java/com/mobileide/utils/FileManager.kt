package com.mobileide.utils

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * Creates a project directory structure
     */
    fun createProjectDirectory(projectName: String): File {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdir()
        }
        
        val projectDir = File(projectsDir, projectName)
        if (!projectDir.exists()) {
            projectDir.mkdir()
        }
        
        // Create standard Android project structure
        createDirectories(
            File(projectDir, "src/main/java"),
            File(projectDir, "src/main/res/layout"),
            File(projectDir, "src/main/res/values"),
            File(projectDir, "src/main/res/drawable"),
            File(projectDir, "src/main/res/mipmap"),
            File(projectDir, "bin"),
            File(projectDir, "build")
        )
        
        return projectDir
    }
    
    /**
     * Creates a file with the given content
     */
    fun createFile(file: File, content: String): Boolean {
        return try {
            // Ensure parent directories exist
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
            }
            true
        } catch (e: IOException) {
            Timber.e(e, "Failed to create file: ${file.absolutePath}")
            false
        }
    }
    
    /**
     * Reads a file's content as a string
     */
    fun readFile(file: File): String? {
        return try {
            file.readText()
        } catch (e: IOException) {
            Timber.e(e, "Failed to read file: ${file.absolutePath}")
            null
        }
    }
    
    /**
     * Deletes a file or directory
     */
    fun delete(file: File): Boolean {
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }
    
    /**
     * Lists all files in a directory
     */
    fun listFiles(directory: File): List<File> {
        return if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Finds all Java files in a directory (recursively)
     */
    fun findJavaFiles(directoryPath: String): List<File> {
        val directory = File(directoryPath)
        val result = mutableListOf<File>()
        
        if (directory.exists() && directory.isDirectory) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile && (file.name.endsWith(".java") || file.name.endsWith(".kt"))) {
                    result.add(file)
                }
            }
        }
        
        return result
    }
    
    /**
     * Creates multiple directories
     */
    private fun createDirectories(vararg directories: File) {
        directories.forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
}