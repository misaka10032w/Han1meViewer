package com.yenaly.han1meviewer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.SearchOption

class ReleaseDateAdapter(
    private val items: List<SearchOption>,
    private val onClick: (SearchOption) -> Unit
) : RecyclerView.Adapter<ReleaseDateAdapter.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<MaterialCardView>(R.id.card)
        private val text = view.findViewById<TextView>(R.id.textTitle)

        fun bind(item: SearchOption, position: Int) {
            text.text = item.value
            card.strokeWidth = if (position == selectedPosition) 4 else 0
            card.isChecked = position == selectedPosition

            view.setOnClickListener {
                val prev = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPosition)
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }
}