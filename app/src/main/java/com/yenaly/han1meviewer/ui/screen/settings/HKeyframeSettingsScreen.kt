package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ComponentPreview
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSliderItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem

data class HKeyframeSettingsUiState(
    val hKeyframesEnable: Boolean,
    val hKeyframesSummary: String,
    val sharedHKeyframesEnable: Boolean,
    val sharedHKeyframesUseFirst: Boolean,
    val showCommentWhenCountdown: Boolean,
    val whenCountdownRemind: Int,
    val whenCountdownRemindSummary: String,
)

@Composable
fun HKeyframeSettingsScreen(
    state: HKeyframeSettingsUiState,
    onHKeyframesEnableChange: (Boolean) -> Unit,
    onOpenHKeyframeManage: () -> Unit,
    onSharedHKeyframesEnableChange: (Boolean) -> Unit,
    onSharedHKeyframesUseFirstChange: (Boolean) -> Unit,
    onOpenSharedHKeyframeManage: () -> Unit,
    onShowCommentWhenCountdownChange: (Boolean) -> Unit,
    onWhenCountdownRemindChange: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            SettingSwitchItem(
                title = stringResource(R.string.h_keyframes_enable),
                summary = state.hKeyframesSummary,
                checked = state.hKeyframesEnable,
                iconRes = R.drawable.baseline_h_24,
                onCheckedChange = onHKeyframesEnableChange,
            )
        }

        if (state.hKeyframesEnable) {
            item { HKeyframeGroupTitle(stringResource(R.string.manage)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.h_keyframe_manage),
                    iconRes = R.drawable.baseline_manage_24,
                    onClick = onOpenHKeyframeManage,
                )
            }

            item { HKeyframeGroupTitle(stringResource(R.string.shared)) }
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.shared_h_keyframes_enable),
                    summary = stringResource(R.string.shared_h_keyframes_enable_tip),
                    checked = state.sharedHKeyframesEnable,
                    iconRes = R.drawable.baseline_share_24,
                    onCheckedChange = onSharedHKeyframesEnableChange,
                )
            }

            if (state.sharedHKeyframesEnable) {
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.shared_h_keyframes_use_first),
                        summary = stringResource(R.string.shared_h_keyframes_use_first_tip),
                        checked = state.sharedHKeyframesUseFirst,
                        iconRes = R.drawable.baseline_share_first_24,
                        onCheckedChange = onSharedHKeyframesUseFirstChange,
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.shared_h_keyframe_manage),
                        summary = stringResource(R.string.shared_h_keyframe_manage_tip),
                        iconRes = R.drawable.baseline_online_manage_24,
                        onClick = onOpenSharedHKeyframeManage,
                    )
                }
            }

            item { HKeyframeGroupTitle(stringResource(R.string.custom)) }
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.show_prompt_when_countdown),
                    checked = state.showCommentWhenCountdown,
                    iconRes = R.drawable.baseline_count_down_24,
                    onCheckedChange = onShowCommentWhenCountdownChange,
                )
            }
            item {
                SettingSliderItem(
                    title = stringResource(R.string.when_countdown_remind),
                    summary = state.whenCountdownRemindSummary,
                    value = state.whenCountdownRemind,
                    valueRange = 5..30,
                    iconRes = R.drawable.ic_baseline_alert_24,
                    onValueChange = onWhenCountdownRemindChange,
                )
            }
        }
    }
}

@Composable
private fun HKeyframeGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HKeyframeSettingsScreenPreview() {
    ComponentPreview {
        HKeyframeSettingsScreen(
            state = HKeyframeSettingsUiState(
                hKeyframesEnable = true,
                hKeyframesSummary = "开启后，播放器顶部会显示🥵",
                sharedHKeyframesEnable = true,
                sharedHKeyframesUseFirst = false,
                showCommentWhenCountdown = false,
                whenCountdownRemind = 10,
                whenCountdownRemindSummary = "将会在 10 秒前倒数计时提醒 (預設)",
            ),
            onHKeyframesEnableChange = {},
            onOpenHKeyframeManage = {},
            onSharedHKeyframesEnableChange = {},
            onSharedHKeyframesUseFirstChange = {},
            onOpenSharedHKeyframeManage = {},
            onShowCommentWhenCountdownChange = {},
            onWhenCountdownRemindChange = {},
        )
    }
}
