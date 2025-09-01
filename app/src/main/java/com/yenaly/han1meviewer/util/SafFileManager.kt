package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.yenaly.han1meviewer.APP_NAME
import com.yenaly.han1meviewer.HFileManager.DEF_VIDEO_COVER_TYPE
import com.yenaly.han1meviewer.HFileManager.HANIME_DOWNLOAD_FOLDER
import com.yenaly.han1meviewer.HFileManager.createVideoCoverName
import com.yenaly.han1meviewer.HFileManager.getDownloadVideoCoverFile
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * SAF (Storage Access Framework)
 * @author misaka10032w
 * @time 2025/08/16
 * 提供通过Android存储访问框架(SAF)进行文件操作的封装，主要功能包括：
 * - 管理持久化的URI权限
 * - 创建和维护应用下载目录结构
 * - 提供视频和封面文件的读写操作
 * - 自动处理.nomedia文件防止媒体扫描
 * - 支持MIME类型自动识别
 *
 * 目录结构：
 * /选择的目录/
 *   └── [HANIME_DOWNLOAD_FOLDER]
 *       └── {videoCode}/  (单个视频存储目录)
 *           ├── video文件
 *           └── cover文件
 *
 * 注意：所有操作需要已获取有效的URI权限
 */
object SafFileManager {

