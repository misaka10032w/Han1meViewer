package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.github.Latest

private val urlPattern = Regex("(https?://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)")

private fun cleanUrl(url: String): String {
    var cleaned = url
    while (cleaned.endsWith(")") || cleaned.endsWith("]") || cleaned.endsWith("}") ||
        cleaned.endsWith(",") || cleaned.endsWith(".")
    ) {
        cleaned = cleaned.dropLast(1)
    }
    return cleaned
}

@Composable
fun UpdateDialog(
    latest: Latest,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val updateContentLabel = stringResource(R.string.update_content)
    val linkColor = MaterialTheme.colorScheme.primary
    val changelog = remember(latest, linkColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                append(latest.version)
            }
            append("\n\n")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(updateContentLabel)
            }
            append("\n")
            var lastIndex = 0
            urlPattern.findAll(latest.changelog).forEach { match ->
                val start = match.range.first
                append(latest.changelog.substring(lastIndex, start))
                val url = cleanUrl(match.value)
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(url)
                }

                lastIndex = match.range.last + 1
            }
            append(latest.changelog.substring(lastIndex))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_version_found)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(text = changelog)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
