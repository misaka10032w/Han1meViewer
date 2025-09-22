package com.yenaly.han1meviewer.ui.viewmodel

import android.util.Log
import androidx.annotation.IntDef
import com.yenaly.han1meviewer.logic.model.VideoComments

/**
 * 连通 [com.yenaly.han1meviewer.ui.fragment.home.preview.PreviewFragment] 和
 * [com.yenaly.han1meviewer.ui.fragment.home.preview.PreviewCommentFragment] 的预取器
 */
class PreviewCommentPrefetcher private constructor(
    private val commentViewModel: CommentViewModel
) {

    @IntDef(flag = true, value = [Scope.PREVIEW_ACTIVITY, Scope.PREVIEW_COMMENT_ACTIVITY])
    annotation class Scope {
        companion object {
            const val PREVIEW_ACTIVITY = 1
            const val PREVIEW_COMMENT_ACTIVITY = 1 shl 1
        }
    }

    companion object {
        private const val TAG = "PreviewCommentPrefetcher"

        private var prefetcher: PreviewCommentPrefetcher? = null

        fun here(viewModel: CommentViewModel): PreviewCommentPrefetcher {
            return prefetcher ?: PreviewCommentPrefetcher(viewModel).also { prefetcher = it }
        }

        fun bye(@Scope scope: Int) {
            prefetcher?.also {
                it.activityMask = it.activityMask and scope.inv()
                if (it.activityMask == 0) {
                    prefetcher = null
                    Log.i(TAG, "bye executed successfully")
                } else {
                    if (it.activityMask and Scope.PREVIEW_ACTIVITY != 0) {
                        Log.i(
                            TAG, "bye executed failed: " +
                                    "prefetcher is still alive cuz of PreviewActivity"
                        )
                    }
                    if (it.activityMask and Scope.PREVIEW_COMMENT_ACTIVITY != 0) {
                        Log.i(
                            TAG, "bye executed failed: " +
                                    "prefetcher is still alive cuz of PreviewCommentActivity"
                        )
                    }
                }
            }
        }
    }

    private var activityMask = 0

    val commentFlow get() = commentViewModel.videoCommentFlow

    fun tag(@Scope scope: Int) {
        activityMask = activityMask or scope
    }

    fun fetch(type: String, code: String) {
        commentViewModel.getComment(type, code)
    }

    fun update(comments: List<VideoComments.VideoComment>) {
        commentViewModel.updateComments(comments)
    }
}