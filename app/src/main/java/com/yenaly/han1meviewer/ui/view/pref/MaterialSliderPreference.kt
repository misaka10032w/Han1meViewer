package com.yenaly.han1meviewer.ui.view.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import com.yenaly.han1meviewer.R

class MaterialSliderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    private var sliderValue: Float = 0f
    private var max = 100f
    private var min = 0f
    private var step = 1f

    init {
        layoutResource  = R.layout.pref_material_slider
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val slider = holder.findViewById(R.id.slider) as? Slider ?: return

        slider.valueFrom = min
        slider.valueTo = max
        if (step > 0f) {
            slider.stepSize = step
        } else {
            slider.stepSize = 0f // 0 表示连续模式
        }
        slider.value = sliderValue

        slider.addOnChangeListener { _, value, _ ->
            sliderValue = value
            persistFloat(value)
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        sliderValue = when (defaultValue) {
            is Float -> getPersistedFloat(defaultValue)
            is Int -> getPersistedFloat(defaultValue.toFloat())
            else -> getPersistedFloat(min)
        }
    }

    /**
     * 设置滑条的范围和步长。
     *
     * @param min 滑条的最小值（包含）
     * @param max 滑条的最大值（包含）
     * @param step 每步的间隔。如果为 0f，则表示连续滑动。
     *
     * 示例：
     * ```
     * setSliderRange(0f, 8f, 1f)
     * ```
     */
    fun setSliderRange(min: Float, max: Float, step: Float) {
        this.min = min
        this.max = max
        this.step = step
        notifyChanged()
    }
    /**
     * 设置滑条值变化时的 Summary 显示逻辑。
     *
     * @param converter 接收当前滑条值，返回显示用的字符串。
     * @param action 可选：每次值变化时要执行的动作。
     *
     * 示例：
     * ```
     * setSummaryConverter(
     *     converter = { value -> "当前值：${value.toInt()} KB/s" }
     * )
     * ```
     */
    fun setSummaryConverter(converter: (Float) -> CharSequence?, action: ((Float) -> Unit)? = null) {
        this.summary = converter(sliderValue)
        this.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Float) {
                summary = converter(newValue)
                action?.invoke(newValue)
            }
            true
        }
    }

}
