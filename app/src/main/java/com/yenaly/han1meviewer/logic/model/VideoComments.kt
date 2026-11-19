package com.yenaly.han1meviewer.logic.model

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/20 020 21:56
 */
data class VideoComments(
    val videoComment: MutableList<VideoComment>,
    val currentUserId: String? = null,
    val csrfToken: String? = null,
    // 2025-11 改版：评论分批加载（每批 100 条）。
    // 首屏响应尾部带 #comment-scroll-sentinel（data-has-more / data-next-offset）；
    // 后续带 offset 的请求返回 {items_html, has_more, next_offset}。
    val hasMoreComments: Boolean = false,
    val nextCommentOffset: Int? = null,
) {
    data class VideoComment(
        // 頭像
        val avatar: String,
        // 作者
        val username: String,
        // 發佈日期
        val date: String,
        // 内容
        val content: String,
        // 點讚數量，登入前不能憑藉post獲取
        var thumbUp: Int? = null,
        // 是否为子评论
        val isChildComment: Boolean,
        // 是否有更多回覆
        val hasMoreReplies: Boolean = false,
        //更多回复数量
        val replyCount: Int? = 0,
        // 評論id，登入前不能憑藉post獲取
        val id: String? = "-1",
        // post 相關
        val post: POST,
        //举报相关
        val redirectUrl: String? = null,
        val reportableId: String? = null,
        val reportableType: String? = null
    ) {

        val replyTargetIdOrNull get() = post.foreignId ?: id
        val stableKey get() = replyTargetIdOrNull
            ?: reportableId
            ?: buildString {
                append(username)
                append('|')
                append(date)
                append('|')
                append(content.hashCode())
            }
        val realLikesCount get() = thumbUp
        fun incLikesCount(cancel: Boolean = false): VideoComment {
            return thumbUp?.let {
                copy(
                    thumbUp = it + if (cancel) -1 else 1,
                    post = post.copy(
                        likeCommentStatus = !cancel,
                        unlikeCommentStatus = false
                    )
                )
            } ?: this
        }

        fun decLikesCount(cancel: Boolean = false): VideoComment {
            return thumbUp?.let {
                copy(
                    thumbUp = it - if (cancel) -1 else 1,
                    post = post.copy(
                        likeCommentStatus = false,
                        unlikeCommentStatus = !cancel
                    )
                )
            } ?: this
        }

        data class POST(
            // 對方id
            val foreignId: String? = null,
            // 你點的
            var isPositive: Boolean = false,
            var commentLikesCount: Int? = null,
            var commentLikesSum: Int? = null,
            // 你之前有沒有點過讚
            var likeCommentStatus: Boolean = false,
            // 你之前有沒有點過踩
            var unlikeCommentStatus: Boolean = false,
        )
    }
}

/**
 * 用於 評論交互 Flow 的返回值
 */
data class VideoCommentArgs(
    // 當前評論所處adapter位置
    val commentPosition: Int,
    // 你當前點擊的是讚還是踩，和comment裏的isPositive不一樣
    val isPositive: Boolean,
    val comment: VideoComments.VideoComment,
)

/**
 * createComment / replyComment 的響應結果。
 *
 * 2025-11 改版後網站在響應裏直接返回新評論的渲染 HTML（single_video_comment）
 * 與新的評論總數（comment_count），網頁前端是本地插入、不重新拉取列表的；
 * App 同樣處理：評論超過一批（100 條）時新評論不一定在第一頁，盲目刷新反而看不到。
 */
data class PostCommentResult(
    // 解析出來的新評論；響應結構變化等解析失敗時為 null（上層退回刷新邏輯）
    val comment: VideoComments.VideoComment? = null,
    // 新的評論總數（僅 createComment 返回）
    val commentCount: Int? = null,
    // 回覆所屬的父評論 id（僅 replyComment 返回，等於響應裏的 comment_id）
    val parentCommentId: String? = null,
)