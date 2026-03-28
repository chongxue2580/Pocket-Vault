package com.excelsior.pocketvault.data.repository

import android.content.Context
import androidx.biometric.BiometricManager
import com.excelsior.pocketvault.core.common.SettingsKeys
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.data.local.dao.VaultDao
import com.excelsior.pocketvault.data.local.entity.SettingEntity
import com.excelsior.pocketvault.data.security.CryptoManager
import com.excelsior.pocketvault.domain.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val dao: VaultDao,
    private val cryptoManager: CryptoManager,
    @ApplicationContext private val context: Context,
) : SecurityRepository {

    override suspend fun encrypt(value: String): String = cryptoManager.encrypt(value)

    override suspend fun decrypt(cipherText: String): Result<String> = runCatching {
        cryptoManager.decrypt(cipherText)
    }

    override suspend fun savePin(pin: String) {
        saveHashedSecret(
            rawValue = pin,
            saltKey = SettingsKeys.PIN_SALT,
            hashKey = SettingsKeys.PIN_HASH,
        )
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return verifyHashedSecret(
            rawValue = pin,
            saltKey = SettingsKeys.PIN_SALT,
            hashKey = SettingsKeys.PIN_HASH,
        )
    }

    override suspend fun clearPin() {
        clearHashedSecret(
            saltKey = SettingsKeys.PIN_SALT,
            hashKey = SettingsKeys.PIN_HASH,
        )
    }

    override suspend fun saveSecondPin(pin: String) {
        saveHashedSecret(
            rawValue = pin,
            saltKey = SettingsKeys.SECOND_PIN_SALT,
            hashKey = SettingsKeys.SECOND_PIN_HASH,
        )
    }

    override suspend fun verifySecondPin(pin: String): Boolean {
        return verifyHashedSecret(
            rawValue = pin,
            saltKey = SettingsKeys.SECOND_PIN_SALT,
            hashKey = SettingsKeys.SECOND_PIN_HASH,
        )
    }

    override suspend fun clearSecondPin() {
        clearHashedSecret(
            saltKey = SettingsKeys.SECOND_PIN_SALT,
            hashKey = SettingsKeys.SECOND_PIN_HASH,
        )
    }

    override fun isBiometricAvailable(): Boolean {
        val result = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    private suspend fun saveHashedSecret(
        rawValue: String,
        saltKey: String,
        hashKey: String,
    ) {
        val salt = randomSalt()
        val hash = VaultFormatters.sha256("$salt:$rawValue")
        dao.upsertSetting(SettingEntity(saltKey, salt))
        dao.upsertSetting(SettingEntity(hashKey, hash))
    }

    private suspend fun verifyHashedSecret(
        rawValue: String,
        saltKey: String,
        hashKey: String,
    ): Boolean {
        val settings = dao.getSettings().associate { it.key to it.value }
        val salt = settings[saltKey] ?: return false
        val hash = settings[hashKey] ?: return false
        return VaultFormatters.sha256("$salt:$rawValue") == hash
    }

    private suspend fun clearHashedSecret(
        saltKey: String,
        hashKey: String,
    ) {
        dao.deleteSetting(saltKey)
        dao.deleteSetting(hashKey)
    }
}
