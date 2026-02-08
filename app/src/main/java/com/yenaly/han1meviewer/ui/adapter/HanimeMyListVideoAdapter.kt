package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.itxca.spannablex.spannable
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VideoCoverSize
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.yenaly_libs.utils.dp

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/26 026 16:38
 */
class HanimeMyListVideoAdapter(val onItemClick: (HanimeInfo) -> Unit) : BaseQuickAdapter<HanimeInfo, QuickViewHolder>(COMPARATOR) {

    init {
        isStateViewEnable = true
    }

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<HanimeInfo>() {
            override fun areItemsTheSame(
                oldItem: HanimeInfo,
                newItem: HanimeInfo,
            ): Boolean {
                return oldItem.videoCode == newItem.videoCode
            }

            override fun areContentsTheSame(
                oldItem: HanimeInfo,
                newItem: HanimeInfo,
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: HanimeInfo?) {
        item ?: return
        holder.getView<TextView>(R.id.title).text = item.title
        holder.getView<ImageView>(R.id.cover).load(item.coverUrl) {
            crossfade(true)
            placeholder(R.drawable.akarin)
        }
        holder.getView<TextView>(R.id.is_playing).isVisible = item.isPlaying
        holder.getView<TextView>(R.id.duration).text = item.duration
        holder.getView<TextView>(R.id.time).apply {
            if (item.uploadTime != null) {
                holder.getView<View>(R.id.icon_time).isGone = false
                text = item.uploadTime
            } else {
                holder.getView<View>(R.id.icon_time).isGone = true
            }
        }
        holder.getView<TextView>(R.id.views).apply {
            if (item.views != null) {
                holder.getView<View>(R.id.icon_views).isGone = false
                text = item.views
            } else {
                holder.getView<View>(R.id.icon_views).isGone = true
            }
        }
        holder.getView<TextView>(R.id.artist).apply {
            if (item.genre == null && item.currentArtist == null) {
                isGone = true
                return@apply
            }
            isGone = false
            text = spannable {
                item.genre.span {
                    margin(4.dp)
                    when (item.genre) {
                        "3D" -> color(Color.rgb(245, 171, 53))
                        "COS" -> color(Color.rgb(165, 55, 253))
                        "同人" -> color(Color.rgb(241, 130, 141))
                        else -> color(Color.RED)
                    }
                }
                item.currentArtist.text()
            }
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): QuickViewHolder {
        return QuickViewHolder(R.layout.item_hanime_video, parent).also { viewHolder ->
            viewHolder.getView<View>(R.id.frame).layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            viewHolder.getView<ImageView>(R.id.cover).scaleType = ImageView.ScaleType.CENTER_CROP
            viewHolder.itemView.apply {
                setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    val item = getItem(position)
                    onItemClick(item)
                }
                // setOnLongClickListener 由各自的 Fragment 实现
            }
            with(VideoCoverSize.Simplified) {
                viewHolder.getView<ViewGroup>(R.id.cover_wrapper).resizeForVideoCover()
            }
        }
    }
}