    const val KEY_TREE_URI = "saf_download_path"
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "flv", "mov", "webm")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    private fun isSafReady(): Boolean = !Preferences.safDownloadPath.isNullOrBlank()
    fun buildOpenDirectoryIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra("android.provider.extra.SHOW_ADVANCED", true)
        }
    }

    /**
     * 持久化保存URI权限并将URI存储到SharedPreferences中。
     *
     * @param context 上下文对象，用于获取ContentResolver
     * @param data 包含URI数据的Intent对象，通常来自ActivityResult回调
     */
    fun persistUriPermission(context: Context, data: Intent?) {
        val treeUri = data?.data ?: return
        val contentResolver = context.contentResolver
        val flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        contentResolver.takePersistableUriPermission(treeUri, flags)
        Preferences.preferenceSp.edit {
            putString(KEY_TREE_URI, treeUri.toString())
        }
    }

    /**
     * 获取已保存的URI。
     *
     * @return 从Preferences中读取的URI字符串转换而成的Uri对象，若未保存则返回null
     */
    fun getSavedUri(): Uri? {
        val uriStr = Preferences.safDownloadPath
        return uriStr?.toUri()
    }

    /**
     * 获取应用下载目录（使用Storage Access Framework）。
     *
     * 1. 检查SAF是否已初始化（已授权URI）
     * 2. 从Preferences获取保存的URI并验证有效性
     * 3. 确保目录存在.nomedia文件
     *
     * @return 有效的下载目录DocumentFile对象，若未准备就绪或URI无效则返回null
     */
    fun getAppDownloadFolderSaf(context: Context): DocumentFile? {
        if (!isSafReady()) return null
        val uri = runCatching { Preferences.safDownloadPath?.toUri() }.getOrNull() ?: return null
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        if (!tree.isDirectory) return null
        tree.ensureNoMedia()
        return tree
    }

    /**
     * 获取指定视频代码对应的下载目录（自动创建嵌套目录结构）
     *
     * 目录结构：/SAF根目录/[HANIME_DOWNLOAD_FOLDER]/[videoCode]/
     * 会自动创建.nomedia文件
     *
     * @param videoCode 视频唯一标识码
     * @return 视频存储目录DocumentFile对象，若根目录无效或创建失败则返回null
     */
    fun getDownloadVideoFolderSaf(context: Context, videoCode: String): DocumentFile? {
        val root = getAppDownloadFolderSaf(context) ?: return null
        val hanime = root.ensureChildDir(HANIME_DOWNLOAD_FOLDER) ?: return null
        val videoDir = hanime.ensureChildDir(videoCode) ?: return null
        videoDir.ensureNoMedia()
        return videoDir
    }

    /**
     * 获取或创建视频封面文件DocumentFile对象
     *
     * @param videoCode 视频唯一标识码
     * @param title 视频标题（用于生成文件名）
     * @param suffix 封面文件后缀（默认[DEF_VIDEO_COVER_TYPE]）
     * @return 封面文件DocumentFile对象，若目录无效或创建失败则返回null
     */
    fun getDownloadVideoCoverDoc(
        context: Context,
        videoCode: String,
        title: String,
        suffix: String = DEF_VIDEO_COVER_TYPE
    ): DocumentFile? {
        val dir = getDownloadVideoFolderSaf(context, videoCode) ?: return null
        val name = createVideoCoverName(title, suffix)
        dir.findFile(name)?.let { return it }
        return dir.createFile(mimeForExt(suffix), name)
    }

    /**
     * 获取封面文件的输出流和URI（支持SAF和传统文件系统）
     *
     * @param videoCode 视频唯一标识码
     * @param title 视频标题
     * @param suffix 封面文件后缀（默认[DEF_VIDEO_COVER_TYPE]）
     * @return Pair包含输出流和URI，若创建失败则返回Pair(null, null)
     */
    fun openOutputStreamForCover(
        context: Context,
        videoCode: String,
        title: String,
        suffix: String = DEF_VIDEO_COVER_TYPE
    ): Pair<OutputStream?, Uri?> {
        return if (isSafReady()) {
            val doc = getDownloadVideoCoverDoc(context, videoCode, title, suffix) ?: return Pair(
                null,
                null
            )
            val os = context.contentResolver.openOutputStream(doc.uri, "rwt")
            Pair(os, doc.uri)
        } else {
            val file = getDownloadVideoCoverFile(context, videoCode, title, suffix)
            file.parentFile?.mkdirs()
            Pair(FileOutputStream(file), file.toUri())
        }
    }

    /**
     * 确保当前目录下存在指定名称的子目录（不存在则创建）。
     *
     * @param name 子目录名称
     * @return 存在的或新建的子目录DocumentFile对象，若已存在同名文件（非目录）则返回null
     */
    private fun DocumentFile.ensureChildDir(name: String): DocumentFile? {
        findFile(name)?.let { return if (it.isDirectory) it else null }
        return createDirectory(name)
    }

    /**
     * 确保当前目录下存在.nomedia文件（不存在则创建）
     */
    private fun DocumentFile.ensureNoMedia() {
        if (findFile(".nomedia") == null) {
            createFile("application/octet-stream", ".nomedia")
        }
    }

    /**
     * 获取视频文件的Uri（适用于SAF存储）
     *
     * @param videoCode 视频唯一标识码
     * @param fileName 目标文件名（包含扩展名）
     * @return 文件Uri，若使用私有存储或创建失败则返回null
     *
     * 目录结构：/SAF根目录/HANIME_DOWNLOAD_FOLDER/{videoCode}/{fileName}
     */
    fun getDownloadVideoFileUri(
        context: Context,
        videoCode: String,
        fileName: String
    ): Uri? {
        if (!isSafReady()) return null
        val treeUri = Preferences.safDownloadPath?.toUri() ?: return null
        val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val rootDir = docTree.findFile(HANIME_DOWNLOAD_FOLDER)
            ?: docTree.createDirectory(HANIME_DOWNLOAD_FOLDER)
            ?: return null
        val videoDir = rootDir.findFile(videoCode)
            ?: rootDir.createDirectory(videoCode)
            ?: return null
        val ext = fileName.substringAfterLast('.', "")
        val mimeType = mimeForExt(ext)
        val target = videoDir.findFile(fileName)
            ?: videoDir.createFile(mimeType, fileName)
            ?: return null

        return target.uri
    }

    /**
     * 根据文件扩展名获取对应的MIME类型
     *
     * @param ext 文件扩展名（不包含点）
     * @return 对应的MIME类型字符串，未知类型默认返回application/octet-stream
     */
    private fun mimeForExt(ext: String): String = when (ext.lowercase()) {
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }

    /**
     * 将私有下载目录的所有文件迁移到 SAF 自定义路径
     * @param context Context
     * @param dao 用于升级数据库中文件uri路径
     * @param onProgress 回调 (已迁移文件数, 总文件数)
     */
    fun migratePrivateToSaf(
        context: Context,
        dao: HanimeDownloadDao? = null,
        onProgress: ((migrated: Int, total: Int) -> Unit)? = null
    ) = CoroutineScope(Dispatchers.IO).launch {
        val privateFolder =
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APP_NAME)
        val hanimeDownloadFolder = File(privateFolder, HANIME_DOWNLOAD_FOLDER)

        if (!hanimeDownloadFolder.exists() || !hanimeDownloadFolder.isDirectory) {
            Log.e("Migrate", "hanime_download 文件夹不存在")
            return@launch
        }

        val treeUri = Preferences.safDownloadPath?.toUri()
        if (treeUri == null) {
            Log.e("Migrate", "SAF treeUri 为空")
            return@launch
        }

        val rootDocFile = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDocFile == null) {
            Log.e("Migrate", "无法获取 DocumentFile 根目录")
            return@launch
        }

        val hanimeDownloadDoc = rootDocFile.findFile(HANIME_DOWNLOAD_FOLDER)
            ?: rootDocFile.createDirectory(HANIME_DOWNLOAD_FOLDER)

        if (hanimeDownloadDoc == null) {
            Log.e("Migrate", "创建/获取 HANIME_DOWNLOAD_FOLDER 失败")
            return@launch
        }

        val folders = hanimeDownloadFolder.listFiles { it.isDirectory } ?: arrayOf()
        val total = folders.size
        var migrated = 0

        Log.d("Migrate", "开始迁移，总文件夹数: $total")

        for (folder in folders) {
            Log.d("Migrate", "正在迁移文件夹: ${folder.name}")

            // 检查目标文件夹是否已存在
            var folderDoc = hanimeDownloadDoc.findFile(folder.name)

            if (folderDoc == null) {
                folderDoc = hanimeDownloadDoc.createDirectory(folder.name)
                if (folderDoc == null) {
                    Log.e("Migrate", "创建视频文件夹失败: ${folder.name}")
                    continue
                }
            } else {
                folder.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val existingFile = folderDoc.findFile(file.name)
                        if (existingFile != null) {
                            existingFile.delete()
                            Log.d("Migrate", "删除冲突文件: ${file.name}")
                        }
                    }
                }
            }

            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    if (folderDoc.findFile(file.name) == null) {
                        val mimeType = mimeForExt(file.extension.lowercase())
                        val newFile = folderDoc.createFile(mimeType, file.name)
                        if (newFile == null) {
                            Log.e("Migrate", "创建文件失败: ${file.name}")
                            return@forEach
                        }

                        try {
                            file.inputStream().use { input ->
                                context.contentResolver.openOutputStream(newFile.uri)
                                    ?.use { output ->
                                        input.copyTo(output)
                                    }
                            }
                            Log.d("Migrate", "已迁移文件: ${file.name}")
                        } catch (e: Exception) {
                            Log.e("Migrate", "复制文件出错: ${file.name}", e)
                        }
                    } else {
                        Log.d("Migrate", "文件已存在，跳过: ${file.name}")
                    }
                }
            }

            // 删除原始文件夹及文件
            folder.deleteRecursively()

            migrated++
            Log.d("Migrate", "已完成文件夹: ${folder.name} ($migrated/$total)")

            withContext(Dispatchers.Main) {
                onProgress?.invoke(migrated, total)
            }
        }
        try {
            if (dao != null) {
                scanAndImportHanimeDownloads(context, dao)
            }
        } catch (e: Exception) {
            Log.e("saf", e.message.toString())
        }
        withContext(Dispatchers.Main) {
            onProgress?.invoke(migrated, total)
        }
        Log.d("Migrate", "迁移完成，总文件夹数: $total")
    }

    /**
     * 扫描自定义目录中的所有符合规则的视频文件夹并导入数据库
     * @param context Context
     * @param dao 用于升级数据库中文件uri路径
     */
    suspend fun scanAndImportHanimeDownloads(
        context: Context,
        dao: HanimeDownloadDao
    ) {
        val treeUri = Preferences.safDownloadPath?.toUri() ?: return
        val rootDocFile = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val hanimeDownloadDoc = rootDocFile.findFile(HANIME_DOWNLOAD_FOLDER) ?: return

        hanimeDownloadDoc.listFiles()
            .filter { it.isDirectory }
            .forEach { folderDoc ->
                val videoCode = folderDoc.name ?: return@forEach

                try {
                    // 加载元数据
                    val infoFile = folderDoc.findFile("info.json") ?: return@forEach

                    context.contentResolver.openInputStream(infoFile.uri)?.use { input ->
                        val json = input.bufferedReader().use { it.readText() }
                        val jsonObj =
                            kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject

                        val title = jsonObj["title"]?.jsonPrimitive?.content ?: ""
                        val coverUrl = jsonObj["coverUrl"]?.jsonPrimitive?.content ?: ""
                        val addDate = System.currentTimeMillis()
                        val videoUrls = jsonObj["videoUrls"]?.jsonObject

                        // 获取视频文件的 DocumentFile
                        val videoFile = folderDoc.listFiles()
                            .firstOrNull { file ->
                                file.isFile && file.name?.substringAfterLast('.', "")
                                    ?.lowercase() in VIDEO_EXTENSIONS
                            }
                        val pattern = """_(\d+P)\.mp4$""".toRegex()
                        val quality = pattern.find(videoFile?.name.toString())?.groupValues?.get(1)
                            ?: "unknow"
                        val videoUrl = videoUrls?.get(quality)
                            ?.jsonObject
                            ?.get("link")
                            ?.jsonPrimitive
                            ?.content

                        // 获取封面文件的 DocumentFile
                        val coverFile = folderDoc.listFiles()
                            .firstOrNull { file ->
                                file.isFile && file.name?.substringAfterLast('.', "")
                                    ?.lowercase() in IMAGE_EXTENSIONS
                            }

                        val videoUri = videoFile?.uri?.toString() ?: ""
                        val coverUri = coverFile?.uri?.toString()
                        val existing = dao.find(videoCode)
                        if (existing != null) {
                            val updated = existing.copy(
                                videoUri = videoUri,
                                coverUri = coverUri,
                                //                               quality = quality
                            )
                            dao.update(updated)
                            Log.d("ImportHanime", "已存在，更新 videoUri/coverUri: $videoCode")
                        } else {
                            val entity = HanimeDownloadEntity(
                                coverUrl = coverUrl,
                                coverUri = coverUri,
                                title = title,
                                addDate = addDate,
                                videoCode = videoCode,
                                videoUri = videoUri,
                                quality = quality,
                                videoUrl = videoUrl.toString(),
                                length = 0L,
                                downloadedLength = 0L,
                                state = DownloadState.Finished
                            )
                            dao.insert(entity)
                            Log.d("ImportHanime", "导入完成: $videoCode")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ImportHanime", "导入失败: $videoCode", e)
                }
            }
    }

    /**
     * 检查一个视频文件夹是否有效
     * @param folder 本地 File 文件夹
     * @param updateMetadata 传入一个方法，负责生成元数据
     * @return true 表示有效文件夹
     */
    fun isValidHanimeFolder(
        folder: File,
        updateMetadata: (videoCode: String) -> Unit
    ): Boolean {
        if (!folder.exists() || !folder.isDirectory) return false
        val hasVideo = folder.listFiles()?.any { file ->
            file.isFile && VIDEO_EXTENSIONS.contains(file.extension.lowercase())
        } ?: false

        if (!hasVideo) return false

        val videoCode = folder.name
        updateMetadata(videoCode)
        return true
    }

    /**
     * 升级缺少的元数据文件info.json
     * @param context context
     * @param videoCode 视频id
     * @param uri 视频目录
     * @return true 表示创建成功
     */
    fun updateMetadata(
        context: Context,
        videoCode: String,
        uri: Uri
    ): Boolean {
        //TODO 在做了在做了
        return true
    }

    /**
     * 检查当前设置的自定义目录权限
     * @param context context
     * @return true 表示创建成功
     */
    fun checkSafPermissions(context: Context): Boolean {
        val treeUri = Preferences.safDownloadPath?.toUri() ?: return false
        val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        return try {
            val testFile = docTree.createFile("text/plain", ".test_permission")
            val result = testFile != null
            testFile?.delete()
            result
        } catch (e: Exception) {
            Log.w("HFileMigrator", "SAF 权限检查失败", e)
            false
        }
    }
}