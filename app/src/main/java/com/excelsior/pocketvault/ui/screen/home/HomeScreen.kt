@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.excelsior.pocketvault.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.core.designsystem.component.VaultChip
import com.excelsior.pocketvault.core.designsystem.component.VaultEmptyState
import com.excelsior.pocketvault.core.designsystem.component.VaultFolderBadge
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultItemCard
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.core.designsystem.component.VaultPrimaryFab
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.ItemProtectionLevel
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.VaultItem
import com.excelsior.pocketvault.domain.usecase.ObserveAppearanceSettingsUseCase
import com.excelsior.pocketvault.domain.usecase.DeleteItemUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveHomeOrderUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveItemsUseCase
import com.excelsior.pocketvault.domain.usecase.SaveHomeOrderUseCase
import com.excelsior.pocketvault.domain.usecase.VerifyAppLockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onSearchClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onCreate: () -> Unit,
    onSecureSpace: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onSearchClick = onSearchClick,
        onItemClick = onItemClick,
        onCreate = onCreate,
        onSecureSpace = onSecureSpace,
        onToggleSelectionMode = viewModel::toggleSelectionMode,
        onToggleSelection = viewModel::toggleSelection,
        onEnterSelection = viewModel::enterSelectionMode,
        onDeleteSelected = viewModel::requestDeleteSelected,
        onMoveItem = viewModel::moveItem,
        onCommitMove = viewModel::commitMove,
        onDeletePinChanged = viewModel::onDeletePinChanged,
        onConfirmDeleteAuth = viewModel::confirmDeleteSelected,
        onDismissDeleteAuth = viewModel::dismissDeleteAuth,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onFilterSelected: (HomeFilter) -> Unit,
    onSearchClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onCreate: () -> Unit,
    onSecureSpace: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onEnterSelection: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onCommitMove: () -> Unit,
    onDeletePinChanged: (String) -> Unit,
    onConfirmDeleteAuth: () -> Unit,
    onDismissDeleteAuth: () -> Unit,
) {
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val itemStepPx = 108f
    val listState = rememberLazyListState()
    val useDoubleLayout = state.cardStyle == "double" && !state.selectionMode

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (useDoubleLayout) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(2) }) {
                    HomeHeader(
                        state = state,
                        onSearchClick = onSearchClick,
                        onSecureSpace = onSecureSpace,
                        onToggleSelectionMode = onToggleSelectionMode,
                        onFilterSelected = onFilterSelected,
                    )
                }
                if (state.items.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        VaultEmptyState(
                            title = "还没有内容",
                            description = "",
                            actionLabel = "新建",
                            onAction = onCreate,
                        )
                    }
                } else {
                    items(state.items, key = { it.id }) { item ->
                        VaultItemCard(item = item, onClick = { onItemClick(item.id) })
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HomeHeader(
                        state = state,
                        onSearchClick = onSearchClick,
                        onSecureSpace = onSecureSpace,
                        onToggleSelectionMode = onToggleSelectionMode,
                        onDeleteSelected = onDeleteSelected,
                        onFilterSelected = onFilterSelected,
                    )
                }
                if (state.items.isEmpty()) {
                    item {
                        VaultEmptyState(
                            title = "还没有内容",
                            description = "",
                            actionLabel = "新建",
                            onAction = onCreate,
                        )
                    }
                } else {
                    itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                        HomeNoteRow(
                            item = item,
                            selected = state.selectedIds.contains(item.id),
                            selectionMode = state.selectionMode,
                            canDrag = state.canDrag,
                            modifier = Modifier.graphicsLayer {
                                translationY = if (draggingItemId == item.id) dragOffsetPx else 0f
                            },
                            onClick = {
                                if (state.selectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    onItemClick(item.id)
                                }
                            },
                            onLongPress = {
                                if (!state.selectionMode) {
                                    onEnterSelection(item.id)
                                }
                            },
                            onSelectionClick = { onToggleSelection(item.id) },
                            onDragStart = {
                                if (state.canDrag) {
                                    draggingItemId = item.id
                                    dragOffsetPx = 0f
                                }
                            },
                            onDrag = { dragAmount ->
                                if (!state.canDrag) return@HomeNoteRow
                                dragOffsetPx += dragAmount
                                if (dragOffsetPx > itemStepPx && index < state.items.lastIndex) {
                                    onMoveItem(index, index + 1)
                                    dragOffsetPx -= itemStepPx
                                } else if (dragOffsetPx < -itemStepPx && index > 0) {
                                    onMoveItem(index, index - 1)
                                    dragOffsetPx += itemStepPx
                                }
                            },
                            onDragEnd = {
                                draggingItemId = null
                                dragOffsetPx = 0f
                                onCommitMove()
                            },
                        )
                    }
                }
            }
        }
        VaultPrimaryFab(
            label = "新建",
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        )
    }

    if (state.deleteAuthVisible) {
        VaultDeleteAuthDialog(
            pin = state.deleteAuthPin,
            error = state.deleteAuthError,
            onPinChanged = onDeletePinChanged,
            onConfirm = onConfirmDeleteAuth,
            onDismiss = onDismissDeleteAuth,
        )
    }
}

