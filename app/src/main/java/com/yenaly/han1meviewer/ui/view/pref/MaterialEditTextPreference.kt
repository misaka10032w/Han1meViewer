package com.yenaly.han1meviewer.ui.view.pref

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.preference.EditTextPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.itxca.spannablex.Span.Companion.dp
import com.yenaly.han1meviewer.R

class MaterialEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditTextPreference(context, attrs) {
    private var itemHint: CharSequence? = null
    private var defaultSummary: CharSequence? = null

    init {
        defaultSummary = summary

        summaryProvider = SummaryProvider<EditTextPreference> { pref ->
            val value = pref.text
            if (value.isNullOrBlank()) {
                defaultSummary
            } else {
                value
            }
        }
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.hint)) {
            itemHint = getText(0)
        }
    }

    override fun onClick() {
        val currentValue = text
        val input = TextInputEditText(context).apply {
            setText(currentValue)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
            setHorizontallyScrolling(false)
            currentValue?.let { setSelection(it.length) }
        }

        val layout = TextInputLayout(context).apply {
            hint = itemHint ?: dialogTitle ?: title
            setPadding(
                dp(24), dp(8), dp(24), dp(0)
            )
            addView(input)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(dialogTitle ?: title)
            .setMessage(dialogMessage)
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newValue = input.text?.toString()
                if (callChangeListener(newValue)) {
                    text = newValue
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}