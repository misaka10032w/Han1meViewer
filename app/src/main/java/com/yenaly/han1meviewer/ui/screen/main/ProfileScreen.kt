package com.yenaly.han1meviewer.ui.screen.main

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ExpressiveEntryCard
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

/** “我的”页面快捷入口卡片的统一高度。 */
private val ProfileEntryCardHeight = 168.dp

/** 快捷入口卡片的最小宽度，同时保证窄屏下至少两列。 */
private val ProfileEntryCardMinWidth = 160.dp

/**
 * “我的”一级页面，承载账号、签到、创作中心和设置等入口。
 *
 * 页面使用 M3 Expressive 风格：顶部为账号信息大卡片，下方为自适应快捷入口网格。
 */
@Composable
fun ProfileScreen(
    isLoggedIn: Boolean,
    avatarUrl: String?,
    username: String?,
    currentSite: String,
    onAccountClick: () -> Unit,
    onAvatarLongClick: () -> Unit,
    onDailyCheckInClick: () -> Unit,
    onCreatorCenterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = username?.takeIf { it.isNotBlank() }
        ?: stringResource(if (isLoggedIn) R.string.not_logged_in else R.string.login)

    val entries = listOf(
        ProfileEntry(
            icon = Icons.Filled.ThumbUp,
            title = stringResource(R.string.has_mastur),
            subtitle = stringResource(R.string.profile_check_in_desc),
            onClick = onDailyCheckInClick,
        ),
        ProfileEntry(
            icon = Icons.Filled.VideoCall,
            title = stringResource(R.string.creator_center),
            subtitle = stringResource(R.string.profile_creator_center_desc),
            onClick = onCreatorCenterClick,
        ),
        ProfileEntry(
            icon = Icons.Filled.Settings,
            title = stringResource(R.string.settings),
            subtitle = stringResource(R.string.profile_settings_desc),
            onClick = onSettingsClick,
        ),
        ProfileEntry(
            icon = Icons.Filled.SwapHoriz,
            title = stringResource(R.string.switch_site),
            subtitle = currentSite,
            onClick = onSwitchSiteClick,
        ),
    )

    HanimeScaffold(
        title = stringResource(R.string.my_account),
        onBack = null,
        modifier = modifier,
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPadding = 20.dp
            val itemSpacing = 12.dp
            val columns = maxOf(
                2,
                ((maxWidth - horizontalPadding * 2 + itemSpacing) /
                        (ProfileEntryCardMinWidth + itemSpacing)).toInt(),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                item(
                    key = "profile_header",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    ProfileHeaderCard(
                        avatarUrl = avatarUrl,
                        displayName = displayName,
                        currentSite = currentSite,
                        onClick = onAccountClick,
                        onLongClick = onAvatarLongClick,
                    )
                }
                item(
                    key = "profile_section",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Text(
                        text = stringResource(R.string.quick_entries),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
                items(
                    items = entries,
                    key = { it.title },
                    span = { GridItemSpan(1) },
                ) { entry ->
                    ExpressiveEntryCard(
                        icon = entry.icon,
                        title = entry.title,
                        subtitle = entry.subtitle,
                        // 统一预留两行副标题并固定卡片高度，避免四个快捷入口大小不一致。
                        subtitleMinLines = 2,
                        modifier = Modifier.height(ProfileEntryCardHeight),
                        onClick = entry.onClick,
                    )
                }
            }
        }
    }
}

private data class ProfileEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun ProfileHeaderCard(
    avatarUrl: String?,
    displayName: String,
    currentSite: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(76.dp),
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.bg_default_header),
                    fallback = painterResource(R.drawable.bg_default_header),
                    error = painterResource(R.drawable.bg_default_header),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_profile),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SiteBadge(text = currentSite)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SiteBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