@Composable
private fun VaultDeleteAuthDialog(
    pin: String,
    error: String?,
    onPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            ),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(text = "删除前输入主密码", style = MaterialTheme.typography.titleLarge)
                VaultPasswordField(
                    value = pin,
                    onValueChange = onPinChanged,
                    label = "密码",
                    placeholder = "至少 4 位",
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "取消")
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = pin.length >= 4,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "确认")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onSearchClick: () -> Unit,
    onSecureSpace: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onFilterSelected: (HomeFilter) -> Unit,
    onDeleteSelected: () -> Unit = {},
) {
    var securePullPx by remember { mutableFloatStateOf(0f) }
    var securePullTriggered by remember { mutableStateOf(false) }
    val securePullThreshold = 72f

    Column(
        modifier = Modifier.pointerInput(state.selectionMode, onSecureSpace) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (state.selectionMode) return@detectVerticalDragGestures
                    if (dragAmount > 0f) {
                        securePullPx += dragAmount
                        if (securePullPx >= securePullThreshold && !securePullTriggered) {
                            securePullTriggered = true
                            securePullPx = 0f
                            change.consume()
                            onSecureSpace()
                        }
                    } else if (dragAmount < 0f) {
                        securePullPx = 0f
                        securePullTriggered = false
                    }
                },
                onDragEnd = {
                    securePullPx = 0f
                    securePullTriggered = false
                },
                onDragCancel = {
                    securePullPx = 0f
                    securePullTriggered = false
                },
            )
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VaultTopBar(
            title = if (state.selectionMode) "${state.selectedIds.size} 已选" else "便签",
            subtitle = null,
            actions = {
                if (state.selectionMode) {
                    IconButton(onClick = onDeleteSelected, enabled = state.selectedIds.isNotEmpty()) {
                        Icon(imageVector = VaultIcons.Delete, contentDescription = null)
                    }
                    IconButton(onClick = onToggleSelectionMode) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                } else {
                    IconButton(onClick = onSearchClick) {
                        Icon(imageVector = VaultIcons.Search, contentDescription = null)
                    }
                    IconButton(onClick = onToggleSelectionMode) {
                        Icon(imageVector = VaultIcons.Edit, contentDescription = null)
                    }
                }
            },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HomeFilter.entries.forEach { filter ->
                VaultChip(
                    label = filter.label,
                    selected = state.selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                )
            }
        }
    }
}

