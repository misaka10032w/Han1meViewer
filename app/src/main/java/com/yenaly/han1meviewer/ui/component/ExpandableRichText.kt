package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.longText

private const val UrlAnnotationTag = "url"
private val UrlRegex = Regex("""(https?://\S+)""")

@Composable
fun ExpandableRichText(
    text: String,
    modifier: Modifier = Modifier,
    maxCollapsedLines: Int = 4,
    onLinkClick: ((String) -> Unit)? = null,
) {
    if (text.isBlank()) return

    val annotatedText = remember(text) { buildLinkAnnotatedString(text) }
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    var expanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "expandable-rich-text",
            ) { isExpanded ->
                SelectionContainer {
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) Int.MAX_VALUE else maxCollapsedLines,
                        overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            layoutResult = result
                            if (!isExpanded) {
                                hasOverflow = result.hasVisualOverflow
                            }
                        },
                        modifier = Modifier.pointerInput(annotatedText, layoutResult) {
                            detectTapGestures { tapOffset ->
                                val result = layoutResult ?: return@detectTapGestures
                                val offset = result.getOffsetForPosition(tapOffset)
                                annotatedText
                                    .getStringAnnotations(UrlAnnotationTag, offset, offset)
                                    .firstOrNull()
                                    ?.item
                                    ?.let { url ->
                                        if (onLinkClick != null) onLinkClick(url)
                                        else uriHandler.openUri(url)
                                    }
                            }
                        },
                    )
                }
            }
            if (!expanded && hasOverflow) {
                Text(
                    text = stringResource(R.string.expand),
                    color = linkColor,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                        .clickable { expanded = true }
                        .padding(4.dp)
                )
            }

            if (expanded) {
                Text(
                    text = stringResource(R.string.collapse),
                    color = linkColor,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                        .clickable { expanded = false }
                        .padding(4.dp)
                )
            }
        }
    }
}

private fun buildLinkAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        UrlRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (start > currentIndex) {
                append(text.substring(currentIndex, start))
            }
            val url = match.value
            pushStringAnnotation(tag = UrlAnnotationTag, annotation = url)
            pushStyle(
                SpanStyle(
                    color = Color.Unspecified,
                    textDecoration = TextDecoration.Underline,
                )
            )
            append(url)
            pop()
            pop()
            currentIndex = end
        }
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun ExpandableRichTextPreview() {
    ComponentPreview {
        ExpandableRichText(
            text = longText
        )
    }
}
