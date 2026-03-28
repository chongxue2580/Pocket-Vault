package com.excelsior.pocketvault.domain.usecase

import com.excelsior.pocketvault.domain.model.AppearanceSettings
import com.excelsior.pocketvault.domain.model.SearchHistoryEntry
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.ThemeMode
import com.excelsior.pocketvault.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAppearanceSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppearanceSettings> = repository.observeAppearanceSettings()
}

class ObserveSecuritySettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<SecuritySettings> = repository.observeSecuritySettings()
}

class ObserveSearchHistoryUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<List<SearchHistoryEntry>> = repository.observeSearchHistory()
}

class ObserveHomeOrderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<List<String>> = repository.observeHomeOrder()
}

class SaveSearchQueryUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(query: String) = repository.saveSearchQuery(query)
}

class ClearSearchHistoryUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke() = repository.clearSearchHistory()
}

class SaveHomeOrderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(ids: List<String>) = repository.saveHomeOrder(ids)
}

class UpdateThemeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class UpdateCardStyleUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(style: String) = repository.setCardStyle(style)
}

class UpdateSecuritySettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend fun setAppLockEnabled(enabled: Boolean) = repository.setAppLockEnabled(enabled)
    suspend fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    suspend fun setRequireAuthForSecrets(enabled: Boolean) = repository.setRequireAuthForSecrets(enabled)
    suspend fun setScreenshotProtection(enabled: Boolean) = repository.setScreenshotProtection(enabled)
    suspend fun setAutoLockSeconds(seconds: Int) = repository.setAutoLockSeconds(seconds)
}
