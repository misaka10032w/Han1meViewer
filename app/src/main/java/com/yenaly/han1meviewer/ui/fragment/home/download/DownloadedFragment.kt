package com.yenaly.han1meviewer.ui.fragment.home.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.FragmentListOnlyBinding
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.adapter.HanimeDownloadedRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.setStateViewLayout
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.activity
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.launch

/**
 * 已下载影片
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/08/01 001 17:45
 */
class DownloadedFragment : YenalyFragment<FragmentListOnlyBinding>(), StateLayoutMixin {

    val viewModel by viewModels<DownloadViewModel>()

    private val adapter by unsafeLazy { HanimeDownloadedRvAdapter(this) }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentListOnlyBinding {
        return FragmentListOnlyBinding.inflate(inflater, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        binding.rvList.layoutManager = LinearLayoutManager(context)
        binding.rvList.adapter = adapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.rvList) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBar.bottom)
            WindowInsetsCompat.CONSUMED
        }
        adapter.setStateViewLayout(R.layout.layout_empty_view)
        loadAllSortedDownloadedHanime()
    }

    override fun bindDataObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloaded.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    adapter.submitList(it) {
                      //  binding.rvList.scrollToPosition(0)
                    }
                }
        }
    }

    fun onToolbarMenuSelected(menuItem: MenuItem): Boolean {
        viewModel.currentSortOptionId = menuItem.itemId
        menuItem.isChecked = true
        return loadAllSortedDownloadedHanime()
    }

    override fun onResume() {
        super.onResume()
        activity<AppCompatActivity>().supportActionBar?.setSubtitle(R.string.downloaded)
    }

    // #issue-18: 添加下载区排序
    private fun loadAllSortedDownloadedHanime(): Boolean = when (viewModel.currentSortOptionId) {
        R.id.sm_sort_by_alphabet_ascending -> {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.TITLE,
                ascending = true
            )
            true
        }

        R.id.sm_sort_by_alphabet_descending -> {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.TITLE,
                ascending = false
            )
            true
        }

        R.id.sm_sort_by_date_ascending -> {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.ID,
                ascending = true
            )
            true
        }

        R.id.sm_sort_by_date_descending -> {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.ID,
                ascending = false
            )
            true
        }

        else -> false
    }
}