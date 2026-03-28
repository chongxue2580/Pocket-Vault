package com.excelsior.pocketvault.ui.screen.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.domain.usecase.IsBiometricAvailableUseCase
import com.excelsior.pocketvault.domain.usecase.VerifyAppLockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val verifyAppLockUseCase: VerifyAppLockUseCase,
    isBiometricAvailableUseCase: IsBiometricAvailableUseCase,
) : ViewModel() {

    private val biometricAvailable = isBiometricAvailableUseCase()
    private val _uiState = MutableStateFlow(UnlockUiState(biometricAvailable = biometricAvailable))
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    fun onPinChanged(value: String) {
        _uiState.update { it.copy(pin = value.take(32), error = null) }
    }

    fun resetForChallenge() {
        _uiState.update {
            UnlockUiState(
                biometricAvailable = biometricAvailable,
            )
        }
    }

    fun verifyPin() {
        viewModelScope.launch {
            val success = verifyAppLockUseCase(_uiState.value.pin)
            if (success) {
                _uiState.update { it.copy(isUnlocked = true, error = null) }
            } else {
                _uiState.update { it.copy(error = "PIN 不正确，请重试。") }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(isUnlocked = true, error = null) }
    }
}

data class UnlockUiState(
    val pin: String = "",
    val biometricAvailable: Boolean = false,
    val isUnlocked: Boolean = false,
    val error: String? = null,
)
