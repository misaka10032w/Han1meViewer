package com.yenaly.han1meviewer.base

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.StringRes
import com.yenaly.han1meviewer.R
import kotlin.math.roundToInt

/**
 * 应用内字体缩放档位。
 *
 * [System] 跟随系统但受 [MAX_SYSTEM_FONT_SCALE] 限幅，其余档位直接使用 [factor]。
 * 默认 [Normal]（固定 1.0），系统字体再大也不影响应用。
 */
enum class AppFontScale(val key: String, val factor: Float, @StringRes val displayNameRes: Int) {
    System("system", 1f, R.string.follow_system),
    Small("small", 0.85f, R.string.font_scale_small),
    Normal("normal", 1f, R.string.font_scale_normal),
    Large("large", 1.15f, R.string.font_scale_large),
    ExtraLarge("xlarge", 1.3f, R.string.font_scale_xlarge);

    companion object {

        /** 跟随系统时的上限。系统字体最大可达 1.5~2.0，直接跟随后固定高度容器会装不下文字。 */
        const val MAX_SYSTEM_FONT_SCALE = 1.2f

        val DEFAULT = Normal

        fun fromKey(key: String?): AppFontScale = entries.firstOrNull { it.key == key } ?: DEFAULT

        /** 对已限幅的输入再次调用结果不变，因此 Context 层与 Compose 层可以叠加使用。 */
        fun resolve(systemFontScale: Float, pref: AppFontScale): Float {
            return if (pref == System) {
                systemFontScale.coerceAtMost(MAX_SYSTEM_FONT_SCALE)
            } else {
                pref.factor
            }
        }
    }
}

/**
 * 「显示大小」兼容：系统放大 display size 后 density 变大、窗口逻辑宽度变小（如 411dp → 320dp）。
 * 这里保证窗口逻辑宽度不低于 [MIN_LOGICAL_WIDTH_DP]，超出部分用降低 density 抵消。
 */
object AppDisplayDensity {

    /** 手机基准逻辑宽度，平板 / 折叠屏展开态一般远大于该值。 */
    const val MIN_LOGICAL_WIDTH_DP = 360f

    /** 「忽略系统显示大小」默认开关。 */
    const val DEFAULT_ENABLED = true

    /** 窗口占屏宽达到该比例才视为全屏，避免分屏 / 小窗 / 画中画被误压 density。 */
    private const val FULLSCREEN_RATIO = 0.9f

    fun resolve(
        currentDensity: Float,
        windowWidthPx: Int,
        displayWidthPx: Int,
        enabled: Boolean,
    ): Float {
        if (!enabled || currentDensity <= 0f || windowWidthPx <= 0 || displayWidthPx <= 0) {
            return currentDensity
        }
        if (windowWidthPx < displayWidthPx * FULLSCREEN_RATIO) return currentDensity
        val logicalWidthDp = windowWidthPx / currentDensity
        if (logicalWidthDp >= MIN_LOGICAL_WIDTH_DP) return currentDensity
        return windowWidthPx / MIN_LOGICAL_WIDTH_DP
    }

    /**
     * 把兼容密度写进 [config]。
     *
     * Dialog / Popup / BottomSheet 等独立窗口会按自身 View context 重新解析 density，
     * Compose 层的 `LocalDensity` 覆盖跨不过窗口边界，只能在 Configuration 里改。
     * 同时按新密度换算 dp 字段，保持 Configuration 与窗口一致。
     *
     * @return [config] 是否被修改
     */
    fun applyTo(
        config: Configuration,
        metrics: DisplayMetrics,
        displayWidthPx: Int,
        enabled: Boolean,
    ): Boolean {
        if (!enabled || metrics.density <= 0f) return false
        if (config.screenWidthDp == Configuration.SCREEN_WIDTH_DP_UNDEFINED) return false

        val systemDensity = metrics.density
        val targetDensity = resolve(
            currentDensity = systemDensity,
            windowWidthPx = (config.screenWidthDp * systemDensity).roundToInt(),
            displayWidthPx = displayWidthPx,
            enabled = true,
        )
        if (targetDensity == systemDensity) return false

        val scale = systemDensity / targetDensity
        config.densityDpi = (targetDensity * DisplayMetrics.DENSITY_DEFAULT).roundToInt()
        config.screenWidthDp = (config.screenWidthDp * scale).roundToInt()
        if (config.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
            config.screenHeightDp = (config.screenHeightDp * scale).roundToInt()
        }
        if (config.smallestScreenWidthDp != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
            config.smallestScreenWidthDp = (config.smallestScreenWidthDp * scale).roundToInt()
        }
        return true
    }
}

/**
 * 屏幕物理像素宽度，不受系统「显示大小」影响。
 *
 * 不要改用 `getSystemService(DisplayManager)`：`attachBaseContext` 阶段调用会 NPE。
 */
internal fun displayRealWidthPx(): Int = Resources.getSystem().displayMetrics.widthPixels
