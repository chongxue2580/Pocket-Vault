package com.excelsior.pocketvault.core.common

import java.util.Base64

private const val ENCRYPTED_PREFIX = "enc:"
private const val PLAIN_PREFIX = "plain:"

fun isStoredSecretEncrypted(storedValue: String): Boolean = !storedValue.startsWith(PLAIN_PREFIX)

suspend fun encodeStoredSecret(
    rawValue: String,
    encrypted: Boolean,
    encryptor: suspend (String) -> String,
): String = if (encrypted) {
    ENCRYPTED_PREFIX + encryptor(rawValue)
} else {
    val encoded = Base64.getEncoder().encodeToString(rawValue.toByteArray(Charsets.UTF_8))
    PLAIN_PREFIX + encoded
}

suspend fun decodeStoredSecret(
    storedValue: String,
    decryptor: suspend (String) -> Result<String>,
): Result<String> = when {
    storedValue.startsWith(PLAIN_PREFIX) -> runCatching {
        val encoded = storedValue.removePrefix(PLAIN_PREFIX)
        String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
    }

    storedValue.startsWith(ENCRYPTED_PREFIX) -> decryptor(storedValue.removePrefix(ENCRYPTED_PREFIX))

    else -> decryptor(storedValue)
}