@Composable
private fun HomeNoteRow(
    item: VaultItem,
    selected: Boolean,
    selectionMode: Boolean,
    canDrag: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val rowInteractionSource = remember { MutableInteractionSource() }
    VaultGlassCard(
        modifier = modifier,
        containerColor = item.noteRowSurface(),
        onClick = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .combinedClickable(
                    interactionSource = rowInteractionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.noteRowBadge())
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (item.protectionLevel == ItemProtectionLevel.NONE) item.type.icon() else VaultIcons.Lock,
                    contentDescription = null,
                    tint = item.noteRowAccent(),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.displayTitle(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.displayBody()?.let { body ->
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.folder?.let { folder ->
                        VaultFolderBadge(text = folder.name)
                    }
                    if (item.isPinned) {
                        Text(
                            text = "置顶",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = VaultFormatters.formatDateTime(item.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            when {
                selectionMode -> {
                    SelectionIndicator(
                        selected = selected,
                        onClick = onSelectionClick,
                    )
                }

                canDrag -> {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(48.dp)
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd,
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.y)
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = VaultIcons.More,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(22.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onClick,
            ),
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            },
        ),
    ) {
        if (selected) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = VaultIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    observeHomeOrderUseCase: ObserveHomeOrderUseCase,
    observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase,
    private val saveHomeOrderUseCase: SaveHomeOrderUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val verifyAppLockUseCase: VerifyAppLockUseCase,
) : androidx.lifecycle.ViewModel() {
    private val selectedFilter = MutableStateFlow(HomeFilter.ALL)
    private val selectionMode = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val customOrderIds = MutableStateFlow<List<String>?>(null)
    private val deleteAuthVisible = MutableStateFlow(false)
    private val deleteAuthPin = MutableStateFlow("")
    private val deleteAuthError = MutableStateFlow<String?>(null)

    private val baseOrderedItems = combine(
        observeItemsUseCase(),
        observeHomeOrderUseCase(),
    ) { items, homeOrder ->
        applyHomeOrder(items, homeOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val orderedItems = combine(
        baseOrderedItems,
        customOrderIds,
    ) { items, customOrder ->
        if (customOrder.isNullOrEmpty()) {
            items
        } else {
            applyHomeOrder(items, customOrder)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val homeSelectionState = combine(
        selectedFilter,
        selectionMode,
        selectedIds,
        observeAppearanceSettingsUseCase(),
    ) { filter, inSelectionMode, currentSelectedIds, appearance ->
        HomeLocalState(
            filter = filter,
            selectionMode = inSelectionMode,
            selectedIds = currentSelectedIds,
            cardStyle = appearance.cardStyle,
        )
    }

    private val homeDeleteAuthState = combine(
        deleteAuthVisible,
        deleteAuthPin,
        deleteAuthError,
    ) { authVisible, authPin, authError ->
        Triple(authVisible, authPin, authError)
    }

    private val homeLocalState = combine(
        homeSelectionState,
        homeDeleteAuthState,
    ) { selectionState, deleteAuth ->
        selectionState.copy(
            deleteAuthVisible = deleteAuth.first,
            deleteAuthPin = deleteAuth.second,
            deleteAuthError = deleteAuth.third,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        orderedItems,
        homeLocalState,
    ) { items, local ->
        val filteredItems = applyFilter(items, local.filter)
        HomeUiState(
            selectedFilter = local.filter,
            selectionMode = local.selectionMode,
            selectedIds = local.selectedIds.intersect(filteredItems.map(VaultItem::id).toSet()),
            canDrag = !local.selectionMode && local.filter == HomeFilter.ALL,
            cardStyle = local.cardStyle,
            deleteAuthVisible = local.deleteAuthVisible,
            deleteAuthPin = local.deleteAuthPin,
            deleteAuthError = local.deleteAuthError,
            items = filteredItems,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onFilterSelected(filter: HomeFilter) {
        selectedFilter.update { filter }
        selectedIds.update { emptySet() }
        selectionMode.update { false }
    }

    fun toggleSelectionMode() {
        val next = !selectionMode.value
        selectionMode.update { next }
        if (!next) {
            selectedIds.update { emptySet() }
        }
    }

    fun enterSelectionMode(itemId: String) {
        selectionMode.update { true }
        selectedIds.update { setOf(itemId) }
    }

    fun toggleSelection(itemId: String) {
        selectedIds.update { current ->
            if (current.contains(itemId)) current - itemId else current + itemId
        }
    }

    fun requestDeleteSelected() {
        val selected = uiState.value.items.filter { selectedIds.value.contains(it.id) }
        if (selected.isEmpty()) return
        if (selected.any { it.protectionLevel != ItemProtectionLevel.NONE }) {
            deleteAuthVisible.update { true }
            deleteAuthPin.update { "" }
            deleteAuthError.update { null }
            return
        }
        deleteSelectedNow()
    }

    private fun deleteSelectedNow() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { deleteItemUseCase(it) }
            val newOrder = orderedItems.value.map(VaultItem::id).filterNot(ids::contains)
            saveHomeOrderUseCase(newOrder)
            selectedIds.update { emptySet() }
            selectionMode.update { false }
        }
    }

    fun onDeletePinChanged(value: String) {
        deleteAuthPin.update { value.take(32) }
        deleteAuthError.update { null }
    }

    fun dismissDeleteAuth() {
        deleteAuthVisible.update { false }
        deleteAuthPin.update { "" }
        deleteAuthError.update { null }
    }

    fun confirmDeleteSelected() {
        viewModelScope.launch {
            val verified = verifyAppLockUseCase(deleteAuthPin.value)
            if (!verified) {
                deleteAuthError.update { "主密码不正确。" }
                return@launch
            }
            dismissDeleteAuth()
            deleteSelectedNow()
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = orderedItems.value.map(VaultItem::id).toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        customOrderIds.update { current }
    }

    fun commitMove() {
        val order = customOrderIds.value ?: return
        viewModelScope.launch {
            saveHomeOrderUseCase(order)
            customOrderIds.update { null }
        }
    }

    private fun applyFilter(items: List<VaultItem>, filter: HomeFilter): List<VaultItem> = when (filter) {
        HomeFilter.ALL -> items
        HomeFilter.LINK -> items.filter { it.type == ItemType.LINK }
        HomeFilter.TEXT -> items.filter { it.type == ItemType.TEXT }
        HomeFilter.IMAGE -> items.filter { it.type == ItemType.IMAGE }
        HomeFilter.CREDENTIAL -> items.filter { it.type == ItemType.CREDENTIAL }
        HomeFilter.PINNED -> items.filter { it.isPinned }
        HomeFilter.RECENT -> items.sortedByDescending { it.updatedAt }
    }

    private fun applyHomeOrder(items: List<VaultItem>, homeOrder: List<String>): List<VaultItem> {
        if (items.isEmpty()) return emptyList()
        val indexMap = homeOrder.withIndex().associate { it.value to it.index }
        val known = items.filter { indexMap.containsKey(it.id) }.sortedBy { indexMap.getValue(it.id) }
        val fresh = items.filterNot { indexMap.containsKey(it.id) }.sortedByDescending { it.updatedAt }
        return fresh + known
    }
}

enum class HomeFilter(val label: String) {
    ALL("全部"),
    TEXT("便签"),
    LINK("链接"),
    IMAGE("图片"),
    CREDENTIAL("账号"),
    PINNED("置顶"),
    RECENT("最近"),
}

data class HomeUiState(
    val selectedFilter: HomeFilter = HomeFilter.ALL,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val canDrag: Boolean = false,
    val cardStyle: String = "single",
    val deleteAuthVisible: Boolean = false,
    val deleteAuthPin: String = "",
    val deleteAuthError: String? = null,
    val items: List<VaultItem> = emptyList(),
)

private data class HomeLocalState(
    val filter: HomeFilter = HomeFilter.ALL,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val cardStyle: String = "single",
    val deleteAuthVisible: Boolean = false,
    val deleteAuthPin: String = "",
    val deleteAuthError: String? = null,
)

private fun VaultItem.displayTitle(): String = when (this) {
    is com.excelsior.pocketvault.domain.model.LinkVaultItem -> if (protectionLevel != ItemProtectionLevel.NONE && !titleVisibleWhenProtected) "加密链接" else title.ifBlank { siteName }
    is com.excelsior.pocketvault.domain.model.TextVaultItem -> if (protectionLevel != ItemProtectionLevel.NONE && !titleVisibleWhenProtected) "加密便签" else title?.takeIf { it.isNotBlank() } ?: content.take(24)
    is com.excelsior.pocketvault.domain.model.ImageVaultItem -> if (protectionLevel != ItemProtectionLevel.NONE && !titleVisibleWhenProtected) "加密图片" else title ?: "图片"
    is com.excelsior.pocketvault.domain.model.CredentialVaultItem -> if (protectionLevel != ItemProtectionLevel.NONE && !titleVisibleWhenProtected) "加密账号" else title.ifBlank { username }
}

private fun VaultItem.displayBody(): String? = if (protectionLevel != ItemProtectionLevel.NONE) {
    "已加密"
} else {
    when (this) {
        is com.excelsior.pocketvault.domain.model.LinkVaultItem -> note ?: VaultFormatters.compactUrl(url)
        is com.excelsior.pocketvault.domain.model.TextVaultItem -> content
        is com.excelsior.pocketvault.domain.model.ImageVaultItem -> note ?: "图片收藏"
        is com.excelsior.pocketvault.domain.model.CredentialVaultItem -> websiteUrl ?: username
    }
}

private fun ItemType.icon() = when (this) {
    ItemType.LINK -> VaultIcons.Link
    ItemType.TEXT -> VaultIcons.Text
    ItemType.IMAGE -> VaultIcons.Image
    ItemType.CREDENTIAL -> VaultIcons.Credential
}

private fun ItemType.badgeColor(): Color = when (this) {
    ItemType.LINK -> Color(0xFFF3E7CF)
    ItemType.TEXT -> Color(0xFFF0E4C8)
    ItemType.IMAGE -> Color(0xFFE6E4D8)
    ItemType.CREDENTIAL -> Color(0xFFE6DDD4)
}

private fun ItemType.iconTint(): Color = when (this) {
    ItemType.LINK -> Color(0xFF8C6222)
    ItemType.TEXT -> Color(0xFF8A6B1F)
    ItemType.IMAGE -> Color(0xFF5D6C63)
    ItemType.CREDENTIAL -> Color(0xFF7A4C3D)
}

@Composable
private fun VaultItem.noteRowSurface(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (colorTheme) {
        "sage" -> if (darkTheme) Color(0xFF1A211D) else Color(0xFFFBFCFA)
        "sunset" -> if (darkTheme) Color(0xFF241D19) else Color(0xFFFFFAF6)
        "night" -> if (darkTheme) Color(0xFF181B1F) else Color(0xFFF7F7F8)
        else -> if (darkTheme) Color(0xFF171A1D) else Color(0xFFFFFFFF)
    }
}

@Composable
private fun VaultItem.noteRowBadge(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (colorTheme) {
        "sage" -> if (darkTheme) Color(0xFF29332D) else Color(0xFFF0F5EE)
        "sunset" -> if (darkTheme) Color(0xFF352B24) else Color(0xFFFAEEE3)
        "night" -> if (darkTheme) Color(0xFF262C33) else Color(0xFFEFF2F6)
        else -> if (darkTheme) Color(0xFF2A2F36) else Color(0xFFF3F5F8)
    }
}

@Composable
private fun VaultItem.noteRowAccent(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (colorTheme) {
        "sage" -> if (darkTheme) Color(0xFFB8D0BA) else Color(0xFF5D7962)
        "sunset" -> if (darkTheme) Color(0xFFF2B17A) else Color(0xFFB96F3C)
        "night" -> if (darkTheme) Color(0xFFD9E1EC) else Color(0xFF526173)
        else -> if (darkTheme) Color(0xFFD6DFEB) else Color(0xFF4E6178)
    }
}
