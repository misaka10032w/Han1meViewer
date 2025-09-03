package com.yenaly.han1meviewer.ui.fragment.video

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yenaly.han1meviewer.COMMENT_ID
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.PopUpFragmentChildCommentBinding
import com.yenaly.han1meviewer.logic.model.ReportReason
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.adapter.VideoCommentRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.util.setGravity
import com.yenaly.yenaly_libs.base.YenalyBottomSheetDialogFragment
import com.yenaly.yenaly_libs.utils.arguments
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.unsafeLazy
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
    //向上查找搞到VideoFragment初始化好的CommentViewModel
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
    private var reportReason: List<ReportReason>? = null
    private val replyAdapter by unsafeLazy {
        VideoCommentRvAdapter(this){ item ->
            if (reportReason == null) {
                reportReason = viewModel.reportReason
            }
            var checkedIndex = -1
            val reportDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.whats_wrong_with_him))
                .setIcon(R.drawable.ic_baseline_report_24)
                .setSingleChoiceItems(
                    reportReason?.map { it.value }?.toTypedArray(),
                    checkedIndex
                ) { _, which ->
                    checkedIndex = which
                }
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(getString(R.string.submit)) { _, _ ->
                    if (checkedIndex != -1) {
                        val chosen = reportReason!![checkedIndex]
                        val reasonKey = chosen.reasonKey ?: chosen.value
                        viewModel.reportComment(
                            reasonKey,
                            viewModel.currentUserId,
                            "${ Preferences.baseUrl }watch?v=${viewModel.code}",
                            item.reportableType,
                            item.reportableId
                        )
                        Log.i("ReportComment", "viewModel.reportComment: \n" +
                                "chosen: $reasonKey\n" +
                                "currentUserId: ${viewModel.currentUserId}\n" +
                                "redirectUrl: ${ Preferences.baseUrl }watch?v=${viewModel.code}\n" +
                                "reportableType: ${item.reportableType}\n" +
                                "reportableId: ${item.reportableId}")
                    } else {
                        showShortToast(getString(R.string.report_reason_hint))
                    }
                }
                .create()
            reportDialog.show()
        }
    }

    override fun getViewBinding(layoutInflater: LayoutInflater) =
        PopUpFragmentChildCommentBinding.inflate(layoutInflater)

    override fun initData(savedInstanceState: Bundle?, dialog: Dialog) {
        if (commentId == null) dialog.dismiss()

//        binding.root.minimumHeight = appScreenHeight / 2
        binding.rvReply.layoutManager = LinearLayoutManager(context)
        binding.rvReply.adapter = replyAdapter
        binding.rvReplyContainer.layoutParams.height = getWindowHeight() / 2
        viewModel.getCommentReply(commentId!!)

        lifecycleScope.launch {
            viewModel.videoReplyStateFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.load_reply_failed)
                        dialog.dismiss()
                    }

                    is WebsiteState.Loading -> Unit

                    is WebsiteState.Success -> {
                        binding.rvReplyContainer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.videoReplyFlow.collectLatest { list ->
                replyAdapter.submitList(list)
                attachRedDotCount(list.size)
            }
        }

        lifecycleScope.launch {
            viewModel.postReplyFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.send_failed)
                        replyAdapter.replyPopup?.enableSendButton()
                    }

                    is WebsiteState.Loading -> {
                        showShortToast(R.string.sending_reply)
                    }

                    is WebsiteState.Success -> {
                        showShortToast(R.string.send_success)
                        replyAdapter.replyPopup?.enableSendButton()
                        viewModel.getCommentReply(commentId!!)
                        replyAdapter.replyPopup?.dismiss()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.commentLikeFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> showShortToast(state.throwable.message)
                    is WebsiteState.Loading -> Unit
                    is WebsiteState.Success -> {
                        viewModel.handleCommentLike(state.info)
                        replyAdapter.notifyItemChanged(state.info.commentPosition, 0)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.reportMessage.collect { msg ->
                val text = if (msg.args.isNotEmpty()) {
                    getString(msg.resId, *msg.args.toTypedArray())
                } else {
                    getString(msg.resId)
                }
                val snackBar = Snackbar.make(
                    dialog.findViewById(android.R.id.content),
                    text,
                    Snackbar.LENGTH_LONG
                )
                val textView = snackBar.view.findViewById<TextView>(
                    com.google.android.material.R.id.snackbar_text
                )
                textView.maxLines = 5
                textView.ellipsize = null
                textView.isSingleLine = false
                snackBar.show()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val screenHeight = getWindowHeight()
            sheet.minimumHeight = screenHeight / 2
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.isFitToContents = false
            behavior.peekHeight = screenHeight / 2
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearVideoReplyList()
        replyAdapter.submitList(emptyList())
    }
    private fun getWindowHeight(): Int {
        val window = dialog?.window ?: return resources.displayMetrics.heightPixels
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            window.windowManager.defaultDisplay.height
        }
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
    @OptIn(ExperimentalBadgeUtils::class)
    private fun attachRedDotCount(count: Int) {
        val badgeDrawable = BadgeDrawable.create(requireContext())
        badgeDrawable.isVisible = count > 0
        badgeDrawable.number = count
        BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.tvChildComment)
        badgeDrawable.setGravity(binding.tvChildComment, Gravity.END, 8.dp)
    }
}