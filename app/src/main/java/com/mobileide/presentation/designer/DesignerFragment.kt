package com.mobileide.presentation.designer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.mobileide.databinding.FragmentDesignerBinding
import com.mobileide.designer.ComponentType
import com.mobileide.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DesignerFragment : Fragment() {
    
    private var _binding: FragmentDesignerBinding? = null
    private val binding get() = _binding!!
    
    private val mainViewModel: MainViewModel by activityViewModels()
    private val designerViewModel: DesignerViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesignerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUiDesigner()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupUiDesigner() {
        binding.uiDesignerView.apply {
            // Set component added listener
            setOnComponentAddedListener { componentType, x, y ->
                Timber.d("Component added: $componentType at ($x, $y)")
                designerViewModel.addComponent(componentType, x, y)
            }
            
            // Set component selected listener
            setOnComponentSelectedListener { view ->
                Timber.d("Component selected: $view")
                designerViewModel.selectComponent(view)
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnPreview.setOnClickListener {
            showPreview()
        }
        
        binding.btnGenerateXml.setOnClickListener {
            generateXml()
        }
        
        binding.btnClear.setOnClickListener {
            clearDesigner()
        }
        
        binding.btnSave.setOnClickListener {
            saveLayout()
        }
    }
    
    private fun observeViewModel() {
        // Observe current project from MainViewModel
        mainViewModel.currentProject.observe(viewLifecycleOwner) { project ->
            project?.let {
                designerViewModel.setCurrentProject(it)
            }
        }
        
        // Observe current layout file
        designerViewModel.currentLayoutFile.observe(viewLifecycleOwner) { layoutFile ->
            layoutFile?.let {
                binding.tvLayoutName.text = it.name
            }
        }
        
        // Observe generated XML
        designerViewModel.generatedXml.observe(viewLifecycleOwner) { xml ->
            binding.xmlPreviewTextView.text = xml
        }
    }
    
    private fun showPreview() {
        // Generate XML and show preview
        val xml = binding.uiDesignerView.generateLayoutXml()
        designerViewModel.setGeneratedXml(xml)
        
        // Show XML preview
        binding.xmlPreviewContainer.visibility = View.VISIBLE
    }
    
    private fun generateXml() {
        // Generate XML and save to file
        val xml = binding.uiDesignerView.generateLayoutXml()
        designerViewModel.setGeneratedXml(xml)
        
        // Show XML preview
        binding.xmlPreviewContainer.visibility = View.VISIBLE
    }
    
    private fun clearDesigner() {
        // Clear the designer canvas
        binding.uiDesignerView.clearCanvas()
        
        // Clear generated XML
        designerViewModel.setGeneratedXml("")
        
        // Hide XML preview
        binding.xmlPreviewContainer.visibility = View.GONE
    }
    
    private fun saveLayout() {
        // Generate XML and save to file
        val xml = binding.uiDesignerView.generateLayoutXml()
        designerViewModel.saveLayoutFile(xml)
        
        // Hide XML preview
        binding.xmlPreviewContainer.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}