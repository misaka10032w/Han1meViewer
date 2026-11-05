package com.yenaly.han1meviewer.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HA1_GITHUB_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SettingInfoItem
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.util.browse
import kotlinx.coroutines.delay

private const val EASTER_EGG_TAP_COUNT = 7

/**
 * 设置 -> 关于 -> 关于 页面。
 *
 * 展示版本、构建、运行环境和项目入口。
 * 连续点击顶部应用信息卡片 7 次会触发暂时占位的彩蛋 Toast。
 */
@Composable
fun AboutAppScreen() {
    val context = LocalContext.current
    var easterEggTapCount by remember { mutableIntStateOf(0) }
    val easterEggToast = stringResource(R.string.about_easter_egg_toast)

    LaunchedEffect(easterEggTapCount) {
        if (easterEggTapCount in 1 until EASTER_EGG_TAP_COUNT) {
            delay(2_000)
            easterEggTapCount = 0
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            AboutHeaderCard(
                onClick = {
                    easterEggTapCount++
                    if (easterEggTapCount >= EASTER_EGG_TAP_COUNT) {
                        easterEggTapCount = 0
                        GlobalToasts.show(
                            easterEggToast,
                            level = GlobalToasts.ToastLevel.INFO,
                        )
                    }
                },
            )
        }

        item { AboutGroupTitle(stringResource(R.string.about_app_info)) }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_build_type),
                valueText = BuildConfig.BUILD_TYPE,
                iconRes = R.drawable.baseline_custom_24,
            )
        }
        if (BuildConfig.COMMIT_SHA.isNotBlank()) {
            item {
                SettingInfoItem(
                    title = stringResource(R.string.about_build_commit),
                    valueText = BuildConfig.COMMIT_SHA.take(7),
                    iconRes = R.drawable.baseline_custom_24,
                )
            }
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_package_name),
                valueText = context.packageName,
                iconRes = R.drawable.ic_baseline_info_24,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_current_site),
                valueText = Preferences.baseUrl,
                iconRes = R.drawable.baseline_domain_24,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_android_version),
                valueText = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                iconRes = R.drawable.baseline_data_usage_24,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_device_model),
                valueText = listOf(Build.MANUFACTURER, Build.MODEL)
                    .filter { it.isNotBlank() }
                    .joinToString(" "),
                iconRes = R.drawable.baseline_mobile_data_24,
            )
        }

        item { AboutGroupTitle(stringResource(R.string.about_project_support)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.about_github_repo),
                summary = stringResource(R.string.about_github_repo_summary),
                iconRes = R.drawable.ic_baseline_info_24,
                onClick = { context.browse(HA1_GITHUB_URL) },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.submit_bug),
                summary = stringResource(R.string.submit_bug_summary),
                iconRes = R.drawable.baseline_bug_report_24,
                onClick = { context.browse(HA1_GITHUB_ISSUE_URL) },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.forum),
                summary = stringResource(R.string.forum_summary),
                iconRes = R.drawable.baseline_forum_24,
                onClick = { context.browse(HA1_GITHUB_FORUM_URL) },
            )
        }

        item {
            Text(
                text = stringResource(R.string.about_thanks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun AboutHeaderCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_new),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.hanime_app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AboutGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 8.dp,
            end = 8.dp,
            top = 16.dp,
            bottom = 4.dp,
        ),
    )
}
