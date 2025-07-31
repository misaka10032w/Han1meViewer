package com.yenaly.han1meviewer.ui.popup

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lxj.xpopup.core.BottomPopupView
import com.yenaly.han1meviewer.R
import com.yenaly.yenaly_libs.utils.unsafeLazy

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/09/20 020 09:48
 */
class ReplyPopup(context: Context) : BottomPopupView(context) {

    private val editText by unsafeLazy { findViewById<TextInputEditText>(R.id.et_comment) }
    private val btnSend by unsafeLazy { findViewById<MaterialButton>(R.id.btn_send) }

    private var commentPrefix: String? = null
    private var sendListener: OnClickListener? = null

    override fun getImplLayoutId() = R.layout.pop_up_reply

    override fun onCreate() {
        super.onCreate()
        editText.hint = hint
        commentPrefix?.let(editText::append)
        sendListener?.let(btnSend::setOnClickListener)
        //防止连续点击干出去好几个评论，评论区有时候出现好几个相同的评论，一猜就是APP发出去的，防止站长发现给加验证码。
        btnSend.setOnClickListener {
            btnSend.isEnabled = false
            sendListener?.onClick(it)
        }
    }
    /**
     * [onKeyboardHeightChange]方法在某些情况下只让出了IME高度，没有让出沉浸式导航栏高度
     */
    @Suppress("DEPRECATION")
    override fun onKeyboardHeightChange(height: Int) {
        super.onKeyboardHeightChange(height)
        val insets = rootView.rootWindowInsets
        val navHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
        } else {
            if (height == 0) {
                insets?.systemWindowInsetBottom ?: 0
            } else {
                (insets?.systemWindowInsetBottom ?: 0) - height
            }
        }
        translationY = if (height > 0) -(height + navHeight).toFloat() else 0f
    }
    fun enableSendButton() {
        btnSend.isEnabled = true
    }
    /**
     * 得到你输入的内容
     */
    val comment get() = editText.text.toString()

    /**
     * 设置提示
     */
    var hint: CharSequence? = null

    /**
     * 设置前缀，用于回复子评论
     *
     * 例如：@xxx something
     */
    fun initCommentPrefix(username: String) {
        commentPrefix = "@$username "
    }

    /**
     * 设置发送按钮监听器
     */
    fun setOnSendListener(listener: OnClickListener) {
        this.sendListener = listener
    }
}