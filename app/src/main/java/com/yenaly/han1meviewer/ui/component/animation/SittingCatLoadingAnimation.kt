package com.yenaly.han1meviewer.ui.component.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun SittingCatLoadingAnimation(
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