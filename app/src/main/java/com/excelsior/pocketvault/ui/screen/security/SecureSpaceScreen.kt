@file:OptIn(ExperimentalLayoutApi::class)

package com.excelsior.pocketvault.ui.screen.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.data.security.LockStateManager
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.core.designsystem.component.VaultEmptyState
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultFolderBadge
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.usecase.ObserveAppearanceSettingsUseCase
import com.excelsior.pocketvault.domain.usecase.DeleteItemUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecureItemsUseCase
import com.excelsior.pocketvault.domain.usecase.VerifyAppLockUseCase
import com.excelsior.pocketvault.domain.usecase.VerifySecondPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SecureSpaceRoute(
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: SecureSpaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.leave()
        onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.onEnter()
    }

    SecureSpaceScreen(
        state = state,
        onBack = onBack,
        onItemClick = onItemClick,
        onToggleSelectionMode = viewModel::toggleSelectionMode,
        onToggleSelection = viewModel::toggleSelection,
        onDeleteSelected = viewModel::deleteSelected,
        onPinChanged = viewModel::onPinChanged,
        onSecondPinChanged = viewModel::onSecondPinChanged,
        onConfirm = viewModel::confirmUnlock,
        onDismiss = {
            viewModel.leave()
            onBack()
        },
    )
}

