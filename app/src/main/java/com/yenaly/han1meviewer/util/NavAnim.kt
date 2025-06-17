package com.yenaly.han1meviewer.util

import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.yenaly.han1meviewer.R

object NavAnim {

    /**
     * 从右进左出动画，用于正向导航
     */
    fun slideInFromRight(restore:Boolean): NavOptions {
        return navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.stay
                popEnter = R.anim.stay
                popExit = R.anim.slide_out_right
            }
            restoreState = restore
        }
    }

    /**
     * 从底部弹入动画（可选扩展）
     */
    fun slideInFromBottom(restore:Boolean): NavOptions {
        return navOptions {
            anim {
                enter = R.anim.slide_in_from_bottom
                exit = R.anim.fade_out
                popEnter = R.anim.fade_in
                popExit = R.anim.slide_out_to_bottom
            }
            restoreState = restore
        }
    }
}
