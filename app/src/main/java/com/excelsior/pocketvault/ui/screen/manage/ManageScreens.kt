@file:OptIn(ExperimentalLayoutApi::class)

package com.excelsior.pocketvault.ui.screen.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.core.designsystem.component.VaultChip
import com.excelsior.pocketvault.core.designsystem.component.VaultEmptyState
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultTextField
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.usecase.DeleteFolderUseCase
import com.excelsior.pocketvault.domain.usecase.DeleteTagUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveFoldersUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveItemsUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveTagsUseCase
import com.excelsior.pocketvault.domain.usecase.UpsertFolderUseCase
import com.excelsior.pocketvault.domain.usecase.UpsertTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun FolderManageRoute(
    onBack: () -> Unit,
    viewModel: FolderManageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingFolder by remember { mutableStateOf<FolderEditorDraft?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderSummary?>(null) }
    var migrateToFolderId by remember { mutableStateOf<String?>(null) }

    FolderManageScreen(
        state = state,
        onBack = onBack,
        onCreate = { editingFolder = FolderEditorDraft() },
        onEdit = { folder ->
            editingFolder = FolderEditorDraft(
                id = folder.id,
                name = folder.name,
                color = folder.color,
                icon = folder.icon,
                createdAt = folder.createdAt,
            )
        },
        onDelete = { folder ->
            deletingFolder = folder
            migrateToFolderId = null
        },
    )

    editingFolder?.let { draft ->
        FolderEditorDialog(
            draft = draft,
            onDismiss = { editingFolder = null },
            onSave = { updated ->
                viewModel.saveFolder(updated)
                editingFolder = null
            },
        )
    }

    deletingFolder?.let { folder ->
        FolderDeleteDialog(
            folder = folder,
            candidates = state.folders.filterNot { it.id == folder.id },
            migrateToFolderId = migrateToFolderId,
            onMigrateChange = { migrateToFolderId = it },
            onDismiss = { deletingFolder = null },
            onConfirm = {
                viewModel.deleteFolder(folder.id, migrateToFolderId)
                deletingFolder = null
            },
        )
    }
}

@Composable
fun TagManageRoute(
    onBack: () -> Unit,
    viewModel: TagManageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTag by remember { mutableStateOf<TagEditorDraft?>(null) }
    var deletingTag by remember { mutableStateOf<TagSummary?>(null) }

    TagManageScreen(
        state = state,
        onBack = onBack,
        onCreate = { editingTag = TagEditorDraft() },
        onEdit = { tag ->
            editingTag = TagEditorDraft(
                id = tag.id,
                name = tag.name,
                color = tag.color,
                createdAt = tag.createdAt,
            )
        },
        onDelete = { deletingTag = it },
    )

    editingTag?.let { draft ->
        TagEditorDialog(
            draft = draft,
            onDismiss = { editingTag = null },
            onSave = { updated ->
                viewModel.saveTag(updated)
                editingTag = null
            },
        )
    }

    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text(text = "删除标签") },
            text = { Text(text = "删除后，已关联内容会移除该标签，但不会删除内容本身。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag.id)
                        deletingTag = null
                    },
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
private fun FolderManageScreen(
    state: FolderManageUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (FolderSummary) -> Unit,
    onDelete: (FolderSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = "收藏夹",
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(imageVector = VaultIcons.Edit, contentDescription = null)
                    }
                },
            )
        }
        if (state.folders.isEmpty()) {
            item {
                VaultEmptyState(
                    title = "还没有收藏夹",
                    description = "",
                    actionLabel = "新建收藏夹",
                    onAction = onCreate,
                )
            }
        } else {
            items(state.folders, key = { it.id }) { folder ->
                ManageFolderCard(
                    folder = folder,
                    onEdit = { onEdit(folder) },
                    onDelete = { onDelete(folder) },
                )
            }
        }
    }
}

@Composable
private fun TagManageScreen(
    state: TagManageUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (TagSummary) -> Unit,
    onDelete: (TagSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = "标签",
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(imageVector = VaultIcons.Label, contentDescription = null)
                    }
                },
            )
        }
        if (state.tags.isEmpty()) {
            item {
                VaultEmptyState(
                    title = "还没有标签",
                    description = "",
                    actionLabel = "新建标签",
                    onAction = onCreate,
                )
            }
        } else {
            items(state.tags, key = { it.id }) { tag ->
                ManageTagCard(
                    tag = tag,
                    onEdit = { onEdit(tag) },
                    onDelete = { onDelete(tag) },
                )
            }
        }
    }
}

