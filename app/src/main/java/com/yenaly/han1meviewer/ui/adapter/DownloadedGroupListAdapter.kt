package com.yenaly.han1meviewer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity

class DownloadedGroupListAdapter(
    private val onDelete: (DownloadGroupEntity) -> Unit
) : ListAdapter<DownloadGroupEntity, DownloadedGroupListAdapter.ViewHolder>(COMPARATOR) {

    companion object{
        val COMPARATOR = object : DiffUtil.ItemCallback<DownloadGroupEntity>() {
            override fun areItemsTheSame(
                oldItem: DownloadGroupEntity,
                newItem: DownloadGroupEntity
            ) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: DownloadGroupEntity,
                newItem: DownloadGroupEntity
            ) =
                oldItem == newItem
        }
    }

    class ViewHolder(
        itemView: View,
        private val onDelete: (DownloadGroupEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = itemView.findViewById(R.id.name)
        val deleteButton: ImageButton = itemView.findViewById(R.id.icon_delete)

        fun bind(group: DownloadGroupEntity) {
            nameView.text = group.name

            deleteButton.isEnabled =
                group.id != DownloadGroupEntity.DEFAULT_GROUP_ID

            deleteButton.alpha =
                if (deleteButton.isEnabled) 1f else 0.3f

            deleteButton.setOnClickListener {
                onDelete(group)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_icon_r_with_text, parent, false)

        return ViewHolder(view, onDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
