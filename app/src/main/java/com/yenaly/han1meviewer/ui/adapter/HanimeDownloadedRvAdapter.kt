package com.yenaly.han1meviewer.ui.adapter

import android.content.ContentResolver
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yenaly.han1meviewer.FROM_DOWNLOAD
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.databinding.ItemHanimeDownloadedBinding
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.logic.model.DownloadedNode
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.fragment.home.download.DownloadedFragment
import com.yenaly.han1meviewer.util.HImageMeower.loadUnhappily
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.yenaly_libs.utils.dpF
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.showLongToast
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * @project Han1meViewer
 * @author Yenaly Liew - 创建 (2023/11/26)
 * 初始版本
 * @author Misaka10032w - 更新 (2025/11/27)
 * 实现分组展示和展开/折叠功能
 * 实现分组移动、重命名等
 */

class HanimeDownloadedRvAdapter(
    private val fragment: DownloadedFragment,
    private val onHeaderClick: (DownloadHeaderNode) -> Unit,
    private val onLongClickVideoItem: (VideoWithCategories) -> Unit,
    private val onLongClickGroupHeader: (DownloadHeaderNode) -> Unit
) : ListAdapter<DownloadedNode, RecyclerView.ViewHolder>(COMPARATOR) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        val COMPARATOR = object : DiffUtil.ItemCallback<DownloadedNode>() {
            override fun areItemsTheSame(
                oldItem: DownloadedNode,
                newItem: DownloadedNode,
            ): Boolean {
                return when {
                    oldItem is DownloadHeaderNode && newItem is DownloadHeaderNode ->
                        oldItem.groupKey == newItem.groupKey

                    oldItem is DownloadItemNode && newItem is DownloadItemNode ->
                        oldItem.data.video.id == newItem.data.video.id

                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: DownloadedNode,
                newItem: DownloadedNode,
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class DownloadItemViewHolder(
        val binding: ItemHanimeDownloadedBinding
    ) : RecyclerView.ViewHolder(binding.root)

    inner class DownloadHeaderViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        val tvGroupTitle: TextView = view.findViewById(R.id.tvGroupTitle)
        val ivArrow: ImageView = view.findViewById(R.id.ivArrow)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DownloadHeaderNode -> TYPE_HEADER
            is DownloadItemNode -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(
                    R.layout.item_downloaded_group_header,
                    parent,
                    false
                )
                DownloadHeaderViewHolder(view).also { viewHolder ->
                    setupHeaderClickListeners(viewHolder)
                }
            }

            TYPE_ITEM -> {
                DownloadItemViewHolder(
                    ItemHanimeDownloadedBinding.inflate(inflater, parent, false)
                ).also { viewHolder ->
                    setupItemClickListeners(context, viewHolder)
                }
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        when (holder) {
            is DownloadHeaderViewHolder -> {
                val headerNode = item as DownloadHeaderNode
                bindHeader(holder, headerNode)
            }

            is DownloadItemViewHolder -> {
                val itemNode = item as DownloadItemNode
                bindItem(context, holder, itemNode)
            }
        }
    }

    private fun bindHeader(holder: DownloadHeaderViewHolder, node: DownloadHeaderNode) {
        holder.tvGroupTitle.text = applicationContext.getString(
            R.string.group_title_format,
            node.groupKey,
            node.originalVideos.size
        )
        holder.ivArrow.rotation = if (node.isExpanded) 0f else -180f
    }

    @OptIn(ExperimentalTime::class)
    private fun bindItem(context: Context, holder: DownloadItemViewHolder, node: DownloadItemNode) {
        val item = node.data
        val binding = holder.binding

        binding.tvTitle.text = item.video.title
        binding.tvVideoCode.text = item.video.videoCode
        binding.ivCover.loadUnhappily(item.video.coverUri, item.video.coverUrl)

        holder.itemView.post {
            if (holder.itemView.height == binding.vCoverBg.height) return@post
            binding.vCoverBg.updateLayoutParams<FrameLayout.LayoutParams> {
                height = holder.itemView.height
            }
            binding.ivCoverBg.updateLayoutParams<FrameLayout.LayoutParams> {
                height = holder.itemView.height
            }
        }

        // 封面背景模糊效果
        binding.ivCoverBg.apply {
            loadUnhappily(item.video.coverUri, item.video.coverUrl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(
                    RenderEffect.createBlurEffect(
                        8.dpF, 8.dpF,
                        Shader.TileMode.CLAMP
                    )
                )
            }
        }

        binding.tvAddedTime.text =
           Instant.fromEpochMilliseconds(item.video.addDate).toLocalDateTime(
                TimeZone.currentSystemDefault()
            ).format(LOCAL_DATE_TIME_FORMAT)

        val uri = item.video.videoUri.toUri()
        val realSize = try {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                }
                "file" -> File(uri.path ?: "").length()
                else -> 0L
            }
        } catch (e: Exception) {
            showLongToast(context.getString(R.string.some_videos_moved_or_deleted))
            e.printStackTrace()
            0L
        }

        binding.tvSize.text = if (realSize <= 0L) "???" else realSize.formatFileSizeV2()
        binding.tvQuality.text = item.video.quality
    }

    private fun setupHeaderClickListeners(viewHolder: DownloadHeaderViewHolder) {
        // 短按标题
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val node = getItem(position) as? DownloadHeaderNode
                node?.let(onHeaderClick)
            }
        }

        // 长按标题
        viewHolder.itemView.setOnLongClickListener {
            val position = viewHolder.bindingAdapterPosition
            val headerNode = getItem(position) as? DownloadHeaderNode
            if (headerNode != null) {
                onLongClickGroupHeader(headerNode)
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }
    }

    private fun setupItemClickListeners(context: Context, viewHolder: DownloadItemViewHolder) {
        // 短按视频单元
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            val itemNode = getItem(position) as? DownloadItemNode ?: return@setOnClickListener
            context.startVideoFragment(itemNode.data.video.videoCode)
        }
        // 长按视频单元
        viewHolder.itemView.setOnLongClickListener {
            val position = viewHolder.bindingAdapterPosition
            val itemNode = getItem(position) as? DownloadItemNode
            val item = itemNode?.data
            if (item != null) {
                onLongClickVideoItem(item)
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }

        // 删除视频按钮
        viewHolder.binding.btnDelete.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            val itemNode = getItem(position) as? DownloadItemNode
            val item = itemNode?.data
            item?.let {
                context.showAlertDialog {
                    setTitle(R.string.sure_to_delete)
                    setMessage(context.getString(R.string.prepare_to_delete_s, it.video.title))
                    setPositiveButton(R.string.confirm) { _, _ ->
                        SafFileManager.deleteDownloadVideoFolder(
                            context,
                            it.video.videoCode
                        )
                        fragment.viewModel.deleteDownloadHanimeBy(
                            it.video.videoCode,
                            it.video.quality
                        )
                    }
                    setNegativeButton(R.string.cancel, null)
                }
            }
        }

        // 本地播放按钮
        viewHolder.binding.btnLocalPlayback.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            val itemNode = getItem(position) as? DownloadItemNode ?: return@setOnClickListener
            val item = itemNode.data
            val args = bundleOf(
                VIDEO_CODE to item.video.videoCode,
                FROM_DOWNLOAD to true
            )
            (context as? MainActivity)?.navController?.navigate(
                R.id.videoFragment,
                args
            )
        }

        // 外部播放器播放按钮
        viewHolder.binding.btnExtPlayer.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            val itemNode = getItem(position) as? DownloadItemNode ?: return@setOnClickListener
            val item = itemNode.data
            context.openDownloadedHanimeVideoLocally(item.video.videoUri, onFileNotFound = {
                context.showAlertDialog {
                    setTitle(R.string.video_not_exist)
                    setMessage(R.string.video_deleted_sure_to_delete_item)
                    setPositiveButton(R.string.delete) { _, _ ->
                        fragment.viewModel.deleteDownloadHanimeBy(
                            item.video.videoCode,
                            item.video.quality
                        )
                    }
                    setNegativeButton(R.string.cancel, null)
                }
            })
        }
    }

    private fun Context.startVideoFragment(videoCode: String) {
        (fragment.requireActivity() as? MainActivity)?.showVideoDetailFragment(videoCode)
    }
}