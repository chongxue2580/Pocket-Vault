package com.excelsior.pocketvault.domain.usecase

import com.excelsior.pocketvault.domain.repository.SecurityRepository
import javax.inject.Inject

class EncryptCredentialUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(raw: String): String = repository.encrypt(raw)
}

class DecryptCredentialUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(cipherText: String): Result<String> = repository.decrypt(cipherText)
}

class SavePinUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(pin: String) = repository.savePin(pin)
}

class VerifyAppLockUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(pin: String): Boolean = repository.verifyPin(pin)
}

class ClearPinUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke() = repository.clearPin()
}

class SaveSecondPinUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(pin: String) = repository.saveSecondPin(pin)
}

class VerifySecondPinUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(pin: String): Boolean = repository.verifySecondPin(pin)
}

class ClearSecondPinUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke() = repository.clearSecondPin()
}

class IsBiometricAvailableUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    operator fun invoke(): Boolean = repository.isBiometricAvailable()
}