@Composable
private fun ManageFolderCard(
    folder: FolderSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    VaultGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(color = folder.color, label = folder.name.take(1))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = folder.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${folder.itemCount} 条内容",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = VaultIcons.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = VaultIcons.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ManageTagCard(
    tag: TagSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    VaultGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(color = tag.color, label = "#")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "#${tag.name}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${tag.itemCount} 条内容正在使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = VaultIcons.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = VaultIcons.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: String,
    label: String,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(parseColor(color)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun FolderEditorDialog(
    draft: FolderEditorDraft,
    onDismiss: () -> Unit,
    onSave: (FolderEditorDraft) -> Unit,
) {
    var current by remember(draft) { mutableStateOf(draft) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (draft.id == null) "新建收藏夹" else "编辑收藏夹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                VaultTextField(
                    value = current.name,
                    onValueChange = { current = current.copy(name = it) },
                    label = "名称",
                    placeholder = "例如：灵感 / 常用网站 / 私密账号",
                )
                Text(text = "颜色", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    folderPalette.forEach { color ->
                        ColorOption(
                            color = color,
                            selected = current.color == color,
                            onClick = { current = current.copy(color = color) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(current) }) {
                Text(text = "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun TagEditorDialog(
    draft: TagEditorDraft,
    onDismiss: () -> Unit,
    onSave: (TagEditorDraft) -> Unit,
) {
    var current by remember(draft) { mutableStateOf(draft) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (draft.id == null) "新建标签" else "编辑标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                VaultTextField(
                    value = current.name,
                    onValueChange = { current = current.copy(name = it) },
                    label = "名称",
                    placeholder = "例如：设计 / 重要 / 学习",
                )
                Text(text = "颜色", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    folderPalette.forEach { color ->
                        ColorOption(
                            color = color,
                            selected = current.color == color,
                            onClick = { current = current.copy(color = color) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(current) }) {
                Text(text = "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun FolderDeleteDialog(
    folder: FolderSummary,
    candidates: List<FolderSummary>,
    migrateToFolderId: String?,
    onMigrateChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "删除收藏夹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "删除“${folder.name}”前，决定里面的内容去向。")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VaultChip(
                        label = "设为无归属",
                        selected = migrateToFolderId == null,
                        onClick = { onMigrateChange(null) },
                    )
                    candidates.forEach { candidate ->
                        VaultChip(
                            label = "迁移到 ${candidate.name}",
                            selected = migrateToFolderId == candidate.id,
                            onClick = { onMigrateChange(candidate.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun ColorOption(
    color: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(parseColor(color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
            )
        }
    }
}

@HiltViewModel
class FolderManageViewModel @Inject constructor(
    observeFoldersUseCase: ObserveFoldersUseCase,
    observeItemsUseCase: ObserveItemsUseCase,
    private val upsertFolderUseCase: UpsertFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
) : androidx.lifecycle.ViewModel() {
    val uiState: StateFlow<FolderManageUiState> = combine(
        observeFoldersUseCase(),
        observeItemsUseCase(),
    ) { folders, items ->
        FolderManageUiState(
            folders = folders.map { folder ->
                FolderSummary(
                    id = folder.id,
                    name = folder.name,
                    color = folder.color,
                    icon = folder.icon,
                    createdAt = folder.createdAt,
                    itemCount = items.count { it.folder?.id == folder.id },
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FolderManageUiState(),
    )

    fun saveFolder(draft: FolderEditorDraft) {
        viewModelScope.launch {
            val name = draft.name.trim()
            if (name.isBlank()) return@launch
            upsertFolderUseCase(
                Folder(
                    id = draft.id ?: UUID.randomUUID().toString(),
                    name = name,
                    icon = draft.icon,
                    color = draft.color,
                    createdAt = draft.createdAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteFolder(folderId: String, migrateToFolderId: String?) {
        viewModelScope.launch {
            deleteFolderUseCase(folderId, migrateToFolderId)
        }
    }
}

@HiltViewModel
class TagManageViewModel @Inject constructor(
    observeTagsUseCase: ObserveTagsUseCase,
    observeItemsUseCase: ObserveItemsUseCase,
    private val upsertTagUseCase: UpsertTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
) : androidx.lifecycle.ViewModel() {
    val uiState: StateFlow<TagManageUiState> = combine(
        observeTagsUseCase(),
        observeItemsUseCase(),
    ) { tags, items ->
        TagManageUiState(
            tags = tags.map { tag ->
                TagSummary(
                    id = tag.id,
                    name = tag.name,
                    color = tag.color,
                    createdAt = tag.createdAt,
                    itemCount = items.count { item -> item.tags.any { it.id == tag.id } },
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagManageUiState(),
    )

    fun saveTag(draft: TagEditorDraft) {
        viewModelScope.launch {
            val name = draft.name.trim()
            if (name.isBlank()) return@launch
            upsertTagUseCase(
                Tag(
                    id = draft.id ?: UUID.randomUUID().toString(),
                    name = name,
                    color = draft.color,
                    createdAt = draft.createdAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            deleteTagUseCase(tagId)
        }
    }
}

data class FolderManageUiState(
    val folders: List<FolderSummary> = emptyList(),
)

data class TagManageUiState(
    val tags: List<TagSummary> = emptyList(),
)

data class FolderSummary(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val createdAt: Long,
    val itemCount: Int,
)

data class TagSummary(
    val id: String,
    val name: String,
    val color: String,
    val createdAt: Long,
    val itemCount: Int,
)

data class FolderEditorDraft(
    val id: String? = null,
    val name: String = "",
    val color: String = folderPalette.first(),
    val icon: String = "folder",
    val createdAt: Long? = null,
)

data class TagEditorDraft(
    val id: String? = null,
    val name: String = "",
    val color: String = folderPalette.first(),
    val createdAt: Long? = null,
)

private val folderPalette = listOf(
    "#9CB4D8",
    "#A4B68D",
    "#D68C6E",
    "#8B9FE8",
    "#D2A7C5",
    "#77A89C",
)

private fun parseColor(value: String): Color = runCatching { Color(android.graphics.Color.parseColor(value)) }
    .getOrDefault(Color(0xFF9CB4D8))
