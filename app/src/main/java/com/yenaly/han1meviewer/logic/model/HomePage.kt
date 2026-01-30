package com.yenaly.han1meviewer.logic.model

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:45
 */
data class HomePage(
    val csrfToken: String?,
    val avatarUrl: String?,
    val username: String?,
    val banner: Banner?,
    val latestHanime: MutableList<HanimeInfo>,
    val latestRelease: MutableList<HanimeInfo>,
    val ecchiAnime: MutableList<HanimeInfo>,
    val shortEpisodeAnime: MutableList<HanimeInfo>,
    val twoPointFiveDAnime: MutableList<HanimeInfo>,
    val threeDCG: MutableList<HanimeInfo>,
    val motionAnime: MutableList<HanimeInfo>,
    val twoDAnime: MutableList<HanimeInfo>,
    val aiGenerated: MutableList<HanimeInfo>,
    val mmd: MutableList<HanimeInfo>,
    val cosplay: MutableList<HanimeInfo>,
    val watchingNow: MutableList<HanimeInfo>,
    val newAnimeTrailer: MutableList<HanimeInfo>,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String
) {
    data class Banner(
        val title: String,
        val description: String?,
        val picUrl: String,
        // 目前网站的策略是，先你吗加载广告，然后再让你跳转
        // 我不敢保证他会把 videoCode 放在哪里，所以暂时可以为空
        val videoCode: String?,
    )
}