package com.yenaly.han1meviewer.ui.view.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JZDataSource
import cn.jzvd.JZMediaInterface
import cn.jzvd.JZUtils
import cn.jzvd.JzvdStd
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.itxca.spannablex.spannable
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.adapter.HKeyframeRvAdapter
import com.yenaly.han1meviewer.ui.adapter.SuperResolutionAdapter
import com.yenaly.han1meviewer.ui.adapter.VideoSpeedAdapter
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
import com.yenaly.han1meviewer.util.setStateViewLayout
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.OrientationManager
import com.yenaly.yenaly_libs.utils.appScreenWidth
import com.yenaly.yenaly_libs.utils.navBarHeight
import com.yenaly.yenaly_libs.utils.statusBarHeight
import com.yenaly.yenaly_libs.utils.unsafeLazy
import com.yenaly.yenaly_libs.utils.view.removeItself
import java.util.Timer
import kotlin.math.absoluteValue

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/18 018 15:54
 */
class HJzvdStd @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : JzvdStd(context, attrs), OnLongClickListener {
    interface FullscreenListener {
        fun onFullscreenChanged(isFullscreen: Boolean)
    }
    var fullscreenListener: FullscreenListener? = null
    companion object {
        // 相當於重寫了
        /**
         * 滑动操作的阈值
         */
        const val THRESHOLD = 10

        // 相當於重寫了
        /**
         * 默認滑動調整進度條的靈敏度 越大播放进度条滑动越慢
         */
        const val DEF_PROGRESS_SLIDE_SENSITIVITY = 5

        const val DEF_COUNTDOWN_SEC = 10

        /**
         * 默認速度
         */
        const val DEF_SPEED = 1.0F

        /**
         * 默認速度的索引
         */
        const val DEF_SPEED_INDEX = 2

        /**
         * 默認長按速度是原先速度的幾倍
         */
        const val DEF_LONG_PRESS_SPEED_TIMES = 2.5F

        /**
         * 速度列表
         */
        val speedArray = floatArrayOf(
            0.5F, 0.75F,
            1.0F, 1.25F, 1.5F, 1.75F,
            2.0F, 2.25F, 2.5F, 2.75F,
            3.0F,
        )

        /**
         * 速度列表的字符串
         */
        val speedStringArray = Array(speedArray.size) { "${speedArray[it]}x" }
        const val DEF_SUPER_RESOLUTION_INDEX = 0
    }

