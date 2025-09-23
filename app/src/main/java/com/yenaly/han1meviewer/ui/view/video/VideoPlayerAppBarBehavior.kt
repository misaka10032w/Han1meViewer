package com.yenaly.han1meviewer.ui.view.video

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout

class VideoPlayerAppBarBehavior @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout.Behavior(context, attrs) {

    var disableScroll = false

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        ev: MotionEvent
    ): Boolean {
        return if (disableScroll) false else super.onInterceptTouchEvent(parent, child, ev)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: android.view.View,
        target: android.view.View,
        axes: Int,
        type: Int
    ): Boolean {
        return if (disableScroll) false else super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }
}
