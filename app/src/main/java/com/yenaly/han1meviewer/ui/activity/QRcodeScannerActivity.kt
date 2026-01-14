package com.yenaly.han1meviewer.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import java.util.Locale

class QRcodeScannerActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HanimeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.title_activity_qrcode_scanner),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Localized description"
                                    )
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    ScanCookieScreen(innerPadding) { scannedCookie ->
                        val resultIntent = Intent().apply {
                            putExtra("cookie", scannedCookie)
                            Log.i("LoginActivity", scannedCookie)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }
    }

    private fun applyAppLocale(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lang = prefs.getString("app_language", "system") ?: "system"
        val newLocale = when (lang) {
            "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
            "zh" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            else -> Resources.getSystem().configuration.locales.get(0)
        }

        Locale.setDefault(newLocale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)
        return context.createConfigurationContext(config)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyAppLocale(newBase))
    }
}

@Composable
fun ScanCookieScreen(innerPadding: PaddingValues, onCookieScanned: (String) -> Unit) {
    val scannedText = remember { mutableStateOf("") }
    var showGuide by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (showGuide) {
            CookieGuideDialog { showGuide = false }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = scannedText.value,
                onValueChange = { scannedText.value = it },
                label = { Text("Cookies") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp),
                maxLines = 20,
                singleLine = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onCookieScanned(scannedText.value)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanCookieScreenPreview() {
    ScanCookieScreen(PaddingValues(0.dp),{})
}

@Composable
fun CookieGuideDialog(onDismiss: () -> Unit) {
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
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.import_cookies_intro)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(R.drawable.cookies_intro1),
                    contentDescription = stringResource(R.string.cookies_import_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    )
}