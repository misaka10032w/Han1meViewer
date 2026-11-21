package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import kotlin.math.roundToInt

/**
 * 带猫爪图标的 M3 开关。
 *
 * 切换时猫爪会「撞墙摇晃」：先顺着拇指滑动的方向快速倾倒、撞上轨道边缘，
 * 再高弹性地来回晃几下回正。
 */
@Composable
fun PawSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val pawRotation = remember { Animatable(0f) }
    // 首次组合时不播动画，只在用户切换时摇晃。
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        if (!settled) {
            settled = true
            return@LaunchedEffect
        }
        // 开启时拇指向右滑（撞右墙、顺时针倾倒），关闭时相反。
        val wall = if (checked) 50f else -50f
        // 第一段：快速冲向墙。
        pawRotation.animateTo(wall, tween(durationMillis = 70))
        // 第二段：低阻尼弹性摇晃着回正。
        pawRotation.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_paw_24),
                contentDescription = null,
                modifier = Modifier
                    .size(SwitchDefaults.IconSize)
                    .graphicsLayer { rotationZ = pawRotation.value },
            )
        },
    )
}

@Composable
fun SettingSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconRes: Int? = null,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
        PawSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
fun SettingNavigationItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    valueText: String? = null,
    iconRes: Int? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!valueText.isNullOrBlank()) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
fun SettingInfoItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    valueText: String? = null,
    iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!valueText.isNullOrBlank()) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 猫头滑块拇指
 */
@Composable
private fun CatHeadSliderThumb(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(30.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_cat_head_24),
            contentDescription = null,
            modifier = Modifier.padding(2.dp),
        )
    }
}

@Composable
fun SettingSliderItem(
    title: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    summary: String? = null,
    iconRes: Int? = null,
) {
    val totalSteps = if (step > 0) {
        ((valueRange.last - valueRange.first) / step) - 1
    } else {
        0
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            iconRes?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val sliderState = remember(valueRange, totalSteps) {
            SliderState(
                value = value.toFloat(),
                steps = totalSteps.coerceAtLeast(0),
                trackRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            )
        }
        LaunchedEffect(value) {
            if (!sliderState.isDragging) {
                sliderState.value = value.toFloat()
            }
        }
        Slider(
            state = sliderState,
            onValueChange = {
                // SliderState 不会自动跟随手势，必须同步写入当前拖动位置。
                sliderState.value = it
                onValueChange(it.roundToInt())
            },
            thumb = { CatHeadSliderThumb() },
        )
    }
}
@Composable
fun SettingChoiceItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_fill_cat_head_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingSwitchItemPreview() {
    ComponentPreview {
        SettingSwitchItem(
            title = "启用动态取色",
            summary = "Android 12 及以上可使用系统取色",
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingNavigationItemPreview() {
    ComponentPreview {
        SettingNavigationItem(
            title = "播放器内核",
            summary = "当前使用 ExoPlayer",
            valueText = "ExoPlayer",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingInfoItemPreview() {
    ComponentPreview {
        SettingInfoItem(
            title = "当前版本",
            summary = "Han1meViewer",
            valueText = "v1.0.0",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingSliderItemPreview() {
    ComponentPreview {
        SettingSliderItem(
            title = "检查更新跳出视窗间隔天数",
            summary = "7天\n最近还没跳出过更新视窗哦",
            value = 7,
            valueRange = 0..30,
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingChoiceItemPreview() {
    ComponentPreview {
        SettingChoiceItem(
            title = "跟随系统",
            selected = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
fun SittingCatAnimation(
    modifier: Modifier = Modifier,
    catColor: Color = MaterialTheme.colorScheme.primary,
    shadowColor: Color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.4f)
) {
    val transition = rememberInfiniteTransition(label = "cat")

    val tailAngle by transition.animateFloat(
        initialValue = -8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tailWag"
    )

    Canvas(modifier = modifier.size(200.dp)) {
        val scaleX = size.width / 200f
        val scaleY = size.height / 200f

        withTransform({
            scale(scaleX = scaleX, scaleY = scaleY, pivot = Offset.Zero)
        }) {
            drawOval(
                color = shadowColor,
                topLeft = Offset(52f, 161f),
                size = Size(92f, 10f)
            )

            val tailPath = Path().apply {
                moveTo(118f, 158f)
                cubicTo(148f, 158f, 162f, 132f, 148f, 106f)
                cubicTo(142f, 94f, 152f, 84f, 160f, 88f)
            }
            withTransform({
                rotate(degrees = tailAngle, pivot = Offset(120f, 158f))
            }) {
                drawPath(
                    path = tailPath,
                    color = catColor,
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )
            }

            val bodyPath = Path().apply {
                moveTo(66f, 86f)
                cubicTo(56f, 108f, 52f, 136f, 58f, 156f)
                cubicTo(60f, 163f, 66f, 165f, 74f, 165f)
                lineTo(114f, 165f)
                cubicTo(124f, 165f, 128f, 156f, 124f, 144f)
                cubicTo(118f, 125f, 110f, 105f, 104f, 86f)
                close()
            }
            drawPath(path = bodyPath, color = catColor)

            drawCircle(color = catColor, radius = 24f, center = Offset(85f, 74f))

            val leftEar = Path().apply {
                moveTo(63f, 66f)
                lineTo(61f, 42f)
                lineTo(80f, 54f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(107f, 66f)
                lineTo(109f, 42f)
                lineTo(90f, 54f)
                close()
            }
            drawPath(path = leftEar, color = catColor)
            drawPath(path = rightEar, color = catColor)
        }
    }
}