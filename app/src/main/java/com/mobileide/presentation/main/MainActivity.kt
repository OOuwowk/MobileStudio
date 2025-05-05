package com.mobileide.presentation.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.mobileide.R
import com.mobileide.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupNavigation()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.projectListFragment)
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
        
        // Hide bottom navigation on project list screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.projectListFragment) {
                binding.bottomNav.visibility = View.GONE
            } else {
                binding.bottomNav.visibility = View.VISIBLE
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentProject.observe(this) { project ->
            project?.let {
                supportActionBar?.title = it.name
            } ?: run {
                supportActionBar?.title = getString(R.string.app_name)
            }
        }
        
        viewModel.buildStatus.observe(this) { status ->
            when (status) {
                is BuildStatus.Building -> {
                    Snackbar.make(binding.root, "Building project...", Snackbar.LENGTH_SHORT).show()
                }
                is BuildStatus.Success -> {
                    Snackbar.make(binding.root, "Build successful", Snackbar.LENGTH_SHORT).show()
                }
                is BuildStatus.Error -> {
                    Snackbar.make(binding.root, "Build failed: ${status.message}", Snackbar.LENGTH_LONG).show()
                }
                else -> { /* Idle state, do nothing */ }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_build -> {
                viewModel.buildProject()
                true
            }
            R.id.action_run -> {
                viewModel.runProject()
                true
            }
            R.id.action_debug -> {
                viewModel.debugProject()
                true
            }
            R.id.action_settings -> {
                navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}