package com.yenaly.han1meviewer.ui.view.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.yenaly.han1meviewer.R

class MaterialSliderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.seekBarPreferenceStyle,
    defStyleRes: Int = 0
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var mStepSize: Float = 1f
    private var mLabelBehavior: Int = LabelFormatter.LABEL_FLOATING

    init {
        layoutResource  = R.layout.pref_material_slider
        attrs?.let {
            val androidNS = "http://schemas.android.com/apk/res/android"
            val appNS = "http://schemas.android.com/apk/res-auto"
            mStepSize = it.getAttributeFloatValue(androidNS, "stepSize", 1f)
            mLabelBehavior = it.getAttributeIntValue(appNS, "labelBehavior", LabelFormatter.LABEL_FLOATING)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val slider = holder.findViewById(R.id.slider) as? Slider

        slider?.apply {
            valueFrom = min.toFloat()
            valueTo = max.toFloat()
            stepSize = mStepSize
            labelBehavior = mLabelBehavior
            val savedValue = getPersistedInt(value.toInt())
            value = savedValue.coerceIn(valueFrom.toInt(),valueTo.toInt()).toFloat()
            clearOnChangeListeners()
            addOnChangeListener { _, newValue, fromUser ->
                if (fromUser) {
                    if (callChangeListener(newValue.toInt())) {
                        persistInt(newValue.toInt())
                    }
                }
            }
        }
    }
}
