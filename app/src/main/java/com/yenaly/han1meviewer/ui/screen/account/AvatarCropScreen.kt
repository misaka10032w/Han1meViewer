package com.yenaly.han1meviewer.ui.screen.account

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

private val AvatarCropBoxSize = 280.dp

@Composable
fun AvatarCropScreen(
    sourceUri: String,
    onBack: () -> Unit,
    onConfirm: (File) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageBitmap by produceState<Bitmap?>(initialValue = null, sourceUri) {
        value = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(sourceUri.toUri())?.use(BitmapFactory::decodeStream)
        }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var cropping by remember { mutableStateOf(false) }
    var containerWidthPxState by remember { mutableFloatStateOf(0f) }
    var containerHeightPxState by remember { mutableFloatStateOf(0f) }
    var cropBoxSizePxState by remember { mutableFloatStateOf(0f) }

    HanimeScaffold(
        title = stringResource(R.string.crop_avatar),
        onBack = onBack,
        actions = {
            val bitmap = imageBitmap
            Button(
                onClick = {
                    if (bitmap == null) return@Button
                    cropping = true
                    scope.launch {
                        val cropped = withContext(Dispatchers.IO) {
                            createCroppedAvatarFile(
                                cacheDir = context.cacheDir,
                                bitmap = bitmap,
                                scale = scale,
                                offset = offset,
                                containerWidthPx = containerWidthPxState,
                                containerHeightPx = containerHeightPxState,
                                cropBoxSizePx = cropBoxSizePxState,
                            )
                        }
                        cropping = false
                        if (cropped != null) {
                            onConfirm(cropped)
                        }
                    }
                },
                enabled = bitmap != null && !cropping,
            ) {
                if (cropping) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.confirm))
                }
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            containerWidthPxState = with(density) { maxWidth.toPx() }
            containerHeightPxState = with(density) { maxHeight.toPx() }
            cropBoxSizePxState = with(density) { AvatarCropBoxSize.toPx() }

            val bitmap = imageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sourceUri) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offset += pan
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
                Box(
                    modifier = Modifier
                        .size(AvatarCropBoxSize)
                        .border(2.dp, Color.White)
                        .background(Color.Transparent)
                        .align(Alignment.Center),
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

private fun createCroppedAvatarFile(
    cacheDir: File,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    containerWidthPx: Float,
    containerHeightPx: Float,
    cropBoxSizePx: Float,
): File? {
    return runCatching {
        val fitScale = min(
            containerWidthPx / bitmap.width.toFloat(),
            containerHeightPx / bitmap.height.toFloat(),
        )
        val transformedScale = fitScale * scale
        val displayedWidthPx = bitmap.width * transformedScale
        val displayedHeightPx = bitmap.height * transformedScale
        val imageLeftPx = (containerWidthPx - displayedWidthPx) / 2f + offset.x
        val imageTopPx = (containerHeightPx - displayedHeightPx) / 2f + offset.y

        val cropLeftOnScreen = (containerWidthPx - cropBoxSizePx) / 2f
        val cropTopOnScreen = (containerHeightPx - cropBoxSizePx) / 2f

        val left = ((cropLeftOnScreen - imageLeftPx) / transformedScale)
            .roundToInt()
            .coerceIn(0, bitmap.width - 1)
        val top = ((cropTopOnScreen - imageTopPx) / transformedScale)
            .roundToInt()
            .coerceIn(0, bitmap.height - 1)
        val cropSize = (cropBoxSizePx / transformedScale)
            .roundToInt()
            .coerceAtLeast(1)

        val safeCropSize = min(cropSize, min(bitmap.width - left, bitmap.height - top))
            .coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(bitmap, left, top, safeCropSize, safeCropSize)
        val output = cropped.scale(1024, 1024)
        val avatarFile = File(cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(avatarFile).use {
            output.compress(Bitmap.CompressFormat.JPEG, 92, it)
        }
        if (cropped != output) cropped.recycle()
        output.recycle()
        avatarFile
    }.getOrNull()
}
