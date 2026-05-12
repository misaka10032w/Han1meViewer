package com.yenaly.han1meviewer.util

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE

fun NavController.openVideo(code: String) {
    navigate(
        R.id.videoFragment,
        bundleOf(VIDEO_CODE to code),
    )
}

fun Fragment.openVideo(code: String) {
    findNavController().openVideo(code)
}
