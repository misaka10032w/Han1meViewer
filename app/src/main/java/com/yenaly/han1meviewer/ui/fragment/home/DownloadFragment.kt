package com.yenaly.han1meviewer.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.parseAsHtml
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ActivityDownloadBinding
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.ui.fragment.home.download.DownloadedFragment
import com.yenaly.han1meviewer.ui.fragment.home.download.DownloadingFragment
import com.yenaly.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.base.YenalyFragment
import com.yenaly.yenaly_libs.utils.showLongToast
import com.yenaly.yenaly_libs.utils.view.attach
import com.yenaly.yenaly_libs.utils.view.setUpFragmentStateAdapter
import kotlinx.coroutines.launch

/**
 * 下载影片总Fragment，暫時由[DownloadedFragment]全權托管
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/08/01 001 17:44
 */
class DownloadFragment : YenalyFragment<ActivityDownloadBinding>(){

    companion object {
        const val TAG = "HoppinByte"
        private const val HB = """<span style="color: #FF0000;"><b>H</b></span>oppin<b>Byte</b>"""
        val hbSpannedTitle = HB.parseAsHtml()
        private val tabNameArray = intArrayOf(R.string.downloading, R.string.downloaded)
    }
    private val dao: HanimeDownloadDao
        get() = DownloadDatabase.instance.hanimeDownloadDao

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityDownloadBinding {
        return ActivityDownloadBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //需要设置个背景，否则会因系统需要绘制过渡动画,但是主页图像为软件绘制而触发
        // [Software rendering doesn't support hardware bitmaps]
        val backgroundColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface)
        view.setBackgroundColor(backgroundColor)
    }

    override fun initData(savedInstanceState: Bundle?) {
        val activity = requireActivity() as AppCompatActivity

        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.let {
            it.title = hbSpannedTitle
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeActionContentDescription(R.string.back)
        }

        binding.toolbar.setNavigationOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }

        binding.viewPager.setUpFragmentStateAdapter(this) {
            addFragment { DownloadingFragment() }
            addFragment { DownloadedFragment() }
        }

        binding.tabLayout.attach(binding.viewPager) { tab, position ->
            tab.setText(tabNameArray[position])
        }
        binding.importDownloadedFab.setOnClickListener {
            if (!Preferences.safDownloadPath.isNullOrBlank() && !Preferences.isUsePrivateStorage){
                requireContext().showAlertDialog {
                    setTitle(getString(R.string.read_download_dir_title))
                    setMessage(getString(R.string.read_download_dir_message))
                    setPositiveButton(R.string.ok){_,_->
                        lifecycleScope.launch {
                            scanAndImportHanimeDownloads(requireContext(),dao)
                        }
                        showLongToast(getString(R.string.read_success))
                    }
                    setNegativeButton(getString(R.string.cancel)){  _, _ -> }
                }
            } else {
                showLongToast(getString(R.string.select_custom_directory))
            }
        }
    }
}