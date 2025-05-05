package com.mobileide.designer.advanced;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mobileide.designer.model.DesignerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel لمصمم الواجهات المتقدم
 */
public class AdvancedUIDesignerViewModel extends ViewModel {
    
    // قائمة العناصر في التصميم
    private final MutableLiveData<List<DesignerComponent>> components = new MutableLiveData<>(new ArrayList<>());
    
    // العنصر المحدد حاليًا
    private final MutableLiveData<DesignerComponent> selectedComponent = new MutableLiveData<>();
    
    // XML المولد
    private final MutableLiveData<String> generatedXml = new MutableLiveData<>("");
    
    /**
     * الحصول على قائمة العناصر
     */
    public LiveData<List<DesignerComponent>> getComponents() {
        return components;
    }
    
    /**
     * إضافة عنصر جديد
     */
    public void addComponent(DesignerComponent component) {
        List<DesignerComponent> currentList = components.getValue();
        if (currentList != null) {
            currentList.add(component);
            components.setValue(currentList);
        }
    }
    
    /**
     * حذف عنصر
     */
    public void removeComponent(DesignerComponent component) {
        List<DesignerComponent> currentList = components.getValue();
        if (currentList != null) {
            currentList.remove(component);
            components.setValue(currentList);
            
            // إذا كان العنصر المحذوف هو المحدد حاليًا، إلغاء التحديد
            if (component.equals(selectedComponent.getValue())) {
                selectedComponent.setValue(null);
            }
        }
    }
    
    /**
     * تحديث عنصر
     */
    public void updateComponent(DesignerComponent component) {
        List<DesignerComponent> currentList = components.getValue();
        if (currentList != null) {
            // البحث عن العنصر واستبداله
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(component.getId())) {
                    currentList.set(i, component);
                    break;
                }
            }
            components.setValue(currentList);
            
            // تحديث العنصر المحدد إذا كان هو نفسه
            if (selectedComponent.getValue() != null && 
                selectedComponent.getValue().getId().equals(component.getId())) {
                selectedComponent.setValue(component);
            }
        }
    }
    
    /**
     * الحصول على العنصر المحدد
     */
    public LiveData<DesignerComponent> getSelectedComponent() {
        return selectedComponent;
    }
    
    /**
     * تحديد عنصر
     */
    public void selectComponent(DesignerComponent component) {
        selectedComponent.setValue(component);
    }
    
    /**
     * الحصول على XML المولد
     */
    public LiveData<String> getGeneratedXml() {
        return generatedXml;
    }
    
    /**
     * تعيين XML المولد
     */
    public void setGeneratedXml(String xml) {
        generatedXml.setValue(xml);
    }
    
    /**
     * مسح التصميم
     */
    public void clearDesign() {
        components.setValue(new ArrayList<>());
        selectedComponent.setValue(null);
        generatedXml.setValue("");
    }
}