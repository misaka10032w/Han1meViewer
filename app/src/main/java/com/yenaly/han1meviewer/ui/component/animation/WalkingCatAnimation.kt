package com.yenaly.han1meviewer.ui.component.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Immutable
data class WalkingCatPalette(
    val bodyColor: Color,
    val earInnerColor: Color,
    val eyeColor: Color,
    val noseColor: Color,
    val backgroundColor: Color
) {
    companion object {
        @Composable
        fun themeDefault(
            bodyColor: Color = MaterialTheme.colorScheme.primary,
            earInnerColor: Color = MaterialTheme.colorScheme.tertiary,
            eyeColor: Color = MaterialTheme.colorScheme.onPrimary,
            noseColor: Color = MaterialTheme.colorScheme.secondary,
            backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
        ): WalkingCatPalette = WalkingCatPalette(
            bodyColor = bodyColor,
            earInnerColor = earInnerColor,
            eyeColor = eyeColor,
            noseColor = noseColor,
            backgroundColor = backgroundColor
        )
    }
}

@Composable
fun WalkingCatAnimation(
    modifier: Modifier = Modifier,
    palette: WalkingCatPalette = WalkingCatPalette.themeDefault(),
    stepDurationMillis: Int = 680
) {
    val transition = rememberInfiniteTransition(label = "walking_cat_transition")

    val walkPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = stepDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "walk_phase"
    )

    val tailPhase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (stepDurationMillis * 1.35f).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail_phase"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Transparent)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 将画布统一缩放到 100x100 逻辑坐标，保证 1:1 比例一致
            val baseSize = 100f
            val scale = size.minDimension / baseSize
            val offsetX = (size.width - baseSize * scale) * 0.5f
            val offsetY = (size.height - baseSize * scale) * 0.5f

            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                drawWalkingCatRight(
                    phase = walkPhase,
                    tailSwing = tailPhase,
                    palette = palette
                )
            }
        }
    }
}

private fun DrawScope.drawWalkingCatRight(
    phase: Float,
    tailSwing: Float,
    palette: WalkingCatPalette
) {
    val angle = phase * 2f * PI.toFloat()

    // 身体随步频上下微起伏（每步两个峰值 -> 2 * angle）
    val bodyBobY = -abs(sin(angle)) * 2.2f

    // 四条腿摆动角度（度），面向右侧（+X）
    // 远侧腿（偏暗一点更有层次，但仍保持纯实心风格）
    val farColor = palette.bodyColor.copy(alpha = 0.82f)
    val nearColor = palette.bodyColor

    val farBackSwing = sin(angle) * 26f
    val farFrontSwing = sin(angle + PI.toFloat() * 0.55f) * 26f
    val nearBackSwing = sin(angle + PI.toFloat()) * 26f
    val nearFrontSwing = sin(angle + PI.toFloat() * 1.55f) * 26f

    // 1. 远侧腿（后腿、前腿）
    drawCatLeg(
        pivot = Offset(34f, 64f + bodyBobY),
        angleDeg = farBackSwing,
        length = 17f,
        thickness = 8.5f,
        color = farColor
    )
    drawCatLeg(
        pivot = Offset(61f, 64f + bodyBobY),
        angleDeg = farFrontSwing,
        length = 17f,
        thickness = 8.5f,
        color = farColor
    )

    // 2. 尾巴（位于左侧躯干后端，随步伐摆动）
    drawCatTail(
        start = Offset(26f, 54f + bodyBobY),
        swing = tailSwing,
        color = nearColor
    )

    // 3. 实心躯干（圆角胶囊体）
    drawRoundRect(
        color = nearColor,
        topLeft = Offset(24f, 46f + bodyBobY),
        size = Size(45f, 23f),
        cornerRadius = CornerRadius(11.5f, 11.5f)
    )

    // 4. 近侧腿（后腿、前腿）
    drawCatLeg(
        pivot = Offset(32f, 65f + bodyBobY),
        angleDeg = nearBackSwing,
        length = 17.5f,
        thickness = 9f,
        color = nearColor
    )
    drawCatLeg(
        pivot = Offset(59f, 65f + bodyBobY),
        angleDeg = nearFrontSwing,
        length = 17.5f,
        thickness = 9f,
        color = nearColor
    )

    // 5. 头部与尖耳（位于右侧，朝向 +X）
    val headCenter = Offset(68f, 41f + bodyBobY * 1.15f)
    drawCatHead(
        center = headCenter,
        palette = palette
    )
}

private fun DrawScope.drawCatLeg(
    pivot: Offset,
    angleDeg: Float,
    length: Float,
    thickness: Float,
    color: Color
) {
    rotate(degrees = angleDeg, pivot = pivot) {
        drawRoundRect(
            color = color,
            topLeft = Offset(pivot.x - thickness * 0.5f, pivot.y - thickness * 0.35f),
            size = Size(thickness, length),
            cornerRadius = CornerRadius(thickness * 0.5f, thickness * 0.5f)
        )
    }
}

private fun DrawScope.drawCatTail(
    start: Offset,
    swing: Float,
    color: Color
) {
    val tipX = start.x - 14f + swing * 4f
    val tipY = start.y - 20f + swing * 5f
    val ctrl1X = start.x - 11f
    val ctrl1Y = start.y - 6f
    val ctrl2X = start.x - 17f + swing * 2f
    val ctrl2Y = start.y - 16f

    val tailPath = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, tipX, tipY)
    }

    drawPath(
        path = tailPath,
        color = color,
        style = Stroke(width = 7.5f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCatHead(
    center: Offset,
    palette: WalkingCatPalette
) {
    // 左耳（靠后）
    val backEar = Path().apply {
        moveTo(center.x - 11f, center.y - 6f)
        lineTo(center.x - 13f, center.y - 20f)
        lineTo(center.x - 2f, center.y - 12f)
        close()
    }
    drawPath(path = backEar, color = palette.bodyColor)

    // 右耳（靠前）
    val frontEar = Path().apply {
        moveTo(center.x - 1f, center.y - 10f)
        lineTo(center.x + 4f, center.y - 22f)
        lineTo(center.x + 11f, center.y - 9f)
        close()
    }
    drawPath(path = frontEar, color = palette.bodyColor)

    // 前耳粉色内耳三角
    val innerEar = Path().apply {
        moveTo(center.x + 1.5f, center.y - 11f)
        lineTo(center.x + 4.5f, center.y - 18f)
        lineTo(center.x + 8.5f, center.y - 10.5f)
        close()
    }
    drawPath(path = innerEar, color = palette.earInnerColor)

    // 实心圆润猫头
    drawCircle(
        color = palette.bodyColor,
        radius = 14.5f,
        center = center
    )

    // 朝向右方的简约眼睛与小鼻子
    drawCircle(
        color = palette.eyeColor,
        radius = 2.1f,
        center = Offset(center.x + 6.5f, center.y - 1.5f)
    )
    drawCircle(
        color = palette.noseColor,
        radius = 1.6f,
        center = Offset(center.x + 12.2f, center.y + 2.2f)
    )
}

@Preview( widthDp = 240, heightDp = 240, name = "Light Theme")
@Composable
private fun WalkingCatLightPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        WalkingCatAnimation(
            modifier = Modifier.size(200.dp)
        )
    }
}

@Preview( widthDp = 240, heightDp = 240, name = "Dark Theme")
@Composable
private fun WalkingCatDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        WalkingCatAnimation(
            modifier = Modifier.size(200.dp)
        )
    }
}