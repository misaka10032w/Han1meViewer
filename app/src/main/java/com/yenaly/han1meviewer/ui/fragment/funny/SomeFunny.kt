package com.yenaly.han1meviewer.ui.fragment.funny

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import kotlin.math.abs
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
class SomeFunny(
    context: Context,
    private val onTrigger: () -> Unit
) {
    private val konamiPattern = listOf("UP", "UP", "DOWN", "DOWN", "LEFT", "RIGHT", "LEFT", "RIGHT")
    private var inputSequence = mutableListOf<String>()
    private var lastInputTime = 0L
    private val timeLimit = 2000 // 2秒内完成全部输入
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)
    private val minDistance = 10
    // 用于过滤手指未抬起时的重复方向
    private var lastDirectionInMove: String? = null

    fun handleMotionEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> detectDirection(event)
            MotionEvent.ACTION_UP -> {
                resetIfTimeout()
                // 手指抬起后允许再次输入同方向
                lastDirectionInMove = null
            }
        }
        return true
    }

    private fun detectDirection(event: MotionEvent) {
        val (dx, dy) = if (event.historySize > 0) {
            val lastIndex = event.historySize - 1
            event.x - event.getHistoricalX(lastIndex) to
                    event.y - event.getHistoricalY(lastIndex)
        } else {
            0f to 0f
        }

        // 过滤抖动，小于阈值不判定方向
        if (abs(dx) < minDistance && abs(dy) < minDistance) return

        val currentDirection = when {
            // 优先水平判断，LEFT/RIGHT更容易识别
            abs(dx) >= abs(dy) && dx < 0 -> "LEFT"
            abs(dx) >= abs(dy) && dx > 0 -> "RIGHT"
            abs(dy) > abs(dx) && dy < 0 -> "UP"
            abs(dy) > abs(dx) && dy > 0 -> "DOWN"
            else -> null
        }

        if (currentDirection != null) {
            if (currentDirection != lastDirectionInMove) {
                registerInput(currentDirection)
                lastDirectionInMove = currentDirection
            }
        }
    }


    private fun registerInput(direction: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInputTime > timeLimit) {
            inputSequence.clear()
        }
        lastInputTime = currentTime

        inputSequence.add(direction)
        Log.i("funny", "sequence=$inputSequence")
        vibrateOnce()
        // 检查是否匹配
        if (inputSequence.size >= konamiPattern.size) {
            val subList = inputSequence.takeLast(konamiPattern.size)
            if (subList == konamiPattern) {
                inputSequence.clear()
                onTrigger.invoke()
            }
        }
    }

    private fun resetIfTimeout() {
        if (System.currentTimeMillis() - lastInputTime > timeLimit) {
            inputSequence.clear()
        }
    }
    private fun vibrateOnce() {
        val vibrationDuration = 20L
        try {
            vibrator?.let {
                if (!it.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(vibrationDuration)
                }
            }
        } catch (e: Exception) {
            Log.e("Vibration", "Vibration error", e)
        }
    }
}
