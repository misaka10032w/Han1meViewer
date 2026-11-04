package com.yenaly.han1meviewer.util

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.chad.library.adapter4.BaseQuickAdapter
import com.yenaly.han1meviewer.R

/**
 * 设置状态视图
 *
 * 必须在绑定 RV 后才能调用，因为用了 BaseQuickAdapter 里面的 context
 */
fun BaseQuickAdapter<*, *>.setStateViewLayout(
    @LayoutRes layoutRes: Int,
    text: String? = null,
) {
    val view = View.inflate(context, layoutRes, FrameLayout(context))
    view.findViewById<TextView>(R.id.tv_empty).text =
        text ?: context.getString(R.string.here_is_empty)
    stateView = view
}

/**
 * 设置状态视图
 */
fun BaseQuickAdapter<*, *>.setStateViewLayout(
    view: View,
    text: String? = null,
) {
    view.findViewById<TextView>(R.id.tv_empty).text =
        text ?: applicationContext.getString(R.string.here_is_empty)
    stateView = view
}
