package com.mobileide

import android.app.Application
import com.mobileide.compiler.CompilerOptimizer
import com.mobileide.domain.service.ProjectManagerOptimizer
import com.mobileide.editor.BasicAutoCompleteProvider
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MobileIdeApplication : Application() {
    
    @Inject
    lateinit var projectManagerOptimizer: ProjectManagerOptimizer
    
    @Inject
    lateinit var compilerOptimizer: CompilerOptimizer
    
    @Inject
    lateinit var basicAutoCompleteProvider: BasicAutoCompleteProvider
    
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize optimizers
        initializeOptimizers()
    }
    
    private fun initializeOptimizers() {
        // Run optimizations in background
        Thread {
            try {
                // Optimize project management
                projectManagerOptimizer.optimizeProjectScanning()
                projectManagerOptimizer.setupProjectTemplates()
                projectManagerOptimizer.optimizeProjectIndexing()
                
                // Optimize compiler
                compilerOptimizer.optimizeCompilationProcess()
                compilerOptimizer.optimizeJavaCompilation()
                compilerOptimizer.optimizeDexCompilation()
                compilerOptimizer.optimizeResourceProcessing()
                compilerOptimizer.optimizeApkPackaging()
                
                // Set up auto-completion
                basicAutoCompleteProvider.createCodeSnippets()
                
                Timber.d("All optimizations completed")
            } catch (e: Exception) {
                Timber.e(e, "Error during optimization initialization")
            }
        }.start()
    }
}