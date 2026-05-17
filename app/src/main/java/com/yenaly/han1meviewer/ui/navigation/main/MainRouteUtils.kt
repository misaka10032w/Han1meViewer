package com.yenaly.han1meviewer.ui.navigation.main

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.yenaly.han1meviewer.R

internal fun shiftMonthCodeForPreview(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return "%04d%02d".format(year, month)
}

internal fun showAnnouncementDialog(
    context: Context,
    title: String,
    content: Spanned,
    imageUrl: String?,
) {
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_announcement, null, false)
    view.findViewById<TextView>(R.id.dialogTitle).apply {
        text = title
        visibility = View.VISIBLE
    }
    view.findViewById<TextView>(R.id.dialogContent).apply {
        text = content
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
    }
    if (!imageUrl.isNullOrBlank()) {
        view.findViewById<ShapeableImageView>(R.id.dialogImage).apply {
            visibility = View.VISIBLE
            load(imageUrl) {
                placeholder(R.drawable.akarin)
                error(R.drawable.baseline_error_outline_24)
            }
            setOnClickListener {
                Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
                    setContentView(ImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        load(imageUrl)
                        setOnClickListener { dismiss() }
                    })
                    show()
                }
            }
        }
    }
    MaterialAlertDialogBuilder(context)
        .setView(view)
        .setPositiveButton(context.getString(R.string.i_understand), null)
        .show()
}
