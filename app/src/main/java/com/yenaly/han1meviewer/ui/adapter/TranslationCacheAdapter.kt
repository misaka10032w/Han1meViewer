package com.yenaly.han1meviewer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ItemTranslationCacheBinding
import com.yenaly.han1meviewer.logic.TranslationCache

class TranslationCacheAdapter(
    private val onEditClick: (TranslationCache) -> Unit,
    private val onDeleteClick: (TranslationCache) -> Unit
) : ListAdapter<TranslationCache, TranslationCacheAdapter.ViewHolder>(DiffCallback()) {
    
    class ViewHolder(
        private val binding: ItemTranslationCacheBinding,
        private val onEditClick: (TranslationCache) -> Unit,
        private val onDeleteClick: (TranslationCache) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(cache: TranslationCache) {
            binding.apply {
                textOriginal.text = cache.originalText
                textTranslated.text = cache.translatedText
                textType.text = cache.contentType.name
                textLanguage.text = "${cache.sourceLang} → ${cache.targetLang}"
                textVideoCode.text = cache.videoCode ?: "N/A"
                textTimestamp.text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(java.util.Date(cache.timestamp))
                
                // Show/hide translation on click
                itemView.setOnClickListener {
                    textTranslated.isVisible = !textTranslated.isVisible
                }
                
                // Edit button
                btnEdit.setOnClickListener {
                    onEditClick(cache)
                }
                
                // Delete button
                btnDelete.setOnClickListener {
                    onDeleteClick(cache)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTranslationCacheBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DiffCallback : DiffUtil.ItemCallback<TranslationCache>() {
        override fun areItemsTheSame(oldItem: TranslationCache, newItem: TranslationCache): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TranslationCache, newItem: TranslationCache): Boolean {
            return oldItem == newItem
        }
    }
}
