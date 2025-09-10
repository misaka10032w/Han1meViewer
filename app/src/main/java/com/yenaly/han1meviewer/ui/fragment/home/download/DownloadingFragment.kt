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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.FragmentListOnlyBinding
import com.yenaly.han1meviewer.ui.StateLayoutMixin
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adapter.HanimeDownloadingRvAdapter
import com.yenaly.han1meviewer.ui.fragment.IToolbarFragment
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.HImageMeower
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.util.setStateViewLayout
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.activity
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 正在下载的影片
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/08/01 001 17:45
 */
class DownloadingFragment : YenalyFragment<FragmentListOnlyBinding>(), StateLayoutMixin {

    val viewModel by viewModels<DownloadViewModel>()

    private val adapter by unsafeLazy { HanimeDownloadingRvAdapter(this) }

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
        // binding.rvList.itemAnimator?.changeDuration = 0
    }

    override fun bindDataObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadAllDownloadingHanime()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    adapter.submitList(it)
                }
        }
    }

    private suspend fun test() {
        requireContext().requestPostNotificationPermission()
        val uuid = UUID.randomUUID()
        val videoCode = uuid.toString()
        val args = HanimeDownloadWorker.Args(
            quality = "720P",
            downloadUrl = "https://ash-speed.hetzner.com/100MB.bin",
            videoType = "mp4",
            hanimeName = "Test-$uuid",
            videoCode = videoCode,
            coverUrl = HImageMeower.placeholder(100, 200),
        )
        // HanimeDownloadManager.addTask(args, redownload = false)
        HanimeDownloadManagerV2.addTask(args, redownload = false)
    }

    fun onToolbarMenuSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.tb_test -> {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) { test() }
                true
            }

            R.id.tb_start_all -> {
                adapter.items.forEachIndexed { index, entity ->
                    if (!entity.isDownloading) {
                        HanimeDownloadManagerV2.resumeTask(entity)
                        adapter.notifyItemChanged(index)
                    }
                }
                true
            }

            R.id.tb_pause_all -> {
                adapter.items.forEachIndexed { index, entity ->
                    if (entity.isDownloading) {
                        HanimeDownloadManagerV2.stopTask(entity)
                        adapter.notifyItemChanged(index)
                    }
                }
                true
            }

            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        activity<AppCompatActivity>().supportActionBar?.setSubtitle(R.string.downloading)
    }
}