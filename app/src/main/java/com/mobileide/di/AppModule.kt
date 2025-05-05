package com.mobileide.di

import android.content.Context
import com.mobileide.compiler.CompilerOptimizer
import com.mobileide.compiler.CompilerService
import com.mobileide.data.local.AppDatabase
import com.mobileide.data.local.ProjectDao
import com.mobileide.data.repository.ProjectRepositoryImpl
import com.mobileide.domain.repository.ProjectRepository
import com.mobileide.domain.service.ProjectManagerOptimizer
import com.mobileide.editor.BasicAutoCompleteProvider
import com.mobileide.presentation.ui.UIOptimizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }
    
    @Provides
    @Singleton
    fun provideProjectRepository(projectDao: ProjectDao): ProjectRepository {
        return ProjectRepositoryImpl(projectDao)
    }
    
    @Provides
    @Singleton
    fun provideUIOptimizer(@ApplicationContext context: Context): UIOptimizer {
        return UIOptimizer(context)
    }
    
    @Provides
    @Singleton
    fun provideBasicAutoCompleteProvider(@ApplicationContext context: Context): BasicAutoCompleteProvider {
        return BasicAutoCompleteProvider(context)
    }
    
    @Provides
    @Singleton
    fun provideCompilerOptimizer(
        @ApplicationContext context: Context,
        compilerService: CompilerService
    ): CompilerOptimizer {
        return CompilerOptimizer(context, compilerService)
    }
    
    @Provides
    @Singleton
    fun provideProjectManagerOptimizer(
        @ApplicationContext context: Context,
        projectRepository: ProjectRepository
    ): ProjectManagerOptimizer {
        return ProjectManagerOptimizer(context, projectRepository, null) // FileManager will be injected by Hilt
    }
}