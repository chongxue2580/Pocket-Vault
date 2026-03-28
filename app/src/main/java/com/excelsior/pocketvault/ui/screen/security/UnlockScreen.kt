package com.excelsior.pocketvault.ui.screen.security

import android.app.Activity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.domain.model.SecuritySettings

@Composable
fun UnlockGate(
    securitySettings: SecuritySettings,
    onUnlocked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.resetForChallenge()
    }

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) onUnlocked()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = "解锁拾光盒", style = MaterialTheme.typography.headlineMedium)
                VaultPasswordField(
                    value = state.pin,
                    onValueChange = viewModel::onPinChanged,
                    label = "主密码",
                    placeholder = "至少 4 位",
                )
                state.error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = viewModel::verifyPin, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "解锁")
                }
                if (securitySettings.biometricEnabled && state.biometricAvailable) {
                    Button(
                        onClick = {
                            val activity = context as? FragmentActivity ?: return@Button
                            val prompt = BiometricPrompt(
                                activity,
                                ContextCompat.getMainExecutor(context),
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        viewModel.onBiometricSuccess()
                                    }
                                },
                            )
                            prompt.authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("验证身份")
                                    .setSubtitle("使用生物识别解锁本地收藏")
                                    .setNegativeButtonText("取消")
                                    .build(),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "使用生物识别")
                    }
                }
            }
        }
    }
}
