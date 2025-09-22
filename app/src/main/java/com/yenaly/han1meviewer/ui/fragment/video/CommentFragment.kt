package com.yenaly.han1meviewer.ui.fragment.video

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.yenaly.han1meviewer.COMMENT_TYPE
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_COMMENT_PREFIX
import com.yenaly.han1meviewer.databinding.FragmentCommentBinding
import com.yenaly.han1meviewer.logic.model.ReportReason
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.adapter.VideoCommentRvAdapter
import com.yenaly.han1meviewer.ui.popup.ReplyPopup
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.parseTimeStrToMinutes
import com.yenaly.han1meviewer.util.safeSortedBy
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.arguments
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/18 018 21:09
 */
class CommentFragment : YenalyFragment<FragmentCommentBinding>(), StateLayoutMixin {
    private var commentType: String? = null

    val viewModel: CommentViewModel by lazy {
        val parent = parentFragment
        if (parent == null || commentType == PREVIEW_COMMENT_PREFIX) {
            ViewModelProvider(requireActivity())[CommentViewModel::class.java]
        } else {
            ViewModelProvider(parent)[CommentViewModel::class.java]
        }
    }
    private var reportReason:List<ReportReason>? = null
    private val commentTypePrefix by arguments(COMMENT_TYPE, VIDEO_COMMENT_PREFIX)
    private lateinit var sortPopup: PopupMenu
    enum class SortType {
        LATEST, EARLIEST, MOST_REPLY, MOST_LIKES, MOST_DISLIKES
    }
    private val commentAdapter by unsafeLazy {
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
                        val reasonKey = chosen.reasonKey ?:chosen.value
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
    private val replyPopup by unsafeLazy {
        ReplyPopup(requireContext()).also { it.hint = getString(R.string.comment) }
    }

    /**
     * 是否已经预加载了预览评论
     */
    private var isPreviewCommentPrefetched = false

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommentBinding {
        return FragmentCommentBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commentType = arguments?.getString(COMMENT_TYPE)
        Log.i("commentType",commentType.toString())
    }

    override fun initData(savedInstanceState: Bundle?) {
        binding.state.init {
            onEmpty {
                findViewById<TextView>(R.id.tv_empty).setText(R.string.comment_not_found)
            }
        }
        checkBadGuy(requireContext(),R.raw.akarin)
        binding.rvComment.layoutManager = LinearLayoutManager(context)
        binding.rvComment.adapter = commentAdapter
        binding.rvComment.clipToPadding = false
        sortPopup = PopupMenu(requireContext(), binding.btnSort)
        sortPopup.menuInflater.inflate(R.menu.menu_comment_sort, sortPopup.menu)
        if (commentType == PREVIEW_COMMENT_PREFIX) {
            val comments = PreviewCommentPrefetcher.here(viewModel).commentFlow.value
            if (comments.isNotEmpty()) {
                isPreviewCommentPrefetched = true
                binding.btnSort.isVisible = comments.size >= 3
                binding.btnSort.text = getSortText(viewModel.currentSortType)
                binding.btnSort.setOnClickListener { sortPopup.show() }
                sortPopup.setOnMenuItemClickListener { item ->
                    val sortType = when (item.itemId) {
                        R.id.sort_latest -> SortType.LATEST
                        R.id.sort_earliest -> SortType.EARLIEST
                        R.id.sort_most_reply -> SortType.MOST_REPLY
                        R.id.sort_most_likes -> SortType.MOST_LIKES
                        R.id.sort_most_dislikes -> SortType.MOST_DISLIKES
                        else -> viewModel.currentSortType
                    }
                    binding.btnSort.text = getSortText(viewModel.currentSortType)
                    viewModel.setSortType(sortType)
                    commentAdapter.submitList(sortList(comments, viewModel.currentSortType)) {
                        binding.rvComment.scrollToPosition(0)
                    }
                    true
                }
                commentAdapter.submitList(sortList(comments, viewModel.currentSortType))
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.rvComment) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBar.bottom)
            WindowInsetsCompat.CONSUMED
        }
        binding.srlComment.setOnRefreshListener {
            viewModel.getComment(commentTypePrefix, viewModel.code)
        }
        binding.header.apply {
            val accentColor = MaterialColors
                .getColor(this,androidx.appcompat.R.attr.colorPrimary)
            val backgroundColor = MaterialColors
                .getColor(this, com.google.android.material.R.attr.colorOnPrimary)

            setColorSchemeColors(accentColor)
            setProgressBackgroundColorSchemeColor(backgroundColor)
        }
        binding.btnComment.isVisible = isAlreadyLogin
        replyPopup.setOnSendListener {
            viewModel.currentUserId?.let { id ->
                viewModel.postComment(
                    id,
                    viewModel.code, commentTypePrefix, replyPopup.comment
                )
            } ?: showShortToast(R.string.there_is_a_small_issue)
        }
        binding.btnComment.setOnClickListener {
            XPopup.Builder(context)
                .autoOpenSoftInput(true)
                .moveUpToKeyboard(false)
                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow(popupView: BasePopupView?) {
                        binding.btnComment.hide()
                    }

                    override fun onDismiss(popupView: BasePopupView?) {
                        binding.btnComment.show()
                    }
                }).asCustom(replyPopup).show()
        }
        binding.rvComment.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            private var isVisible = true
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 5 && isVisible) {
                    binding.btnSort.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { binding.btnSort.visibility = GONE }
                        .start()
                    isVisible = false
                } else if (dy < -5 && !isVisible) {
                    binding.btnSort.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .withEndAction { binding.btnSort.visibility = VISIBLE }
                        .start()
                    isVisible = true
                }
            }
        })
    }

    override fun bindDataObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.videoCommentStateFlow.collect { state ->
                    binding.rvComment.isGone = state is WebsiteState.Error
                    when (state) {
                        is WebsiteState.Error -> {
                            binding.srlComment.finishRefresh()
                            binding.state.showError(state.throwable)
                        }

                        is WebsiteState.Loading -> {
                            if (!isPreviewCommentPrefetched) {
                                binding.srlComment.autoRefresh()
                            }
                        }

                        is WebsiteState.Success -> {
                            binding.srlComment.finishRefresh()
                            viewModel.currentUserId = state.info.currentUserId
                            showRedDotCount(state.info.videoComment.size)
                            binding.rvComment.isGone = state.info.videoComment.isEmpty()
                            if (state.info.videoComment.isEmpty()) {
                                binding.state.showEmpty()
                            } else {
                                binding.state.showContent()
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.videoCommentFlow.collectLatest { list ->
                    if (!isPreviewCommentPrefetched) {
                        binding.btnSort.isVisible = list.size >= 3
                        binding.btnSort.text = getSortText(viewModel.currentSortType)
                        binding.btnSort.setOnClickListener { sortPopup.show() }
                        sortPopup.setOnMenuItemClickListener { item ->
                            val sortType = when (item.itemId) {
                                R.id.sort_latest -> SortType.LATEST
                                R.id.sort_earliest -> SortType.EARLIEST
                                R.id.sort_most_reply -> SortType.MOST_REPLY
                                R.id.sort_most_likes -> SortType.MOST_LIKES
                                R.id.sort_most_dislikes -> SortType.MOST_DISLIKES
                                else -> viewModel.currentSortType
                            }
                            viewModel.setSortType(sortType)
                            binding.btnSort.text = getSortText(viewModel.currentSortType)
                            commentAdapter.submitList(sortList(list, viewModel.currentSortType)) {
                                binding.rvComment.scrollToPosition(0)
                            }
                            true
                        }
                        if (commentType == PREVIEW_COMMENT_PREFIX) {
                            PreviewCommentPrefetcher.here(viewModel).update(list)
                        }
                    }
                    commentAdapter.submitList(sortList(list, viewModel.currentSortType))
                }
            }
        }

        lifecycleScope.launch {
            viewModel.postCommentFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.send_failed)
                    }

                    is WebsiteState.Loading -> {
                        showShortToast(R.string.sending_comment)
                    }

                    is WebsiteState.Success -> {
                        showShortToast(R.string.send_success)
                        viewModel.getComment(commentTypePrefix, viewModel.code)
                        replyPopup.dismiss()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.postReplyFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> {
                        showShortToast(R.string.send_failed)
                        commentAdapter.replyPopup?.enableSendButton()
                    }

                    is WebsiteState.Loading -> {
                        showShortToast(R.string.sending_reply)
                    }

                    is WebsiteState.Success -> {
                        showShortToast(R.string.send_success)
                        commentAdapter.replyPopup?.enableSendButton()
                        viewModel.getComment(commentTypePrefix, viewModel.code)
                        commentAdapter.replyPopup?.dismiss()
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
                val snackBar = Snackbar.make(requireView(), text, Snackbar.LENGTH_LONG)
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

    private fun showRedDotCount(count: Int) {
        val videoFragment = parentFragment as? VideoFragment
        videoFragment?.showRedDotCount(count)
    }
    private fun sortList(list: List<VideoComments.VideoComment>, type: SortType): List<VideoComments.VideoComment> =
        when (type) {
            SortType.LATEST -> list.safeSortedBy({ parseTimeStrToMinutes(it.date) }, descending = false)
            SortType.EARLIEST -> list.safeSortedBy({ parseTimeStrToMinutes(it.date) }, descending = true)
            SortType.MOST_REPLY -> list.safeSortedBy({ it.replyCount ?: 0 }, descending = true)
            SortType.MOST_LIKES -> list.safeSortedBy({ it.realLikesCount ?: 0 }, descending = true)
            SortType.MOST_DISLIKES -> list.safeSortedBy({ it.realLikesCount ?: 0 }, descending = false)
        }

    private fun getSortText(type: SortType): String =
        when (type) {
            SortType.LATEST -> getString(R.string.sort_by_newest)
            SortType.EARLIEST -> getString(R.string.sort_by_oldest)
            SortType.MOST_REPLY -> getString(R.string.sort_by_replies)
            SortType.MOST_LIKES -> getString(R.string.sort_most_likes)
            SortType.MOST_DISLIKES -> getString(R.string.sort_most_dislikes)
        }
}