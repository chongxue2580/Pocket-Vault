package com.excelsior.pocketvault.ui.navigation

import com.excelsior.pocketvault.domain.model.ItemType

sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object Category : AppRoute("category")
    data object Search : AppRoute("search")
    data object Settings : AppRoute("settings")
    data object SecureSpace : AppRoute("secure-space")
    data object EncryptionSettings : AppRoute("encryption-settings")
    data object FolderManage : AppRoute("folders")
    data object TagManage : AppRoute("tags")
    data object Detail : AppRoute("detail/{itemId}?autoUnlock={autoUnlock}") {
        fun create(itemId: String, autoUnlock: Boolean = false) = "detail/$itemId?autoUnlock=$autoUnlock"
    }
    data object Editor : AppRoute("editor/{type}?itemId={itemId}") {
        fun create(type: ItemType, itemId: String? = null): String =
            "editor/${type.name}?itemId=${itemId ?: ""}"
    }
}

val BottomBarRoutes = listOf(
    AppRoute.Home,
    AppRoute.Category,
    AppRoute.Search,
    AppRoute.Settings,
)
