package com.yenaly.han1meviewer.ui.adaptive

import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yenaly.han1meviewer.ui.screen.video.VideoShellContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlayerHostTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun playerHostRemainsAttachedAcrossResizeFullscreenAndPip() {
        var width by mutableStateOf(1000.dp)
        var fullscreen by mutableStateOf(false)
        var pip by mutableStateOf(false)
        var attaches = 0
        var detaches = 0
        lateinit var host: FrameLayout
        compose.setContent {
            val context = LocalContext.current
            val view = remember {
                FrameLayout(context).apply {
                    host = this
                    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) { attaches++ }
                        override fun onViewDetachedFromWindow(v: View) { detaches++ }
                    })
                }
            }
            MaterialTheme {
                Box(Modifier.requiredSize(width, 600.dp)) {
                    VideoShellContent(
                        isInPipMode = pip,
                        isFullscreen = fullscreen,
                        playlistItems = emptyList(),
                        relatedItems = emptyList(),
                        childCommentId = null,
                        onHideRelatedInIntroChange = {},
                        onHidePlaylistInIntroChange = {},
                        onSplitLayoutChange = {},
                        onOpenVideo = {},
                        onUpdateHost = { _, _ -> },
                        mainHostFactory = { view },
                        childCommentPane = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        val originalParent = compose.runOnIdle { host.parent }
        val transitions: List<() -> Unit> = listOf(
            { width = 600.dp },
            { width = 1000.dp },
            { fullscreen = true },
            { fullscreen = false },
            { pip = true },
            { pip = false },
        )
        transitions.forEach { transition ->
            compose.runOnIdle(transition)
            compose.runOnIdle {
                assertTrue(host.isAttachedToWindow)
                assertSame(originalParent, host.parent)
                assertEquals(1, attaches)
                assertEquals(0, detaches)
            }
        }
    }
}
