package com.mobileide.presentation.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobileide.R
import com.mobileide.databinding.DialogNewProjectBinding
import com.mobileide.databinding.FragmentProjectListBinding
import com.mobileide.domain.service.TemplateService
import com.mobileide.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProjectListFragment : Fragment() {
    
    private var _binding: FragmentProjectListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProjectListViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    
    @Inject
    lateinit var templateService: TemplateService
    
    private lateinit var adapter: ProjectAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = ProjectAdapter { project ->
            mainViewModel.openProject(project.id)
            findNavController().navigate(R.id.action_projectListFragment_to_editorFragment)
        }
        
        binding.projectList.apply {
            adapter = this@ProjectListFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupFab() {
        binding.fabNewProject.setOnClickListener {
            showNewProjectDialog()
        }
    }
    
    private fun observeViewModel() {
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            adapter.submitList(projects)
            binding.emptyView.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun showNewProjectDialog() {
        val dialogBinding = DialogNewProjectBinding.inflate(layoutInflater)
        
        // Setup template spinner
        val templates = templateService.getTemplates()
        val templateNames = templates.map { it.name }.toTypedArray()
        
        // TODO: Create a proper adapter for the spinner
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_project)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = dialogBinding.projectName.text.toString()
                val packageName = dialogBinding.packageName.text.toString()
                val templatePosition = dialogBinding.templateSpinner.selectedItemPosition
                
                if (name.isNotBlank() && packageName.isNotBlank()) {
                    viewModel.createProject(name, packageName, templatePosition)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}