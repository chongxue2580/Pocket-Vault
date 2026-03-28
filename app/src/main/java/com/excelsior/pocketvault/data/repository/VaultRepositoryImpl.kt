package com.excelsior.pocketvault.data.repository

import androidx.room.withTransaction
import com.excelsior.pocketvault.core.common.encodeStoredSecret
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.data.local.dao.VaultDao
import com.excelsior.pocketvault.data.local.db.PocketVaultDatabase
import com.excelsior.pocketvault.data.local.entity.CredentialItemEntity
import com.excelsior.pocketvault.data.local.entity.FolderEntity
import com.excelsior.pocketvault.data.local.entity.ImageItemEntity
import com.excelsior.pocketvault.data.local.entity.ItemEntity
import com.excelsior.pocketvault.data.local.entity.ItemSecurityEntity
import com.excelsior.pocketvault.data.local.entity.ItemTagCrossRef
import com.excelsior.pocketvault.data.local.entity.LinkItemEntity
import com.excelsior.pocketvault.data.local.entity.TagEntity
import com.excelsior.pocketvault.data.local.entity.TextItemEntity
import com.excelsior.pocketvault.data.mapper.toDomain
import com.excelsior.pocketvault.domain.model.CredentialDraft
import com.excelsior.pocketvault.domain.model.CredentialVaultItem
import com.excelsior.pocketvault.domain.model.DashboardMetrics
import com.excelsior.pocketvault.domain.model.ImageDraft
import com.excelsior.pocketvault.domain.model.ImageVaultItem
import com.excelsior.pocketvault.domain.model.ItemProtectionLevel
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.LinkDraft
import com.excelsior.pocketvault.domain.model.LinkVaultItem
import com.excelsior.pocketvault.domain.model.TextDraft
import com.excelsior.pocketvault.domain.model.TextVaultItem
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.repository.ImageStorageRepository
import com.excelsior.pocketvault.domain.repository.InsightRepository
import com.excelsior.pocketvault.domain.repository.ItemRepository
import com.excelsior.pocketvault.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val database: PocketVaultDatabase,
    private val dao: VaultDao,
    private val imageStorageRepository: ImageStorageRepository,
    private val securityRepository: SecurityRepository,
) : ItemRepository, InsightRepository {

    override fun observeItems(): Flow<List<VaultItem>> = dao.observeItemBundles().map { bundles ->
        bundles.mapNotNull { it.toDomain() }
            .filter { it.protectionLevel != ItemProtectionLevel.SUPER }
    }

    override fun observeSecureItems(): Flow<List<VaultItem>> = dao.observeItemBundles().map { bundles ->
        bundles.mapNotNull { it.toDomain() }
            .filter { it.protectionLevel == ItemProtectionLevel.SUPER }
    }

    override fun observeItem(itemId: String): Flow<VaultItem?> = dao.observeItemBundle(itemId).map { bundle ->
        bundle?.toDomain()
    }

    override suspend fun getItem(itemId: String): VaultItem? = dao.getItemBundle(itemId)?.toDomain()

    override suspend fun upsertLink(draft: LinkDraft): String {
        val id = draft.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.getItemEntity(id)
            val url = draft.url.trim()
            upsertItemEntity(
                item = ItemEntity(
                    id = id,
                    type = ItemType.LINK.name,
                    title = draft.title.trim(),
                    summary = if (draft.protectionLevel == ItemProtectionLevel.NONE) {
                        draft.note.takeIf(String::isNotBlank)
                    } else {
                        "已加密"
                    },
                    folderId = draft.folderId,
                    isPinned = draft.isPinned,
                    isFavorite = draft.isFavorite,
                    colorTheme = draft.colorTheme,
                    coverStyle = draft.coverStyle,
                    archived = false,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            dao.upsertLinkItem(
                LinkItemEntity(
                    itemId = id,
                    url = url,
                    siteName = VaultFormatters.hostFromUrl(url),
                    note = draft.note.ifBlank { null },
                    previewTitle = draft.previewTitle.ifBlank { null },
                    previewDescription = draft.previewDescription.ifBlank { null },
                ),
            )
            upsertItemSecurity(id, draft.protectionLevel, draft.titleVisibleWhenProtected)
            refreshTags(id, draft.tagIds)
        }
        return id
    }

    override suspend fun upsertText(draft: TextDraft): String {
        val id = draft.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.getItemEntity(id)
            upsertItemEntity(
                item = ItemEntity(
                    id = id,
                    type = ItemType.TEXT.name,
                    title = draft.title?.ifBlank { null },
                    summary = if (draft.protectionLevel == ItemProtectionLevel.NONE) {
                        draft.content.take(110)
                    } else {
                        "已加密"
                    },
                    folderId = draft.folderId,
                    isPinned = draft.isPinned,
                    isFavorite = draft.isFavorite,
                    colorTheme = draft.colorTheme,
                    coverStyle = draft.coverStyle,
                    archived = false,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            dao.upsertTextItem(
                TextItemEntity(
                    itemId = id,
                    content = draft.content,
                    source = draft.source.ifBlank { null },
                    quoteAuthor = draft.quoteAuthor.ifBlank { null },
                ),
            )
            upsertItemSecurity(id, draft.protectionLevel, draft.titleVisibleWhenProtected)
            refreshTags(id, draft.tagIds)
        }
        return id
    }

    override suspend fun upsertImage(draft: ImageDraft): String {
        val id = draft.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val existing = draft.id?.let { getItem(it) } as? ImageVaultItem
        val stored = when {
            draft.pickedUri != null -> {
                val imported = imageStorageRepository.importImage(draft.pickedUri)
                existing?.let {
                    imageStorageRepository.removeImage(it.localImagePath, it.thumbnailPath)
                }
                imported
            }

            draft.localImagePath.isNotBlank() && draft.thumbnailPath.isNotBlank() -> com.excelsior.pocketvault.domain.model.StoredImage(
                originalPath = draft.localImagePath,
                thumbnailPath = draft.thumbnailPath,
                aspectRatio = draft.aspectRatio,
            )

            existing != null -> com.excelsior.pocketvault.domain.model.StoredImage(
                originalPath = existing.localImagePath,
                thumbnailPath = existing.thumbnailPath,
                aspectRatio = existing.aspectRatio,
            )

            else -> error("图片尚未选择")
        }
        database.withTransaction {
            val itemEntity = dao.getItemEntity(id)
            upsertItemEntity(
                item = ItemEntity(
                    id = id,
                    type = ItemType.IMAGE.name,
                    title = draft.title?.ifBlank { null },
                    summary = if (draft.protectionLevel == ItemProtectionLevel.NONE) {
                        draft.note.ifBlank { null }
                    } else {
                        "已加密"
                    },
                    folderId = draft.folderId,
                    isPinned = draft.isPinned,
                    isFavorite = draft.isFavorite,
                    colorTheme = draft.colorTheme,
                    coverStyle = draft.coverStyle,
                    archived = false,
                    createdAt = itemEntity?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            dao.upsertImageItem(
                ImageItemEntity(
                    itemId = id,
                    localImagePath = stored.originalPath,
                    thumbnailPath = stored.thumbnailPath,
                    note = draft.note.ifBlank { null },
                    aspectRatio = stored.aspectRatio,
                ),
            )
            upsertItemSecurity(id, draft.protectionLevel, draft.titleVisibleWhenProtected)
            refreshTags(id, draft.tagIds)
        }
        return id
    }

    override suspend fun upsertCredential(draft: CredentialDraft): String {
        val id = draft.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val encryptedPassword = encodeStoredSecret(
            rawValue = draft.password,
            encrypted = draft.secretsEncrypted,
            encryptor = securityRepository::encrypt,
        )
        val encryptedNote = if (draft.note.isBlank()) {
            null
        } else {
            encodeStoredSecret(
                rawValue = draft.note,
                encrypted = draft.secretsEncrypted,
                encryptor = securityRepository::encrypt,
            )
        }
        database.withTransaction {
            val existing = dao.getItemEntity(id)
            upsertItemEntity(
                item = ItemEntity(
                    id = id,
                    type = ItemType.CREDENTIAL.name,
                    title = draft.title.trim(),
                    summary = if (draft.protectionLevel == ItemProtectionLevel.NONE) {
                        draft.username
                    } else {
                        "已加密"
                    },
                    folderId = draft.folderId,
                    isPinned = draft.isPinned,
                    isFavorite = draft.isFavorite,
                    colorTheme = draft.colorTheme,
                    coverStyle = draft.coverStyle,
                    archived = false,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            dao.upsertCredentialItem(
                CredentialItemEntity(
                    itemId = id,
                    siteName = draft.title.trim(),
                    websiteUrl = draft.websiteUrl.ifBlank { null },
                    username = draft.username.trim(),
                    email = draft.email.ifBlank { null },
                    passwordEncrypted = encryptedPassword,
                    noteEncrypted = encryptedNote,
                    lastUsedAt = null,
                ),
            )
            upsertItemSecurity(id, draft.protectionLevel, draft.titleVisibleWhenProtected)
            refreshTags(id, draft.tagIds)
        }
        return id
    }

    override suspend fun deleteItem(itemId: String) {
        val existing = getItem(itemId)
        if (existing is ImageVaultItem) {
            imageStorageRepository.removeImage(existing.localImagePath, existing.thumbnailPath)
        }
        dao.deleteItem(itemId)
    }

    override suspend fun togglePinned(itemId: String) {
        val item = dao.getItemEntity(itemId) ?: return
        dao.updatePinned(
            itemId = itemId,
            isPinned = !item.isPinned,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun toggleFavorite(itemId: String) {
        val item = dao.getItemEntity(itemId) ?: return
        dao.updateFavorite(
            itemId = itemId,
            isFavorite = !item.isFavorite,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun seedDemoData() {
        if (dao.itemCount() > 0) return
        val now = System.currentTimeMillis()
        val folders = listOf(
            FolderEntity(UUID.randomUUID().toString(), "灵感", "lightbulb", "#9CB4D8", now),
            FolderEntity(UUID.randomUUID().toString(), "常用网站", "link", "#A4B68D", now + 1),
            FolderEntity(UUID.randomUUID().toString(), "私密账号", "lock", "#D68C6E", now + 2),
        )
        val tags = listOf(
            TagEntity(UUID.randomUUID().toString(), "设计", "#8BA8D6", now),
            TagEntity(UUID.randomUUID().toString(), "灵感", "#B7A4D8", now + 1),
            TagEntity(UUID.randomUUID().toString(), "重要", "#E0A15D", now + 2),
            TagEntity(UUID.randomUUID().toString(), "离线", "#83A99B", now + 3),
        )
        for (folder in folders) {
            dao.upsertFolder(folder)
        }
        for (tag in tags) {
            dao.upsertTag(tag)
        }
        val demoImage = imageStorageRepository.createDemoArtwork("Pocket Vault")
        upsertLink(
            LinkDraft(
                title = "Material 3 Expressive 指南",
                url = "https://developer.android.com/design/ui/mobile/guides/components/material-overview",
                note = "记录值得借鉴的高级卡片与排版节奏。",
                folderId = folders[1].id,
                tagIds = listOf(tags[0].id, tags[1].id),
                isPinned = true,
            ),
        )
        upsertText(
            TextDraft(
                title = "今日句子",
                content = "把零碎收藏起来，才有可能在未来某天长成一座私人的陈列馆。",
                source = "项目示例文案",
                quoteAuthor = "Pocket Vault",
                folderId = folders[0].id,
                tagIds = listOf(tags[1].id, tags[3].id),
            ),
        )
        upsertImage(
            ImageDraft(
                title = "品牌氛围图",
                note = "应用内生成的离线演示大图。",
                folderId = folders[0].id,
                tagIds = listOf(tags[0].id, tags[1].id),
                localImagePath = demoImage.originalPath,
                thumbnailPath = demoImage.thumbnailPath,
                aspectRatio = demoImage.aspectRatio,
            ),
        )
        upsertCredential(
            CredentialDraft(
                title = "Figma 社区",
                websiteUrl = "https://www.figma.com",
                username = "designer@vault.local",
                email = "designer@vault.local",
                password = "Vault#2026",
                note = "示例数据，真实项目中将使用 Keystore 密钥加密。",
                folderId = folders[2].id,
                tagIds = listOf(tags[2].id),
                isPinned = true,
            ),
        )
    }

    override fun observeDashboardMetrics(): Flow<DashboardMetrics> = observeItems().map { items ->
        DashboardMetrics(
            totalCount = items.size,
            linkCount = items.count { it.type == ItemType.LINK },
            textCount = items.count { it.type == ItemType.TEXT },
            imageCount = items.count { it.type == ItemType.IMAGE },
            credentialCount = items.count { it.type == ItemType.CREDENTIAL },
            pinnedCount = items.count(VaultItem::isPinned),
            folderCounts = items.groupingBy { it.folder?.name ?: "未归档" }.eachCount(),
        )
    }

    override fun searchItems(
        query: String,
        type: ItemType?,
        tagId: String?,
        folderId: String?,
    ): Flow<List<VaultItem>> = observeItems().map { items ->
        val keyword = query.trim().lowercase()
        items.filter { item ->
            val matchesType = type == null || item.type == type
            val matchesTag = tagId == null || item.tags.any { tag -> tag.id == tagId }
            val matchesFolder = folderId == null || item.folder?.id == folderId
            val matchesKeyword = keyword.isBlank() || matchesQuery(item, keyword)
            matchesType && matchesTag && matchesFolder && matchesKeyword
        }
    }

    private suspend fun upsertItemEntity(item: ItemEntity) {
        dao.upsertItem(item)
        clearDetailRecords(item.id)
    }

    private suspend fun upsertItemSecurity(
        itemId: String,
        protectionLevel: ItemProtectionLevel,
        titleVisibleWhenProtected: Boolean,
    ) {
        dao.upsertItemSecurity(
            ItemSecurityEntity(
                itemId = itemId,
                protectionLevel = protectionLevel.name,
                titleVisible = titleVisibleWhenProtected,
            ),
        )
    }

    private suspend fun clearDetailRecords(itemId: String) {
        dao.deleteLinkItem(itemId)
        dao.deleteTextItem(itemId)
        dao.deleteImageItem(itemId)
        dao.deleteCredentialItem(itemId)
    }

    private suspend fun refreshTags(itemId: String, tagIds: List<String>) {
        dao.deleteTagsForItem(itemId)
        if (tagIds.isNotEmpty()) {
            dao.insertItemTags(tagIds.distinct().map { tagId -> ItemTagCrossRef(itemId, tagId) })
        }
    }

    private fun matchesQuery(item: VaultItem, keyword: String): Boolean {
        val values = buildList {
            if (item.protectionLevel == ItemProtectionLevel.NONE || item.titleVisibleWhenProtected) {
                add(item.title.orEmpty())
            }
            add(item.summary.orEmpty())
            add(item.folder?.name.orEmpty())
            addAll(item.tags.map { it.name })
            if (item.protectionLevel != ItemProtectionLevel.NONE) return@buildList
            when (item) {
                is LinkVaultItem -> {
                    add(item.url)
                    add(item.siteName)
                    add(item.note.orEmpty())
                    add(item.previewTitle.orEmpty())
                    add(item.previewDescription.orEmpty())
                }

                is TextVaultItem -> {
                    add(item.content)
                    add(item.source.orEmpty())
                    add(item.quoteAuthor.orEmpty())
                }

                is ImageVaultItem -> {
                    add(item.note.orEmpty())
                }

                is CredentialVaultItem -> {
                    add(item.siteName)
                    add(item.websiteUrl.orEmpty())
                    add(item.username)
                    add(item.email.orEmpty())
                }
            }
        }
        return values.any { it.lowercase().contains(keyword) }
    }
}
