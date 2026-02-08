package com.yenaly.han1meviewer.util

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.NavMainDirections

fun NavController.openVideo(code: String) {
    val directions = NavMainDirections.actionGlobalToVideo(code)
    navigate(directions)
}

fun Fragment.openVideo(code: String) {
    findNavController().openVideo(code)
}