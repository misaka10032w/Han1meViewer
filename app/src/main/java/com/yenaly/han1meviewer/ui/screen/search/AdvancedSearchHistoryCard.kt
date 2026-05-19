package com.yenaly.han1meviewer.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity

@Composable
fun AdvancedSearchHistoryCard(
    history: HanimeAdvancedSearchHistoryEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val type = stringResource(R.string.type)
    val sortOption = stringResource(R.string.sort_option)
    val pairWidely = stringResource(R.string.pair_widely)
    val releaseDate = stringResource(R.string.release_date)
    val duration = stringResource(R.string.duration)
    val tag = stringResource(R.string.tag)
    val brand = stringResource(R.string.brand)
    val conditions = remember(history) {
        buildList {
            history.genre?.takeIf { it.isNotBlank() }?.let { add("$type: $it") }
            history.sort?.takeIf { it.isNotBlank() }?.let { add("$sortOption: $it") }
            if (history.broad == true) add(pairWidely)
            history.date?.takeIf { it.isNotBlank() }?.let { add("$releaseDate: $it") }
            history.duration?.takeIf { it.isNotBlank() }?.let { add("$duration: $it") }
            if (!history.tags.isNullOrBlank()) add("$tag: ${history.tags}")
            if (!history.brands.isNullOrBlank()) add("$brand: ${history.brands}")
        }.joinToString(" || ")
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                history.query?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (conditions.isNotBlank()) {
                    Text(
                        text = conditions,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_delete_24),
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}