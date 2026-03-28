@file:OptIn(ExperimentalLayoutApi::class, ExperimentalCoroutinesApi::class)

package com.excelsior.pocketvault.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.core.designsystem.component.VaultChip
import com.excelsior.pocketvault.core.designsystem.component.VaultEmptyState
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultItemCard
import com.excelsior.pocketvault.core.designsystem.component.VaultSearchBar
import com.excelsior.pocketvault.core.designsystem.component.VaultSectionTitle
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.SearchHistoryEntry
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.usecase.ObserveFoldersUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSearchHistoryUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveTagsUseCase
import com.excelsior.pocketvault.domain.usecase.SaveSearchQueryUseCase
import com.excelsior.pocketvault.domain.usecase.SearchItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChanged,
        onTypeSelected = viewModel::onTypeSelected,
        onFolderSelected = viewModel::onFolderSelected,
        onTagSelected = viewModel::onTagSelected,
        onHistorySelected = viewModel::onHistorySelected,
        onItemClick = onItemClick,
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTypeSelected: (ItemType?) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onTagSelected: (String?) -> Unit,
    onHistorySelected: (String) -> Unit,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = "搜索",
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
            )
        }
        item {
            VaultSearchBar(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = "搜索标题、标签、网址、用户名或备注",
            )
        }
        item {
            VaultSectionTitle(title = "筛选", caption = null)
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VaultChip(label = "全部类型", selected = state.selectedType == null, onClick = { onTypeSelected(null) })
                ItemType.entries.forEach { type ->
                    VaultChip(
                        label = type.searchLabel(),
                        selected = state.selectedType == type,
                        onClick = { onTypeSelected(type) },
                    )
                }
            }
        }
        if (state.folders.isNotEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VaultChip(label = "全部收藏夹", selected = state.selectedFolderId == null, onClick = { onFolderSelected(null) })
                    state.folders.forEach { folder ->
                        VaultChip(
                            label = folder.name,
                            selected = state.selectedFolderId == folder.id,
                            onClick = { onFolderSelected(folder.id.takeUnless { it == state.selectedFolderId }) },
                        )
                    }
                }
            }
        }
        if (state.tags.isNotEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VaultChip(label = "全部标签", selected = state.selectedTagId == null, onClick = { onTagSelected(null) })
                    state.tags.take(8).forEach { tag ->
                        VaultChip(
                            label = "#${tag.name}",
                            selected = state.selectedTagId == tag.id,
                            onClick = { onTagSelected(tag.id.takeUnless { it == state.selectedTagId }) },
                        )
                    }
                }
            }
        }
        if (state.query.isBlank() && state.history.isNotEmpty()) {
            item {
                VaultGlassCard {
                    VaultSectionTitle(title = "最近搜索", caption = null)
                    FlowRow(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.history.forEach { history ->
                            VaultChip(
                                label = history.query,
                                selected = false,
                                onClick = { onHistorySelected(history.query) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = if (state.query.isBlank()) "全部" else "${state.results.size} 条结果",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (state.results.isEmpty()) {
            item {
                VaultEmptyState(
                    title = if (state.query.isBlank()) "开始搜索你的收藏" else "没有找到匹配结果",
                    description = "",
                )
            }
        } else {
            items(state.results, key = { it.id }) { item ->
                VaultItemCard(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    onClick = { onItemClick(item.id) },
                )
            }
        }
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    observeFoldersUseCase: ObserveFoldersUseCase,
    observeTagsUseCase: ObserveTagsUseCase,
    observeSearchHistoryUseCase: ObserveSearchHistoryUseCase,
    private val searchItemsUseCase: SearchItemsUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
) : androidx.lifecycle.ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedType = MutableStateFlow<ItemType?>(null)
    private val selectedFolderId = MutableStateFlow<String?>(null)
    private val selectedTagId = MutableStateFlow<String?>(null)
    private var saveJob: Job? = null

    private val filters = combine(
        query,
        selectedType,
        selectedFolderId,
        selectedTagId,
    ) { currentQuery, type, folderId, tagId ->
        SearchFilterState(currentQuery, type, folderId, tagId)
    }

    private val searchResults = filters.flatMapLatest { params ->
        searchItemsUseCase(
            query = params.query,
            type = params.selectedType,
            tagId = params.selectedTagId,
            folderId = params.selectedFolderId,
        )
    }

    val uiState: StateFlow<SearchUiState> = combine(
        filters,
        searchResults,
        observeFoldersUseCase(),
        observeTagsUseCase(),
        observeSearchHistoryUseCase(),
    ) { filterState, results, folders, tags, history ->
        SearchUiState(
            query = filterState.query,
            selectedType = filterState.selectedType,
            selectedFolderId = filterState.selectedFolderId,
            selectedTagId = filterState.selectedTagId,
            results = results,
            folders = folders,
            tags = tags,
            history = history,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun onQueryChanged(value: String) {
        query.update { value }
        scheduleSave(value)
    }

    fun onHistorySelected(value: String) {
        query.update { value }
        scheduleSave(value)
    }

    fun onTypeSelected(type: ItemType?) {
        selectedType.update { type }
    }

    fun onFolderSelected(folderId: String?) {
        selectedFolderId.update { folderId }
    }

    fun onTagSelected(tagId: String?) {
        selectedTagId.update { tagId }
    }

    private fun scheduleSave(value: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            val normalized = value.trim()
            if (normalized.length < 2) return@launch
            delay(450)
            if (query.value.trim() == normalized) {
                saveSearchQueryUseCase(normalized)
            }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val selectedType: ItemType? = null,
    val selectedFolderId: String? = null,
    val selectedTagId: String? = null,
    val results: List<VaultItem> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val history: List<SearchHistoryEntry> = emptyList(),
)

private data class SearchFilterState(
    val query: String,
    val selectedType: ItemType?,
    val selectedFolderId: String?,
    val selectedTagId: String?,
)

private fun ItemType.searchLabel(): String = when (this) {
    ItemType.LINK -> "链接"
    ItemType.TEXT -> "便签"
    ItemType.IMAGE -> "图片"
    ItemType.CREDENTIAL -> "密码"
}
