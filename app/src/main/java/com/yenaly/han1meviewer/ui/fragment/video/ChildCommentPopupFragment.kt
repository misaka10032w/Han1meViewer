package com.yenaly.han1meviewer.ui.fragment.video

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.util.Log

import androidx.annotation.OptIn
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yenaly.han1meviewer.COMMENT_ID
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.PopUpFragmentChildCommentBinding
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.adapter.VideoCommentRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.util.setGravity
import com.yenaly.yenaly_libs.base.YenalyBottomSheetDialogFragment
import com.yenaly.yenaly_libs.utils.appScreenHeight
import com.yenaly.yenaly_libs.utils.unsafeLazy
import com.yenaly.yenaly_libs.utils.arguments
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showShortToast

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/21 021 22:58
 */
class ChildCommentPopupFragment :
    YenalyBottomSheetDialogFragment<PopUpFragmentChildCommentBinding>() {

    val commentId by arguments<String>(COMMENT_ID)
    val viewModel by viewModels<CommentViewModel>()
    private val replyAdapter by unsafeLazy {
        VideoCommentRvAdapter(this@ChildCommentPopupFragment)
    }

    override fun getViewBinding(layoutInflater: LayoutInflater) =
        PopUpFragmentChildCommentBinding.inflate(layoutInflater)

    override fun initData(savedInstanceState: Bundle?, dialog: Dialog) {
        if (commentId == null) dialog.dismiss()

        binding.root.minimumHeight = appScreenHeight / 2
        binding.rvReply.layoutManager = LinearLayoutManager(context)
        binding.rvReply.adapter = replyAdapter
        binding.pbLoading.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        viewModel.getCommentReply(commentId!!)

        lifecycleScope.launch {
            viewModel.videoReplyStateFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(getString(R.string.load_reply_failed))
                        dialog.dismiss()
                        binding.pbLoading.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }

                    is WebsiteState.Loading -> {
                        binding.pbLoading.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }

                    is WebsiteState.Success -> {
                        binding.pbLoading.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.videoReplyFlow.collectLatest { list ->
                replyAdapter.submitList(list)
                attachRedDotCount(list.size)

                if (list.isNullOrEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.postReplyFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(getString(R.string.send_failed))
                    }

                    is WebsiteState.Loading -> {
                        showShortToast(getString(R.string.sending_reply))
                    }

                    is WebsiteState.Success -> {
                        showShortToast(getString(R.string.send_success))
                        viewModel.getCommentReply(commentId!!)
                        replyAdapter.replyPopup?.dismiss()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.commentLikeFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        val errorMessage = state.throwable.message ?: getString(R.string.comment_not_found)
                        showShortToast(errorMessage)
                    }
                    is WebsiteState.Loading -> Unit
                    is WebsiteState.Success -> {
                        viewModel.handleCommentLike(state.info)
                        replyAdapter.notifyItemChanged(state.info.commentPosition, 0)
                    }
                }
            }
        }
    }

    // 核心逻辑：接受 orientation 参数
    private fun adjustBottomSheetBehavior(orientation: Int) {
        val behavior = (dialog as? BottomSheetDialog)?.behavior

        // <-- 在这里添加日志，看看传入的 orientation 是什么！
        Log.d("BottomSheetDebug", "adjustBottomSheetBehavior - 传入的 orientation: $orientation")

        behavior?.apply {
                 state = BottomSheetBehavior.STATE_EXPANDED
               //  state = BottomSheetBehavior.STATE_COLLAPSED 竖屏半屏
                setExpandedOffset(0)
                dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        }
        dialog?.window?.decorView?.requestLayout()
    }


    // 重写 onStart() 方法
    override fun onStart() {
        super.onStart()
        // <-- 在 onStart 中添加日志，看看 resources.configuration.orientation 是什么！
        Log.d("BottomSheetDebug", "onStart - resources.configuration.orientation: ${resources.configuration.orientation}")
        adjustBottomSheetBehavior(resources.configuration.orientation)
    }



    @OptIn(ExperimentalBadgeUtils::class)
    private fun attachRedDotCount(count: Int) {
        val badgeDrawable = BadgeDrawable.create(requireContext())
        badgeDrawable.isVisible = count > 0
        badgeDrawable.number = count
        BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.tvChildComment)
        badgeDrawable.setGravity(binding.tvChildComment, Gravity.END, 8.dp)
    }
}