package com.yenaly.han1meviewer.util

import android.view.View
import android.view.ViewGroup

fun View.removeItself() {
    (parent as? ViewGroup)?.removeView(this)
}
