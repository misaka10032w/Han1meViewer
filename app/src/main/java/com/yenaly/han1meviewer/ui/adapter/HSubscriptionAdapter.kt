package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import coil.load
import coil.transform.CircleCropTransformation
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Subscription
import com.yenaly.han1meviewer.ui.fragment.search.SearchFragment
import com.yenaly.han1meviewer.util.showAlertDialog

class HSubscriptionAdapter(
    private val fragment: SearchFragment
) : BaseQuickAdapter<Subscription, QuickViewHolder>(COMPARATOR) {

    companion object {
        const val DELETE = 1
        const val CHECK = 1 shl 1
    }

    private object COMPARATOR : DiffUtil.ItemCallback<Subscription>() {
        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Subscription, newItem: Subscription): Int {
            var bitmap = 0
            if (oldItem.isDeleteVisible != newItem.isDeleteVisible) {
                bitmap = bitmap or DELETE
            }
            return bitmap
        }
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: Subscription?) {
        item ?: return
        val currentBrand = fragment.viewModel.subscriptionBrand
        holder.getView<CheckBox>(R.id.cb_select).apply {
            isVisible = item.name == currentBrand
            isChecked = isVisible
        }
        holder.getView<View>(R.id.btn_delete).isVisible = item.isDeleteVisible
        holder.setText(R.id.tv_artist, item.name)
        holder.getView<ImageView>(R.id.iv_artist).apply {
            load(item.avatarUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        }
    }

    override fun onBindViewHolder(
        holder: QuickViewHolder,
        position: Int,
        item: Subscription?,
        payloads: List<Any>
    ) {
        item ?: return
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position, item)
            return
        }
        val payload = payloads.first() as Int
        if (payload and DELETE != 0) {
            holder.getView<View>(R.id.btn_delete).isVisible = item.isDeleteVisible
            holder.getView<View>(R.id.cb_select).isVisible =
                item.name == fragment.viewModel.subscriptionBrand && !item.isDeleteVisible
        }
        if (payload and CHECK != 0) {
            holder.getView<CheckBox>(R.id.cb_select).apply {
                val selected = item.name == fragment.viewModel.subscriptionBrand
                isVisible = selected
                isChecked = selected
            }
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): QuickViewHolder {
        return QuickViewHolder(R.layout.item_h_subscription, parent).apply {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                val item = getItem(position) ?: return@setOnClickListener
                if (item.isDeleteVisible) return@setOnClickListener

                val cb = getView<CheckBox>(R.id.cb_select)
                if (cb.isChecked) {
                    cb.isVisible = false
                    cb.isChecked = false
                    fragment.viewModel.subscriptionBrand = null
                    fragment.setSearchText(null)
                    notifyItemChanged(position, CHECK)
                } else {
                    cb.isVisible = true
                    cb.isChecked = true
                    fragment.viewModel.subscriptionBrand = item.name
                    fragment.setSearchText(item.name, canTextChange = false)
                    notifyItemRangeChanged(0, itemCount, CHECK)
                }
            }

            itemView.setOnLongClickListener {
                val currentBrand = fragment.viewModel.subscriptionBrand
                val newList = if (getView<View>(R.id.btn_delete).isGone) {
                    items.map {
                        it.copy(
                            isDeleteVisible = true,
                            isCheckBoxVisible = false
                        )
                    }
                } else {
                    items.map {
                        it.copy(
                            isDeleteVisible = false,
                            isCheckBoxVisible = it.name == currentBrand
                        )
                    }
                }
                submitList(newList)
                true
            }

            getView<View>(R.id.btn_delete).setOnClickListener {
                val position = bindingAdapterPosition
                val item = getItem(position) ?: return@setOnClickListener

                fragment.requireContext().showAlertDialog {
                    setTitle(R.string.sure_to_delete)
                    setMessage(context.getString(R.string.sure_to_delete_s, item.name))
                    setPositiveButton(R.string.confirm) { _, _ ->
                      //  fragment.viewModel.subscription.deleteSubscription(item.artistId, position)
                    }
                    setNegativeButton(R.string.cancel, null)
                }
            }
        }
    }
}
