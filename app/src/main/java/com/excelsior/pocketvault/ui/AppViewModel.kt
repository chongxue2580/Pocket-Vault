package com.excelsior.pocketvault.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.data.security.LockStateManager
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.ThemeMode
import com.excelsior.pocketvault.domain.usecase.ObserveAppearanceSettingsUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.SeedDemoDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase,
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    private val seedDemoDataUseCase: SeedDemoDataUseCase,
    private val lockStateManager: LockStateManager,
) : ViewModel() {

    private val appearance = observeAppearanceSettingsUseCase()
    private val security = observeSecuritySettingsUseCase()

    val uiState: StateFlow<AppUiState> = combine(
        appearance,
        security,
        lockStateManager.isUnlocked,
    ) { appearanceState, securityState, unlocked ->
        AppUiState(
            themeMode = appearanceState.themeMode,
            securitySettings = securityState,
            cardStyle = appearanceState.cardStyle,
            lockRequired = securityState.appLockEnabled && securityState.hasPin && !unlocked,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        viewModelScope.launch {
            seedDemoDataUseCase()
        }
    }

    fun onAppForeground() {
        val securityState = uiState.value.securitySettings
        if (securityState.appLockEnabled && securityState.hasPin && lockStateManager.shouldRequireUnlock(securityState.autoLockSeconds)) {
            lockStateManager.lockNow()
        }
    }

    fun onAppBackground() {
        lockStateManager.onBackgrounded()
    }

    fun onUnlockSuccess() {
        lockStateManager.markUnlocked()
    }

    fun resetSecureSpace() {
        lockStateManager.resetSecureSpace()
    }
}

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val securitySettings: SecuritySettings = SecuritySettings(),
    val cardStyle: String = "single",
    val lockRequired: Boolean = false,
)
