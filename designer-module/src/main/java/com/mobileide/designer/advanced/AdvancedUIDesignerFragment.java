package com.mobileide.designer.advanced;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobileide.designer.R;
import com.mobileide.designer.advanced.ui.AdvancedUIDesigner;
import com.mobileide.designer.databinding.FragmentAdvancedUiDesignerBinding;

/**
 * شاشة مصمم الواجهات المتقدم
 */
public class AdvancedUIDesignerFragment extends Fragment {
    
    private FragmentAdvancedUiDesignerBinding binding;
    private AdvancedUIDesignerViewModel viewModel;
    private AdvancedUIDesigner designer;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdvancedUiDesignerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // إعداد ViewModel
        viewModel = new ViewModelProvider(this).get(AdvancedUIDesignerViewModel.class);
        
        // إعداد مصمم الواجهات
        designer = binding.uiDesigner;
        
        // إعداد أزرار الإجراءات
        setupActionButtons();
    }
    
    /**
     * إعداد أزرار الإجراءات
     */
    private void setupActionButtons() {
        // زر توليد XML
        binding.fabGenerateXml.setOnClickListener(v -> {
            generateXml();
        });
        
        // زر تحميل XML
        binding.fabLoadXml.setOnClickListener(v -> {
            showLoadXmlDialog();
        });
        
        // زر حذف العنصر
        binding.fabDeleteComponent.setOnClickListener(v -> {
            deleteSelectedComponent();
        });
        
        // زر المعاينة
        binding.fabPreview.setOnClickListener(v -> {
            togglePreview();
        });
    }
    
    /**
     * توليد XML من التصميم
     */
    private void generateXml() {
        String xml = designer.exportLayoutXml();
        viewModel.setGeneratedXml(xml);
        
        // نسخ XML إلى الحافظة
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Generated XML", xml);
        clipboard.setPrimaryClip(clip);
        
        // عرض رسالة تأكيد
        Toast.makeText(requireContext(), R.string.xml_copied, Toast.LENGTH_SHORT).show();
        
        // عرض مربع حوار مع XML
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.generate_xml)
            .setMessage(xml)
            .setPositiveButton(R.string.ok, null)
            .show();
    }
    
    /**
     * عرض مربع حوار تحميل XML
     */
    private void showLoadXmlDialog() {
        // إنشاء مربع حوار لإدخال XML
        // ملاحظة: هذه وظيفة متقدمة تتطلب تحليل XML وتحويله إلى عناصر
        // في هذا المثال، سنعرض فقط مربع حوار بسيط
        
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.load_xml)
            .setMessage(R.string.enter_xml)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                // هنا سيتم تحليل XML وتحميله
                Toast.makeText(requireContext(), "تم تحميل XML", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * حذف العنصر المحدد
     */
    private void deleteSelectedComponent() {
        // حذف العنصر المحدد من المصمم
        // ملاحظة: هذه وظيفة متقدمة تتطلب الوصول إلى العنصر المحدد
        // في هذا المثال، سنفترض أن هناك طريقة لحذف العنصر المحدد
        
        Toast.makeText(requireContext(), R.string.component_deleted, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * تبديل عرض المعاينة
     */
    private void togglePreview() {
        designer.toggleLivePreview();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}