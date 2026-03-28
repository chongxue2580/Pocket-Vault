package com.excelsior.pocketvault.data.mapper

import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.core.common.isStoredSecretEncrypted
import com.excelsior.pocketvault.data.local.entity.FolderEntity
import com.excelsior.pocketvault.data.local.entity.TagEntity
import com.excelsior.pocketvault.data.local.entity.VaultItemBundle
import com.excelsior.pocketvault.domain.model.CredentialVaultItem
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.ImageVaultItem
import com.excelsior.pocketvault.domain.model.ItemProtectionLevel
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.LinkVaultItem
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.model.TextVaultItem
import com.excelsior.pocketvault.domain.model.VaultItem

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    icon = icon,
    color = color,
    createdAt = createdAt,
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    color = color,
    createdAt = createdAt,
)

fun VaultItemBundle.toDomain(): VaultItem? {
    val folderModel = folder?.toDomain()
    val tagModels = tags.map(TagEntity::toDomain)
    val protectionLevel = security
        ?.protectionLevel
        ?.let { runCatching { ItemProtectionLevel.valueOf(it) }.getOrNull() }
        ?: ItemProtectionLevel.NONE
    val titleVisibleWhenProtected = security?.titleVisible ?: false
    return when (ItemType.valueOf(item.type)) {
        ItemType.LINK -> link?.let {
            LinkVaultItem(
                id = item.id,
                title = item.title.orEmpty(),
                summary = item.summary ?: it.note,
                protectionLevel = protectionLevel,
                titleVisibleWhenProtected = titleVisibleWhenProtected,
                folder = folderModel,
                tags = tagModels,
                isPinned = item.isPinned,
                isFavorite = item.isFavorite,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                colorTheme = item.colorTheme,
                coverStyle = item.coverStyle,
                archived = item.archived,
                url = it.url,
                siteName = it.siteName.ifBlank { VaultFormatters.hostFromUrl(it.url) },
                note = it.note,
                previewTitle = it.previewTitle,
                previewDescription = it.previewDescription,
            )
        }

        ItemType.TEXT -> text?.let {
            TextVaultItem(
                id = item.id,
                title = item.title,
                summary = item.summary ?: it.content.take(96),
                protectionLevel = protectionLevel,
                titleVisibleWhenProtected = titleVisibleWhenProtected,
                folder = folderModel,
                tags = tagModels,
                isPinned = item.isPinned,
                isFavorite = item.isFavorite,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                colorTheme = item.colorTheme,
                coverStyle = item.coverStyle,
                archived = item.archived,
                content = it.content,
                source = it.source,
                quoteAuthor = it.quoteAuthor,
            )
        }

        ItemType.IMAGE -> image?.let {
            ImageVaultItem(
                id = item.id,
                title = item.title,
                summary = item.summary ?: it.note,
                protectionLevel = protectionLevel,
                titleVisibleWhenProtected = titleVisibleWhenProtected,
                folder = folderModel,
                tags = tagModels,
                isPinned = item.isPinned,
                isFavorite = item.isFavorite,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                colorTheme = item.colorTheme,
                coverStyle = item.coverStyle,
                archived = item.archived,
                localImagePath = it.localImagePath,
                thumbnailPath = it.thumbnailPath,
                note = it.note,
                aspectRatio = it.aspectRatio,
            )
        }

        ItemType.CREDENTIAL -> credential?.let {
            CredentialVaultItem(
                id = item.id,
                title = item.title ?: it.siteName,
                summary = item.summary ?: it.username,
                protectionLevel = protectionLevel,
                titleVisibleWhenProtected = titleVisibleWhenProtected,
                folder = folderModel,
                tags = tagModels,
                isPinned = item.isPinned,
                isFavorite = item.isFavorite,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                colorTheme = item.colorTheme,
                coverStyle = item.coverStyle,
                archived = item.archived,
                siteName = it.siteName,
                websiteUrl = it.websiteUrl,
                username = it.username,
                email = it.email,
                encryptedPassword = it.passwordEncrypted,
                encryptedNote = it.noteEncrypted,
                secretsEncrypted = isStoredSecretEncrypted(it.passwordEncrypted),
                lastUsedAt = it.lastUsedAt,
            )
        }
    }
}
