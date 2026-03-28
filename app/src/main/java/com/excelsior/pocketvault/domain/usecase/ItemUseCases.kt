package com.excelsior.pocketvault.domain.usecase

import com.excelsior.pocketvault.domain.model.CredentialDraft
import com.excelsior.pocketvault.domain.model.DashboardMetrics
import com.excelsior.pocketvault.domain.model.ImageDraft
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.LinkDraft
import com.excelsior.pocketvault.domain.model.TextDraft
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.repository.InsightRepository
import com.excelsior.pocketvault.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveItemsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(): Flow<List<VaultItem>> = repository.observeItems()
}

class ObserveSecureItemsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(): Flow<List<VaultItem>> = repository.observeSecureItems()
}

class ObserveItemDetailUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(itemId: String): Flow<VaultItem?> = repository.observeItem(itemId)
}

class GetItemDetailUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(itemId: String): VaultItem? = repository.getItem(itemId)
}

class CreateLinkItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(draft: LinkDraft): String = repository.upsertLink(draft)
}

class CreateTextItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(draft: TextDraft): String = repository.upsertText(draft)
}

class CreateImageItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(draft: ImageDraft): String = repository.upsertImage(draft)
}

class CreateCredentialItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(draft: CredentialDraft): String = repository.upsertCredential(draft)
}

class DeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(itemId: String) = repository.deleteItem(itemId)
}

class TogglePinUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(itemId: String) = repository.togglePinned(itemId)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(itemId: String) = repository.toggleFavorite(itemId)
}

class SearchItemsUseCase @Inject constructor(
    private val repository: InsightRepository,
) {
    operator fun invoke(
        query: String,
        type: ItemType? = null,
        tagId: String? = null,
        folderId: String? = null,
    ): Flow<List<VaultItem>> = repository.searchItems(query, type, tagId, folderId)
}

class ObserveDashboardMetricsUseCase @Inject constructor(
    private val repository: InsightRepository,
) {
    operator fun invoke(): Flow<DashboardMetrics> = repository.observeDashboardMetrics()
}

class SeedDemoDataUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke() = repository.seedDemoData()
}
