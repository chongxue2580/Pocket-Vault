package com.excelsior.pocketvault.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockStateManager @Inject constructor() {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isSecureSpaceUnlocked = MutableStateFlow(false)
    val isSecureSpaceUnlocked: StateFlow<Boolean> = _isSecureSpaceUnlocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    fun markSecureSpaceUnlocked() {
        _isSecureSpaceUnlocked.value = true
    }

    fun resetSecureSpace() {
        _isSecureSpaceUnlocked.value = false
    }

    fun lockNow() {
        _isUnlocked.value = false
        _isSecureSpaceUnlocked.value = false
    }

    fun onBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
        _isSecureSpaceUnlocked.value = false
    }

    fun shouldRequireUnlock(timeoutSeconds: Int): Boolean {
        if (!_isUnlocked.value) return true
        if (timeoutSeconds <= 0 || backgroundTimestamp == 0L) return false
        val elapsed = System.currentTimeMillis() - backgroundTimestamp
        return elapsed >= timeoutSeconds * 1000L
    }
}
