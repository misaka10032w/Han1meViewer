package com.yenaly.han1meviewer.logic.network.service

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/09/19 019 17:44
 */
interface HanimeCommentService {
    @GET("loadComment")
    suspend fun getComments(
        @Query("type") type: String, // 類似 "video", "preview"
        @Query("id") code: String,
        // 2025-11 改版：评论分批加载（每批 100 条），首屏之後帶 offset 翻頁
        @Query("offset") offset: Int? = null,
    ): Response<ResponseBody>

    @GET("loadReplies")
    suspend fun getCommentReply(
        @Query("id") commentId: String,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("createComment")
    suspend fun postComment(
        @Field("_token") csrfToken: String?,
        @Field("comment-user-id") currentUserId: String,
        @Field("comment-type") type: String, // 類似 "video", "preview"
        @Field("comment-foreign-id") targetUserId: String,
        @Field("comment-text") text: String,
        @Field("comment-count") count: Int = 1, // 感觉没什么用，仅前端用
        // 2025-11 改版後網站已移除 comment-is-political 欄位
        @Header("X-CSRF-TOKEN") csrfToken_1: String? = csrfToken,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("replyComment")
    suspend fun postCommentReply(
        @Field("_token") csrfToken: String?,
        @Field("reply-comment-id") replyCommentId: String,
        @Field("reply-comment-text") text: String,
        @Header("X-CSRF-TOKEN") csrfToken_1: String? = csrfToken,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("commentLike")
    suspend fun likeComment(
        @Field("_token") csrfToken: String?,
        @Field("foreign_type") foreignType: String,
        @Field("foreign_id") foreignId: String?,
        @Field("is_positive") isPositive: Int, // 你選擇的是讚還是踩，1是讚，0是踩
        @Field("comment-likes-count") commentLikesCount: Int,
        @Field("comment-likes-sum") commentLikesSum: Int,
        @Field("like-comment-status") likeCommentStatus: Int, // 你之前有沒有點過讚，1是0否
        @Field("unlike-comment-status") unlikeCommentStatus: Int, // 你之前有沒有點過踩，1是0否
        // 2025-11 改版後網站已移除 comment-like-user-id 欄位
        @Header("X-CSRF-TOKEN") csrfToken_1: String? = csrfToken,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("user/{userId}/report")
    suspend fun submitReport(
        @Path("userId") userId: String?,
        @Field("_token") csrfToken: String?,
        @Field("redirect-url") redirectUrl: String,
        @Field("reportable-id") reportableId: String?,
        @Field("reportable-type") reportableType: String?,
        @Field("reason") reason: String,
        @Header("X-CSRF-TOKEN") csrfToken_1: String? = csrfToken
    ): Response<ResponseBody>
}