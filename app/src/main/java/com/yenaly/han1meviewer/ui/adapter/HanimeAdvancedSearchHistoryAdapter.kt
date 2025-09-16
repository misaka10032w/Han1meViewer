package com.yenaly.han1meviewer.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ItemHanimeAdvancedSearchHistoryBinding
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity

class HanimeAdvancedSearchHistoryAdapter(
    private val onDelete: (HanimeAdvancedSearchHistoryEntity) -> Unit,
    private val onClick: (HanimeAdvancedSearchHistoryEntity) -> Unit
) : ListAdapter<HanimeAdvancedSearchHistoryEntity, HanimeAdvancedSearchHistoryAdapter.ViewHolder>(
    DiffCallback
) {

    object DiffCallback : DiffUtil.ItemCallback<HanimeAdvancedSearchHistoryEntity>() {
        override fun areItemsTheSame(
            oldItem: HanimeAdvancedSearchHistoryEntity,
            newItem: HanimeAdvancedSearchHistoryEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: HanimeAdvancedSearchHistoryEntity,
            newItem: HanimeAdvancedSearchHistoryEntity
        ): Boolean = oldItem == newItem
    }

    inner class ViewHolder(val binding: ItemHanimeAdvancedSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HanimeAdvancedSearchHistoryEntity) = with(binding) {
            val ctx = root.context
            tvQuery.text = item.query?.takeIf { it.isNotBlank() } ?: ""
            tvQuery.isVisible = !item.query.isNullOrBlank()

            val conditions = buildList {
                item.genre?.takeIf { it.isNotBlank() }?.let { add("${ctx.getString(R.string.type)}: $it") }
                item.sort?.takeIf { it.isNotBlank() }?.let { add("${ctx.getString(R.string.sort_option)}: $it") }
                if (item.broad == true) add(ctx.getString(R.string.pair_widely))
                item.date?.takeIf { it.isNotBlank() }?.let { add("${ctx.getString(R.string.release_date)}: $it") }
                item.duration?.takeIf { it.isNotBlank() }?.let { add("${ctx.getString(R.string.duration)}: $it") }
                if (!item.tags.isNullOrBlank()) add("${ctx.getString(R.string.tag)}: ${item.tags}")
                if (!item.brands.isNullOrBlank()) add("${ctx.getString(R.string.brand)}: ${item.brands}")
            }.joinToString(" || ")

            tvConditions.text = conditions

            btnDelete.setOnClickListener { onDelete(item) }
            cardHistory.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHanimeAdvancedSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