    init {
        gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (state == STATE_PLAYING || state == STATE_PAUSE) {
                        Log.d(TAG, "doubleClick [" + this.hashCode() + "] ")
                        startButton.performClick()
                    }
                    return super.onDoubleTap(e)
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (!mChangeBrightness && !mChangeVolume) {
                        onClickUiToggle()
                    }
                    return super.onSingleTapConfirmed(e)
                }
            })
    }

    /**
     * 用戶定義的是否顯示底部進度條
     */
    private val showBottomProgress = Preferences.showBottomProgress

    /**
     * 用戶定義的默認速度
     */
    private val userDefSpeed = Preferences.playerSpeed

    /**
     * 用戶定義的默認速度的索引
     */
    private val userDefSpeedIndex = speedArray.indexOfFirst { it == userDefSpeed }

    /**
     * 用戶定義的滑動調整進度條的靈敏度
     */
    private val userDefSlideSensitivity = Preferences.slideSensitivity.toRealSensitivity()

    /**
     * 用戶定義的默認長按速度是原先速度的幾倍
     */
    private val userDefLongPressSpeedTimes = Preferences.longPressSpeedTime

    /**
     * 用戶定義的倒數提醒毫秒數
     */
    private val userDefWhenCountdownRemind = Preferences.whenCountdownRemind

    /**
     * 用戶定義的是否在倒數時顯示評論
     */
    private val userDefShowCommentWhenCountdown = Preferences.showCommentWhenCountdown

    /**
     * 用戶定義的是否啟用關鍵H幀
     */
    private val isHKeyframeEnabled = Preferences.hKeyframesEnable

    /**
     * 當前速度的索引，如果设置速度的话，修改这个，别动 [videoSpeed]
     */
    private var currentSpeedIndex = userDefSpeedIndex
        @SuppressLint("SetTextI18n")
        set(value) {
            field = value

            val updateText = {
                tvSpeed.text = if (value == DEF_SPEED_INDEX) {
                    context.getString(R.string.speed)
                } else {
                    speedStringArray[value]
                }
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                updateText()
            } else {
                tvSpeed.post(updateText)
            }

            videoSpeed = speedArray[value]
            // #issue-14: 有些机器到这里可能会报空指针异常，所以加了个判断，但是不知道为什么会报空指针异常
            if (jzDataSource.objects == null) {
                jzDataSource.objects = arrayOf(userDefSpeedIndex)
            }
            jzDataSource.objects[0] = value
        }
    fun getSuperResolutionArray(): Array<String> = arrayOf(
        context.getString(R.string.super_resolution_off),
        context.getString(R.string.super_resolution_performance),
        context.getString(R.string.super_resolution_quality)
    )
    private var superResolutionIndex = 0
        set(value) {
            field = value
            if (value != DEF_SUPER_RESOLUTION_INDEX) {
                superResolution.text = getSuperResolutionArray()[value]
            } else {
                superResolution.text = context.getString(R.string.anime_4k)
            }
            if (mediaInterface is MpvMediaKernel) {
                val kernel = mediaInterface as MpvMediaKernel
                kernel.setSuperResolution(value)
            }
        }

    private lateinit var tvSpeed: TextView
    private lateinit var tvKeyframe: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnGoHome: ImageView
    private lateinit var topBarContainer: LinearLayout
    private lateinit var layoutTop: View
    private lateinit var layoutBottom: View
    private lateinit var gestureLock: ImageView
    var gestureLocked = false
    var savedProgress: Long = 0L
    private lateinit var btnResumeProgress: MaterialButton
    private val handler = Handler(Looper.getMainLooper())
    private val hideResumeBtnRunnable  = Runnable {
        btnResumeProgress.visibility = GONE
    }
    private var hasRestoredProgress  = false
    lateinit var orientationManager: OrientationManager
    private lateinit var superResolution: TextView

    var hKeyframe: HKeyframeEntity? = null
        set(value) {
            field = value
            hKeyframeAdapter.submitList(value?.keyframes)
            hKeyframeAdapter.isLocal = value?.let { it.author == null } ?: true
        }

    var videoCode: String? = null

    private val hKeyframeAdapter: HKeyframeRvAdapter by unsafeLazy { initHKeyframeAdapter() }
    private val switchPlayerKernel = Preferences.switchPlayerKernel
    var onVideoStateChanged: ((state: Int) -> Unit)? = null

    /**
     * 初始化關鍵H幀的 Adapter，最好不用 lazy
     *
     * 但我還是最終用了 lazy，要不然首次 submitList 收不到
     */
    private fun initHKeyframeAdapter() = run {
        val videoCode = checkNotNull(this.videoCode) {
            "If you want to use HKeyframeAdapter, you must set videoCode first."
        }
        HKeyframeRvAdapter(videoCode).apply {
            setOnItemClickListener { _, _, position ->
                val keyframe = getItem(position)
                mediaInterface?.seekTo(keyframe.position)
                startProgressTimer()
            }
        }
    }
    private fun isNeedResumeProgress(): Boolean {
        return savedProgress > 5000 && Preferences.allowResumePlayback && !hasRestoredProgress
    }

    /**
     * 關鍵H幀的點擊事件
     *
     * 作用：打開 Dialog，顯示關鍵H幀的列表
     */
    var onKeyframeClickListener: ((View) -> Unit)? = null

    /**
     * 回到主頁的點擊事件
     *
     * 作用：關閉所有的 VideoActivity
     */
    var onGoHomeClickListener: ((View) -> Unit)? = null

    /**
     * 關鍵H幀的長按事件
     *
     * 作用：將當前時刻加入關鍵H幀
     */
    var onKeyframeLongClickListener: ((View) -> Unit)? = null

    private var videoSpeed: Float = userDefSpeed
        set(value) {
            field = value
            mediaInterface?.let { mi ->
                val isPlaying = mi.isPlaying
                mi.setSpeed(value)
                if (!isPlaying) {
                    mi.pause()
                }
            }
        }

    /**
     * 是否觸發了長按快進
     */
    @Volatile
    private var isSpeedGestureDetected = false
    private var screenBrightnessBK = -1f
    private var isAdjustBrightness = false
    /**
     * 長按快進檢測
     */
    // #issue-20: 长按倍速功能添加
    private val speedGestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val mi: JZMediaInterface? = mediaInterface
                        if (mi != null && mi.isPlaying) {
                            setSpeedInternal(videoSpeed * userDefLongPressSpeedTimes)
                            textureViewContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            isSpeedGestureDetected = true
                        }
                    }
                }
            }
        })

    override fun getLayoutId() = R.layout.layout_jzvd_with_speed

    override fun init(context: Context?) {
        super.init(context)
        SAVE_PROGRESS = false
        tvSpeed = findViewById(R.id.tv_speed)
        superResolution = findViewById(R.id.super_resolution)
        tvKeyframe = findViewById(R.id.tv_keyframe)
        tvTimer = findViewById(R.id.tv_timer)
        btnGoHome = findViewById(R.id.go_home)
        layoutTop = findViewById(R.id.layout_top)
        layoutBottom = findViewById(R.id.layout_bottom)
        btnResumeProgress = findViewById(R.id.btn_resume_progress)
        topBarContainer = findViewById(R.id.top_bar_container)
        gestureLock = findViewById(R.id.lock)
        textureViewContainer.isHapticFeedbackEnabled = true
        tvSpeed.setOnClickListener(this)
        tvKeyframe.setOnClickListener(this)
        tvKeyframe.setOnLongClickListener(this)
        btnGoHome.setOnClickListener(this)
        superResolution.setOnClickListener(this)
        btnResumeProgress.setOnClickListener {
            hasRestoredProgress = true
            mediaInterface.seekTo(0L)
            btnResumeProgress.visibility = GONE
        }

        fullscreenButton.setOnClickListener {
            (context as? FragmentActivity)
                ?.supportFragmentManager
                ?.fragments
                ?.filterIsInstance<VideoFragment>()
                ?.firstOrNull()
            if (screen == SCREEN_FULLSCREEN) {
                gotoNormalScreen()
            } else {
                gotoFullscreen()
            }
        }
        gestureLock.setOnClickListener {
            gestureLocked = !gestureLocked
        }

    }

    override fun setUp(jzDataSource: JZDataSource?, screen: Int) {
        super.setUp(jzDataSource, screen, ExoMediaKernel::class.java)
    }

    fun setUp(jzDataSource: JZDataSource?, screen: Int, kernel: HMediaKernel.Type) {
        setUp(jzDataSource, screen, kernel.clazz)
    }
    fun setControlsVisible(visible: Boolean) {
        findViewById<View>(R.id.tv_speed)?.isVisible = visible
        findViewById<View>(R.id.tv_keyframe)?.isVisible = visible
        findViewById<View>(R.id.tv_timer)?.isVisible = visible
        findViewById<View>(R.id.go_home)?.isVisible = visible
        findViewById<View>(R.id.layout_top)?.isVisible = visible
        findViewById<View>(R.id.layout_bottom)?.isVisible = visible
    }
    override fun setUp(jzDataSource: JZDataSource?, screen: Int, clazz: Class<*>) {
        super.setUp(jzDataSource, screen, clazz)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val preferredQuality = prefs.getString("default_video_quality", null)
        if (Preferences.disableMobileDataWarning){
            WIFI_TIP_DIALOG_SHOWED = true
        }
        if (!preferredQuality.isNullOrBlank()) {
            val index = jzDataSource?.urlsMap?.keys?.indexOf(preferredQuality)
            if (index != -1) {
                if (jzDataSource != null) {
                    if (index != null) {
                        jzDataSource.currentUrlIndex = index
                    }
                }
            } else {
                Log.w("CustomJzvdStd-Settings", "清晰度 $preferredQuality 不可用，使用默认清晰度")
            }
        }
        Log.d("CustomJzvdStd-Settings", buildString {
            append("default_video_quality: ")
            appendLine(preferredQuality)
            append("showBottomProgress: ")
            appendLine(showBottomProgress)
            append("userDefSpeed: ")
            appendLine(userDefSpeed)
            append("userDefSpeedIndex: ")
            appendLine(userDefSpeedIndex)
            append("userDefSlideSensitivity: ")
            appendLine(userDefSlideSensitivity)
        })
        titleTextView.isInvisible = true
        if (bottomProgressBar != null && !showBottomProgress) {
            bottomProgressBar.removeItself()
            bottomProgressBar = ProgressBar(context)
        }
        screenBrightnessBK = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ).toFloat() / 255f
        } catch (_: SettingNotFoundException) {
            -1f
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (v.id) {
            R.id.surface_container -> {
                speedGestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        if (isSpeedGestureDetected) {
                            setSpeedInternal(videoSpeed)
                            isSpeedGestureDetected = false
                        }
                    }
                }
            }
        }
        return super.onTouch(v, event)
    }

    fun autoFullscreen(orientation: OrientationManager.ScreenOrientation) {
        autoFullscreen(if (orientation === OrientationManager.ScreenOrientation.LANDSCAPE) 1.0f else -1.0f)
    }

    override fun onClickUiToggle() {
        if (!bottomContainer.isVisible) {
            setSystemTimeAndBattery()
            clarity.text = jzDataSource.currentKey.toString()
        }
        when (state) {
            STATE_PREPARING -> {
                changeUiToPreparing()
                if (!bottomContainer.isVisible) {
                    setSystemTimeAndBattery()
                }
            }

            STATE_PLAYING -> {
                if (gestureLocked) {
                    post {
                        gestureLock.isVisible = !gestureLock.isVisible
                    }
                    changeUiToPlayingClearSafe()
                } else {
                    if (bottomContainer.isVisible) {
                        post {
                            gestureLock.isVisible = false
                        }
                        changeUiToPlayingClearSafe()
                    } else {
                        post {
                            gestureLock.isVisible = true
                        }
                        changeUiToPlayingShowSafe()
                    }
                }
            }

            STATE_PAUSE -> {
                if (bottomContainer.isVisible) {
                    changeUiToPauseClear()
                } else {
                    changeUiToPauseShow()
                }
            }

            STATE_PREPARING_PLAYING -> {
                if (bottomContainer.isVisible) {
                    changeUiToPreparingPlayingClear()
                } else {
                    changeUiToPreparingPlayingShow()
                }
            }
        }
    }

    override fun onStatePreparingPlaying() {
        super.onStatePreparingPlaying()
        if (jzDataSource.objects == null) {
            jzDataSource.objects = arrayOf(userDefSpeedIndex)
            currentSpeedIndex = userDefSpeedIndex
        } else {
            currentSpeedIndex = jzDataSource.objects.first() as Int
        }
    }

    // #issue-232: 快进滑动一加载就会出现操作栏，很影响观看体验
    override fun changeUIToPreparingPlaying() {
        when (screen) {
            SCREEN_FULLSCREEN -> {
                setAllControlsVisiblitySafe(
                    INVISIBLE, INVISIBLE, INVISIBLE,
                    VISIBLE, INVISIBLE, INVISIBLE, INVISIBLE
                )
                updateStartImage()
            }
        }
    }

    override fun setScreenNormal() {
        super.setScreenNormal()
        updateVideoPlayerSize(false)
        backButton.isVisible = true
        tvSpeed.isVisible = false
        tvKeyframe.isVisible = false
        titleTextView.isInvisible = true
        tvTimer.isInvisible = true
        btnGoHome.isVisible = true
        topBarContainer.isVisible = false
        superResolution.isVisible = false
        gestureLock.isVisible = false

        layoutTop.updatePadding(left = 0, right = 0)
        layoutBottom.updatePadding(left = 0, right = 0)
        tvTimer.updatePadding(left = 0, right = 0)
        bottomProgressBar.updatePadding(left = 0, right = 0)
    }

    override fun setScreenFullscreen() {
        super.setScreenFullscreen()
        updateVideoPlayerSize(true)
        tvSpeed.isVisible = true
        if (isHKeyframeEnabled) tvKeyframe.isVisible = true
        titleTextView.isVisible = true
        btnGoHome.isVisible = false
        topBarContainer.isVisible = true
        clarity.isVisible = true
        superResolution.isVisible = switchPlayerKernel == HMediaKernel.Type.MpvPlayer.name
        gestureLock.isVisible = true
        val statusBarHeight = statusBarHeight
        val navBarHeight = navBarHeight
        layoutTop.updatePadding(left = statusBarHeight, right = navBarHeight)
        layoutBottom.updatePadding(left = statusBarHeight, right = navBarHeight)
        tvTimer.updatePadding(left = statusBarHeight)
        bottomProgressBar.updatePadding(left = statusBarHeight, right = navBarHeight)
    }

    override fun clickBack() {
        Log.i("fun_clickBack", "player_backBtn_clicked")
        if (context is MainActivity && screen == SCREEN_FULLSCREEN) {
            gotoNormalScreen()
            return
        }
        when {
            CONTAINER_LIST.isNotEmpty() && CURRENT_JZVD != null -> { //判断条件，因为当前所有goBack都是回到普通窗口
                CURRENT_JZVD.gotoNormalScreen()
            }

            CONTAINER_LIST.isEmpty() && CURRENT_JZVD != null && CURRENT_JZVD.screen != SCREEN_NORMAL -> { //退出直接进入的全屏
                CURRENT_JZVD.clearFloatScreen()
            }
            else -> {
                findNavController().navigateUp()
            }
        }
    }

    override fun clickClarity() {
        this.onCLickUiToggleToClear()
        val colorPrimary = MaterialColors.getColor(
            context,
            androidx.appcompat.R.attr.colorPrimary,
            Color.RED)
        val inflater = this.jzvdContext.getSystemService("layout_inflater") as LayoutInflater
        val layout = inflater.inflate(R.layout.layout_jzvd_clarity, null as ViewGroup?) as LinearLayout
        val mQualityListener = OnClickListener { v1: View? ->
            val index = v1!!.tag as Int
            this.jzDataSource.currentUrlIndex = index
            this.changeUrl(this.jzDataSource, this.currentPositionWhenPlaying)
            this.clarity.text = this.jzDataSource.currentKey.toString()

            for (j in 0..<layout.size) {
                if (j == this.jzDataSource.currentUrlIndex) {
                    (layout.getChildAt(j) as TextView).setTextColor(colorPrimary)
                } else {
                    (layout.getChildAt(j) as TextView).setTextColor("#ffffff".toColorInt())
                }
            }
            if (this.clarityPopWindow != null) {
                this.clarityPopWindow.dismiss()
            }
        }

        for (j in 0..<this.jzDataSource.urlsMap.size) {
            val key = this.jzDataSource.getKeyFromDataSource(j)
            val clarityItem = inflate(
                this.jzvdContext,
                R.layout.layout_jzvd_clarity_item,
                null as ViewGroup?
            ) as TextView
            clarityItem.text = key
            clarityItem.tag = j
            layout.addView(clarityItem, j)
            clarityItem.setOnClickListener(mQualityListener)
            if (j == this.jzDataSource.currentUrlIndex) {
                clarityItem.setTextColor(colorPrimary)
            }
        }

        this.clarityPopWindow =
            PopupWindow(layout, JZUtils.dip2px(this.jzvdContext, 240.0f), -1, true)
        this.clarityPopWindow.animationStyle = cn.jzvd.R.style.pop_animation
        this.clarityPopWindow.contentView = layout
        this.clarityPopWindow.showAtLocation(this.textureViewContainer, 8388613, 0, 0)
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.tv_speed -> clickSpeed()
            R.id.tv_keyframe -> onKeyframeClickListener?.invoke(v)
            R.id.super_resolution -> clickSuperResolution()
            R.id.go_home -> {
                if (screen != SCREEN_FULLSCREEN) {
                    findNavController().navigate(
                        R.id.nv_home_page,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.nav_main, true).build()
                    )
                } else {
                    onGoHomeClickListener?.invoke(v)
                }
            }

        }
    }
    @SuppressLint("InflateParams")
    fun clickSuperResolution() {
        onCLickUiToggleToClear()
        val inflater = LayoutInflater.from(context).inflate(R.layout.jz_layout_speed, null)
        val rv = inflater.findViewById<RecyclerView>(R.id.rv_video_speed)
        val popup = PopupWindow(
            inflater, JZUtils.dip2px(jzvdContext, 240f),
            LayoutParams.MATCH_PARENT, true
        ).apply {
            contentView = inflater
            animationStyle = cn.jzvd.R.style.pop_animation
        }
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = SuperResolutionAdapter(superResolutionIndex,getSuperResolutionArray().toList()).apply {
            setOnItemClickListener { _, _, position ->
                superResolutionIndex = position
                popup.dismiss()
            }
        }
        popup.showAtLocation(textureViewContainer, Gravity.END, 0, 0)
    }

    override fun onLongClick(v: View): Boolean {
        return when (v.id) {
            R.id.tv_keyframe -> {
                onKeyframeLongClickListener?.invoke(v)
                return true
            }
            else -> false
        }
    }

    override fun onCompletion() {
        if (screen == SCREEN_FULLSCREEN) {
            onStateAutoComplete()
        } else {
            super.onCompletion()
        }
        posterImageView.isGone = true
    }

    override fun touchActionMove(x: Float, y: Float) {
        if (gestureLocked) {
            return
        }
        val deltaX = x - mDownX
        var deltaY = y - mDownY
        val absDeltaX = deltaX.absoluteValue
        val absDeltaY = deltaY.absoluteValue
        // 此處進行了修改，未全屏也能調節進度
        Log.d(TAG, "mDownX=$mDownX, screenWidth=${JZUtils.getScreenWidth(context)}")
        if (screen != SCREEN_TINY && !isSpeedGestureDetected) {
            //拖动的是NavigationBar和状态栏
            if (mDownX > appScreenWidth
                || mDownY < JZUtils.getStatusBarHeight(context)
            ) {
                return
            }
            if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                    cancelProgressTimer()
                    if (absDeltaX >= THRESHOLD) {
                        // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                        // 否则会因为media player的状态非法导致App Crash
                        if (state != STATE_ERROR) {
                            mChangePosition = true
                            mGestureDownPosition = currentPositionWhenPlaying
                        }
                    } else {
                        //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                        Log.i("appScreenWidth",appScreenWidth.toString())
                        if (mDownX < appScreenWidth * 0.5f) { //左侧改变亮度
                            mChangeBrightness = true
                            isAdjustBrightness = true
                            val lp = JZUtils.getWindow(context).attributes
                            if (lp.screenBrightness < 0) {
                                try {
                                    mGestureDownBrightness = Settings.System.getInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS
                                    ).toFloat()
                                    Log.i(
                                        TAG,
                                        "current system brightness: $mGestureDownBrightness"
                                    )
                                } catch (e: SettingNotFoundException) {
                                    e.printStackTrace()
                                }
                            } else {
                                mGestureDownBrightness = lp.screenBrightness * 255
                                Log.i(
                                    TAG,
                                    "current activity brightness: $mGestureDownBrightness"
                                )
                            }
                        } else { //右侧改变声音
                            mChangeVolume = true
                            if (mAudioManager == null) {
                                mAudioManager = context.getSystemService()
                            }
                            mGestureDownVolume =
                                mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        }
                    }
                }
            }
        }

        if (mChangePosition) {
            val totalTimeDuration = duration
            mSeekTimePosition =
                (mGestureDownPosition + deltaX * totalTimeDuration / (mScreenWidth * userDefSlideSensitivity)).toLong()
            if (mSeekTimePosition < 0) mSeekTimePosition = 0
            if (mSeekTimePosition > totalTimeDuration) mSeekTimePosition = totalTimeDuration
            val seekTime = JZUtils.stringForTime(mSeekTimePosition)
            val totalTime = JZUtils.stringForTime(totalTimeDuration)
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration)
        }

        if (mChangeVolume) {
            deltaY = -deltaY
            val max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val deltaV = (max * deltaY * 3 / mScreenHeight).toInt()
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0)
            //dialog中显示百分比
            val volumePercent =
                (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight).toInt()
            showVolumeDialog(-deltaY, volumePercent)
        }

        if (mChangeBrightness) {
            deltaY = -deltaY
            val deltaV = (255 * deltaY * 3 / mScreenHeight).toInt()
            val params = JZUtils.getWindow(context).attributes
            if ((mGestureDownBrightness + deltaV) / 255 >= 1) { //这和声音有区别，必须自己过滤一下负值
                params.screenBrightness = 1f
            } else if ((mGestureDownBrightness + deltaV) / 255 <= 0) {
                params.screenBrightness = 0.01f
            } else {
                params.screenBrightness = (mGestureDownBrightness + deltaV) / 255
            }
            JZUtils.getWindow(context).attributes = params
            //dialog中显示百分比
            val brightnessPercent =
                (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight).toInt()
            showBrightnessDialog(brightnessPercent)
//            mDownY = y;
        }
    }

    private var savedConstraintLayoutParams: ConstraintLayout.LayoutParams? = null

    override fun gotoNormalScreen() {
        gobakFullscreenTime = System.currentTimeMillis() // 退出全屏时间
        fullscreenListener?.onFullscreenChanged(false)
        Log.i(TAG,"${isAdjustBrightness}、${screenBrightnessBK}、${JZUtils.getWindow(context).attributes.screenBrightness}")
        if (isAdjustBrightness) {
            val window = JZUtils.getWindow(context)
            if (window != null) {
                val params = window.attributes
                params.screenBrightness = screenBrightnessBK.coerceIn(0f, 1f) //恢复亮度
                window.attributes = params
            }
            isAdjustBrightness = false
        }
        // 从 decorView 移除全屏播放器视图
        val decorView = (JZUtils.scanForActivity(jzvdContext)).window.decorView as ViewGroup
        decorView.removeView(this)
        // 恢复到原始容器
        val originalContainer = CONTAINER_LIST.lastOrNull()
        if (originalContainer != null){
            CONTAINER_LIST.pop()
        } else {
            Log.e("JZVD", "CONTAINER_LIST is empty!")
            return
        }
        var layoutParams = blockLayoutParams
        if (originalContainer is ConstraintLayout) {
            layoutParams = savedConstraintLayoutParams ?: ConstraintLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        } else if (originalContainer is FrameLayout) {
            layoutParams = LayoutParams(blockLayoutParams)
        }
        // 把播放器重新添加回原来的位置
        originalContainer.addView(this, blockIndex, layoutParams)
        originalContainer.requestLayout()
        // 设置播放器状态并恢复系统UI和方向
        setScreenNormal()
        JZUtils.showStatusBar(jzvdContext)
        val activity = JZUtils.scanForActivity(jzvdContext)
        if (activity != null) {
            orientationManager.unlockOrientation(activity)
        }
        JZUtils.showSystemUI(jzvdContext)
    }

    override fun gotoFullscreen() {
        gotoFullscreenTime = System.currentTimeMillis()
        fullscreenListener?.onFullscreenChanged(true)
        val vg = parent as? ViewGroup ?: return
        val activity = JZUtils.scanForActivity(jzvdContext)
        jzvdContext = vg.context

        // 保存容器与布局信息
        blockLayoutParams = layoutParams
        blockIndex = vg.indexOfChild(this)
        blockWidth = width
        blockHeight = height

        if (blockLayoutParams is ConstraintLayout.LayoutParams) {
            savedConstraintLayoutParams =
                ConstraintLayout.LayoutParams(blockLayoutParams as ConstraintLayout.LayoutParams)
        }
        // 从原来容器中移除播放器
        vg.removeView(this)
        CONTAINER_LIST.push(vg)
        val decorView = (JZUtils.scanForActivity(jzvdContext)).window.decorView as ViewGroup
        val fullLayout = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        var videoWidth = 0
        var videoHeight = 0
        when (mediaInterface) {
            is ExoMediaKernel -> {
                val mediaKernel = CURRENT_JZVD?.mediaInterface as? ExoMediaKernel
                videoWidth  = mediaKernel?.videoRealWidth?:0
                videoHeight = mediaKernel?.videoRealHeight?:0
                Log.i(TAG,"mediaInterface:$mediaKernel,videoWidth:$videoWidth,videoHeight:$videoHeight")
            }
            is MpvMediaKernel -> {
                val mediaKernel = CURRENT_JZVD?.mediaInterface as? MpvMediaKernel
                videoWidth  = mediaKernel?.videoRealWidth?:0
                videoHeight = mediaKernel?.videoRealHeight?:0
                Log.i(TAG,"mediaInterface:$mediaKernel,videoWidth:$videoWidth,videoHeight:$videoHeight")
            }
            is SystemMediaKernel -> {
                val mediaKernel = CURRENT_JZVD?.mediaInterface as? SystemMediaKernel
                videoWidth  = mediaKernel?.videoRealWidth?:0
                videoHeight = mediaKernel?.videoRealHeight?:0
                Log.i(TAG,"mediaInterface:$mediaKernel,videoWidth:$videoWidth,videoHeight:$videoHeight")
            }
        }

        val isPortraitVideo = videoWidth > 0 && videoHeight > 0 && videoWidth < videoHeight
        Log.i(TAG,"mediaInterface:$mediaInterface,videoWidth:$videoWidth,videoHeight:$videoHeight")
        if (isPortraitVideo) {
            pivotY = 0f
            scaleY = 0.5f
        }
        decorView.addView(this, fullLayout)
        if (isPortraitVideo) {
            animate()
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        setScreenFullscreen()
        JZUtils.hideStatusBar(jzvdContext)
        if (isPortraitVideo) {
            activity?.let {
                orientationManager.lockOrientation(
                    it,
                    OrientationManager.ScreenOrientation.PORTRAIT
                )
            }
        } else {
            JZUtils.setRequestedOrientation(jzvdContext, FULLSCREEN_ORIENTATION)
        }
        JZUtils.hideSystemUI(jzvdContext)
    }


    override fun onStatePreparingChangeUrl() {
        Log.i(TAG, "onStatePreparingChangeUrl " + " [" + this.hashCode() + "] ")
        state = STATE_PREPARING_CHANGE_URL

        // 原方法直接使用下面的方法，會導致全屏切換清晰度返回正常界面時重置影片。
        // 所以重寫，只抄過調用的方法的一部分。
        // releaseAllVideos()
        CURRENT_JZVD?.let {
            it.reset()
            CURRENT_JZVD = null
        }

        startVideo()
    }

    override fun showWifiDialog() {
        jzvdContext.showAlertDialog {
            setTitle("Warning!")
            setMessage(cn.jzvd.R.string.tips_not_wifi)
            setPositiveButton(cn.jzvd.R.string.tips_not_wifi_confirm) { _, _ ->
                WIFI_TIP_DIALOG_SHOWED = true
                if (state == STATE_PAUSE) startButton.performClick() else startVideo()
            }
            setNegativeButton(cn.jzvd.R.string.tips_not_wifi_cancel) { _, _ ->
                releaseAllVideos()
                clearFloatScreen()
            }
        }
    }

    // 原來是 300 period 我改成了 100 爲了計時準確
    override fun startProgressTimer() {
        Log.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ")
        cancelProgressTimer()
        UPDATE_PROGRESS_TIMER = Timer()
        mProgressTimerTask = ProgressTimerTask()
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 100)
    }

    override fun onProgress(progress: Int, position: Long, duration: Long) {
        super.onProgress(progress, position, duration)
        if (screen == SCREEN_FULLSCREEN) hKeyframe?.let {
            var match = false
            for ((index, kf) in it.keyframes.withIndex()) {
                val interval = kf.position - position
                if (interval in 0L..<userDefWhenCountdownRemind) {
                    val timeLong = interval / 1_000L
                    val spannable = spannable {
                        if (userDefShowCommentWhenCountdown) {
                            "#${index + 1}".span {
                                relativeSize(proportion = 0.7F)
                            }
                            if (!kf.prompt.isNullOrBlank()) {
                                " ${kf.prompt}".span {
                                    relativeSize(proportion = 0.7F)
                                }
                            }
                            newline()
                        }
                        val time = if (timeLong >= 1) {
                            (timeLong + 1).toString()
                        } else {
                            val timeFloat = interval / 1_000F
                            "%.1f".format(timeFloat)
                        }
                        time.span {
                            style(Typeface.BOLD)
                        }
                    }
                    tvTimer.text = spannable
                    match = true
                    break
                }
            }
            tvTimer.isInvisible = !match
        } ?: run { tvTimer.isInvisible = true }
    }

    private fun changeUiToPreparingPlayingClear() {
        when (screen) {
            SCREEN_NORMAL, SCREEN_FULLSCREEN -> {
                setAllControlsVisiblitySafe(
                    INVISIBLE, INVISIBLE, INVISIBLE,
                    VISIBLE, INVISIBLE, INVISIBLE, INVISIBLE
                )
            }
        }
    }

    private fun changeUiToPreparingPlayingShow() {
        when (screen) {
            SCREEN_NORMAL, SCREEN_FULLSCREEN -> {
                setAllControlsVisiblitySafe(
                    VISIBLE, VISIBLE, INVISIBLE,
                    VISIBLE, INVISIBLE, VISIBLE, INVISIBLE
                )
            }
        }
    }

    //安卓7会报错CalledFromWrongThreadException
    fun setAllControlsVisiblitySafe(
        topCon: Int, bottomCon: Int, startBtn: Int, loadingPro: Int,
        posterImg: Int, bottomPro: Int, retryLayout: Int
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setAllControlsVisiblity(topCon, bottomCon, startBtn, loadingPro, posterImg, bottomPro, retryLayout)
        } else {
            post {
                setAllControlsVisiblity(topCon, bottomCon, startBtn, loadingPro, posterImg, bottomPro, retryLayout)
            }
        }
    }
    fun changeUiToPlayingClearSafe() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            changeUiToPlayingClear()
        } else {
            post {
                changeUiToPlayingClear()
            }
        }
    }
    fun changeUiToPlayingShowSafe() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            changeUiToPlayingShow()
        } else {
            post {
                changeUiToPlayingShow()
            }
        }
    }

    override fun onStatePlaying() {
        Log.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ")
        onVideoStateChanged?.invoke(STATE_PLAYING)
        if (isNeedResumeProgress()) {
            post {
                btnResumeProgress.visibility = VISIBLE
                handler.removeCallbacks(hideResumeBtnRunnable)
                handler.postDelayed(hideResumeBtnRunnable, 5000)
            }
        }
        if (state == STATE_PREPARED) { //如果是准备完成视频后第一次播放，先判断是否需要跳转进度。
            Log.d(TAG, "onStatePlaying:STATE_PREPARED ")
            mAudioManager =
                applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                    .build()
                mAudioManager.requestAudioFocus(audioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                mAudioManager.requestAudioFocus(
                    onAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            if (seekToInAdvance != 0L) {
                mediaInterface.seekTo(seekToInAdvance)
                seekToInAdvance = 0
            } else {
                val position = JZUtils.getSavedProgress(context, jzDataSource.currentUrl)
                if (position != 0L) {
                    mediaInterface.seekTo(position) //这里为什么区分开呢，第一次的播放和resume播放是不一样的。 这里怎么区分是一个问题。然后
                }
            }
        }
        if (isNeedResumeProgress()) {
            mediaInterface.seekTo(savedProgress)
            hasRestoredProgress = true
        }
        state = STATE_PLAYING
        startProgressTimer()
        changeUiToPlayingClearSafe()
    }
    override fun onStatePause() {
        super.onStatePause()
        onVideoStateChanged?.invoke(STATE_PAUSE)
    }
    override fun onStateAutoComplete() {
        super.onStateAutoComplete()
        onVideoStateChanged?.invoke(STATE_AUTO_COMPLETE)
    }

    override fun onStatePreparing() {
        super.onStatePreparing()
        onVideoStateChanged?.invoke(STATE_PREPARING)
    }

    // #issue-14: 之前用 XPopup 三键模式下会有 bug，无法呼出，所以换成这个
    @SuppressLint("InflateParams")
    fun clickSpeed() {
        onCLickUiToggleToClear()
        val inflater = LayoutInflater.from(context).inflate(R.layout.jz_layout_speed, null)
        val rv = inflater.findViewById<RecyclerView>(R.id.rv_video_speed)
        val popup = PopupWindow(
            inflater, JZUtils.dip2px(jzvdContext, 240f),
            LayoutParams.MATCH_PARENT, true
        ).apply {
            contentView = inflater
            animationStyle = cn.jzvd.R.style.pop_animation
        }
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = VideoSpeedAdapter(currentSpeedIndex).apply {
            setOnItemClickListener { _, _, position ->
                currentSpeedIndex = position
                popup.dismiss()
            }
        }
        popup.showAtLocation(textureViewContainer, Gravity.END, 0, 0)
    }

    @SuppressLint("InflateParams")
    fun clickHKeyframe(v: View) {
        onCLickUiToggleToClear()
        val inflater = LayoutInflater.from(context).inflate(R.layout.jz_layout_speed, null)
        val rv = inflater.findViewById<RecyclerView>(R.id.rv_video_speed)
        val popup = PopupWindow(
            inflater, JZUtils.dip2px(jzvdContext, 240f),
            LayoutParams.MATCH_PARENT, true
        ).apply {
            contentView = inflater
            animationStyle = cn.jzvd.R.style.pop_animation
        }
        rv.layoutManager = LinearLayoutManager(v.context)
        val adapter = hKeyframeAdapter
        rv.adapter = adapter
        adapter.setStateViewLayout(
            inflate(v.context, R.layout.layout_empty_view, null),
            this@HJzvdStd.context.getString(R.string.here_is_empty) + "\n"
                    + this@HJzvdStd.context.getString(R.string.long_press_to_add_h_keyframe)
        )
        popup.showAtLocation(textureViewContainer, Gravity.END, 0, 0)
    }

    /**
     * 这个 setSpeed 的 bug 太多了，不同机型效果不一定相同，不得不套个 try-catch。 (previous)
     *
     * PS: 套 try-catch 没用，因为在 post 里面，所以还是会报错，只能在调用的地方 try-catch 了。
     *
     * #issue-28 就是这个问题，如果我在 HJZMediaSystem 中 setSpeed 方法里加的判断不起作用，
     * 那么那个机型就先别用这个功能了。
     */
    private fun setSpeedInternal(speed: Float) {
        mediaInterface?.setSpeed(speed)
    }

    /**
     * 將靈敏度轉換為實際數值，很多用戶對滑動要求挺高，
     * 靈敏度太高沒人在乎，所以高靈敏度照舊，低靈敏度差別大一點
     */
    private fun @receiver:IntRange(from = 1, to = 9) Int.toRealSensitivity(): Int {
        return when (this) {
            1, 2, 3, 4, 5 -> this
            6 -> 7
            7 -> 10
            8 -> 20
            9 -> 40
            else -> throw IllegalStateException("Invalid sensitivity value: $this")
        }
    }
    private fun updateVideoPlayerSize(fullscreen: Boolean) {
        if (mediaInterface is MpvMediaKernel) {
            val kernel = mediaInterface as MpvMediaKernel
            post {
                if (fullscreen) {
                    Log.i(TAG, "updateVideoPlayerSize: $width x $height")
                    kernel.updateSurFaceSize(width, height)
                } else {
                    kernel.updateSurFaceSize(width, height)
                }
            }
        }
    }
}