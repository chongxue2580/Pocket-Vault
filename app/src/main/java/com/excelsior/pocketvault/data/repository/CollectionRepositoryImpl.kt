package com.excelsior.pocketvault.data.repository

import com.excelsior.pocketvault.data.local.dao.VaultDao
import com.excelsior.pocketvault.data.local.entity.FolderEntity
import com.excelsior.pocketvault.data.local.entity.TagEntity
import com.excelsior.pocketvault.data.mapper.toDomain
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.repository.FolderRepository
import com.excelsior.pocketvault.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val dao: VaultDao,
) : FolderRepository, TagRepository {

    override fun observeFolders(): Flow<List<Folder>> = dao.observeFolders().map { folders ->
        folders.map(FolderEntity::toDomain)
    }

    override suspend fun upsertFolder(folder: Folder) {
        dao.upsertFolder(
            FolderEntity(
                id = folder.id,
                name = folder.name,
                icon = folder.icon,
                color = folder.color,
                createdAt = folder.createdAt,
            ),
        )
    }

    override suspend fun deleteFolder(folderId: String, migrateToFolderId: String?) {
        dao.migrateFolderItems(folderId, migrateToFolderId)
        dao.deleteFolder(folderId)
    }

    override fun observeTags(): Flow<List<Tag>> = dao.observeTags().map { tags ->
        tags.map(TagEntity::toDomain)
    }

    override suspend fun upsertTag(tag: Tag) {
        dao.upsertTag(
            TagEntity(
                id = tag.id,
                name = tag.name,
                color = tag.color,
                createdAt = tag.createdAt,
            ),
        )
    }

    override suspend fun deleteTag(tagId: String) {
        dao.deleteItemLinksForTag(tagId)
        dao.deleteTag(tagId)
    }
}
