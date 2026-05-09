package com.yenaly.han1meviewer.ui.fragment.home.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadingScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.DownloadViewModel
import com.yenaly.han1meviewer.util.HImageMeower
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class DownloadingFragment : Fragment() {

    private val viewModel by viewModels<DownloadViewModel>()
    private var currentItems: List<HanimeDownloadEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    DownloadingScreen(
                        downloadingFlow = viewModel.loadAllDownloadingHanime(),
                        onPauseItem = HanimeDownloadManagerV2::stopTask,
                        onResumeItem = HanimeDownloadManagerV2::resumeTask,
                        onDeleteItem = HanimeDownloadManagerV2::deleteTask,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadAllDownloadingHanime().collect { items ->
                    currentItems = items
                }
            }
        }
    }

    private suspend fun createTestTask() {
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
        HanimeDownloadManagerV2.addTask(args, redownload = false)
    }

    private fun resumeAll(items: List<HanimeDownloadEntity>) {
        items.forEach { entity ->
            if (!entity.isDownloading) {
                HanimeDownloadManagerV2.resumeTask(entity)
            }
        }
    }

    private fun pauseAll(items: List<HanimeDownloadEntity>) {
        items.forEach { entity ->
            if (entity.isDownloading) {
                HanimeDownloadManagerV2.stopTask(entity)
            }
        }
    }

    fun onToolbarMenuSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.tb_test -> {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    createTestTask()
                }
                true
            }

            R.id.tb_start_all -> {
                resumeAll(currentItems)
                true
            }

            R.id.tb_pause_all -> {
                pauseAll(currentItems)
                true
            }

            else -> false
        }
    }
}
