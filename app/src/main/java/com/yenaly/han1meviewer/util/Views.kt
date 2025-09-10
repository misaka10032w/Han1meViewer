package com.yenaly.han1meviewer.util

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.GravityInt
import androidx.annotation.Px
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayout
import kotlin.math.max

fun Button.setDrawableTop(@DrawableRes drawableRes: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(0, drawableRes, 0, 0)
}

fun TabLayout.getTextViewAt(position: Int): TextView? {
    val container = getChildAt(0) as? ViewGroup
    val tab = container?.getChildAt(position) as? ViewGroup
    return tab?.children?.filterIsInstance<TextView>()?.singleOrNull()
}

fun TabLayout.getOrCreateBadgeOnTextViewAt(
    position: Int,
    targetView: View?,
    @GravityInt gravity: Int,
    @Px spacing: Int = 0,
    action: BadgeDrawable.() -> Unit
) {
    val tab = getTabAt(position) ?: return
    val target = targetView ?: getTextViewAt(position)
    target?.post {
        tab.orCreateBadge.apply(action).apply {
            setGravity(target, gravity, spacing)
        }
    }
}

fun BadgeDrawable.setGravity(
    targetView: View,
    @GravityInt gravity: Int,
    @Px spacing: Int = 0
) {
    badgeGravity = BadgeDrawable.TOP_END
    when (gravity) {
        Gravity.START -> {
            verticalOffset = (targetView.height + this.intrinsicHeight) / 2
            horizontalOffset = targetView.width + this.intrinsicWidth + spacing
        }

        Gravity.END -> {
            verticalOffset = (targetView.height + this.intrinsicHeight) / 2
            horizontalOffset = -spacing
        }

        Gravity.BOTTOM -> {
            verticalOffset = targetView.height + this.intrinsicHeight + spacing
            horizontalOffset = (targetView.width + this.intrinsicWidth) / 2
        }

        Gravity.TOP -> {
            verticalOffset = -spacing
            horizontalOffset = (targetView.width + this.intrinsicWidth) / 2
        }
    }
}

fun calculateSpanCount(recyclerView: RecyclerView, itemMinWidthDp: Int): Int {
    val density = recyclerView.resources.displayMetrics.density
    val itemMinWidthPx = (itemMinWidthDp * density).toInt()
    val totalSpace = recyclerView.measuredWidth - recyclerView.paddingLeft - recyclerView.paddingRight
    return max(2, totalSpace / itemMinWidthPx)
}