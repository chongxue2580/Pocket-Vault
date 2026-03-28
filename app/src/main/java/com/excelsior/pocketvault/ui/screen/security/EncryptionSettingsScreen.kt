package com.excelsior.pocketvault.ui.screen.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.core.designsystem.component.VaultSectionTitle
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.usecase.ClearPinUseCase
import com.excelsior.pocketvault.domain.usecase.ClearSecondPinUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.SavePinUseCase
import com.excelsior.pocketvault.domain.usecase.SaveSecondPinUseCase
import com.excelsior.pocketvault.domain.usecase.UpdateSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.VerifyAppLockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun EncryptionSettingsRoute(
    onBack: () -> Unit,
    viewModel: EncryptionSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EncryptionSettingsScreen(
        state = state,
        onBack = onBack,
        onAccessPinChanged = viewModel::onAccessPinChanged,
        onUnlock = viewModel::unlock,
        onMainPinChanged = viewModel::onMainPinChanged,
        onSaveMainPin = viewModel::saveMainPin,
        onClearMainPin = viewModel::clearMainPin,
        onSecondPinChanged = viewModel::onSecondPinChanged,
        onSaveSecondPin = viewModel::saveSecondPin,
        onClearSecondPin = viewModel::clearSecondPin,
    )
}

@Composable
fun EncryptionSettingsScreen(
    state: EncryptionSettingsUiState,
    onBack: () -> Unit,
    onAccessPinChanged: (String) -> Unit,
    onUnlock: () -> Unit,
    onMainPinChanged: (String) -> Unit,
    onSaveMainPin: () -> Unit,
    onClearMainPin: () -> Unit,
    onSecondPinChanged: (String) -> Unit,
    onSaveSecondPin: () -> Unit,
    onClearSecondPin: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = "加密设置",
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
            )
        }
        if (state.requiresAccessPin && !state.accessGranted) {
            item {
                VaultGlassCard {
                    VaultSectionTitle(title = "输入主密码", caption = null)
                    VaultPasswordField(
                        value = state.accessPin,
                        onValueChange = onAccessPinChanged,
                        label = "主密码",
                        placeholder = "至少 4 位",
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    state.notice?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = onUnlock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        enabled = state.accessPin.length >= 4,
                    ) {
                        Text(text = "进入")
                    }
                }
            }
        } else {
            item {
                VaultGlassCard {
                    VaultSectionTitle(title = "主密码", caption = null)
                    VaultPasswordField(
                        value = state.mainPinDraft,
                        onValueChange = onMainPinChanged,
                        label = "主密码",
                        placeholder = "至少 4 位",
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Button(
                        onClick = onSaveMainPin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        enabled = state.mainPinDraft.length >= 4,
                    ) {
                        Text(text = if (state.securitySettings.hasPin) "更新主密码" else "保存主密码")
                    }
                    if (state.securitySettings.hasPin) {
                        Button(
                            onClick = onClearMainPin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        ) {
                            Text(text = "清除主密码")
                        }
                    }
                }
            }
            item {
                VaultGlassCard {
                    VaultSectionTitle(title = "二次密码", caption = null)
                    VaultPasswordField(
                        value = state.secondPinDraft,
                        onValueChange = onSecondPinChanged,
                        label = "二次密码",
                        placeholder = "至少 4 位",
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Button(
                        onClick = onSaveSecondPin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        enabled = state.mainPinExists && state.secondPinDraft.length >= 4,
                    ) {
                        Text(text = if (state.securitySettings.hasSecondPin) "更新二次密码" else "保存二次密码")
                    }
                    if (state.securitySettings.hasSecondPin) {
                        Button(
                            onClick = onClearSecondPin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        ) {
                            Text(text = "清除二次密码")
                        }
                    }
                }
            }
            state.notice?.let { notice ->
                item {
                    VaultGlassCard {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class EncryptionSettingsViewModel @Inject constructor(
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    private val savePinUseCase: SavePinUseCase,
    private val clearPinUseCase: ClearPinUseCase,
    private val saveSecondPinUseCase: SaveSecondPinUseCase,
    private val clearSecondPinUseCase: ClearSecondPinUseCase,
    private val verifyAppLockUseCase: VerifyAppLockUseCase,
    private val updateSecuritySettingsUseCase: UpdateSecuritySettingsUseCase,
) : androidx.lifecycle.ViewModel() {
    private val accessPin = MutableStateFlow("")
    private val accessGranted = MutableStateFlow(false)
    private val mainPinDraft = MutableStateFlow("")
    private val secondPinDraft = MutableStateFlow("")
    private val notice = MutableStateFlow<String?>(null)

    private val localState = combine(
        accessPin,
        accessGranted,
        mainPinDraft,
        secondPinDraft,
        notice,
    ) { currentAccessPin, granted, mainDraft, secondDraft, currentNotice ->
        LocalEncryptionSettingsState(
            accessPin = currentAccessPin,
            accessGranted = granted,
            mainPinDraft = mainDraft,
            secondPinDraft = secondDraft,
            notice = currentNotice,
        )
    }

    val uiState: StateFlow<EncryptionSettingsUiState> = combine(
        observeSecuritySettingsUseCase(),
        localState,
    ) { securitySettings, local ->
        EncryptionSettingsUiState(
            securitySettings = securitySettings,
            accessPin = local.accessPin,
            accessGranted = local.accessGranted || !securitySettings.hasPin,
            requiresAccessPin = securitySettings.hasPin,
            mainPinExists = securitySettings.hasPin,
            mainPinDraft = local.mainPinDraft,
            secondPinDraft = local.secondPinDraft,
            notice = local.notice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EncryptionSettingsUiState(),
    )

    fun onAccessPinChanged(value: String) {
        accessPin.update { value.take(32) }
        notice.update { null }
    }

    fun onMainPinChanged(value: String) {
        mainPinDraft.update { value.take(32) }
        notice.update { null }
    }

    fun onSecondPinChanged(value: String) {
        secondPinDraft.update { value.take(32) }
        notice.update { null }
    }

    fun unlock() {
        viewModelScope.launch {
            val verified = verifyAppLockUseCase(accessPin.value)
            if (!verified) {
                notice.update { "PIN 不正确。" }
                return@launch
            }
            accessGranted.update { true }
            accessPin.update { "" }
            notice.update { null }
        }
    }

    fun saveMainPin() {
        viewModelScope.launch {
            val pin = mainPinDraft.value
            if (pin.length != 4) {
                notice.update { "主密码至少 4 位。" }
                return@launch
            }
            savePinUseCase(pin)
            updateSecuritySettingsUseCase.setAppLockEnabled(true)
            accessGranted.update { true }
            mainPinDraft.update { "" }
            notice.update { "主密码已保存。" }
        }
    }

    fun clearMainPin() {
        viewModelScope.launch {
            clearPinUseCase()
            clearSecondPinUseCase()
            updateSecuritySettingsUseCase.setAppLockEnabled(false)
            updateSecuritySettingsUseCase.setBiometricEnabled(false)
            accessGranted.update { false }
            mainPinDraft.update { "" }
            secondPinDraft.update { "" }
            notice.update { "主密码与二次密码已清除。" }
        }
    }

    fun saveSecondPin() {
        viewModelScope.launch {
            val pin = secondPinDraft.value
            if (pin.length != 4) {
                notice.update { "二次密码至少 4 位。" }
                return@launch
            }
            saveSecondPinUseCase(pin)
            secondPinDraft.update { "" }
            notice.update { "二次密码已保存。" }
        }
    }

    fun clearSecondPin() {
        viewModelScope.launch {
            clearSecondPinUseCase()
            secondPinDraft.update { "" }
            notice.update { "二次密码已清除。" }
        }
    }
}

data class EncryptionSettingsUiState(
    val securitySettings: SecuritySettings = SecuritySettings(),
    val accessPin: String = "",
    val accessGranted: Boolean = false,
    val requiresAccessPin: Boolean = false,
    val mainPinExists: Boolean = false,
    val mainPinDraft: String = "",
    val secondPinDraft: String = "",
    val notice: String? = null,
)

private data class LocalEncryptionSettingsState(
    val accessPin: String = "",
    val accessGranted: Boolean = false,
    val mainPinDraft: String = "",
    val secondPinDraft: String = "",
    val notice: String? = null,
)
