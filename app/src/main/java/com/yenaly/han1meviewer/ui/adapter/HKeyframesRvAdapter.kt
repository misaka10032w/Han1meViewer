package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import cn.jzvd.JZUtils
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.google.android.material.button.MaterialButton
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.util.showAlertDialog
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

                    val view = View.inflate(context, R.layout.dialog_modify_h_keyframe, null)
                    val etPrompt = view.findViewById<TextView>(R.id.et_prompt)
                    val etPosition = view.findViewById<TextView>(R.id.et_position)
                    etPrompt.text = item.prompt
                    etPosition.text = item.position.toString()

                    context.showAlertDialog {
                        setTitle(R.string.modify_h_keyframe)
                        setView(view)
                        setPositiveButton(R.string.confirm) { _, _ ->
                            val prompt = etPrompt.text.toString()
                            val pos = etPosition.text.toString().toLong()
                            when (context) {
                                is MainActivity -> {
                                    context.viewModel.modifyHKeyframe(
                                        videoCode, item, HKeyframeEntity.Keyframe(
                                            position = pos,
                                            prompt = prompt
                                        )
                                    )
                                     showShortToast(R.string.modify_success)
                                }
                            }
                        }
                        setNegativeButton(R.string.cancel, null)
                    }
                }
            }
            viewHolder.getView<MaterialButton>(R.id.btn_delete).apply {
                setOnClickListener {
                    val position = viewHolder.bindingAdapterPosition
                    val item = getItem(position)
                    it.context.showAlertDialog {
                        setTitle(R.string.sure_to_delete)
                        setMessage(JZUtils.stringForTime(item.position))
                        setPositiveButton(R.string.confirm) { _, _ ->
                            when (context) {
                                is MainActivity -> {
                                    context.viewModel.removeHKeyframe(videoCode, item)
                                     showShortToast(R.string.delete_success)
                                }
                            }
                        }
                        setNegativeButton(R.string.cancel, null)
                    }
                }
            }
        }
    }
}