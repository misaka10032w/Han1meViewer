package com.yenaly.han1meviewer.ui.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yenaly.han1meviewer.ui.navigation.main.MainDrawerDestination
import com.yenaly.han1meviewer.ui.screen.main.MainActivityScaffold
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationScaffoldTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switchingBetweenRailAndDrawerKeepsDrawerClosedAndContentAlive() {
        var useRail by mutableStateOf(true)
        lateinit var drawer: DrawerState
        compose.setContent {
            drawer = rememberDrawerState(DrawerValue.Closed)
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    MainActivityScaffold(
                        drawerState = drawer,
                        drawerEnabled = true,
                        selectedDestination = MainDrawerDestination.Home,
                        avatarUrl = null,
                        username = null,
                        isLoggedIn = false,
                        isLoading = false,
                        currentSite = "https://example.invalid",
                        onAvatarClick = {},
                        onAvatarLongClick = {},
                        onSwitchSiteClick = {},
                        onDrawerItemSelected = { true },
                        useRail = useRail,
                    ) {
                        var count by remember { mutableStateOf(0) }
                        Button(onClick = { count++ }) { Text("Content $count") }
                    }
                }
            }
        }
        compose.onNodeWithText("Content 0").performClick()
        listOf(false, true, false).forEach { rail ->
            compose.runOnIdle { useRail = rail }
            compose.onNodeWithText("Content 1").assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(DrawerValue.Closed, drawer.currentValue)
                assertEquals(DrawerValue.Closed, drawer.targetValue)
            }
        }
    }
}
