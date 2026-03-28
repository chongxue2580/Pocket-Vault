package com.excelsior.pocketvault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.excelsior.pocketvault.ui.AppViewModel
import com.excelsior.pocketvault.ui.navigation.PocketVaultApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketVaultApp(appViewModel = appViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.onAppForeground()
    }

    override fun onStop() {
        appViewModel.onAppBackground()
        super.onStop()
    }
}
