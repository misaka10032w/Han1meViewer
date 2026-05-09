package com.yenaly.han1meviewer.ui.fragment.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.HAdvancedSearch
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import com.yenaly.han1meviewer.util.openVideo
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showShortToast
import java.io.Serializable

/**
 * 搜索 Fragment - Compose 化改造版
 * 承载 SearchScreen Composable，剥离 SmartRefreshLayout / StateLayout / HanimeSearchBar 等第三方库
 */
class SearchFragment : Fragment() {

    val viewModel: SearchViewModel by viewModels()

    /** 从导航参数解析传入的高级搜索配置 */
    private fun parseAdvancedSearchArgs(): Pair<String?, Map<String, String>?> {
        val raw = arguments?.get(ADVANCED_SEARCH_MAP) ?: return null to null
        return when (raw) {
            is Map<*, *> -> {
                val params = mutableMapOf<String, String>()
                var query: String? = null
                raw.forEach { (k, v) ->
                    val key = (k as? String)?.uppercase()
                    val value = v?.toString()
                    if (key != null && value != null) {
                        if (key == HAdvancedSearch.QUERY.name) query = value
                        params[key] = value
                    }
                }
                query to params.ifEmpty { null }
            }
            is String -> raw to mapOf(HAdvancedSearch.QUERY.name to raw)
            else -> null to null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val (initialQuery, advancedParams) = parseAdvancedSearchArgs()

        // 将高级搜索参数加载到 ViewModel
        if (advancedParams != null && savedInstanceState == null) {
            loadAdvancedSearchToViewModel(advancedParams)
        }

        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    SearchScreen(
                        viewModel = viewModel,
                        initialQuery = initialQuery,
                        onBack = { navigateBack() },
                        onOpenVideo = { videoCode -> openVideo(videoCode) },
                        onLongPressCopy = { videoCode, title ->
                            copyTextToClipboard(getHanimeShareText(title, videoCode))
                            showShortToast(R.string.copy_to_clipboard)
                        },
                        onOpenAdvancedSearch = { openAdvancedSearch() }
                    )
                }
            }
        }
    }

    /** 将高级搜索参数加载到 SearchViewModel */
    private fun loadAdvancedSearchToViewModel(params: Map<String, String>) {
        params.forEach { (key, value) ->
            try {
                when (HAdvancedSearch.valueOf(key.uppercase())) {
                    HAdvancedSearch.QUERY -> viewModel.query = value
                    HAdvancedSearch.GENRE -> viewModel.genre = value
                    HAdvancedSearch.SORT -> viewModel.sort = value
                    HAdvancedSearch.YEAR -> viewModel.year = value.toIntOrNull()
                    HAdvancedSearch.MONTH -> viewModel.month = value.toIntOrNull()
                    HAdvancedSearch.DURATION -> viewModel.duration = value
                    HAdvancedSearch.TAGS -> {
                        // 简单标签参数处理
                    }
                    HAdvancedSearch.BRANDS -> {
                        // 简单品牌参数处理
                    }
                }
            } catch (_: Exception) {
                Log.w("SearchFragment", "Unknown advanced search key: $key")
            }
        }
    }

    /** 返回到上一页 */
    private fun navigateBack() {
        findNavController().navigateUp()
    }

    /** 打开高级搜索面板（复用现有 SearchOptionsPopupFragment） */
    private fun openAdvancedSearch() {
        try {
            val popup = SearchOptionsPopupFragment()
            popup.show(childFragmentManager, "search_options")
        } catch (_: Exception) {
            showShortToast("高级搜索暂不可用")
        }
    }
}
