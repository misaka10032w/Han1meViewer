package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @project Han1meViewer
 * @author Yenaly Liew - 创建 (2022/08/02)
 * 初始版本
 * @author Misaka10032w - 更新 (2025/11/27)
 * 实现分组展示和展开/折叠功能
 * 实现分组移动、重命名等
 */
class DownloadViewModel(application: Application) : YenalyViewModel(application) {

    private val _downloaded = MutableStateFlow(mutableListOf<VideoWithCategories>())
    val downloaded = _downloaded.asStateFlow()

    /**
     * 下载分组列表的状态流
     *
     * 观察数据库中的下载分组变化，用于驱动分组管理UI的更新。
     *
     * @return StateFlow 包含所有下载分组实体列表，初始为空列表
     */
    val downloadedGroups: StateFlow<List<DownloadGroupEntity>> =
        DatabaseRepo.HanimeDownload.getAllGroups()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                e.printStackTrace()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun loadAllDownloadingHanime() =
        DatabaseRepo.HanimeDownload.loadAllDownloadingHanime()
            .catch { e -> e.printStackTrace() }
            .flowOn(Dispatchers.IO)

    fun loadAllDownloadedHanime(
        sortedBy: HanimeDownloadEntity.SortedBy = HanimeDownloadEntity.SortedBy.ID,
        ascending: Boolean = false,
    ) {
        viewModelScope.launch {
            DatabaseRepo.HanimeDownload.loadAllDownloadedHanime(sortedBy, ascending)
                .catch { e -> e.printStackTrace() }
                .flowOn(Dispatchers.IO)
                .collect {
                    _downloaded.value = it
                }
        }
    }

    /**
     * 更新视频所属的分组
     *
     * 将指定视频移动到新的下载分组中
     *
     * @param videoCode 视频唯一标识码
     * @param selectedGroupId 目标分组的ID
     */
    fun updateVideoGroup(videoCode: String, selectedGroupId: Int) {
        viewModelScope.launch {
            DatabaseRepo.HanimeDownload.updateVideoGroup(videoCode, selectedGroupId)
        }
    }

    /**
     * 创建新的下载分组
     *
     * 在数据库中创建一个新的空分组
     *
     * @param groupName 新分组的名称，不能为空或空白字符串
     */
    fun createNewGroup(groupName: String){
        viewModelScope.launch {
            DatabaseRepo.HanimeDownload.createNewGroup(groupName)
        }
    }

    /**
     * 更新下载分组的名称
     *
     * 修改指定分组的显示名称，如果分组不存在或操作失败会静默处理错误
     *
     * @param groupId 要更新的分组ID
     * @param newName 新的分组名称，不能为空或空白字符串
     *
     * @sample
     * // 将分组ID为1的名称改为"收藏视频"
     * updateGroupName(1, "收藏视频")
     */
    fun updateGroupName(groupId: Int, newName: String){
        viewModelScope.launch {
            try {
                val oldGroupName = DatabaseRepo.HanimeDownload.getGroupById(groupId)
                if (oldGroupName != null){
                    val updatedGroup = oldGroupName.copy(name = newName)
                    DatabaseRepo.HanimeDownload.updateGroup(updatedGroup)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
    fun deleteGroup(group: DownloadGroupEntity){
        viewModelScope.launch {
            DatabaseRepo.HanimeDownload.deleteGroup(group)
        }
    }

    fun updateDownloadHanime(entity: HanimeDownloadEntity) {
        viewModelScope.launch {
            DatabaseRepo.HanimeDownload.update(entity)
        }
    }

    fun deleteDownloadHanimeBy(videoCode: String, quality: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HanimeDownload.delete(videoCode, quality)
        }
    }
}