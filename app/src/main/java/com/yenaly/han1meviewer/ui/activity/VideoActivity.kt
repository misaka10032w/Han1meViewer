package com.yenaly.han1meviewer.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.yenaly.han1meviewer.FROM_DOWNLOAD
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
import com.yenaly.yenaly_libs.utils.intentExtra

class VideoActivity :AppCompatActivity () {

    private val videoCode by intentExtra<String>(VIDEO_CODE)
    private val fromDownload by intentExtra(FROM_DOWNLOAD, false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 可使用 ViewBinding/普通 XML，这里使用简洁方式
        setContentView(R.layout.activity_video)
        if (savedInstanceState == null) {
            val fragment = VideoFragment().apply {
                arguments = bundleOf(
                    VIDEO_CODE to videoCode,
                    FROM_DOWNLOAD to fromDownload
                )
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
    // 在 VideoActivity 中
//    class VideoActivity : AppCompatActivity() {
//        // 中转方法
//        fun forwardRedDotCount(count: Int) {
//            val videoFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? VideoFragment
//            videoFragment?.showRedDotCount(count)
//        }
//    }

}
