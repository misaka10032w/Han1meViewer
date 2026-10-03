package com.yenaly.han1meviewer.ui.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveListDetailTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectedDetailSurvivesNarrowingAndListStateIsRestored() {
        var expanded by mutableStateOf(true)
        var selected by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(if (expanded) 1000.dp else 600.dp, 600.dp)) {
                    AdaptiveListDetail(
                        useListDetail = expanded,
                        showDetail = selected,
                        listWidth = 360.dp,
                        list = {
                            var count by rememberSaveable { mutableStateOf(0) }
                            Button(onClick = { count++; selected = true }) {
                                Text("List $count / ${currentContentWidthDp().value.toInt()}")
                            }
                        },
                        detail = {
                            Button(onClick = { selected = false }) { Text("Selected detail") }
                        },
                        emptyDetail = { Text("Choose an item") },
                    )
                }
            }
        }
        compose.onNodeWithText("List 0 / 360").performClick()
        compose.onNodeWithText("Selected detail").assertIsDisplayed()
        compose.runOnIdle { expanded = false }
        compose.onNodeWithText("List 1 / 360").assertDoesNotExist()
        compose.onNodeWithText("Selected detail").performClick()
        compose.onNodeWithText("List 1 / 600").assertIsDisplayed()
        compose.runOnIdle { expanded = true }
        compose.onNodeWithText("List 1 / 360").assertIsDisplayed()
        compose.onNodeWithText("Choose an item").assertIsDisplayed()
    }
}
