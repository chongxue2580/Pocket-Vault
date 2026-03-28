package com.excelsior.pocketvault.data.repository

import com.excelsior.pocketvault.core.common.SettingsKeys
import com.excelsior.pocketvault.data.local.dao.VaultDao
import com.excelsior.pocketvault.data.local.entity.SettingEntity
import com.excelsior.pocketvault.domain.model.AppearanceSettings
import com.excelsior.pocketvault.domain.model.SearchHistoryEntry
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.ThemeMode
import com.excelsior.pocketvault.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: VaultDao,
) : SettingsRepository {

    override fun observeAppearanceSettings(): Flow<AppearanceSettings> = dao.observeSettings().map(::mapAppearanceSettings)

    override fun observeSecuritySettings(): Flow<SecuritySettings> = dao.observeSettings().map(::mapSecuritySettings)

    override fun observeSearchHistory(): Flow<List<SearchHistoryEntry>> = dao.observeSettings().map { entities ->
        val value = entities.firstOrNull { it.key == SettingsKeys.SEARCH_HISTORY }?.value.orEmpty()
        if (value.isBlank()) {
            emptyList()
        } else {
            value.split("||")
                .mapNotNull { chunk ->
                    val parts = chunk.split("::", limit = 2)
                    if (parts.size == 2) {
                        SearchHistoryEntry(parts[1], parts[0].toLongOrNull() ?: 0L)
                    } else {
                        null
                    }
                }
        }
    }

    override fun observeHomeOrder(): Flow<List<String>> = dao.observeSettings().map { entities ->
        entities.firstOrNull { it.key == SettingsKeys.HOME_ORDER }
            ?.value
            .orEmpty()
            .split('|')
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        put(SettingsKeys.THEME_MODE, mode.name)
    }

    override suspend fun setCardStyle(style: String) {
        put(SettingsKeys.CARD_STYLE, style)
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        put(SettingsKeys.APP_LOCK_ENABLED, enabled.toString())
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        put(SettingsKeys.BIOMETRIC_ENABLED, enabled.toString())
    }

    override suspend fun setRequireAuthForSecrets(enabled: Boolean) {
        put(SettingsKeys.REQUIRE_SECRET_AUTH, enabled.toString())
    }

    override suspend fun setScreenshotProtection(enabled: Boolean) {
        put(SettingsKeys.SCREENSHOT_PROTECTION, enabled.toString())
    }

    override suspend fun setAutoLockSeconds(seconds: Int) {
        put(SettingsKeys.AUTO_LOCK_SECONDS, seconds.toString())
    }

    override suspend fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        val normalized = query.trim()
        val settings = dao.getSettings()
        val raw = settings.firstOrNull { it.key == SettingsKeys.SEARCH_HISTORY }?.value.orEmpty()
        val existing = if (raw.isBlank()) emptyList() else raw.split("||")
            .mapNotNull { entry ->
                val parts = entry.split("::", limit = 2)
                if (parts.size == 2) SearchHistoryEntry(parts[1], parts[0].toLongOrNull() ?: 0L) else null
            }
        val merged = buildList {
            add(SearchHistoryEntry(normalized, System.currentTimeMillis()))
            addAll(existing.filterNot { it.query.equals(normalized, ignoreCase = true) })
        }.take(8)
        val encoded = merged.joinToString("||") { "${it.timestamp}::${it.query}" }
        put(SettingsKeys.SEARCH_HISTORY, encoded)
    }

    override suspend fun clearSearchHistory() {
        dao.deleteSetting(SettingsKeys.SEARCH_HISTORY)
    }

    override suspend fun saveHomeOrder(ids: List<String>) {
        val normalized = ids.distinct().joinToString("|")
        put(SettingsKeys.HOME_ORDER, normalized)
    }

    private suspend fun put(key: String, value: String) {
        dao.upsertSetting(SettingEntity(key = key, value = value))
    }

    private fun mapAppearanceSettings(settings: List<SettingEntity>): AppearanceSettings {
        val map = settings.associate { it.key to it.value }
        val theme = map[SettingsKeys.THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        val cardStyle = when (map[SettingsKeys.CARD_STYLE]) {
            "double" -> "double"
            else -> "single"
        }
        return AppearanceSettings(themeMode = theme, cardStyle = cardStyle)
    }

    private fun mapSecuritySettings(settings: List<SettingEntity>): SecuritySettings {
        val map = settings.associate { it.key to it.value }
        return SecuritySettings(
            appLockEnabled = map[SettingsKeys.APP_LOCK_ENABLED]?.toBooleanStrictOrNull() ?: false,
            biometricEnabled = map[SettingsKeys.BIOMETRIC_ENABLED]?.toBooleanStrictOrNull() ?: false,
            requireAuthForSecrets = map[SettingsKeys.REQUIRE_SECRET_AUTH]?.toBooleanStrictOrNull() ?: true,
            screenshotProtection = map[SettingsKeys.SCREENSHOT_PROTECTION]?.toBooleanStrictOrNull() ?: false,
            autoLockSeconds = map[SettingsKeys.AUTO_LOCK_SECONDS]?.toIntOrNull() ?: 30,
            hasPin = !map[SettingsKeys.PIN_HASH].isNullOrBlank(),
            hasSecondPin = !map[SettingsKeys.SECOND_PIN_HASH].isNullOrBlank(),
        )
    }
}
