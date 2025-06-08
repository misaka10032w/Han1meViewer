package com.yenaly.han1meviewer.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.yenaly.han1meviewer.FROM_DOWNLOAD
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
import com.yenaly.yenaly_libs.utils.intentExtra

class VideoActivity :AppCompatActivity (),PermissionRequester {

    private val videoCode by intentExtra<String>(VIDEO_CODE)
    private val fromDownload by intentExtra(FROM_DOWNLOAD, false)
    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1234
    }

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
    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null
    private var onPermanentlyDenied: (() -> Unit)? = null

    //懒得写基类了
    override fun requestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                this.onGranted = onGranted
                this.onDenied = onDenied
                this.onPermanentlyDenied = onPermanentlyDenied
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_WRITE_EXTERNAL_STORAGE)
            }
        } else {
            onGranted()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            val permission = permissions.getOrNull(0)
            val grantResult = grantResults.getOrNull(0)

            if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                when {
                    grantResult == PackageManager.PERMISSION_GRANTED -> {
                        onGranted?.invoke()
                    }

                    shouldShowRequestPermissionRationale(permission) -> {
                        onDenied?.invoke()
                    }

                    else -> {
                        onPermanentlyDenied?.invoke()
                    }
                }
                // 清除引用，防止内存泄露
                onGranted = null
                onDenied = null
                onPermanentlyDenied = null
            }
        }
    }
}
