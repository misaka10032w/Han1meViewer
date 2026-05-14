package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.util.logScreenViewEvent
import com.yenaly.yenaly_libs.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    navController: NavController,
    fallbackDestination: Any,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity<MainActivity>()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = SettingsDestinationSpec.fromDestination(backStackEntry?.destination)
        ?: SettingsDestinationSpec.Home

    fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate(fallbackDestination)
        }
    }

    LaunchedEffect(currentDestination) {
        activity.logScreenViewEvent(currentDestination.screenClassName)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentDestination.showToolbar) {
                TopAppBar(
                    title = { Text(stringResource(currentDestination.titleRes)) },
                    navigationIcon = {
                        IconButton(onClick = ::navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}