@Composable
fun SecureSpaceScreen(
    state: SecureSpaceUiState,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onPinChanged: (String) -> Unit,
    onSecondPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = if (state.selectionMode) "${state.selectedIds.size} 已选" else "加密空间",
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
                actions = {
                    if (state.unlocked) {
                        if (state.selectionMode) {
                            IconButton(onClick = onDeleteSelected, enabled = state.selectedIds.isNotEmpty()) {
                                Icon(imageVector = VaultIcons.Delete, contentDescription = null)
                            }
                            IconButton(onClick = onToggleSelectionMode) {
                                Icon(imageVector = VaultIcons.Back, contentDescription = null)
                            }
                        } else {
                            IconButton(onClick = onToggleSelectionMode) {
                                Icon(imageVector = VaultIcons.Edit, contentDescription = null)
                            }
                        }
                    }
                },
            )
        }
        if (!state.securitySettings.hasPin) {
            item {
                VaultEmptyState(
                    title = "请先设置主 PIN",
                    description = "",
                    actionLabel = "返回",
                    onAction = onDismiss,
                )
            }
        } else if (!state.unlocked) {
            item {
                VaultGlassCard {
                    Text(
                        text = if (state.requireSecondPin) "输入二次密码" else "输入主密码",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    VaultPasswordField(
                        value = if (state.requireSecondPin) state.secondPin else state.pin,
                        onValueChange = if (state.requireSecondPin) onSecondPinChanged else onPinChanged,
                        label = if (state.requireSecondPin) "二次密码" else "主密码",
                        placeholder = "至少 4 位",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    state.error?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(text = "取消")
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            enabled = if (state.requireSecondPin) state.secondPin.length >= 4 else state.pin.length >= 4,
                        ) {
                            Text(text = "进入")
                        }
                    }
                }
            }
        } else if (state.items.isEmpty()) {
            item {
                VaultEmptyState(
                    title = "加密空间为空",
                    description = "",
                )
            }
        } else if (state.cardStyle == "double") {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((state.items.size.coerceAtLeast(1) * 214).dp),
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        SecureSpaceItemCard(
                            item = item,
                            compact = true,
                            selected = state.selectionMode && state.selectedIds.contains(item.id),
                            onClick = {
                                if (state.selectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    onItemClick(item.id)
                                }
                            },
                        )
                    }
                }
            }
        } else {
            items(state.items, key = { it.id }) { item ->
                SecureSpaceItemCard(
                    item = item,
                    compact = false,
                    selected = state.selectionMode && state.selectedIds.contains(item.id),
                    onClick = {
                        if (state.selectionMode) {
                            onToggleSelection(item.id)
                        } else {
                            onItemClick(item.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SecureSpaceItemCard(
    item: VaultItem,
    compact: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box {
        VaultGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 202.dp else 164.dp),
            onClick = onClick,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                ) {
                    Icon(
                        imageVector = item.type.secureIcon(),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = item.secureTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = item.secureSummary(),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compact) 4 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.folder?.let { folder ->
                    VaultFolderBadge(text = folder.name)
                }
                if (item.isPinned) {
                    VaultFolderBadge(text = "置顶")
                }
            }
            Text(
                text = VaultFormatters.formatDateTimeCompact(item.updatedAt),
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = VaultIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@HiltViewModel
class SecureSpaceViewModel @Inject constructor(
    observeSecureItemsUseCase: ObserveSecureItemsUseCase,
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val verifyAppLockUseCase: VerifyAppLockUseCase,
    private val verifySecondPinUseCase: VerifySecondPinUseCase,
    private val lockStateManager: LockStateManager,
) : androidx.lifecycle.ViewModel() {
    private val challenge = kotlinx.coroutines.flow.MutableStateFlow(SecureChallengeState())
    private val selectionMode = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val selectedIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())

    private val localState = combine(
        challenge,
        selectionMode,
        selectedIds,
    ) { challengeState, inSelectionMode, currentSelectedIds ->
        SecureLocalState(
            challenge = challengeState,
            selectionMode = inSelectionMode,
            selectedIds = currentSelectedIds,
        )
    }

    val uiState: StateFlow<SecureSpaceUiState> = combine(
        observeSecureItemsUseCase(),
        observeSecuritySettingsUseCase(),
        observeAppearanceSettingsUseCase(),
        lockStateManager.isSecureSpaceUnlocked,
        localState,
    ) { items, securitySettings, appearance, unlocked, local ->
        SecureSpaceUiState(
            items = items,
            securitySettings = securitySettings,
            cardStyle = appearance.cardStyle,
            unlocked = unlocked,
            pin = local.challenge.pin,
            secondPin = local.challenge.secondPin,
            requireSecondPin = local.challenge.requireSecondPin,
            error = local.challenge.error,
            selectionMode = local.selectionMode,
            selectedIds = local.selectedIds.intersect(items.map(VaultItem::id).toSet()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SecureSpaceUiState(),
    )

    fun onEnter() {
        if (lockStateManager.isSecureSpaceUnlocked.value) return
        challenge.update {
            it.copy(
                requireSecondPin = false,
                pin = "",
                secondPin = "",
                error = null,
            )
        }
    }

    fun leave() {
        lockStateManager.resetSecureSpace()
        challenge.update {
            it.copy(
                requireSecondPin = false,
                pin = "",
                secondPin = "",
                error = null,
            )
        }
        selectionMode.update { false }
        selectedIds.update { emptySet() }
    }

    fun onPinChanged(value: String) {
        challenge.update { it.copy(pin = value.take(32), error = null) }
    }

    fun onSecondPinChanged(value: String) {
        challenge.update { it.copy(secondPin = value.take(32), error = null) }
    }

    fun confirmUnlock() {
        val state = uiState.value
        if (!state.securitySettings.hasPin) return
        viewModelScope.launch {
            val challengeState = challenge.value
            if (!challengeState.requireSecondPin) {
                val verified = verifyAppLockUseCase(challengeState.pin)
                if (!verified) {
                    challenge.update { it.copy(error = "PIN 不正确。") }
                    return@launch
                }
                if (state.securitySettings.hasSecondPin) {
                    challenge.update {
                        it.copy(
                            requireSecondPin = true,
                            secondPin = "",
                            error = null,
                        )
                    }
                    return@launch
                }
                lockStateManager.markSecureSpaceUnlocked()
                return@launch
            }

            val secondVerified = verifySecondPinUseCase(challengeState.secondPin)
            if (!secondVerified) {
                challenge.update { it.copy(error = "二次密码不正确。") }
                return@launch
            }
            lockStateManager.markSecureSpaceUnlocked()
        }
    }

    fun toggleSelectionMode() {
        val next = !selectionMode.value
        selectionMode.update { next }
        if (!next) {
            selectedIds.update { emptySet() }
        }
    }

    fun toggleSelection(itemId: String) {
        selectedIds.update { current ->
            if (current.contains(itemId)) current - itemId else current + itemId
        }
    }

    fun deleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { itemId ->
                deleteItemUseCase(itemId)
            }
            selectedIds.update { emptySet() }
            selectionMode.update { false }
        }
    }
}

data class SecureSpaceUiState(
    val items: List<VaultItem> = emptyList(),
    val securitySettings: SecuritySettings = SecuritySettings(),
    val cardStyle: String = "single",
    val unlocked: Boolean = false,
    val pin: String = "",
    val secondPin: String = "",
    val requireSecondPin: Boolean = false,
    val error: String? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
)

private data class SecureChallengeState(
    val pin: String = "",
    val secondPin: String = "",
    val requireSecondPin: Boolean = false,
    val error: String? = null,
)

private data class SecureLocalState(
    val challenge: SecureChallengeState = SecureChallengeState(),
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
)

private fun VaultItem.forSecureDisplay(): VaultItem = when (this) {
    is com.excelsior.pocketvault.domain.model.LinkVaultItem -> copy(
        protectionLevel = com.excelsior.pocketvault.domain.model.ItemProtectionLevel.NONE,
    )
    is com.excelsior.pocketvault.domain.model.TextVaultItem -> copy(
        protectionLevel = com.excelsior.pocketvault.domain.model.ItemProtectionLevel.NONE,
    )
    is com.excelsior.pocketvault.domain.model.ImageVaultItem -> copy(
        protectionLevel = com.excelsior.pocketvault.domain.model.ItemProtectionLevel.NONE,
    )
    is com.excelsior.pocketvault.domain.model.CredentialVaultItem -> copy(
        protectionLevel = com.excelsior.pocketvault.domain.model.ItemProtectionLevel.NONE,
    )
}

private fun VaultItem.secureTitle(): String = when (this) {
    is com.excelsior.pocketvault.domain.model.LinkVaultItem -> title.ifBlank { siteName }
    is com.excelsior.pocketvault.domain.model.TextVaultItem -> title?.takeIf { it.isNotBlank() } ?: content.take(20)
    is com.excelsior.pocketvault.domain.model.ImageVaultItem -> title ?: "未命名图片"
    is com.excelsior.pocketvault.domain.model.CredentialVaultItem -> siteName
}

private fun VaultItem.secureSummary(): String = when (this) {
    is com.excelsior.pocketvault.domain.model.LinkVaultItem -> note ?: VaultFormatters.compactUrl(url)
    is com.excelsior.pocketvault.domain.model.TextVaultItem -> content
    is com.excelsior.pocketvault.domain.model.ImageVaultItem -> note ?: "图片"
    is com.excelsior.pocketvault.domain.model.CredentialVaultItem -> websiteUrl ?: username
}

private fun ItemType.secureIcon() = when (this) {
    ItemType.LINK -> VaultIcons.Link
    ItemType.TEXT -> VaultIcons.Text
    ItemType.IMAGE -> VaultIcons.Image
    ItemType.CREDENTIAL -> VaultIcons.Credential
}
