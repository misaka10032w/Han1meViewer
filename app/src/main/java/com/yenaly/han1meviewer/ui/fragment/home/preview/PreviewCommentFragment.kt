package com.yenaly.han1meviewer.ui.fragment.home.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.yenaly.han1meviewer.COMMENT_TYPE
import com.yenaly.han1meviewer.DATE_CODE
import com.yenaly.han1meviewer.PREVIEW_COMMENT_PREFIX
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.FragmentPreviewCommentBinding
import com.yenaly.han1meviewer.ui.fragment.video.CommentFragment
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.makeBundle

class PreviewCommentFragment : YenalyFragment<FragmentPreviewCommentBinding>() {

    val viewModel by viewModels<CommentViewModel>()

    private val date: String by lazy { requireArguments().getString("date")!! }
    private val dateCode: String by lazy { requireArguments().getString(DATE_CODE)!! }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPreviewCommentBinding =
        FragmentPreviewCommentBinding.inflate(inflater, container, false)


    override fun onDestroyView() {
        super.onDestroyView()
        PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
    }

    override fun initData(savedInstanceState: Bundle?) {
        // Toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeActionContentDescription(R.string.back)
            it.title = getString(R.string.latest_hanime_comment, date)
        }

        viewModel.code = dateCode

        PreviewCommentPrefetcher.here()
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)

        val commentFragment =
            CommentFragment().makeBundle(COMMENT_TYPE to PREVIEW_COMMENT_PREFIX)
        childFragmentManager.beginTransaction()
            .add(R.id.fcv_pre_comment, commentFragment)
            .commit()
    }

    companion object {
        fun newInstance(date: String, dateCode: String) = PreviewCommentFragment().apply {
            arguments = bundleOf(
                "date" to date,
                DATE_CODE to dateCode
            )
        }
    }
}
