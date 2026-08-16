package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.compose.ui.text.input.KeyboardType
import androidx.recyclerview.widget.DiffUtil
import cn.jzvd.JZUtils
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.google.android.material.button.MaterialButton
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.TextInputField
import com.yenaly.yenaly_libs.utils.findActivityOrNull
import com.yenaly.yenaly_libs.utils.showShortToast

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/26 026 17:42
 */
class HKeyframeRvAdapter(
    private val videoCode: String,
    keyframe: HKeyframeEntity? = null,
) : BaseQuickAdapter<HKeyframeEntity.Keyframe, QuickViewHolder>(
    keyframe?.keyframes.orEmpty(),COMPARATOR
) {

    init {
        isStateViewEnable = true
    }

    /**
     * 是否是本地关键帧
     *
     * @return false if is shared, true otherwise.
     */
    var isLocal: Boolean = true

    var isShared: Boolean = false

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<HKeyframeEntity.Keyframe>() {
            override fun areItemsTheSame(
                oldItem: HKeyframeEntity.Keyframe,
                newItem: HKeyframeEntity.Keyframe,
            ) = oldItem.position == newItem.position

            override fun areContentsTheSame(
                oldItem: HKeyframeEntity.Keyframe,
                newItem: HKeyframeEntity.Keyframe,
            ) = oldItem == newItem
        }
    }

    override fun onBindViewHolder(
        holder: QuickViewHolder,
        position: Int,
        item: HKeyframeEntity.Keyframe?,
    ) {
        item ?: return
        holder.setText(R.id.tv_keyframe, JZUtils.stringForTime(item.position))
        holder.setText(R.id.tv_index, "#${holder.bindingAdapterPosition + 1}")

        holder.setGone(R.id.btn_delete, !isLocal)
        holder.setGone(R.id.btn_edit, !isLocal)

        if (!item.prompt.isNullOrBlank()) {
            holder.setGone(R.id.tv_prompt, false)
            holder.setText(R.id.tv_prompt, "➥ " + item.prompt)
        } else {
            holder.setGone(R.id.tv_prompt, true)
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): QuickViewHolder {
        return QuickViewHolder(R.layout.item_h_keyframe, parent).also { viewHolder ->
            if (isShared) return@also
            viewHolder.getView<MaterialButton>(R.id.btn_edit).apply {
                setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    val item = getItem(position)

                    GlobalDialogs.show(
                        GlobalDialogs.InputRequest(
                            title = context.getString(R.string.modify_h_keyframe),
                            fields = listOf(
                                TextInputField(
                                    label = context.getString(R.string.position_ms),
                                    initialValue = item.position.toString(),
                                    keyboardType = KeyboardType.Number,
                                ),
                                TextInputField(
                                    label = context.getString(R.string.prompt),
                                    initialValue = item.prompt.orEmpty(),
                                ),
                            ),
                            confirmText = context.getString(R.string.confirm),
                            dismissText = context.getString(R.string.cancel),
                            onConfirm = { values ->
                                val pos = values.getOrElse(0) { item.position.toString() }
                                    .toLongOrNull() ?: item.position
                                val prompt = values.getOrElse(1) { item.prompt.orEmpty() }
                                context.findActivityOrNull<MainActivity>()?.let { activity ->
                                    activity.viewModel.modifyHKeyframe(
                                        videoCode, item, HKeyframeEntity.Keyframe(
                                            position = pos,
                                            prompt = prompt
                                        )
                                    )
                                    showShortToast(R.string.modify_success)
                                }
                            },
                        )
                    )
                }
            }
            viewHolder.getView<MaterialButton>(R.id.btn_delete).apply {
                setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    val item = getItem(position)
                    GlobalDialogs.show(
                        GlobalDialogs.ConfirmRequest(
                            title = context.getString(R.string.sure_to_delete),
                            message = JZUtils.stringForTime(item.position),
                            confirmText = context.getString(R.string.confirm),
                            dismissText = context.getString(R.string.cancel),
                            onConfirm = {
                                context.findActivityOrNull<MainActivity>()?.let { activity ->
                                    activity.viewModel.removeHKeyframe(videoCode, item)
                                    showShortToast(R.string.delete_success)
                                }
                            },
                        )
                    )
                }
            }
        }
    }
}