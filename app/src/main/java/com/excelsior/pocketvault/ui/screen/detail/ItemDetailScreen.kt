@file:OptIn(ExperimentalLayoutApi::class, ExperimentalCoroutinesApi::class)

package com.excelsior.pocketvault.ui.screen.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.excelsior.pocketvault.core.common.decodeStoredSecret
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.data.security.LockStateManager
import com.excelsior.pocketvault.core.designsystem.component.VaultActionChip
import com.excelsior.pocketvault.core.designsystem.component.VaultConfirmDialog
import com.excelsior.pocketvault.core.designsystem.component.VaultEmptyState
import com.excelsior.pocketvault.core.designsystem.component.VaultFolderBadge
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.core.designsystem.component.VaultSectionTitle
import com.excelsior.pocketvault.core.designsystem.component.VaultTagRow
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.CredentialVaultItem
import com.excelsior.pocketvault.domain.model.ImageVaultItem
import com.excelsior.pocketvault.domain.model.ItemProtectionLevel
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.LinkVaultItem
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.TextVaultItem
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.usecase.DecryptCredentialUseCase
import com.excelsior.pocketvault.domain.usecase.DeleteItemUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveItemDetailUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.ToggleFavoriteUseCase
import com.excelsior.pocketvault.domain.usecase.TogglePinUseCase
import com.excelsior.pocketvault.domain.usecase.VerifyAppLockUseCase
import com.excelsior.pocketvault.domain.usecase.VerifySecondPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ItemDetailRoute(
    itemId: String,
    autoUnlock: Boolean,
    onBack: () -> Unit,
    onEdit: (ItemType, String) -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(itemId, autoUnlock) {
        viewModel.load(itemId, autoUnlock)
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    LaunchedEffect(state.item?.id, state.requiresItemUnlock, state.itemUnlocked, state.itemUnlockDialogVisible) {
        if (state.item != null && state.requiresItemUnlock && !state.itemUnlocked && !state.itemUnlockDialogVisible) {
            viewModel.requestItemUnlock()
        }
    }

    ItemDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onTogglePinned = viewModel::togglePinned,
        onToggleFavorite = viewModel::toggleFavorite,
        onDeleteRequested = viewModel::requestDelete,
        onToggleSecret = viewModel::toggleSecretVisibility,
        onSecretPinChanged = viewModel::onSecretPinChanged,
        onConfirmSecretChallenge = viewModel::confirmSecretUnlock,
        onDismissSecretChallenge = viewModel::dismissSecretChallenge,
        onRequestItemUnlock = viewModel::requestItemUnlock,
        onItemPinChanged = viewModel::onItemPinChanged,
        onItemSecondPinChanged = viewModel::onItemSecondPinChanged,
        onConfirmItemUnlock = viewModel::confirmItemUnlock,
        onDismissItemUnlock = onBack,
        onDeletePinChanged = viewModel::onDeletePinChanged,
        onConfirmDeleteAuth = viewModel::confirmDeleteAuth,
        onDismissDeleteAuth = viewModel::dismissDeleteAuth,
    )
}

