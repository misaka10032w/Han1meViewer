package com.yenaly.han1meviewer.ui.adapter

import android.R
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import androidx.core.graphics.toColorInt
import com.google.android.material.color.MaterialColors

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/26 026 16:09
 */
class SuperResolutionAdapter(private var currentIndex: Int, items: List<String>) : BaseQuickAdapter<String, QuickViewHolder>(
    items.toMutableList()
) {
    private val colorPrimary: Int
        get() = MaterialColors.getColor(
            context,
            androidx.appcompat.R.attr.colorPrimary,
            Color.BLACK
        )

    init {
        isStateViewEnable = true
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: String?) {
        holder.setText(R.id.text1, item)
        holder.setTextColor(
            R.id.text1,
            if (currentIndex == holder.bindingAdapterPosition) colorPrimary
            else "#ffffff".toColorInt()
        )
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): QuickViewHolder {
        return QuickViewHolder(R.layout.simple_list_item_1, parent).also { viewHolder ->
            viewHolder.getView<TextView>(R.id.text1).gravity = Gravity.CENTER
        }
    }
}