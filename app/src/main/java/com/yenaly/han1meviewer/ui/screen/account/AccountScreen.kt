package com.yenaly.han1meviewer.ui.screen.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.UserAccount
import com.yenaly.han1meviewer.logic.model.UserAccountAction
import com.yenaly.han1meviewer.logic.model.UserAccountSubmittingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.viewmodel.UserAccountViewModel
import com.yenaly.han1meviewer.util.pickVisualMedia
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    viewModel: UserAccountViewModel,
    onBack: () -> Unit,
    onOpenAvatarCrop: (String) -> Unit,
    pendingAvatarCropResult: String?,
    onAvatarCropResultConsumed: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.accountState.collectAsStateWithLifecycle()
    val submittingState by viewModel.submittingState.collectAsStateWithLifecycle()
    val modifyFailed = stringResource(R.string.modify_failed)
    val modifySuccess = stringResource(R.string.modify_success)
    LaunchedEffect(Unit) {
        viewModel.loadAccount()
    }

    LaunchedEffect(pendingAvatarCropResult) {
        val filePath = pendingAvatarCropResult ?: return@LaunchedEffect
        viewModel.updateAvatar(java.io.File(filePath))
        onAvatarCropResultConsumed()
    }

    LaunchedEffect(viewModel) {

        viewModel.actionFlow.collect { event ->
            when (event.state) {
                is WebsiteState.Error -> {
                    showShortToast(event.state.throwable.message ?: modifyFailed)
                }

                is WebsiteState.Success -> {
                    val message = when (event.action) {
                        UserAccountAction.ProfileUpdated -> modifySuccess
                        UserAccountAction.PasswordUpdated -> modifySuccess
                        UserAccountAction.AvatarUpdated -> modifySuccess
                    }
                    showShortToast(message)
                }

                WebsiteState.Loading -> Unit
            }
        }
    }

    HanimeScaffold(
        title = stringResource(R.string.my_account),
        onBack = onBack,
    ) { paddingValues ->
        PageContent(
            isLoading = state is WebsiteState.Loading,
            isError = state is WebsiteState.Error,
            isEmpty = false,
            onRetry = { viewModel.loadAccount(forceReload = true) },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorContent(
                        title = stringResource(R.string.load_failed_retry),
                        onRetry = { viewModel.loadAccount(forceReload = true) },
                    )
                }
            },
        ) {
            val account = (state as? WebsiteState.Success)?.info ?: return@PageContent
            AccountContent(
                account = account,
                submittingState = submittingState,
                contentPadding = paddingValues,
                onUpdateProfile = viewModel::updateProfile,
                onUpdatePassword = viewModel::updatePassword,
                onPickAvatar = {
                    val pickedUri = context.pickVisualMedia(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (pickedUri != null) {
                        onOpenAvatarCrop(pickedUri.toString())
                    }
                },
                onLogout = onLogout,
            )
        }
    }
}

@Composable
private fun AccountContent(
    account: UserAccount,
    submittingState: UserAccountSubmittingState,
    contentPadding: PaddingValues,
    onUpdateProfile: (String, String) -> Unit,
    onUpdatePassword: (String, String, String) -> Unit,
    onPickAvatar: suspend () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(account.username) { mutableStateOf(account.username) }
    var email by rememberSaveable(account.email) { mutableStateOf(account.email) }

    var oldPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var newPasswordConfirm by rememberSaveable { mutableStateOf("") }

    var oldPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val isUpdatingProfile = submittingState == UserAccountSubmittingState.UpdatingProfile
    val isUpdatingPassword = submittingState == UserAccountSubmittingState.UpdatingPassword
    val isUpdatingAvatar = submittingState == UserAccountSubmittingState.UpdatingAvatar
    val isSubmitting = submittingState != UserAccountSubmittingState.Idle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.size(108.dp)
                    ) {
                        AsyncImage(
                            model = account.avatarUrl,
                            contentDescription = account.username,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.bg_default_header),
                            error = painterResource(R.drawable.bg_default_header),
                            fallback = painterResource(R.drawable.bg_default_header),
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = { scope.launch { onPickAvatar() } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                    ) {
                        if (isUpdatingAvatar) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_edit_24),
                                contentDescription = stringResource(R.string.change_avatar),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "@ ${account.userId}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.account_stats_summary, account.subscriberCount, account.videoCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.username)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Button(
                    onClick = { onUpdateProfile(name.trim(), email.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting && name.isNotBlank() && email.isNotBlank(),
                ) {
                    if (isUpdatingProfile) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(stringResource(R.string.updating))
                    } else {
                        Text(stringResource(R.string.update_profile))
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.change_password),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(stringResource(R.string.old_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(
                                imageVector = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = newPasswordConfirm,
                    onValueChange = { newPasswordConfirm = it },
                    label = { Text(stringResource(R.string.confirm_new_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                TextButton(
                    onClick = { context.browse("${com.yenaly.han1meviewer.HANIME_BASE_URL}password/reset") },
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        if (newPassword != newPasswordConfirm) {
                            showShortToast(R.string.password_not_match)
                        } else {
                            onUpdatePassword(oldPassword, newPassword, newPasswordConfirm)
                            oldPassword = ""
                            newPassword = ""
                            newPasswordConfirm = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting && oldPassword.isNotBlank() && newPassword.isNotBlank() && newPasswordConfirm.isNotBlank(),
                ) {
                    if (isUpdatingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(stringResource(R.string.changing))
                    } else {
                        Text(stringResource(R.string.change_password))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout), fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun AccountScreenPreview() {
    ComponentPreview {
        AccountContent(
            account = UserAccount(
                csrfToken = "token",
                avatarUrl = "https://picsum.photos/200",
                username = "你的名字",
                email = "username@gmail.com",
                userId = "987654",
                subscriberCount = 0,
                videoCount = 9,
            ),
            contentPadding = PaddingValues(),
            onUpdateProfile = { _, _ -> },
            onUpdatePassword = { _, _, _ -> },
            onPickAvatar = {},
            onLogout = {},
            submittingState = UserAccountSubmittingState.Idle,
        )
    }
}
