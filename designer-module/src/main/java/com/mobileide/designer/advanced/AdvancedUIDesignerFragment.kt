package com.mobileide.designer.advanced

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mobileide.designer.R
import com.mobileide.designer.databinding.FragmentAdvancedUiDesignerBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * واجهة المستخدم لمصمم الواجهات المتقدم
 */
@AndroidEntryPoint
class AdvancedUIDesignerFragment : Fragment() {
    
    private var _binding: FragmentAdvancedUiDesignerBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UIDesignerViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedUiDesignerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // إعداد زر توليد XML
        binding.fabGenerateXml.setOnClickListener {
            generateXml()
        }
        
        // إعداد زر حذف العنصر المحدد
        binding.fabDeleteComponent.setOnClickListener {
            deleteSelectedComponent()
        }
        
        // إعداد زر تحميل XML
        binding.fabLoadXml.setOnClickListener {
            showLoadXmlDialog()
        }
    }
    
    private fun observeViewModel() {
        // مراقبة العنصر المحدد
        viewModel.selectedComponent.observe(viewLifecycleOwner) { component ->
            // تفعيل/تعطيل زر الحذف
            binding.fabDeleteComponent.isEnabled = component != null
        }
    }
    
    /**
     * توليد XML من التصميم
     */
    private fun generateXml() {
        try {
            val xml = binding.uiDesignerView.generateXml()
            
            // عرض XML في مربع حوار
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.generate_xml)
                .setMessage(xml)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            
            // نسخ XML إلى الحافظة
            val clipboard = requireContext().getSystemService(android.content.ClipboardManager::class.java)
            val clip = android.content.ClipData.newPlainText("XML", xml)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(requireContext(), "تم نسخ XML إلى الحافظة", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.e(e, "Error generating XML")
            Toast.makeText(requireContext(), "خطأ في توليد XML", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * حذف العنصر المحدد
     */
    private fun deleteSelectedComponent() {
        viewModel.deleteSelectedComponent()
    }
    
    /**
     * عرض مربع حوار لتحميل XML
     */
    private fun showLoadXmlDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = "أدخل XML هنا"
            isSingleLine = false
            minLines = 5
            maxLines = 10
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.load_xml)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val xml = editText.text.toString()
                if (xml.isNotEmpty()) {
                    binding.uiDesignerView.loadFromXml(xml)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}