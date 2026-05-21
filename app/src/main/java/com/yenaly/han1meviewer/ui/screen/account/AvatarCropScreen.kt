package com.yenaly.han1meviewer.ui.screen.account

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cn.mucute.compose.avatar.cropper.AvatarCropper
import cn.mucute.compose.avatar.cropper.CropShape
import cn.mucute.compose.avatar.cropper.rememberCropState
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvatarCropScreen(
    sourceUri: String,
    onBack: () -> Unit,
    onConfirm: (File) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cropState = rememberCropState()

    var originalImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                val uri = sourceUri.toUri()
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                originalImageBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onBack() }
            }
        }
    }
    HanimeScaffold(
        title = stringResource(R.string.crop_avatar),
        onBack = onBack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            originalImageBitmap?.let { bitmap ->
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarCropper(
                            imageBitmap = bitmap,
                            state = cropState,
                            shape = CropShape.Square,
                            modifier = Modifier.fillMaxSize(),
                            backgroundColor = MaterialTheme.colorScheme.background
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onBack,
                            enabled = !isProcessing
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                if (isProcessing) return@Button
                                isProcessing = true
                                scope.launch {
                                    val croppedResult = cropState.crop(bitmap)

                                    val file = withContext(Dispatchers.IO) {
                                        saveImageBitmapToFile(context, croppedResult!!)
                                    }

                                    if (file != null) {
                                        onConfirm(file)
                                    } else {
                                        isProcessing = false
                                    }
                                }
                            },
                            enabled = !isProcessing
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }

        }
    }
}

private fun saveImageBitmapToFile(
    context: android.content.Context,
    imageBitmap: ImageBitmap
): File? {
    return try {
        val bitmap = imageBitmap.asAndroidBitmap()
        val cacheDir = context.cacheDir
        val avatarFile = File(cacheDir, "avatar_${System.currentTimeMillis()}.jpg")

        FileOutputStream(avatarFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        avatarFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
