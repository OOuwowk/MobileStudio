package com.mobileide.editor.optimized;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.util.SparseArray;
import android.view.View;

import com.mobileide.editor.BaseCodeEditor;
import com.mobileide.editor.syntax.SyntaxHighlightSpan;
import com.mobileide.editor.utils.TextChangeWatcher;

/**
 * محرر أكواد محسن مع تحسينات الأداء للأجهزة المحمولة
 */
public class OptimizedCodeEditor extends BaseCodeEditor {
    private final SparseArray<SyntaxHighlightSpan> cachedHighlightSpans;
    private final TextChangeWatcher textChangeWatcher;
    
    public OptimizedCodeEditor(Context context) {
        super(context);
        cachedHighlightSpans = new SparseArray<>();
        
        // تحسين أداء التمرير
        setVerticalScrollBarEnabled(true);
        setFadingEdgeLength(0);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // تحسين معالجة النصوص الكبيرة
        textChangeWatcher = new TextChangeWatcher() {
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // إعادة تمييز الصيغة فقط للجزء المتغير
                rehighlightAffectedLines(start, before, count);
            }
        };
        
        Editable editableText = getEditableText();
        if (editableText != null) {
            editableText.setSpan(textChangeWatcher, 0, editableText.length(), 
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }
    
    /**
     * إعادة تمييز الأسطر المتأثرة بالتغيير
     */
    private void rehighlightAffectedLines(int start, int before, int count) {
        if (getLayout() == null) return;
        
        // تحديد الأسطر المتأثرة بالتغيير
        int startLine = getLayout().getLineForOffset(start);
        int endLine = getLayout().getLineForOffset(start + count);
        
        // إعادة تمييز الصيغة فقط للأسطر المتأثرة
        for (int i = startLine; i <= endLine; i++) {
            highlightLine(i);
        }
    }
    
    /**
     * تمييز سطر محدد
     */
    private void highlightLine(int lineNumber) {
        // استخدام التمييز المخزن مؤقتًا إذا كان متاحًا
        if (cachedHighlightSpans.get(lineNumber) != null) {
            applyHighlightSpan(lineNumber, cachedHighlightSpans.get(lineNumber));
            return;
        }
        
        // إنشاء تمييز جديد وتخزينه مؤقتًا
        SyntaxHighlightSpan span = createHighlightSpan(lineNumber);
        cachedHighlightSpans.put(lineNumber, span);
        applyHighlightSpan(lineNumber, span);
    }
    
    /**
     * إنشاء تمييز صيغة لسطر محدد
     */
    private SyntaxHighlightSpan createHighlightSpan(int lineNumber) {
        // هذه الطريقة ستكون مختلفة حسب تنفيذ الفئة الأساسية
        // هنا نفترض أن هناك طريقة لإنشاء تمييز صيغة
        return new SyntaxHighlightSpan();
    }
    
    /**
     * تطبيق تمييز صيغة على سطر محدد
     */
    private void applyHighlightSpan(int lineNumber, SyntaxHighlightSpan span) {
        // هذه الطريقة ستكون مختلفة حسب تنفيذ الفئة الأساسية
        // هنا نفترض أن هناك طريقة لتطبيق تمييز صيغة
    }
    
    /**
     * مسح ذاكرة التخزين المؤقت للتمييز
     */
    public void clearHighlightCache() {
        cachedHighlightSpans.clear();
    }
    
    /**
     * تحديث التمييز للنص بالكامل
     */
    @Override
    public void highlightSyntax() {
        clearHighlightCache();
        super.highlightSyntax();
    }
    
    /**
     * تحرير الموارد عند التدمير
     */
    @Override
    protected void onDetachedFromWindow() {
        clearHighlightCache();
        super.onDetachedFromWindow();
    }
}