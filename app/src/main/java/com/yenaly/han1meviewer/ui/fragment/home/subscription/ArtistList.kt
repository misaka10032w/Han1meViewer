package com.yenaly.han1meviewer.ui.fragment.home.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.SubscriptionItem

@Composable
fun ArtistList(artists: List<SubscriptionItem>, onClickArtist: (String) -> Unit) {
    Column {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            artists.forEach { artist ->
                ArtistItem(artist = artist, onClick = { onClickArtist(artist.artistName) })
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun ArtistItem(artist: SubscriptionItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RetryableImage(
            model = artist.avatar,
            contentDescription = artist.artistName,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Gray)
                .border(4.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            placeholder = painterResource(R.drawable.baseline_data_usage_24),
            error = painterResource(R.drawable.baseline_error_outline_24)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = artist.artistName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun SubscriptionAppPreview() {
    val fakeArtists = listOf(
        SubscriptionItem("初音未来", "null"),
        SubscriptionItem("绫波丽", "null"),
        SubscriptionItem("阿库娅", "null"),
        SubscriptionItem("初音未来", "null"),
        SubscriptionItem("绫波丽", "null"),
        SubscriptionItem("阿库娅", "null"),
        SubscriptionItem("初音未来", "null"),
        SubscriptionItem("绫波丽", "null"),
        SubscriptionItem("阿库娅", "null")
    )
    ArtistList(
        fakeArtists,
        onClickArtist = {}
    )
}