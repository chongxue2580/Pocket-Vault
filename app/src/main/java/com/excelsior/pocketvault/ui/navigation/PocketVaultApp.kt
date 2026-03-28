package com.excelsior.pocketvault.ui.navigation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.excelsior.pocketvault.core.designsystem.component.VaultScaffold
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.core.designsystem.theme.PocketVaultTheme
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.ui.AppViewModel
import com.excelsior.pocketvault.ui.screen.category.CategoryRoute
import com.excelsior.pocketvault.ui.screen.detail.ItemDetailRoute
import com.excelsior.pocketvault.ui.screen.editor.EditorRoute
import com.excelsior.pocketvault.ui.screen.home.HomeRoute
import com.excelsior.pocketvault.ui.screen.manage.FolderManageRoute
import com.excelsior.pocketvault.ui.screen.manage.TagManageRoute
import com.excelsior.pocketvault.ui.screen.search.SearchRoute
import com.excelsior.pocketvault.ui.screen.security.EncryptionSettingsRoute
import com.excelsior.pocketvault.ui.screen.security.SecureSpaceRoute
import com.excelsior.pocketvault.ui.screen.security.UnlockGate
import com.excelsior.pocketvault.ui.screen.settings.SettingsRoute

@Composable
fun PocketVaultApp(appViewModel: AppViewModel) {
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(uiState.securitySettings.screenshotProtection) {
        if (uiState.securitySettings.screenshotProtection) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    PocketVaultTheme(themeMode = uiState.themeMode) {
        VaultScaffold(
            bottomBar = {
                if (BottomBarRoutes.any { route -> currentDestination?.hierarchy?.any { it.route == route.route } == true }) {
                    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(30.dp),
                            color = if (isLightTheme) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            },
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = if (isLightTheme) 0.12f else 0.2f),
                            ),
                            shadowElevation = if (isLightTheme) 10.dp else 0.dp,
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                            ) {
                                BottomBarRoutes.forEach { route ->
                                    val icon = when (route) {
                                        AppRoute.Home -> VaultIcons.Home
                                        AppRoute.Category -> VaultIcons.Category
                                        AppRoute.Search -> VaultIcons.Search
                                        AppRoute.Settings -> VaultIcons.Settings
                                        else -> VaultIcons.Home
                                    }
                                    val label = when (route) {
                                        AppRoute.Home -> "首页"
                                        AppRoute.Category -> "分类"
                                        AppRoute.Search -> "搜索"
                                        AppRoute.Settings -> "设置"
                                        else -> route.route
                                    }
                                    NavigationBarItem(
                                        selected = currentDestination?.hierarchy?.any { it.route == route.route } == true,
                                        onClick = {
                                            if (route == AppRoute.Home) {
                                                navController.popBackStack(AppRoute.Home.route, inclusive = false)
                                            } else {
                                                navController.navigate(route.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        icon = { Icon(imageVector = icon, contentDescription = null) },
                                        label = { Text(text = label) },
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.Home.route,
                modifier = Modifier.padding(paddingValues),
            ) {
                composable(AppRoute.Home.route) {
                    HomeRoute(
                        onSearchClick = { navController.navigate(AppRoute.Search.route) },
                        onItemClick = { navController.navigate(AppRoute.Detail.create(it)) },
                        onCreate = { navController.navigate(AppRoute.Editor.create(ItemType.TEXT)) },
                        onSecureSpace = {
                            appViewModel.resetSecureSpace()
                            navController.navigate(AppRoute.SecureSpace.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(AppRoute.Category.route) {
                    CategoryRoute(onItemClick = { navController.navigate(AppRoute.Detail.create(it)) })
                }
                composable(AppRoute.Search.route) {
                    SearchRoute(
                        onBack = { navController.popBackStack() },
                        onItemClick = { navController.navigate(AppRoute.Detail.create(it)) },
                    )
                }
                composable(AppRoute.Settings.route) {
                    SettingsRoute(
                        onManageFolders = { navController.navigate(AppRoute.FolderManage.route) },
                        onManageTags = { navController.navigate(AppRoute.TagManage.route) },
                        onEncryption = { navController.navigate(AppRoute.EncryptionSettings.route) },
                    )
                }
                composable(AppRoute.FolderManage.route) {
                    FolderManageRoute(onBack = { navController.popBackStack() })
                }
                composable(AppRoute.TagManage.route) {
                    TagManageRoute(onBack = { navController.popBackStack() })
                }
                composable(AppRoute.SecureSpace.route) {
                    SecureSpaceRoute(
                        onBack = { navController.popBackStack() },
                        onItemClick = { navController.navigate(AppRoute.Detail.create(it)) },
                    )
                }
                composable(AppRoute.EncryptionSettings.route) {
                    EncryptionSettingsRoute(onBack = { navController.popBackStack() })
                }
                composable(AppRoute.Detail.route) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()
                    val autoUnlock = backStackEntry.arguments?.getString("autoUnlock")?.toBooleanStrictOrNull() ?: false
                    ItemDetailRoute(
                        itemId = itemId,
                        autoUnlock = autoUnlock,
                        onBack = { navController.popBackStack() },
                        onEdit = { type, id -> navController.navigate(AppRoute.Editor.create(type, id)) },
                    )
                }
                composable(AppRoute.Editor.route) { backStackEntry ->
                    val typeName = backStackEntry.arguments?.getString("type") ?: ItemType.LINK.name
                    val itemId = backStackEntry.arguments?.getString("itemId")?.takeIf { it.isNotBlank() }
                    EditorRoute(
                        type = ItemType.valueOf(typeName),
                        itemId = itemId,
                        onBack = { navController.popBackStack() },
                        onManageFolders = { navController.navigate(AppRoute.FolderManage.route) },
                        onSaved = { savedId, protectionLevel ->
                            navController.navigate(
                                AppRoute.Detail.create(
                                    itemId = savedId,
                                    autoUnlock = protectionLevel != com.excelsior.pocketvault.domain.model.ItemProtectionLevel.NONE,
                                ),
                            ) {
                                popUpTo(AppRoute.Home.route)
                            }
                        },
                    )
                }
            }
        }
        if (uiState.lockRequired) {
            UnlockGate(
                securitySettings = uiState.securitySettings,
                onUnlocked = appViewModel::onUnlockSuccess,
            )
        }
    }
}
