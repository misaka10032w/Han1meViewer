package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * 折叠惯性每秒速度衰减系数（指数摩擦）。
 *
 * 取 2.0 时初速 3000px/s 的总行程约 1500px，与系统滚动惯性
 * （splineBasedDecay）同量级：一次轻快甩动能折完头部并把可感知的
 * 余速转赠给列表，慢速拖拽释放则自然停在当前折叠位。
 */
private const val FLING_FRICTION_PER_SECOND = 2f

/**
 * 播放页折叠头部（纯 Compose，取代 CoordinatorLayout + AppBarLayout + CollapsingToolbarLayout）。
 *
 * 头部高度随折叠真实收缩，内容区始终紧贴头部下缘（`Column(header + content)`）。
 *
 * ### 性能要点：折叠几何全部下沉到 layout/draw 阶段
 * 折叠比例（拖拽每帧变化）只在 [Modifier.layout] 与 [derivedStateOf] 中读取，
 * 组合期间不依赖任何折叠值——拖拽/锁定动画的每一帧只触发测量与布局，
 * 不引发整棵播放页子树的重组，内容列表（评论 LazyColumn）不闪烁。
 *
 * 滚动协议为标准 collapsing：
 * - 上滑：先折叠头部（[NestedScrollConnection.onPreScroll]），折完才轮到内容滚动；
 * - 下滑且头部处于折叠态：preScroll 阶段优先展开（父层先于子的
 *   overscroll/PullToRefresh，否则列表置顶时的下拉量会被其消费）；
 * - 惯性（fling）：释放速度优先驱动折叠/展开（与拖拽协议一致的方向归属），
 *   头部到达端点后的余速转赠给列表继续滚，速度耗尽则停在当前折叠位
 *   （不做端点吸附，保持跟手）；
 * - [scrollEnabled] 为 false（播放中）时禁止折叠并平滑动画强制展开。
 *
 * @param headerHeightPx 头部展开时的高度（px），播放器比例高度由页面驱动
 * @param scrollEnabled 是否允许折叠（播放中锁定展开）
 * @param initialCollapsed 初始是否折叠（恢复页面滚动状态）
 * @param onCollapseRatioChange 折叠比例回调（0 展开 .. 1 完全折叠），页面持久化用
 * @param modifier 布局修饰符，页面可用 `statusBarsPadding` 等避让系统栏
 * @param headerContent 头部内容（播放器，随高度收缩自适应）
 * @param content 主体内容（tabs，自身含滚动容器）
 */
