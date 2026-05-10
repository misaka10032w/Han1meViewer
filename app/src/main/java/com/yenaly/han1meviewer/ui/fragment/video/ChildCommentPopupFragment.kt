package com.yenaly.han1meviewer.ui.fragment.video

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yenaly.han1meviewer.COMMENT_ID
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.databinding.PopUpFragmentChildCommentBinding
import com.yenaly.han1meviewer.logic.model.VideoCommentArgs
import com.yenaly.han1meviewer.ui.screen.video.ChildCommentScreen
import com.yenaly.han1meviewer.ui.screen.video.CommentMessage
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.yenaly_libs.base.YenalyBottomSheetDialogFragment
import com.yenaly.yenaly_libs.utils.arguments
import kotlinx.coroutines.flow.map

class ChildCommentPopupFragment :
    YenalyBottomSheetDialogFragment<PopUpFragmentChildCommentBinding>() {

    val commentId by arguments<String>(COMMENT_ID)

    val viewModel: CommentViewModel by lazy {
        var ancestor: Fragment? = parentFragment
        while (ancestor != null && ancestor !is VideoFragment) {
            ancestor = ancestor.parentFragment
        }

        if (ancestor != null) {
            ViewModelProvider(ancestor)[CommentViewModel::class.java]
        } else {
            ViewModelProvider(requireActivity())[CommentViewModel::class.java]
        }
    }

    override fun getViewBinding(layoutInflater: LayoutInflater) =
        PopUpFragmentChildCommentBinding.inflate(layoutInflater)

    override fun initData(savedInstanceState: Bundle?, dialog: Dialog) {
        val currentCommentId = commentId ?: run {
            dialog.dismiss()
            return
        }

        binding.composeContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeContent.setContent {
            HanimeTheme {
                ChildCommentScreen(
                    commentsFlow = viewModel.videoReplyFlow,
                    commentStateFlow = viewModel.videoReplyStateFlow,
                    reportMessageFlow = viewModel.reportMessage.map { message ->
                        val text = if (message.args.isNotEmpty()) {
                            getString(message.resId, *message.args.toTypedArray())
                        } else {
                            getString(message.resId)
                        }
                        CommentMessage(text)
                    },
                    postReplyStateFlow = viewModel.postReplyFlow,
                    commentLikeStateFlow = viewModel.commentLikeFlow,
                    reportReasons = viewModel.reportReason,
                    isAlreadyLogin = Preferences.isAlreadyLogin,
                    onRefresh = { viewModel.getCommentReply(currentCommentId) },
                    onReply = { _, text ->
                        viewModel.postReply(currentCommentId, text)
                    },
                    onReport = { comment, reason ->
                        viewModel.reportComment(
                            reason.reasonKey ?: reason.value,
                            viewModel.currentUserId,
                            "${Preferences.baseUrl}watch?v=${viewModel.code}",
                            comment.reportableType,
                            comment.reportableId,
                        )
                    },
                    onThumbUp = { comment ->
                        viewModel.likeChildComment(
                            true,
                            0,
                            comment,
                            likeCommentStatus = comment.post.likeCommentStatus,
                        )
                    },
                    onThumbDown = { comment ->
                        viewModel.likeChildComment(
                            false,
                            0,
                            comment,
                            unlikeCommentStatus = comment.post.unlikeCommentStatus,
                        )
                    },
                    onCommentLikeSuccess = viewModel::handleCommentLike,
                )
            }
        }

        viewModel.getCommentReply(currentCommentId)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = resources.getDimensionPixelSize(com.yenaly.han1meviewer.R.dimen.bottom_sheet_min_height)
            behavior.isFitToContents = true
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            it.minimumHeight = resources.getDimensionPixelSize(com.yenaly.han1meviewer.R.dimen.bottom_sheet_min_height)
        }
    }

    override fun setStyle() { }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearVideoReplyList()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            @Suppress("DEPRECATION")
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isNavigationBarContrastEnforced = false
            }
        }
        return dialog
    }
}
