package com.excelsior.pocketvault.domain.usecase

import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.repository.FolderRepository
import com.excelsior.pocketvault.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFoldersUseCase @Inject constructor(
    private val repository: FolderRepository,
) {
    operator fun invoke(): Flow<List<Folder>> = repository.observeFolders()
}

class ObserveTagsUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    operator fun invoke(): Flow<List<Tag>> = repository.observeTags()
}

class UpsertFolderUseCase @Inject constructor(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(folder: Folder) = repository.upsertFolder(folder)
}

class DeleteFolderUseCase @Inject constructor(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(folderId: String, migrateToFolderId: String?) = repository.deleteFolder(folderId, migrateToFolderId)
}

class UpsertTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(tag: Tag) = repository.upsertTag(tag)
}

class DeleteTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(tagId: String) = repository.deleteTag(tagId)
}