@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onBack: () -> Unit,
    onEdit: (ItemType, String) -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteRequested: () -> Unit,
    onToggleSecret: () -> Unit,
    onSecretPinChanged: (String) -> Unit,
    onConfirmSecretChallenge: () -> Unit,
    onDismissSecretChallenge: () -> Unit,
    onRequestItemUnlock: () -> Unit,
    onItemPinChanged: (String) -> Unit,
    onItemSecondPinChanged: (String) -> Unit,
    onConfirmItemUnlock: () -> Unit,
    onDismissItemUnlock: () -> Unit,
    onDeletePinChanged: (String) -> Unit,
    onConfirmDeleteAuth: () -> Unit,
    onDismissDeleteAuth: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = state.item?.title.orEmpty().ifBlank { "内容详情" },
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
                actions = {
                    state.item?.let { item ->
                        IconButton(onClick = { onEdit(item.type, item.id) }) {
                            Icon(imageVector = VaultIcons.Edit, contentDescription = null)
                        }
                    }
                },
            )
        }
        val vaultItem = state.item
        if (vaultItem == null) {
            item {
                VaultEmptyState(
                    title = "内容不存在",
                    description = "这条收藏可能已经被删除，或者还没有完成加载。",
                    actionLabel = "返回",
                    onAction = onBack,
                )
            }
        } else if (state.requiresItemUnlock && !state.itemUnlocked) {
            item {
                VaultEmptyState(
                    title = "此内容已加密",
                    description = "",
                    actionLabel = "输入密码",
                    onAction = onRequestItemUnlock,
                )
            }
        } else {
            item {
                VaultGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "创 ${VaultFormatters.formatDateTimeCompact(vaultItem.createdAt)} / 更 ${VaultFormatters.formatDateTimeCompact(vaultItem.updatedAt)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        vaultItem.folder?.let { folder ->
                            VaultFolderBadge(text = folder.name)
                        }
                        vaultItem.tags.forEach { tag ->
                            VaultFolderBadge(text = "#${tag.name}")
                        }
                    }
                }
            }
            item {
                DetailActionCard(
                    protectionLevel = vaultItem.protectionLevel,
                    isPinned = vaultItem.isPinned,
                    isFavorite = vaultItem.isFavorite,
                    onTogglePinned = onTogglePinned,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDeleteRequested,
                )
            }
            when (vaultItem) {
                is LinkVaultItem -> {
                    item {
                        LinkDetailCard(
                            item = vaultItem,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(vaultItem.url))
                                context.toast("链接已复制")
                            },
                            onOpen = { context.openLink(vaultItem.url) },
                        )
                    }
                }

                is TextVaultItem -> {
                    item {
                        TextDetailCard(
                            item = vaultItem,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(vaultItem.content))
                                context.toast("正文已复制")
                            },
                        )
                    }
                }

                is ImageVaultItem -> {
                    item {
                        ImageDetailCard(item = vaultItem)
                    }
                }

                is CredentialVaultItem -> {
                    item {
                        CredentialDetailCard(
                            item = vaultItem,
                            state = state,
                            onToggleSecret = onToggleSecret,
                            onCopyUsername = {
                                clipboardManager.setText(AnnotatedString(vaultItem.username))
                                context.toast("用户名已复制")
                            },
                            onCopyPassword = {
                                if (state.decryptedPassword.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(state.decryptedPassword))
                                    context.toast("密码已复制")
                                }
                            },
                            onOpen = {
                                vaultItem.websiteUrl?.takeIf(String::isNotBlank)?.let(context::openLink)
                            },
                        )
                    }
                }
            }
        }
    }

    VaultConfirmDialog(
        visible = showDeleteDialog,
        title = "删除这条收藏？",
        message = "删除后无法恢复，图片原文件也会一并清理。",
        confirmLabel = "删除",
        onConfirm = {
            showDeleteDialog = false
            onDeleteRequested()
        },
        onDismiss = { showDeleteDialog = false },
    )

    SecretUnlockDialog(
        visible = state.secretChallengeVisible,
        pin = state.secretPin,
        error = state.secretChallengeError,
        onPinChanged = onSecretPinChanged,
        onConfirm = onConfirmSecretChallenge,
        onDismiss = onDismissSecretChallenge,
    )

    ItemUnlockDialog(
        visible = state.itemUnlockDialogVisible,
        requireSecondPin = state.itemUnlockRequireSecondPin,
        pin = state.itemUnlockPin,
        secondPin = state.itemUnlockSecondPin,
        error = state.itemUnlockError,
        onPinChanged = onItemPinChanged,
        onSecondPinChanged = onItemSecondPinChanged,
        onConfirm = onConfirmItemUnlock,
        onDismiss = onDismissItemUnlock,
    )

    if (state.deleteAuthVisible) {
        PasswordPromptDialog(
            title = "删除前输入主密码",
            value = state.deleteAuthPin,
            onValueChange = onDeletePinChanged,
            error = state.deleteAuthError,
            onConfirm = onConfirmDeleteAuth,
            onDismiss = onDismissDeleteAuth,
            confirmEnabled = state.deleteAuthPin.length >= 4,
        )
    }
}

