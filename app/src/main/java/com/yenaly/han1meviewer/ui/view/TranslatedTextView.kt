package com.yenaly.han1meviewer.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.yenaly.han1meviewer.logic.TranslationCache
import com.yenaly.han1meviewer.logic.TranslationManager
import kotlinx.coroutines.launch

class TranslatedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    
    private var originalText: String = ""
    private var contentType: TranslationCache.ContentType = TranslationCache.ContentType.OTHER
    private var videoCode: String? = null
    
    fun setTranslatedText(
        text: String,
        contentType: TranslationCache.ContentType,
        videoCode: String? = null,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        this.originalText = text
        this.contentType = contentType
        this.videoCode = videoCode
        
        if (lifecycleOwner != null) {
            lifecycleOwner.lifecycleScope.launch {
                val translationManager = TranslationManager.getInstance(context)
                val translated = translationManager.translate(text, contentType, videoCode)
                setText(translated)
            }
        } else {
            setText(text)
        }
    }
    
    fun getOriginalText(): String = originalText
}