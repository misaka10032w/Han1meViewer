package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VideoCoverSize
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.yenaly_libs.utils.requireActivity

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/26 026 16:38
 */
class HanimeMyListVideoAdapter : BaseQuickAdapter<HanimeInfo, QuickViewHolder>(COMPARATOR) {

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
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): QuickViewHolder {
        return QuickViewHolder(R.layout.item_hanime_video_simplified, parent).also { viewHolder ->
            viewHolder.getView<View>(R.id.frame).layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            viewHolder.getView<ImageView>(R.id.cover).scaleType = ImageView.ScaleType.CENTER_CROP
            viewHolder.itemView.apply {
                setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    val item = getItem(position) ?: return@setOnClickListener
                    val videoCode = item.videoCode
                    //context.activity?.startActivity<VideoActivity>(VIDEO_CODE to videoCode)
                    context.startVideoActivity(videoCode)
                }
                // setOnLongClickListener 由各自的 Fragment 实现
            }
            with(VideoCoverSize.Simplified) {
                viewHolder.getView<ViewGroup>(R.id.cover_wrapper).resizeForVideoCover()
            }
        }
    }
    private fun Context.startVideoActivity(videoCode: String) {
        (requireActivity() as? MainActivity)?.showVideoDetailFragment(videoCode)
    }
}