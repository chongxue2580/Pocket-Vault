package com.excelsior.pocketvault.domain.repository

import android.net.Uri
import com.excelsior.pocketvault.domain.model.AppearanceSettings
import com.excelsior.pocketvault.domain.model.CredentialDraft
import com.excelsior.pocketvault.domain.model.DashboardMetrics
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.ImageDraft
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.LinkDraft
import com.excelsior.pocketvault.domain.model.SearchHistoryEntry
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.StoredImage
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.model.TextDraft
import com.excelsior.pocketvault.domain.model.ThemeMode
import com.excelsior.pocketvault.domain.model.VaultItem
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun observeItems(): Flow<List<VaultItem>>
    fun observeSecureItems(): Flow<List<VaultItem>>
    fun observeItem(itemId: String): Flow<VaultItem?>
    suspend fun getItem(itemId: String): VaultItem?
    suspend fun upsertLink(draft: LinkDraft): String
    suspend fun upsertText(draft: TextDraft): String
    suspend fun upsertImage(draft: ImageDraft): String
    suspend fun upsertCredential(draft: CredentialDraft): String
    suspend fun deleteItem(itemId: String)
    suspend fun togglePinned(itemId: String)
    suspend fun toggleFavorite(itemId: String)
    suspend fun seedDemoData()
}

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>
    suspend fun upsertFolder(folder: Folder)
    suspend fun deleteFolder(folderId: String, migrateToFolderId: String?)
}

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun upsertTag(tag: Tag)
    suspend fun deleteTag(tagId: String)
}

interface SettingsRepository {
    fun observeAppearanceSettings(): Flow<AppearanceSettings>
    fun observeSecuritySettings(): Flow<SecuritySettings>
    fun observeSearchHistory(): Flow<List<SearchHistoryEntry>>
    fun observeHomeOrder(): Flow<List<String>>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setCardStyle(style: String)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setRequireAuthForSecrets(enabled: Boolean)
    suspend fun setScreenshotProtection(enabled: Boolean)
    suspend fun setAutoLockSeconds(seconds: Int)
    suspend fun saveSearchQuery(query: String)
    suspend fun clearSearchHistory()
    suspend fun saveHomeOrder(ids: List<String>)
}

interface SecurityRepository {
    suspend fun encrypt(value: String): String
    suspend fun decrypt(cipherText: String): Result<String>
    suspend fun savePin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun clearPin()
    suspend fun saveSecondPin(pin: String)
    suspend fun verifySecondPin(pin: String): Boolean
    suspend fun clearSecondPin()
    fun isBiometricAvailable(): Boolean
}

interface ImageStorageRepository {
    suspend fun importImage(uri: Uri): StoredImage
    suspend fun removeImage(originalPath: String?, thumbnailPath: String?)
    suspend fun createDemoArtwork(title: String): StoredImage
}

interface InsightRepository {
    fun observeDashboardMetrics(): Flow<DashboardMetrics>
    fun searchItems(
        query: String,
        type: ItemType? = null,
        tagId: String? = null,
        folderId: String? = null,
    ): Flow<List<VaultItem>>
}
