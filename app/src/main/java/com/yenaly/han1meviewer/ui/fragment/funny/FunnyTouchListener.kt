package com.yenaly.han1meviewer.ui.fragment.funny

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View

class FunnyTouchListener (
    context: Context,
    onTrigger: () -> Unit
) : View.OnTouchListener {

    private val detector = SomeFunny(context,onTrigger)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return detector.handleMotionEvent(event)
    }
}