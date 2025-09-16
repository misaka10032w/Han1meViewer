package com.yenaly.han1meviewer.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object AnimeShaders {
    const val SHADERS_DIRECTORY = "shaders"
    // 超分辨率滤镜 (质量) A
    val mpvSuperResolutionArray = arrayOf(
        "Anime4K_Clamp_Highlights.glsl",
        "Anime4K_Restore_CNN_VL.glsl",
        "Anime4K_Upscale_CNN_x2_VL.glsl",
        "Anime4K_AutoDownscalePre_x2.glsl",
        "Anime4K_AutoDownscalePre_x4.glsl",
        "Anime4K_Upscale_CNN_x2_M.glsl"
    )
    // 超分辨率滤镜 (效率) A+A
    val mpvSuperResolutionLiteArray = arrayOf(
        "Anime4K_Clamp_Highlights.glsl",
        "Anime4K_Restore_CNN_M.glsl",
        "Anime4K_Restore_CNN_S.glsl",
        "Anime4K_Upscale_CNN_x2_M.glsl",
        "Anime4K_AutoDownscalePre_x2.glsl",
        "Anime4K_AutoDownscalePre_x4.glsl",
        "Anime4K_Upscale_CNN_x2_S.glsl"
    )

    /**
     * 从 assets/shaders/ 复制所有文件到应用私有目录的 shaders/ 文件夹
     * @return 成功复制的文件数量，-1 表示出错
     */
    fun copyShaderAssets(context: Context): Int {
        try {
            val targetDir = File(context.filesDir, SHADERS_DIRECTORY)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val assetManager = context.assets
            val assetFiles = assetManager.list(SHADERS_DIRECTORY) ?: return 0

            var copiedCount = 0
            for (filename in assetFiles) {
                val targetFile = File(targetDir, filename)
                assetManager.open("$SHADERS_DIRECTORY/$filename").use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        copiedCount++
                    }
                }
            }
            return copiedCount
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        }
    }

    /**
     * 获取应用私有目录的 shaders/ 文件夹路径
     */
    fun getShader(context: Context, type: Int): String {
        val shadersDir = File(context.filesDir, SHADERS_DIRECTORY)
        if (!shadersDir.exists()) {
            throw IllegalStateException("Shader 文件夹不存在: $shadersDir")
        }

        val shaderFiles = when (type) {
            // 效率
            1 -> mpvSuperResolutionLiteArray
            // 质量
            2 -> mpvSuperResolutionArray
            else -> throw IllegalArgumentException("未知 Shader 类型: $type")
        }
        return shaderFiles.joinToString(separator = ":") { shaderFile ->
            File(shadersDir, shaderFile).absolutePath
        }
    }

}