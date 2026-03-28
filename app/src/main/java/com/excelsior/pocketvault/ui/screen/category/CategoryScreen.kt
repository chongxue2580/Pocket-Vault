@file:OptIn(ExperimentalLayoutApi::class)

package com.excelsior.pocketvault.ui.screen.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.excelsior.pocketvault.core.designsystem.component.VaultSectionTitle
import com.excelsior.pocketvault.core.designsystem.component.VaultStatTile
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.usecase.ObserveAppearanceSettingsUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveFoldersUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Composable
fun CategoryRoute(
    onItemClick: (String) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryScreen(
        state = state,
        onTypeSelected = viewModel::onTypeSelected,
        onFolderSelected = viewModel::onFolderSelected,
        onItemClick = onItemClick,
    )
}

@Composable
fun CategoryScreen(
    state: CategoryUiState,
    onTypeSelected: (ItemType?) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onItemClick: (String) -> Unit,
) {
    if (state.cardStyle == "double") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                CategoryHeader(state = state, onTypeSelected = onTypeSelected, onFolderSelected = onFolderSelected)
            }
            if (state.filteredItems.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    VaultEmptyState(title = "这个分类还没有内容", description = "")
                }
            } else {
                items(state.filteredItems, key = { it.id }) { item ->
                    Box {
                        VaultItemCard(
                            item = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(208.dp),
                            onClick = { onItemClick(item.id) },
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CategoryHeader(state = state, onTypeSelected = onTypeSelected, onFolderSelected = onFolderSelected)
            }
            if (state.filteredItems.isEmpty()) {
                item {
                    VaultEmptyState(title = "这个分类还没有内容", description = "")
                }
            } else {
                items(state.filteredItems, key = { it.id }) { item ->
                    Box {
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
    }
}

@Composable
private fun CategoryHeader(
    state: CategoryUiState,
    onTypeSelected: (ItemType?) -> Unit,
    onFolderSelected: (String?) -> Unit,
) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        VaultTopBar(
            title = "分类",
            subtitle = null,
            actions = {
                IconButton(onClick = { onTypeSelected(null); onFolderSelected(null) }) {
                    Icon(imageVector = VaultIcons.Category, contentDescription = null)
                }
            },
        )
        VaultGlassCard {
            VaultSectionTitle(title = "内容概览", caption = null)
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryMetricTile(title = "链接", value = state.linkCount.toString())
                CategoryMetricTile(title = "文字", value = state.textCount.toString())
                CategoryMetricTile(title = "图片", value = state.imageCount.toString())
                CategoryMetricTile(title = "密码", value = state.credentialCount.toString())
            }
        }
        VaultSectionTitle(title = "按类型浏览", caption = null)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultChip(label = "全部", selected = state.selectedType == null, onClick = { onTypeSelected(null) })
            ItemType.entries.forEach { type ->
                VaultChip(
                    label = type.categoryLabel(),
                    selected = state.selectedType == type,
                    onClick = { onTypeSelected(type) },
                )
            }
        }
        if (state.folders.isNotEmpty()) {
            VaultSectionTitle(title = "收藏夹", caption = null)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VaultChip(label = "全部收藏夹", selected = state.selectedFolderId == null, onClick = { onFolderSelected(null) })
                state.folders.forEach { folder ->
                    VaultChip(
                        label = folder.name,
                        selected = state.selectedFolderId == folder.id,
                        onClick = { onFolderSelected(folder.id) },
                    )
                }
            }
        }
        VaultSectionTitle(title = "结果", caption = null)
    }
}

@Composable
private fun CategoryMetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    observeFoldersUseCase: ObserveFoldersUseCase,
    observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase,
) : androidx.lifecycle.ViewModel() {
    private val selectedType = MutableStateFlow<ItemType?>(null)
    private val selectedFolderId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoryUiState> = combine(
        observeItemsUseCase(),
        observeFoldersUseCase(),
        selectedType,
        selectedFolderId,
        observeAppearanceSettingsUseCase(),
    ) { items, folders, type, folderId, appearance ->
        val filteredItems = items.filter { item ->
            (type == null || item.type == type) &&
                (folderId == null || item.folder?.id == folderId)
        }
        CategoryUiState(
            selectedType = type,
            selectedFolderId = folderId,
            cardStyle = appearance.cardStyle,
            folders = folders,
            filteredItems = filteredItems,
            linkCount = items.count { it.type == ItemType.LINK },
            textCount = items.count { it.type == ItemType.TEXT },
            imageCount = items.count { it.type == ItemType.IMAGE },
            credentialCount = items.count { it.type == ItemType.CREDENTIAL },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryUiState(),
    )

    fun onTypeSelected(type: ItemType?) {
        selectedType.update { type }
    }

    fun onFolderSelected(folderId: String?) {
        selectedFolderId.update { folderId }
    }
}

data class CategoryUiState(
    val selectedType: ItemType? = null,
    val selectedFolderId: String? = null,
    val cardStyle: String = "single",
    val folders: List<Folder> = emptyList(),
    val filteredItems: List<VaultItem> = emptyList(),
    val linkCount: Int = 0,
    val textCount: Int = 0,
    val imageCount: Int = 0,
    val credentialCount: Int = 0,
)

private fun ItemType.categoryLabel(): String = when (this) {
    ItemType.LINK -> "链接"
    ItemType.TEXT -> "便签"
    ItemType.IMAGE -> "图片"
    ItemType.CREDENTIAL -> "密码"
}
