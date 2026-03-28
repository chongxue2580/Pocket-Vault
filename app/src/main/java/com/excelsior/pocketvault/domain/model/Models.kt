package com.excelsior.pocketvault.domain.model

import android.net.Uri

enum class ItemType {
    LINK,
    TEXT,
    IMAGE,
    CREDENTIAL,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ItemProtectionLevel {
    NONE,
    STANDARD,
    SUPER,
}

data class Folder(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val createdAt: Long,
)

data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val createdAt: Long,
)

sealed interface VaultItem {
    val id: String
    val type: ItemType
    val title: String?
    val summary: String?
    val protectionLevel: ItemProtectionLevel
    val titleVisibleWhenProtected: Boolean
    val folder: Folder?
    val tags: List<Tag>
    val isPinned: Boolean
    val isFavorite: Boolean
    val createdAt: Long
    val updatedAt: Long
    val colorTheme: String?
    val coverStyle: String?
    val archived: Boolean
}

data class LinkVaultItem(
    override val id: String,
    override val title: String,
    override val summary: String?,
    override val protectionLevel: ItemProtectionLevel,
    override val titleVisibleWhenProtected: Boolean,
    override val folder: Folder?,
    override val tags: List<Tag>,
    override val isPinned: Boolean,
    override val isFavorite: Boolean,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val colorTheme: String?,
    override val coverStyle: String?,
    override val archived: Boolean,
    val url: String,
    val siteName: String,
    val note: String?,
    val previewTitle: String?,
    val previewDescription: String?,
) : VaultItem {
    override val type: ItemType = ItemType.LINK
}

data class TextVaultItem(
    override val id: String,
    override val title: String?,
    override val summary: String?,
    override val protectionLevel: ItemProtectionLevel,
    override val titleVisibleWhenProtected: Boolean,
    override val folder: Folder?,
    override val tags: List<Tag>,
    override val isPinned: Boolean,
    override val isFavorite: Boolean,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val colorTheme: String?,
    override val coverStyle: String?,
    override val archived: Boolean,
    val content: String,
    val source: String?,
    val quoteAuthor: String?,
) : VaultItem {
    override val type: ItemType = ItemType.TEXT
}

data class ImageVaultItem(
    override val id: String,
    override val title: String?,
    override val summary: String?,
    override val protectionLevel: ItemProtectionLevel,
    override val titleVisibleWhenProtected: Boolean,
    override val folder: Folder?,
    override val tags: List<Tag>,
    override val isPinned: Boolean,
    override val isFavorite: Boolean,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val colorTheme: String?,
    override val coverStyle: String?,
    override val archived: Boolean,
    val localImagePath: String,
    val thumbnailPath: String,
    val note: String?,
    val aspectRatio: Float,
) : VaultItem {
    override val type: ItemType = ItemType.IMAGE
}

data class CredentialVaultItem(
    override val id: String,
    override val title: String,
    override val summary: String?,
    override val protectionLevel: ItemProtectionLevel,
    override val titleVisibleWhenProtected: Boolean,
    override val folder: Folder?,
    override val tags: List<Tag>,
    override val isPinned: Boolean,
    override val isFavorite: Boolean,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val colorTheme: String?,
    override val coverStyle: String?,
    override val archived: Boolean,
    val siteName: String,
    val websiteUrl: String?,
    val username: String,
    val email: String?,
    val encryptedPassword: String,
    val encryptedNote: String?,
    val secretsEncrypted: Boolean,
    val lastUsedAt: Long?,
) : VaultItem {
    override val type: ItemType = ItemType.CREDENTIAL
}

data class StoredImage(
    val originalPath: String,
    val thumbnailPath: String,
    val aspectRatio: Float,
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cardStyle: String = "single",
)

data class SecuritySettings(
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val requireAuthForSecrets: Boolean = true,
    val screenshotProtection: Boolean = false,
    val autoLockSeconds: Int = 30,
    val hasPin: Boolean = false,
    val hasSecondPin: Boolean = false,
)

data class DashboardMetrics(
    val totalCount: Int = 0,
    val linkCount: Int = 0,
    val textCount: Int = 0,
    val imageCount: Int = 0,
    val credentialCount: Int = 0,
    val pinnedCount: Int = 0,
    val folderCounts: Map<String, Int> = emptyMap(),
)

data class SearchHistoryEntry(
    val query: String,
    val timestamp: Long,
)

sealed interface VaultDraft {
    val id: String?
    val title: String?
    val folderId: String?
    val tagIds: List<String>
    val isPinned: Boolean
    val isFavorite: Boolean
    val protectionLevel: ItemProtectionLevel
    val titleVisibleWhenProtected: Boolean
    val colorTheme: String
    val coverStyle: String
}

data class LinkDraft(
    override val id: String? = null,
    override val title: String,
    override val folderId: String? = null,
    override val tagIds: List<String> = emptyList(),
    override val isPinned: Boolean = false,
    override val isFavorite: Boolean = false,
    override val protectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
    override val titleVisibleWhenProtected: Boolean = true,
    override val colorTheme: String = "mist",
    override val coverStyle: String = "glass",
    val url: String,
    val note: String = "",
    val previewTitle: String = "",
    val previewDescription: String = "",
) : VaultDraft

data class TextDraft(
    override val id: String? = null,
    override val title: String? = null,
    override val folderId: String? = null,
    override val tagIds: List<String> = emptyList(),
    override val isPinned: Boolean = false,
    override val isFavorite: Boolean = false,
    override val protectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
    override val titleVisibleWhenProtected: Boolean = true,
    override val colorTheme: String = "sage",
    override val coverStyle: String = "quote",
    val content: String,
    val source: String = "",
    val quoteAuthor: String = "",
) : VaultDraft

data class ImageDraft(
    override val id: String? = null,
    override val title: String? = null,
    override val folderId: String? = null,
    override val tagIds: List<String> = emptyList(),
    override val isPinned: Boolean = false,
    override val isFavorite: Boolean = false,
    override val protectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
    override val titleVisibleWhenProtected: Boolean = true,
    override val colorTheme: String = "sunset",
    override val coverStyle: String = "gallery",
    val localImagePath: String = "",
    val thumbnailPath: String = "",
    val note: String = "",
    val aspectRatio: Float = 1f,
    val pickedUri: Uri? = null,
) : VaultDraft

data class CredentialDraft(
    override val id: String? = null,
    override val title: String,
    override val folderId: String? = null,
    override val tagIds: List<String> = emptyList(),
    override val isPinned: Boolean = false,
    override val isFavorite: Boolean = false,
    override val protectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
    override val titleVisibleWhenProtected: Boolean = true,
    override val colorTheme: String = "night",
    override val coverStyle: String = "secure",
    val secretsEncrypted: Boolean = true,
    val websiteUrl: String = "",
    val username: String,
    val email: String = "",
    val password: String,
    val note: String = "",
) : VaultDraft
