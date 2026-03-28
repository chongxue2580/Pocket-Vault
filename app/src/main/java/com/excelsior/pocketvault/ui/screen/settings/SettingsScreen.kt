@file:OptIn(ExperimentalLayoutApi::class)

package com.excelsior.pocketvault.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.core.designsystem.component.VaultChip
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.core.designsystem.component.VaultSectionTitle
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.ThemeMode
import com.excelsior.pocketvault.domain.usecase.ClearPinUseCase
import com.excelsior.pocketvault.domain.usecase.ClearSecondPinUseCase
import com.excelsior.pocketvault.domain.usecase.IsBiometricAvailableUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveAppearanceSettingsUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.SavePinUseCase
import com.excelsior.pocketvault.domain.usecase.SaveSecondPinUseCase
import com.excelsior.pocketvault.domain.usecase.UpdateCardStyleUseCase
import com.excelsior.pocketvault.domain.usecase.UpdateSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.UpdateThemeUseCase
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
fun SettingsRoute(
    onManageFolders: () -> Unit,
    onManageTags: () -> Unit,
    onEncryption: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onThemeSelected = viewModel::onThemeSelected,
        onLayoutSelected = viewModel::onLayoutSelected,
        onAppLockChanged = viewModel::onAppLockChanged,
        onBiometricChanged = viewModel::onBiometricChanged,
        onSecretAuthChanged = viewModel::onSecretAuthChanged,
        onScreenshotProtectionChanged = viewModel::onScreenshotProtectionChanged,
        onAutoLockSelected = viewModel::onAutoLockSelected,
        onManageFolders = onManageFolders,
        onManageTags = onManageTags,
        onEncryption = onEncryption,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onLayoutSelected: (String) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    onSecretAuthChanged: (Boolean) -> Unit,
    onScreenshotProtectionChanged: (Boolean) -> Unit,
    onAutoLockSelected: (Int) -> Unit,
    onManageFolders: () -> Unit,
    onManageTags: () -> Unit,
    onEncryption: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = "设置",
                subtitle = null,
            )
        }
        item {
            VaultGlassCard {
                VaultSectionTitle(
                    title = "外观",
                    caption = null,
                )
                FlowRow(
                    modifier = Modifier.padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        VaultChip(
                            label = mode.label(),
                            selected = state.themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                        )
                    }
                }
                Text(
                    text = "布局",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf("single" to "一排", "double" to "两排").forEach { option ->
                        VaultChip(
                            label = option.second,
                            selected = state.cardStyle == option.first,
                            onClick = { onLayoutSelected(option.first) },
                        )
                    }
                }
            }
        }
        item {
            VaultGlassCard {
                VaultSectionTitle(title = "加密", caption = null)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = onEncryption, modifier = Modifier.weight(1f)) {
                        Text(text = if (state.securitySettings.hasPin) "进入加密设置" else "设置加密")
                    }
                }
            }
        }
        item {
            VaultGlassCard {
                VaultSectionTitle(title = "安全", caption = null)
                SettingsToggleRow(
                    title = "启用应用锁",
                    subtitle = "前台验证",
                    checked = state.securitySettings.appLockEnabled,
                    onCheckedChange = onAppLockChanged,
                    enabled = state.securitySettings.hasPin,
                )
                SettingsToggleRow(
                    title = "生物识别",
                    subtitle = if (state.biometricAvailable) "指纹 / 面容" else "当前设备不可用",
                    checked = state.securitySettings.biometricEnabled,
                    onCheckedChange = onBiometricChanged,
                    enabled = state.biometricAvailable && state.securitySettings.hasPin,
                )
                SettingsToggleRow(
                    title = "敏感信息二次保护",
                    subtitle = "查看密码前验证",
                    checked = state.securitySettings.requireAuthForSecrets,
                    onCheckedChange = onSecretAuthChanged,
                )
                SettingsToggleRow(
                    title = "截图保护",
                    subtitle = "禁止截图",
                    checked = state.securitySettings.screenshotProtection,
                    onCheckedChange = onScreenshotProtectionChanged,
                )
                Text(
                    text = "自动锁定",
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(0, 15, 30, 60, 180).forEach { seconds ->
                        VaultChip(
                            label = if (seconds == 0) "不自动锁定" else "${seconds} 秒",
                            selected = state.securitySettings.autoLockSeconds == seconds,
                            onClick = { onAutoLockSelected(seconds) },
                        )
                    }
                }
            }
        }
        item {
            VaultGlassCard {
                VaultSectionTitle(
                    title = "收藏夹与标签",
                    caption = null,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = onManageFolders, modifier = Modifier.weight(1f)) {
                        Text(text = "管理收藏夹")
                    }
                    Button(onClick = onManageTags, modifier = Modifier.weight(1f)) {
                        Text(text = "管理标签")
                    }
                }
            }
        }
        item {
            VaultGlassCard {
                VaultSectionTitle(
                    title = "关于应用",
                    caption = "拾光盒是一个本地优先的私人便签与收藏应用。",
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.openExternalLink(REPO_URL) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "作者 · Excelsior",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase,
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updateCardStyleUseCase: UpdateCardStyleUseCase,
    private val updateSecuritySettingsUseCase: UpdateSecuritySettingsUseCase,
    private val savePinUseCase: SavePinUseCase,
    private val clearPinUseCase: ClearPinUseCase,
    private val saveSecondPinUseCase: SaveSecondPinUseCase,
    private val clearSecondPinUseCase: ClearSecondPinUseCase,
    isBiometricAvailableUseCase: IsBiometricAvailableUseCase,
) : androidx.lifecycle.ViewModel() {
    private val pinDraft = MutableStateFlow("")
    private val secondPinDraft = MutableStateFlow("")
    private val notice = MutableStateFlow<String?>(null)
    private val biometricAvailable = isBiometricAvailableUseCase()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeAppearanceSettingsUseCase(),
        observeSecuritySettingsUseCase(),
        pinDraft,
        secondPinDraft,
        notice,
    ) { appearance, security, pin, secondPin, currentNotice ->
        SettingsUiState(
            themeMode = appearance.themeMode,
            cardStyle = appearance.cardStyle,
            securitySettings = security,
            pinDraft = pin,
            secondPinDraft = secondPin,
            biometricAvailable = biometricAvailable,
            notice = currentNotice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            updateThemeUseCase(mode)
        }
    }

    fun onLayoutSelected(style: String) {
        viewModelScope.launch {
            updateCardStyleUseCase(style)
        }
    }

    fun onPinChanged(value: String) {
        pinDraft.update { value.filter(Char::isDigit).take(4) }
        notice.update { null }
    }

    fun onSecondPinChanged(value: String) {
        secondPinDraft.update { value.filter(Char::isDigit).take(4) }
        notice.update { null }
    }

    fun savePin() {
        viewModelScope.launch {
            val pin = pinDraft.value
            if (pin.length != 4) {
                notice.update { "PIN 需要是 4 位数字。" }
                return@launch
            }
            savePinUseCase(pin)
            updateSecuritySettingsUseCase.setAppLockEnabled(true)
            notice.update { "PIN 已保存，应用锁已启用。" }
            pinDraft.update { "" }
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            clearPinUseCase()
            clearSecondPinUseCase()
            updateSecuritySettingsUseCase.setAppLockEnabled(false)
            updateSecuritySettingsUseCase.setBiometricEnabled(false)
            notice.update { "PIN 已清除，应用锁已关闭。" }
            pinDraft.update { "" }
            secondPinDraft.update { "" }
        }
    }

    fun saveSecondPin() {
        viewModelScope.launch {
            if (!uiState.value.securitySettings.hasPin) {
                notice.update { "请先保存主 PIN。" }
                return@launch
            }
            val pin = secondPinDraft.value
            if (pin.length != 4) {
                notice.update { "二次密码需要是 4 位数字。" }
                return@launch
            }
            saveSecondPinUseCase(pin)
            notice.update { "二次密码已保存。" }
            secondPinDraft.update { "" }
        }
    }

    fun clearSecondPin() {
        viewModelScope.launch {
            clearSecondPinUseCase()
            notice.update { "二次密码已清除。" }
            secondPinDraft.update { "" }
        }
    }

    fun onAppLockChanged(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !uiState.value.securitySettings.hasPin) {
                notice.update { "请先设置 PIN，再启用应用锁。" }
                return@launch
            }
            updateSecuritySettingsUseCase.setAppLockEnabled(enabled)
        }
    }

    fun onBiometricChanged(enabled: Boolean) {
        viewModelScope.launch {
            updateSecuritySettingsUseCase.setBiometricEnabled(enabled)
        }
    }

    fun onSecretAuthChanged(enabled: Boolean) {
        viewModelScope.launch {
            updateSecuritySettingsUseCase.setRequireAuthForSecrets(enabled)
        }
    }

    fun onScreenshotProtectionChanged(enabled: Boolean) {
        viewModelScope.launch {
            updateSecuritySettingsUseCase.setScreenshotProtection(enabled)
        }
    }

    fun onAutoLockSelected(seconds: Int) {
        viewModelScope.launch {
            updateSecuritySettingsUseCase.setAutoLockSeconds(seconds)
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cardStyle: String = "single",
    val securitySettings: SecuritySettings = SecuritySettings(),
    val pinDraft: String = "",
    val secondPinDraft: String = "",
    val biometricAvailable: Boolean = false,
    val notice: String? = null,
)

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "简白"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

private const val REPO_URL = "https://github.com/chongxue2580/Pocket-Vault"

private fun android.content.Context.openExternalLink(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
