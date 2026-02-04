package com.yenaly.han1meviewer.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.FragmentTranslationCacheBinding
import com.yenaly.han1meviewer.logic.TranslationCache
import com.yenaly.han1meviewer.logic.TranslationManager
import com.yenaly.han1meviewer.ui.adapter.TranslationCacheAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TranslationCacheFragment : Fragment() {
    
    private var _binding: FragmentTranslationCacheBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TranslationCacheAdapter
    private lateinit var translationManager: TranslationManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationCacheBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        translationManager = TranslationManager.getInstance(requireContext())
        
        setupRecyclerView()
        setupToolbar()
        loadCache()
        setupFilterButtons()
    }
    
    private fun setupRecyclerView() {
        adapter = TranslationCacheAdapter(
            onEditClick = { cache ->
                // Show edit dialog
                showEditDialog(cache)
            },
            onDeleteClick = { cache ->
                lifecycleScope.launch {
                    translationManager.deleteCacheItem(cache.id)
                    loadCache()
                }
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.translation_cache)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        
        binding.toolbar.inflateMenu(R.menu.menu_translation_cache)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_stats -> {
                    showStats()
                    true
                }
                R.id.action_clear_all -> {
                    lifecycleScope.launch {
                        translationManager.clearCache()
                        loadCache()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadCache() {
        lifecycleScope.launch {
            val translationManager = TranslationManager.getInstance(requireContext())
            translationManager.cacheDao().getAll().collectLatest { cacheList ->
                adapter.submitList(cacheList)
                binding.emptyView.isVisible = cacheList.isEmpty()
                binding.recyclerView.isVisible = cacheList.isNotEmpty()
            }
        }
    }
    
    private fun setupFilterButtons() {
        val filters = mapOf(
            binding.btnAll to null,
            binding.btnTitles to TranslationCache.ContentType.TITLE,
            binding.btnDescriptions to TranslationCache.ContentType.DESCRIPTION,
            binding.btnComments to TranslationCache.ContentType.COMMENT,
            binding.btnTags to TranslationCache.ContentType.TAG
        )
        
        filters.forEach { (button, contentType) ->
            button?.setOnClickListener {
                // Highlight selected filter
                filters.keys.forEach { it?.isSelected = false }
                button.isSelected = true
                
                // Filter data
                lifecycleScope.launch {
                    val allCache = translationManager.cacheDao().getAll().first()
                    val filtered = if (contentType != null) {
                        allCache.filter { it.contentType == contentType }
                    } else {
                        allCache
                    }
                    adapter.submitList(filtered)
                    binding.emptyView.isVisible = filtered.isEmpty()
                    binding.recyclerView.isVisible = filtered.isNotEmpty()
                }
            }
        }
    }
    
    private fun showEditDialog(cache: TranslationCache) {
        // Create edit dialog
        // This is a simplified version - implement full dialog
    }
    
    private fun showStats() {
        lifecycleScope.launch {
            val stats = translationManager.getStats()
            
            val message = buildString {
                append("Total characters translated: ${stats["totalChars"]}\n")
                append("Total items cached: ${stats["totalItems"]}\n\n")
                
                append("By type:\n")
                (stats["byType"] as? Map<*, *>)?.forEach { (type, count) ->
                    append("  $type: $count\n")
                }
                
                append("\nAPI Key usage:\n")
                (stats["apiKeys"] as? List<*>)?.forEach { keyMap ->
                    val map = keyMap as Map<*, *>
                    append("  ${map["key"]}: ${map["charsUsed"]}/${map["monthlyLimit"]} (${map["remaining"]} remaining)\n")
                }
            }
            
            // Show dialog with stats
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Translation Statistics")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}