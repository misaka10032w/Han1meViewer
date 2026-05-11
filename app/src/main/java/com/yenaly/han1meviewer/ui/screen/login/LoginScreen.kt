package com.yenaly.han1meviewer.ui.screen.login

import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ComponentPreview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShowLoginDialog: () -> Unit,
    onOpenQrScanner: () -> Unit,
    webViewFactory: () -> WebView,
) {
    val refreshingState = rememberPullToRefreshState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login)) },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.scan_for_cookies)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_scan_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = onOpenQrScanner,
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = refreshingState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshingState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            AndroidView(
                factory = { webViewFactory() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun LoginDialog(
    isLoggingIn: Boolean,
    onDismiss: () -> Unit,
    onLogin: (username: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.try_login_here)) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoggingIn,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoggingIn,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLogin(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank() && !isLoggingIn,
            ) {
                Text(stringResource(R.string.login))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoggingIn) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val context = LocalContext.current
    ComponentPreview {
        LoginScreen(
            isRefreshing = true,
            onBack = {},
            onRefresh = {},
            onShowLoginDialog = {},
            onOpenQrScanner = {},
            webViewFactory = {
                WebView(context).apply {
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginDialogPreview() {
    ComponentPreview {
        LoginDialog(
            isLoggingIn = false,
            onDismiss = {},
            onLogin = { _, _ -> },
        )
    }
}
