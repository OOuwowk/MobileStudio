package com.mobileide.presentation.debugger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.mobileide.databinding.FragmentDebuggerBinding
import com.mobileide.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DebuggerFragment : Fragment() {
    
    private var _binding: FragmentDebuggerBinding? = null
    private val binding get() = _binding!!
    
    private val mainViewModel: MainViewModel by activityViewModels()
    private val debuggerViewModel: DebuggerViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebuggerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDebugControls()
        setupVariablesPanel()
        setupLogPanel()
        setupBreakpointsList()
        setupEvaluationPanel()
        observeViewModel()
    }
    
    private fun setupDebugControls() {
        // Setup debug control buttons
        binding.btnResume.setOnClickListener {
            debuggerViewModel.resumeExecution()
        }
        
        binding.btnStepOver.setOnClickListener {
            debuggerViewModel.stepOver()
        }
        
        binding.btnStepInto.setOnClickListener {
            debuggerViewModel.stepInto()
        }
        
        binding.btnStepOut.setOnClickListener {
            debuggerViewModel.stepOut()
        }
        
        binding.btnStop.setOnClickListener {
            debuggerViewModel.stopDebugging()
        }
        
        binding.btnStart.setOnClickListener {
            startDebugging()
        }
    }
    
    private fun setupVariablesPanel() {
        // Setup variables list adapter
        val variablesAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        binding.variablesList.adapter = variablesAdapter
    }
    
    private fun setupLogPanel() {
        // Setup log adapter
        val logAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        binding.logList.adapter = logAdapter
    }
    
    private fun setupBreakpointsList() {
        // Setup breakpoints list adapter
        val breakpointsAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        binding.breakpointsList.adapter = breakpointsAdapter
        
        // Add breakpoint button
        binding.btnAddBreakpoint.setOnClickListener {
            showAddBreakpointDialog()
        }
    }
    
    private fun setupEvaluationPanel() {
        // Evaluate button
        binding.btnEvaluate.setOnClickListener {
            val expression = binding.etExpression.text.toString()
            if (expression.isNotEmpty()) {
                debuggerViewModel.evaluateExpression(expression)
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe debugging state
        debuggerViewModel.isDebugging.observe(viewLifecycleOwner) { isDebugging ->
            updateDebugControlsState(isDebugging)
        }
        
        // Observe variables
        debuggerViewModel.variables.observe(viewLifecycleOwner) { variables ->
            updateVariablesList(variables)
        }
        
        // Observe logs
        debuggerViewModel.logs.observe(viewLifecycleOwner) { logs ->
            updateLogList(logs)
        }
        
        // Observe breakpoints
        debuggerViewModel.breakpoints.observe(viewLifecycleOwner) { breakpoints ->
            updateBreakpointsList(breakpoints)
        }
        
        // Observe evaluation result
        debuggerViewModel.evaluationResult.observe(viewLifecycleOwner) { result ->
            binding.tvEvaluationResult.text = result
        }
        
        // Observe current project from MainViewModel
        mainViewModel.currentProject.observe(viewLifecycleOwner) { project ->
            project?.let {
                debuggerViewModel.setCurrentProject(it)
            }
        }
    }
    
    private fun startDebugging() {
        mainViewModel.currentProject.value?.let { project ->
            Timber.d("Starting debugging for project: ${project.name}")
            debuggerViewModel.startDebugging(project)
        }
    }
    
    private fun updateDebugControlsState(isDebugging: Boolean) {
        // Enable/disable debug controls based on debugging state
        binding.btnResume.isEnabled = isDebugging
        binding.btnStepOver.isEnabled = isDebugging
        binding.btnStepInto.isEnabled = isDebugging
        binding.btnStepOut.isEnabled = isDebugging
        binding.btnStop.isEnabled = isDebugging
        binding.btnStart.isEnabled = !isDebugging
        binding.btnEvaluate.isEnabled = isDebugging
        binding.etExpression.isEnabled = isDebugging
    }
    
    private fun updateVariablesList(variables: List<DebuggerViewModel.Variable>) {
        val adapter = binding.variablesList.adapter as ArrayAdapter<String>
        adapter.clear()
        
        variables.forEach { variable ->
            adapter.add("${variable.name} = ${variable.value} (${variable.type})")
        }
        
        adapter.notifyDataSetChanged()
    }
    
    private fun updateLogList(logs: List<String>) {
        val adapter = binding.logList.adapter as ArrayAdapter<String>
        adapter.clear()
        adapter.addAll(logs)
        adapter.notifyDataSetChanged()
        
        // Scroll to bottom
        binding.logList.setSelection(adapter.count - 1)
    }
    
    private fun updateBreakpointsList(breakpoints: List<DebuggerViewModel.Breakpoint>) {
        val adapter = binding.breakpointsList.adapter as ArrayAdapter<String>
        adapter.clear()
        
        breakpoints.forEach { breakpoint ->
            adapter.add("${breakpoint.file}:${breakpoint.line}")
        }
        
        adapter.notifyDataSetChanged()
    }
    
    private fun showAddBreakpointDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.mobileide.R.layout.dialog_add_breakpoint, null
        )
        
        val etFile = dialogView.findViewById<android.widget.EditText>(com.mobileide.R.id.et_file)
        val etLine = dialogView.findViewById<android.widget.EditText>(com.mobileide.R.id.et_line)
        
        // Pre-fill with current file if available
        mainViewModel.currentFile.value?.let { file ->
            etFile.setText(file.name)
        }
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Breakpoint")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val file = etFile.text.toString()
                val lineStr = etLine.text.toString()
                
                if (file.isNotEmpty() && lineStr.isNotEmpty()) {
                    try {
                        val line = lineStr.toInt()
                        debuggerViewModel.addBreakpoint(file, line)
                    } catch (e: NumberFormatException) {
                        Timber.e("Invalid line number: $lineStr")
                        showErrorMessage("Invalid line number")
                    }
                } else {
                    showErrorMessage("File and line number are required")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    private fun showErrorMessage(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}