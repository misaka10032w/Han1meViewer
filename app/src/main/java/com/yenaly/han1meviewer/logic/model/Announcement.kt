package com.yenaly.han1meviewer.logic.model

import androidx.annotation.Keep
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@Keep
data class Announcement(
    @JvmField val title: String,
    @JvmField val content: String,
    @JvmField val positiveText: String ? = null,
    @JvmField val negativeText: String ? = null,
    @JvmField val timestamp: Long = 0,
    @JvmField val priority: Int = 1,
    @JvmField val imageUrl: String ? = null,
    @JvmField val isActive: Boolean = false
) {
    //firebase初始化需要一个空的构造函数
    constructor() : this("", "", null, null, 0L, 1, null, false)
    @OptIn(ExperimentalTime::class)
    fun getFormattedDate(): String {
        return kotlin.time.Instant
            .fromEpochSeconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LOCAL_DATE_TIME_FORMAT)
    }

    @Composable
    fun getFormatedContent(linkColor: Color = MaterialTheme.colorScheme.primary): AnnotatedString {
        val regex = "(https?://[\\w-]+(\\.[\\w-]+)+([/?%&=]*)?)".toRegex()

        return buildAnnotatedString {
            val matches = regex.findAll(content).toList()

            if (matches.isEmpty()) {
                append(content)
            } else {
                var lastIdx = 0
                for (match in matches) {
                    append(content.substring(lastIdx, match.range.first))
                    withLink(
                        LinkAnnotation.Url(
                            url = match.value,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append(match.value)
                    }
                    lastIdx = match.range.last + 1
                }
                if (lastIdx < content.length) {
                    append(content.substring(lastIdx))
                }
            }
        }
    }
}