package com.yenaly.han1meviewer.ui.fragment.settings

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframesScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.SettingsViewModel
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.decodeFromStringByBase64
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.textFromClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.concurrent.thread

class HKeyframesFragment : androidx.fragment.app.Fragment() {

    private val viewModel by activityViewModels<SettingsViewModel>()
    private val hKeyframesShareRegex = Regex(">>>(.+)<<<")

    override fun onStart() {
        super.onStart()
        (activity as? ToolbarHost)?.setupToolbar(
            getString(R.string.h_keyframe),
            canNavigateBack = true,
        )
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val items = viewModel.loadAllHKeyframes().flowWithLifecycle(lifecycle)
            HanimeTheme {
                items.collectAsStateWithLifecycle(initialValue = emptyList()).value.let { entities ->
                    HKeyframesScreen(
                        items = entities,
                        onOpenVideo = { videoCode ->
                            (activity as? com.yenaly.han1meviewer.ui.activity.MainActivity)?.showVideoDetailFragment(videoCode)
                        },
                        onDeleteEntity = { entity ->
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                DatabaseRepo.HKeyframe.delete(entity)
                            }
                        },
                        onUpdateEntityTitle = { entity, newTitle ->
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                DatabaseRepo.HKeyframe.update(entity.copy(title = newTitle))
                                launch(Dispatchers.Main) {
                                    showShortToast(R.string.modify_success)
                                }
                            }
                        },
                        onDeleteKeyframe = { videoCode, keyframe ->
                            viewModel.removeHKeyframe(videoCode, keyframe)
                            showShortToast(R.string.delete_success)
                        },
                        onUpdateKeyframe = { videoCode, oldKeyframe, newKeyframe ->
                            viewModel.modifyHKeyframe(videoCode, oldKeyframe, newKeyframe)
                            showShortToast(R.string.modify_success)
                        },
                        onCopyShareContent = {
                            it.copyToClipboard()
                            showShortToast(R.string.copy_to_clipboard)
                        },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addHKeyframes()
    }

    private fun addHKeyframes() {
        thread {
            textFromClipboard?.let { text ->
                val matchResult = hKeyframesShareRegex.find(text)
                if (matchResult != null) {
                    val (toBase64) = matchResult.destructured
                    val toJson = toBase64.decodeFromStringByBase64()
                    val entity = Json.decodeFromString<HKeyframeEntity>(toJson)
                    activity?.runOnUiThread {
                        requireContext().showAlertDialog {
                            setTitle(R.string.h_keyframes_shared_by_other_detected)
                            setMessage(
                                getString(
                                    R.string.shared_h_keyframe_detected_msg,
                                    entity.title,
                                    entity.videoCode,
                                    entity.keyframes.size,
                                ).trimIndent()
                            )
                            setPositiveButton(R.string.confirm) { _, _ ->
                                viewModel.insertHKeyframes(entity.copy(lastModifiedTime = System.currentTimeMillis()))
                            }
                            setNegativeButton(R.string.cancel, null)
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        showShortToast(R.string.h_keyframes_shared_by_other_not_detected)
                    }
                }
            } ?: activity?.runOnUiThread {
                showShortToast(R.string.h_keyframes_shared_by_other_not_detected)
            }
        }
    }
}
