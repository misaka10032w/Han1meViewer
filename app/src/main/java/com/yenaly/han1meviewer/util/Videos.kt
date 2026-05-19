package com.yenaly.han1meviewer.util

import androidx.fragment.app.Fragment
import com.yenaly.han1meviewer.ui.activity.MainActivity

fun Fragment.openVideo(code: String) {
    (activity as? MainActivity)?.showVideoDetailFragment(code)
}
