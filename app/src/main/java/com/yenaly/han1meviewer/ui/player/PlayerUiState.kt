package com.yenaly.han1meviewer.ui.player

/**
 * 播放器状态机的状态。
 *
 * 对照旧版 JZVD 的状态语义迁移：
 * [IDLE] ↔ `STATE_IDLE`、[PREPARING] ↔ `STATE_PREPARING`、
 * [BUFFERING] ↔ `STATE_PREPARING_PLAYING`、[PLAYING] ↔ `STATE_PLAYING`、
 * [PAUSED] ↔ `STATE_PAUSE`、[COMPLETED] ↔ `STATE_AUTO_COMPLETE`、[ERROR] ↔ `STATE_ERROR`。
 */
enum class PlayerState {

    /** 空闲：尚未设置媒体，或已重置。 */
    IDLE,

    /** 首次加载准备中。 */
    PREPARING,

    /** 播放中缓冲。 */
    BUFFERING,

    /** 播放中。 */
    PLAYING,

    /** 已暂停。 */
    PAUSED,

    /** 播放完毕。 */
    COMPLETED,

    /** 出错。 */
    ERROR,
}

/**
 * 播放器低频 UI 状态。
 *
 * 变化频率较低的整体状态，收集该流的重组开销可控；
 * 高频的进度数据拆分在 [PlayerProgress]，避免 100ms 全量重组。
 */
data class PlayerUiState(

    /** 当前状态机状态。 */
    val state: PlayerState = PlayerState.IDLE,

    /** 视频标题。 */
    val title: String = "",

    /** 可选清晰度列表（例如 1080P/720P…，顺序由高到低）。 */
    val qualities: List<String> = emptyList(),

    /** 当前清晰度在 [qualities] 中的下标。 */
    val currentQualityIndex: Int = 0,

    /** 当前倍速在 [PlayerDefaults.speedArray] 中的下标。 */
    val speedIndex: Int = PlayerDefaults.DEF_SPEED_INDEX,

    /** 是否单视频循环。 */
    val isLooping: Boolean = false,

    /** 手势是否锁定（锁定后禁用全部手势与控件，仅保留锁开关）。 */
    val isGestureLocked: Boolean = false,

    /** Anime4K 超分档位（0 关闭 / 1 性能 / 2 画质，仅 MPV 内核生效）。 */
    val superResolutionIndex: Int = PlayerDefaults.DEF_SUPER_RESOLUTION_INDEX,

    /** 视频像素宽（含像素宽高比换算），未就绪为 0。 */
    val videoWidth: Int = 0,

    /** 视频像素高，未就绪为 0。 */
    val videoHeight: Int = 0,

    /** 封面 URL，起播前展示。 */
    val posterUrl: String? = null,

    /** 是否已设置可用媒体（false 时中键点击提示无法获取链接）。 */
    val hasMedia: Boolean = false,

    /**
     * 总时长（ms），未知为 0。
     *
     * 由预加载在起播前回填，供底栏时长/进度条分母在起播前展示；
     * 起播后以 [PlayerProgress.durationMs] 为准。
     */
    val durationMs: Long = 0L,
)

/**
 * 播放器高频进度状态。
 *
 * 由控制器以约 100ms 周期刷新，供进度条、时间与关键帧倒计时收集。
 */
data class PlayerProgress(

    /** 当前播放位置（ms）。 */
    val positionMs: Long = 0L,

    /** 总时长（ms），未知为 0。 */
    val durationMs: Long = 0L,

    /** 已缓冲百分比（0..100）。 */
    val bufferedPercent: Int = 0,
)

/**
 * 播放器一次性事件，经 [com.yenaly.han1meviewer.ui.player.HanimePlayerController.events] 发射。
 */
sealed interface PlayerEvent {

    /** 非 WiFi 网络下首次点播，需要页面弹出流量确认框。 */
    data object RequestWifiConfirmation : PlayerEvent

    /**
     * 检测到观看历史进度并已恢复，提示「从头开始播放」。
     *
     * @property savedPositionMs 恢复到的历史位置（ms）
     */
    data class ShowResumeButton(val savedPositionMs: Long) : PlayerEvent
}