@Composable
private fun DetailActionCard(
    protectionLevel: ItemProtectionLevel,
    isPinned: Boolean,
    isFavorite: Boolean,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    VaultGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onTogglePinned,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (isPinned) "取消置顶" else "置顶",
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (protectionLevel != ItemProtectionLevel.SUPER) {
                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (isFavorite) "取消收藏" else "收藏",
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            Button(
                onClick = onDelete,
                modifier = if (protectionLevel != ItemProtectionLevel.SUPER) {
                    Modifier.weight(1f)
                } else {
                    Modifier.weight(1f)
                },
            ) {
                Text(text = "删除", maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun DetailInlineAction(
    label: String,
    icon: ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (destructive) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (destructive) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinkDetailCard(
    item: LinkVaultItem,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
) {
    VaultGlassCard {
        Text(text = item.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = item.url,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        item.note?.takeIf(String::isNotBlank)?.let { note ->
            Text(
                text = note,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultActionChip(label = "复制链接", icon = VaultIcons.Copy, onClick = onCopy)
            VaultActionChip(label = "打开", icon = VaultIcons.Open, onClick = onOpen)
        }
    }
}

@Composable
private fun TextDetailCard(
    item: TextVaultItem,
    onCopy: () -> Unit,
) {
    VaultGlassCard {
        item.title?.takeIf(String::isNotBlank)?.let { title ->
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = item.content,
            modifier = Modifier.padding(top = if (item.title.isNullOrBlank()) 0.dp else 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        item.quoteAuthor?.takeIf(String::isNotBlank)?.let { author ->
            Text(
                text = author,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item.source?.takeIf(String::isNotBlank)?.let { source ->
            Text(
                text = source,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultActionChip(label = "复制全文", icon = VaultIcons.Copy, onClick = onCopy)
        }
    }
}

@Composable
private fun ImageDetailCard(item: ImageVaultItem) {
    var zoom by remember { mutableStateOf(1f) }
    val transformState = rememberTransformableState { scaleChange, _, _ ->
        zoom = (zoom * scaleChange).coerceIn(1f, 4f)
    }
    VaultGlassCard {
        AsyncImage(
            model = item.localImagePath,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
                .transformable(transformState),
        )
        Text(
            text = item.title ?: "未命名图片",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        item.note?.takeIf(String::isNotBlank)?.let { note ->
            Text(
                text = note,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = "宽高比 ${VaultFormatters.aspectRatioText(item.aspectRatio)}",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CredentialDetailCard(
    item: CredentialVaultItem,
    state: ItemDetailUiState,
    onToggleSecret: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onOpen: () -> Unit,
) {
    VaultGlassCard {
        Text(text = item.siteName, style = MaterialTheme.typography.headlineMedium)
        CredentialInfoRow(
            label = "账号",
            value = item.username,
            modifier = Modifier.padding(top = 16.dp),
        )
        item.email?.takeIf(String::isNotBlank)?.let { email ->
            CredentialInfoRow(
                label = "邮箱",
                value = email,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        CredentialInfoRow(
            label = "密码",
            value = if (state.secretVisible) state.decryptedPassword else "••••••••",
            modifier = Modifier.padding(top = 10.dp),
        )
        state.decryptedNote.takeIf(String::isNotBlank)?.let { note ->
            if (state.secretVisible) {
                CredentialInfoRow(
                    label = "备注",
                    value = note,
                    modifier = Modifier.padding(top = 10.dp),
                    singleLine = false,
                )
            }
        }
        if (state.secretError != null) {
            Text(
                text = state.secretError,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DetailActionButton(
                modifier = Modifier.weight(1f),
                label = if (state.secretVisible) {
                    "隐藏密码"
                } else if (item.secretsEncrypted) {
                    "解密查看"
                } else {
                    "显示密码"
                },
                icon = if (state.secretVisible) VaultIcons.Hidden else VaultIcons.Visible,
                onClick = onToggleSecret,
            )
            DetailActionButton(
                modifier = Modifier.weight(1f),
                label = "复制账号",
                icon = VaultIcons.Copy,
                onClick = onCopyUsername,
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.secretVisible && state.decryptedPassword.isNotBlank()) {
                DetailActionButton(
                    modifier = Modifier.weight(1f),
                    label = "复制密码",
                    icon = VaultIcons.Copy,
                    onClick = onCopyPassword,
                )
            }
            item.websiteUrl?.takeIf(String::isNotBlank)?.let {
                DetailActionButton(
                    modifier = Modifier.weight(1f),
                    label = "打开网站",
                    icon = VaultIcons.Open,
                    onClick = onOpen,
                )
            }
        }
        item.tags.takeIf { it.isNotEmpty() }?.let { tags ->
            VaultTagRow(tags = tags, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun CredentialInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun SecretUnlockDialog(
    visible: Boolean,
    pin: String,
    error: String?,
    onPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    PasswordPromptDialog(
        title = "输入主密码",
        value = pin,
        onValueChange = onPinChanged,
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = pin.length >= 4,
    )
}

@Composable
private fun ItemUnlockDialog(
    visible: Boolean,
    requireSecondPin: Boolean,
    pin: String,
    secondPin: String,
    error: String?,
    onPinChanged: (String) -> Unit,
    onSecondPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    PasswordPromptDialog(
        title = if (requireSecondPin) "输入二次密码" else "输入主密码",
        value = if (requireSecondPin) secondPin else pin,
        onValueChange = if (requireSecondPin) onSecondPinChanged else onPinChanged,
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = if (requireSecondPin) secondPin.length >= 4 else pin.length >= 4,
    )
}

@Composable
private fun PasswordPromptDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            ),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                VaultPasswordField(
                    value = value,
                    onValueChange = onValueChange,
                    label = "密码",
                    placeholder = "至少 4 位",
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "取消")
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "确认")
                    }
                }
            }
        }
    }
}

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val observeItemDetailUseCase: ObserveItemDetailUseCase,
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    private val decryptCredentialUseCase: DecryptCredentialUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val togglePinUseCase: TogglePinUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val verifyAppLockUseCase: VerifyAppLockUseCase,
    private val verifySecondPinUseCase: VerifySecondPinUseCase,
    private val lockStateManager: LockStateManager,
) : androidx.lifecycle.ViewModel() {
    private val itemId = MutableStateFlow<String?>(null)
    private val secretVisible = MutableStateFlow(false)
    private val decryptedPassword = MutableStateFlow("")
    private val decryptedNote = MutableStateFlow("")
    private val secretError = MutableStateFlow<String?>(null)
    private val secretChallengeVisible = MutableStateFlow(false)
    private val secretPin = MutableStateFlow("")
    private val secretChallengeError = MutableStateFlow<String?>(null)
    private val itemUnlocked = MutableStateFlow(false)
    private val itemUnlockDialogVisible = MutableStateFlow(false)
    private val itemUnlockRequireSecondPin = MutableStateFlow(false)
    private val itemUnlockPin = MutableStateFlow("")
    private val itemUnlockSecondPin = MutableStateFlow("")
    private val itemUnlockError = MutableStateFlow<String?>(null)
    private val deleteAuthVisible = MutableStateFlow(false)
    private val deleteAuthPin = MutableStateFlow("")
    private val deleteAuthError = MutableStateFlow<String?>(null)
    private val deleted = MutableStateFlow(false)

    private val itemFlow = itemId.filterNotNull().flatMapLatest(observeItemDetailUseCase::invoke)
    private val secretContentState = combine(
        secretVisible,
        decryptedPassword,
        decryptedNote,
        secretError,
    ) { visible, password, note, error ->
        SecretContentState(
            visible = visible,
            password = password,
            note = note,
            error = error,
        )
    }
    private val secretChallengeState = combine(
        secretChallengeVisible,
        secretPin,
        secretChallengeError,
    ) { visible, pin, error ->
        SecretChallengeState(
            visible = visible,
            pin = pin,
            error = error,
        )
    }
    private val secretUiState = combine(
        secretContentState,
        secretChallengeState,
    ) { contentState, challengeState ->
        SecretUiState(
            visible = contentState.visible,
            password = contentState.password,
            note = contentState.note,
            error = contentState.error,
            challengeVisible = challengeState.visible,
            pin = challengeState.pin,
            challengeError = challengeState.error,
        )
    }

    private val itemUnlockFlags = combine(
        itemUnlocked,
        itemUnlockDialogVisible,
        itemUnlockRequireSecondPin,
    ) { unlocked, dialogVisible, requireSecondPin ->
        Triple(unlocked, dialogVisible, requireSecondPin)
    }
    private val itemUnlockValues = combine(
        itemUnlockPin,
        itemUnlockSecondPin,
        itemUnlockError,
    ) { pin, secondPin, error ->
        Triple(pin, secondPin, error)
    }
    private val itemUnlockUiState = combine(
        itemUnlockFlags,
        itemUnlockValues,
    ) { flags, values ->
        ItemUnlockState(
            unlocked = flags.first,
            dialogVisible = flags.second,
            requireSecondPin = flags.third,
            pin = values.first,
            secondPin = values.second,
            error = values.third,
        )
    }

    private val itemAndSecurity = combine(
        itemFlow,
        observeSecuritySettingsUseCase(),
    ) { item, securitySettings ->
        item to securitySettings
    }
    private val secureAndDeleted = combine(
        lockStateManager.isSecureSpaceUnlocked,
        deleted,
    ) { secureSpaceUnlocked, isDeleted ->
        secureSpaceUnlocked to isDeleted
    }
    private val deleteAuthState = combine(
        deleteAuthVisible,
        deleteAuthPin,
        deleteAuthError,
    ) { visible, pin, error ->
        Triple(visible, pin, error)
    }

    val uiState: StateFlow<ItemDetailUiState> = combine(
        itemAndSecurity,
        secretUiState,
        itemUnlockUiState,
        secureAndDeleted,
        deleteAuthState,
    ) { itemAndSecurityState, secretState, unlockState, secureAndDeletedState, deleteAuth ->
        val item = itemAndSecurityState.first
        val securitySettings = itemAndSecurityState.second
        val secureSpaceUnlocked = secureAndDeletedState.first
        val isDeleted = secureAndDeletedState.second
        val requiresItemUnlock = when (item?.protectionLevel) {
            ItemProtectionLevel.STANDARD -> true
            ItemProtectionLevel.SUPER -> !secureSpaceUnlocked
            else -> false
        }
        val effectiveItemUnlocked = when (item?.protectionLevel) {
            ItemProtectionLevel.STANDARD -> unlockState.unlocked
            ItemProtectionLevel.SUPER -> secureSpaceUnlocked || unlockState.unlocked
            else -> true
        }
        ItemDetailUiState(
            item = item,
            securitySettings = securitySettings,
            secretVisible = secretState.visible,
            decryptedPassword = secretState.password,
            decryptedNote = secretState.note,
            secretError = secretState.error,
            secretChallengeVisible = secretState.challengeVisible,
            secretPin = secretState.pin,
            secretChallengeError = secretState.challengeError,
            itemUnlocked = effectiveItemUnlocked,
            requiresItemUnlock = requiresItemUnlock,
            itemUnlockDialogVisible = unlockState.dialogVisible,
            itemUnlockRequireSecondPin = unlockState.requireSecondPin,
            itemUnlockPin = unlockState.pin,
            itemUnlockSecondPin = unlockState.secondPin,
            itemUnlockError = unlockState.error,
            deleteAuthVisible = deleteAuth.first,
            deleteAuthPin = deleteAuth.second,
            deleteAuthError = deleteAuth.third,
            isDeleted = isDeleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemDetailUiState(),
    )

    fun load(value: String, autoUnlock: Boolean = false) {
        if (itemId.value == value) return
        itemId.update { value }
        secretVisible.update { false }
        decryptedPassword.update { "" }
        decryptedNote.update { "" }
        secretError.update { null }
        secretChallengeVisible.update { false }
        secretPin.update { "" }
        secretChallengeError.update { null }
        itemUnlocked.update { autoUnlock }
        itemUnlockDialogVisible.update { false }
        itemUnlockRequireSecondPin.update { false }
        itemUnlockPin.update { "" }
        itemUnlockSecondPin.update { "" }
        itemUnlockError.update { null }
        deleteAuthVisible.update { false }
        deleteAuthPin.update { "" }
        deleteAuthError.update { null }
        deleted.update { false }
    }

    fun requestItemUnlock() {
        val currentItem = uiState.value.item ?: return
        when (currentItem.protectionLevel) {
            ItemProtectionLevel.NONE -> itemUnlocked.update { true }
            ItemProtectionLevel.STANDARD -> {
                itemUnlockDialogVisible.update { true }
                itemUnlockRequireSecondPin.update { false }
                itemUnlockPin.update { "" }
                itemUnlockError.update { null }
            }
            ItemProtectionLevel.SUPER -> {
                if (lockStateManager.isSecureSpaceUnlocked.value) {
                    itemUnlocked.update { true }
                } else {
                    itemUnlockDialogVisible.update { true }
                    itemUnlockRequireSecondPin.update { false }
                    itemUnlockPin.update { "" }
                    itemUnlockError.update { null }
                }
            }
        }
    }

    fun togglePinned() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            togglePinUseCase(currentItem.id)
        }
    }

    fun toggleFavorite() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(currentItem.id)
        }
    }

    fun deleteItem() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            deleteItemUseCase(currentItem.id)
            deleted.update { true }
        }
    }

    fun requestDelete() {
        val currentItem = uiState.value.item ?: return
        if (currentItem.protectionLevel == ItemProtectionLevel.NONE) {
            deleteItem()
            return
        }
        deleteAuthVisible.update { true }
        deleteAuthPin.update { "" }
        deleteAuthError.update { null }
    }

    fun onDeletePinChanged(value: String) {
        deleteAuthPin.update { value.take(32) }
        deleteAuthError.update { null }
    }

    fun dismissDeleteAuth() {
        deleteAuthVisible.update { false }
        deleteAuthPin.update { "" }
        deleteAuthError.update { null }
    }

    fun confirmDeleteAuth() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            val verified = verifyAppLockUseCase(deleteAuthPin.value)
            if (!verified) {
                deleteAuthError.update { "主密码不正确。" }
                return@launch
            }
            dismissDeleteAuth()
            deleteItemUseCase(currentItem.id)
            deleted.update { true }
        }
    }

    fun toggleSecretVisibility() {
        val credential = uiState.value.item as? CredentialVaultItem ?: return
        if (secretVisible.value) {
            hideSecret()
            return
        }
        if (
            false
        ) {
            secretChallengeVisible.update { true }
            secretPin.update { "" }
            secretChallengeError.update { null }
            return
        }
        decryptAndReveal(credential)
    }

    fun onSecretPinChanged(value: String) {
        secretPin.update { value.take(32) }
        secretChallengeError.update { null }
    }

    fun dismissSecretChallenge() {
        secretChallengeVisible.update { false }
        secretPin.update { "" }
        secretChallengeError.update { null }
    }

    fun onItemPinChanged(value: String) {
        itemUnlockPin.update { value.take(32) }
        itemUnlockError.update { null }
    }

    fun onItemSecondPinChanged(value: String) {
        itemUnlockSecondPin.update { value.take(32) }
        itemUnlockError.update { null }
    }

    fun dismissItemUnlock() {
        itemUnlockDialogVisible.update { false }
        itemUnlockPin.update { "" }
        itemUnlockSecondPin.update { "" }
        itemUnlockRequireSecondPin.update { false }
        itemUnlockError.update { null }
    }

    fun confirmItemUnlock() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            if (!itemUnlockRequireSecondPin.value) {
                val verified = verifyAppLockUseCase(itemUnlockPin.value)
                if (!verified) {
                    itemUnlockError.update { "PIN 不正确。" }
                    return@launch
                }
                if (currentItem.protectionLevel == ItemProtectionLevel.SUPER && uiState.value.securitySettings.hasSecondPin) {
                    itemUnlockRequireSecondPin.update { true }
                    itemUnlockSecondPin.update { "" }
                    itemUnlockError.update { null }
                    return@launch
                }
                if (currentItem.protectionLevel == ItemProtectionLevel.SUPER) {
                    lockStateManager.markSecureSpaceUnlocked()
                }
                itemUnlocked.update { true }
                dismissItemUnlock()
                return@launch
            }

            val secondVerified = verifySecondPinUseCase(itemUnlockSecondPin.value)
            if (!secondVerified) {
                itemUnlockError.update { "二次密码不正确。" }
                return@launch
            }
            lockStateManager.markSecureSpaceUnlocked()
            itemUnlocked.update { true }
            dismissItemUnlock()
        }
    }

    fun confirmSecretUnlock() {
        val credential = uiState.value.item as? CredentialVaultItem ?: return
        val pin = secretPin.value
        if (pin.length < 4) {
            secretChallengeError.update { "请输入至少 4 位主密码。" }
            return
        }
        viewModelScope.launch {
            val verified = verifyAppLockUseCase(pin)
            if (!verified) {
                secretChallengeError.update { "PIN 不正确，请重试。" }
                return@launch
            }
            dismissSecretChallenge()
            decryptAndReveal(credential)
        }
    }

    private fun hideSecret() {
        secretVisible.update { false }
        decryptedPassword.update { "" }
        decryptedNote.update { "" }
        secretError.update { null }
    }

    private fun decryptAndReveal(credential: CredentialVaultItem) {
        viewModelScope.launch {
            val password = decodeStoredSecret(
                storedValue = credential.encryptedPassword,
                decryptor = decryptCredentialUseCase::invoke,
            ).getOrElse {
                secretError.update { "密码解密失败，请稍后再试。" }
                return@launch
            }
            val note = credential.encryptedNote?.let { storedValue ->
                decodeStoredSecret(
                    storedValue = storedValue,
                    decryptor = decryptCredentialUseCase::invoke,
                ).getOrNull().orEmpty()
            }.orEmpty()
            decryptedPassword.update { password }
            decryptedNote.update { note }
            secretError.update { null }
            secretVisible.update { true }
        }
    }
}

data class ItemDetailUiState(
    val item: VaultItem? = null,
    val securitySettings: SecuritySettings = SecuritySettings(),
    val itemUnlocked: Boolean = true,
    val requiresItemUnlock: Boolean = false,
    val itemUnlockDialogVisible: Boolean = false,
    val itemUnlockRequireSecondPin: Boolean = false,
    val itemUnlockPin: String = "",
    val itemUnlockSecondPin: String = "",
    val itemUnlockError: String? = null,
    val deleteAuthVisible: Boolean = false,
    val deleteAuthPin: String = "",
    val deleteAuthError: String? = null,
    val secretVisible: Boolean = false,
    val decryptedPassword: String = "",
    val decryptedNote: String = "",
    val secretError: String? = null,
    val secretChallengeVisible: Boolean = false,
    val secretPin: String = "",
    val secretChallengeError: String? = null,
    val isDeleted: Boolean = false,
)

private data class SecretUiState(
    val visible: Boolean = false,
    val password: String = "",
    val note: String = "",
    val error: String? = null,
    val challengeVisible: Boolean = false,
    val pin: String = "",
    val challengeError: String? = null,
)

private data class SecretContentState(
    val visible: Boolean = false,
    val password: String = "",
    val note: String = "",
    val error: String? = null,
)

private data class SecretChallengeState(
    val visible: Boolean = false,
    val pin: String = "",
    val error: String? = null,
)

private data class ItemUnlockState(
    val unlocked: Boolean = false,
    val dialogVisible: Boolean = false,
    val requireSecondPin: Boolean = false,
    val pin: String = "",
    val secondPin: String = "",
    val error: String? = null,
)

private fun VaultItem.detailSubtitle(): String = when (this) {
    is LinkVaultItem -> "链接收藏"
    is TextVaultItem -> "便签收藏"
    is ImageVaultItem -> "图片收藏"
    is CredentialVaultItem -> "账号密码"
}

private fun ItemType.detailLabel(): String = when (this) {
    ItemType.LINK -> "链接详情"
    ItemType.TEXT -> "便签详情"
    ItemType.IMAGE -> "图片详情"
    ItemType.CREDENTIAL -> "密码详情"
}

private fun android.content.Context.openLink(value: String) {
    runCatching {
        val normalized = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
    }.onFailure {
        toast("无法打开链接")
    }
}

private fun android.content.Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
