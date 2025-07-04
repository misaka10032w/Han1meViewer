package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.lxj.xpopup.XPopup
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ItemHanimePreviewNewsV2Binding
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.activity.PreviewActivity
import com.yenaly.han1meviewer.ui.popup.CoilImageLoader
import com.yenaly.han1meviewer.ui.view.BlurTransformation

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/26 026 16:48
 */
class HanimePreviewNewsRvAdapter :
    BaseQuickAdapter<HanimePreview.PreviewInfo, DataBindingHolder<ItemHanimePreviewNewsV2Binding>>() {

    init {
        isStateViewEnable = true
    }

    private val imageLoader = CoilImageLoader()

    override fun onBindViewHolder(
        holder: DataBindingHolder<ItemHanimePreviewNewsV2Binding>,
        position: Int,
        item: HanimePreview.PreviewInfo?,
    ) {
        item ?: return
        holder.binding.ivCoverBig.load(item.coverUrl) {
            crossfade(true)
            transformations(BlurTransformation(context))
        }
        holder.binding.tvTitle.text = item.title
        holder.binding.tvIntroduction.text = item.introduction
        holder.binding.tvBrand.text = item.brand
        holder.binding.tvReleaseDate.text = item.releaseDate
        holder.binding.tvVideoTitle.text = item.videoTitle

        holder.binding.tags.tags = item.tags

        holder.binding.rvPreview.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = PreviewPicRvAdapter(item)
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): DataBindingHolder<ItemHanimePreviewNewsV2Binding> {
        return DataBindingHolder(
            ItemHanimePreviewNewsV2Binding.inflate(
                LayoutInflater.from(context), parent, false
            )
        ).also { viewHolder ->
            viewHolder.binding.tags.lifecycle = (context as? PreviewActivity)?.lifecycle
            viewHolder.binding.tags.isCollapsedEnabled = true
            viewHolder.itemView.apply {
                setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    val item = getItem(position) ?: return@setOnClickListener
                    if (context is PreviewActivity) {
                        item.videoCode?.let { it1 -> context.startMainActivityForVideo(it1) }
                    }
                }
            }
        }
    }

    private fun Context.startMainActivityForVideo(videoCode: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("startVideoCode", videoCode)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private inner class PreviewPicRvAdapter(private val item: HanimePreview.PreviewInfo) :
        BaseQuickAdapter<String, QuickViewHolder>(item.relatedPicsUrl) {
        override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: String?) {
            holder.getView<ImageView>(R.id.iv_preview_news_pic).load(item) {
                crossfade(true)
            }
        }

        override fun onCreateViewHolder(
            context: Context,
            parent: ViewGroup,
            viewType: Int,
        ): QuickViewHolder {
            return QuickViewHolder(
                R.layout.item_hanime_preview_news_pic, parent
            ).also { viewHolder ->
                viewHolder.itemView.setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    XPopup.Builder(context).asImageViewer(
                        it as? ImageView, position, item.relatedPicsUrl, { popupView, pos ->
                            popupView.updateSrcView(recyclerView.getChildAt(pos) as? ImageView)
                        }, imageLoader
                    ).show()
                }
            }
        }
    }
}