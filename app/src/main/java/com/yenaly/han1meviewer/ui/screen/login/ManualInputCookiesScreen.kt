package com.yenaly.han1meviewer.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

@Composable
fun ManualInputCookiesScreen(
    onBack: () -> Unit,
    onCookieScanned: (String) -> Unit,
) {
    HanimeScaffold(
        title = stringResource(R.string.title_activity_qrcode_scanner),
        onBack = onBack,
    ) { innerPadding ->
        ScanCookieContent(innerPadding, onCookieScanned)
    }
}

@Composable
private fun ScanCookieContent(
    innerPadding: PaddingValues,
    onCookieScanned: (String) -> Unit,
) {
    val scannedText = remember { mutableStateOf("") }
    var showGuide by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        if (showGuide) {
            CookieGuideDialog { showGuide = false }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = scannedText.value,
                onValueChange = { scannedText.value = it },
                label = { Text(stringResource(R.string.cookies_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp),
                maxLines = 20,
                singleLine = false,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (scannedText.value.isEmpty() || scannedText.value.length < 100) return@Button
                    onCookieScanned(scannedText.value)
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}

@Composable
private fun CookieGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cookies_import_dismiss))
            }
        },
        title = { Text(stringResource(R.string.cookies_import_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.import_cookies_intro))
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(R.drawable.cookies_intro1),
                    contentDescription = stringResource(R.string.cookies_import_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            }
        },
    )
}