@Composable
fun VideoCollapsingHeader(
    headerHeightPx: Int,
    scrollEnabled: Boolean,
    initialCollapsed: Boolean,
    onCollapseRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    /**
     * 折叠比例（0..1）。
     *
     * 用普通 remember 而非 rememberSaveable：进程重建恢复折叠态时，
     * 头部高度从 0 过渡的组合下播放器测量会错乱，恢复到展开态更稳妥。
     */
    var collapsedRatio by remember {
        mutableFloatStateOf(if (initialCollapsed) 1f else 0f)
    }

    val maxCollapse = headerHeightPx.toFloat()

    // 播放中锁定展开：手动 Animatable 平滑回到 0。
    // 不用 animateFloatAsState——它的 target 读取发生在组合期间，
    // 拖拽中每帧都会重组本组件；Animatable 只在 layout 阶段被读取。
    val lockAnimatable = remember { Animatable(if (initialCollapsed) 1f else 0f) }
    val currentScrollEnabled by rememberUpdatedState(scrollEnabled)

    /** 实际生效的折叠比例：仅 layout/draw 阶段读取，不参与组合。 */
    val effectiveRatio = remember {
        derivedStateOf {
            if (currentScrollEnabled) {
                collapsedRatio.coerceIn(0f, 1f)
            } else {
                lockAnimatable.value.coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(scrollEnabled) {
        if (!scrollEnabled) {
            // 锁定（播放中）：平滑展开到 0
            if (lockAnimatable.value > 0f) {
                lockAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 220))
            }
            // 完成后同步手势态，解锁（暂停）后从 0 开始
            collapsedRatio = 0f
        } else {
            // 解锁期间 lockAnimatable 持续跟随手势比例：锁定瞬间数据源从
            // collapsedRatio 切到 lockAnimatable 时数值无缝衔接（否则会以
            // 滞留初值渲染，出现"先缩到最小再展开"甚至卡死）
            snapshotFlow { collapsedRatio }.collect { lockAnimatable.snapTo(it) }
        }
    }

    // 折叠比例上报：snapshotFlow 只在值实际变化时发射，不引起重组
    LaunchedEffect(Unit) {
        snapshotFlow { effectiveRatio.value }.collect { onCollapseRatioChange(it) }
    }

    fun consumeFold(delta: Float): Float {
        val prev = collapsedRatio
        val next = (prev - delta / maxCollapse).coerceIn(0f, 1f)
        if (next != prev) {
            collapsedRatio = next
            // 返回值必须与 delta 同号同量级（嵌套滚动协议用 available - consumed
            // 计算传给子组件的剩余量；符号写反会让子列表拿到 2 倍 delta，
            // 造成「头部折叠 + 内容双倍滚动」的怪手感）。
            return (prev - next) * maxCollapse
        }
        return 0f
    }

    /** 惯性积分的最小速度（px/s），低于该值视为速度耗尽立即停止。 */
    val minFlingVelocityPx = with(LocalDensity.current) { 50.dp.toPx() }

    /**
     * 折叠轴的惯性：按指数摩擦逐帧把速度积分成折叠量。
     *
     * - 速度耗尽时停在当前折叠位（不做端点吸附，保持跟手）；
     * - 本帧内触到端点时按未走完的距离比例折算残余速度并停止，
     *   剩余速度（[Velocity]）还给调用方——由 [NestedScrollConnection.onPreFling]
     *   转发给列表，形成「先带着惯性折完头部、再带着余速滚列表」的连贯手感；
     * - 每帧经 [withFrameNanos] 驱动，新手指按下时本协程被取消，
     *   头部自然停在当前位置，由新手势接管。
     */
    suspend fun flingCollapse(velocityY: Float): Velocity {
        var velocity = velocityY
        var lastFrame = -1L
        while (abs(velocity) >= minFlingVelocityPx &&
            !((collapsedRatio >= 1f && velocity < 0f) || (collapsedRatio <= 0f && velocity > 0f))
        ) {
            val frame = withFrameNanos { it }
            if (lastFrame >= 0L) {
                val dt = ((frame - lastFrame) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
                val dy = velocity * dt
                val consumed = consumeFold(dy)
                velocity *= exp(-FLING_FRICTION_PER_SECOND * dt)
                if (abs(consumed) < abs(dy) - 0.5f) {
                    // 触到端点：按本帧未走完的距离比例折算残余速度
                    velocity *= (1f - abs(consumed) / abs(dy)).coerceIn(0f, 1f)
                    break
                }
            }
            lastFrame = frame
        }
        return Velocity(0f, velocity)
    }

    val nestedScrollConnection = remember(scrollEnabled, maxCollapse) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!scrollEnabled) return Offset.Zero
                val dy = available.y
                return when {
                    // 上滑：先折叠头部，折完才轮到内容
                    dy < 0 -> Offset(0f, consumeFold(dy))

                    // 下滑且头部处于折叠态：preScroll 阶段优先展开。
                    // 父层 preScroll 先于子组件（含列表 overscroll 拉伸与
                    // PullToRefresh）执行，否则列表置顶时下拉量会被其消费。
                    dy > 0 && collapsedRatio > 0f -> Offset(0f, consumeFold(dy))

                    else -> Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!scrollEnabled) return Offset.Zero
                val dy = available.y
                // 下滑：内容滚到顶后，剩余量展开头部（兜底长列表回滚到顶后的继续展开）
                return if (dy > 0) Offset(0f, consumeFold(dy)) else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!scrollEnabled || available.y == 0f) return Velocity.Zero
                // 释放速度按拖拽协议的方向归属驱动折叠/展开（上滑先折叠、
                // 折叠态下滑先展开）；头部吸收不了（已在速度所指端点）时
                // [flingCollapse] 原样返还速度，惯性全部属于列表。
                val leftover = flingCollapse(available.y)
                return available - leftover
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 头部：高度在 layout 阶段按折叠比例收缩（组合期间不读折叠值）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val ratio = effectiveRatio.value
                    val h = (maxCollapse * (1f - ratio)).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = h, maxHeight = h),
                    )
                    layout(placeable.width, h) { placeable.place(0, 0) }
                }
                .clipToBounds(),
        ) {
            headerContent()
        }
        // 内容：占据剩余空间，嵌套滚动连接挂在内容侧
        Box(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(nestedScrollConnection),
        ) {
            content()
        }
    }
}
