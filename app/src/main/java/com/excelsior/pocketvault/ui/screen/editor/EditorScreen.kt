@file:OptIn(ExperimentalLayoutApi::class)

package com.excelsior.pocketvault.ui.screen.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.core.common.decodeStoredSecret
import com.excelsior.pocketvault.core.designsystem.component.VaultChip
import com.excelsior.pocketvault.core.designsystem.component.VaultGlassCard
import com.excelsior.pocketvault.core.designsystem.component.VaultPasswordField
import com.excelsior.pocketvault.core.designsystem.component.VaultSectionTitle
import com.excelsior.pocketvault.core.designsystem.component.VaultTextField
import com.excelsior.pocketvault.core.designsystem.component.VaultTopBar
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.CredentialDraft
import com.excelsior.pocketvault.domain.model.CredentialVaultItem
import com.excelsior.pocketvault.domain.model.Folder
import com.excelsior.pocketvault.domain.model.ImageDraft
import com.excelsior.pocketvault.domain.model.ImageVaultItem
import com.excelsior.pocketvault.domain.model.ItemProtectionLevel
import com.excelsior.pocketvault.domain.model.ItemType
import com.excelsior.pocketvault.domain.model.LinkDraft
import com.excelsior.pocketvault.domain.model.LinkVaultItem
import com.excelsior.pocketvault.domain.model.SecuritySettings
import com.excelsior.pocketvault.domain.model.Tag
import com.excelsior.pocketvault.domain.model.TextDraft
import com.excelsior.pocketvault.domain.model.TextVaultItem
import com.excelsior.pocketvault.domain.usecase.CreateCredentialItemUseCase
import com.excelsior.pocketvault.domain.usecase.CreateImageItemUseCase
import com.excelsior.pocketvault.domain.usecase.CreateLinkItemUseCase
import com.excelsior.pocketvault.domain.usecase.CreateTextItemUseCase
import com.excelsior.pocketvault.domain.usecase.DecryptCredentialUseCase
import com.excelsior.pocketvault.domain.usecase.GetItemDetailUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveFoldersUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveSecuritySettingsUseCase
import com.excelsior.pocketvault.domain.usecase.ObserveTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun EditorRoute(
    type: ItemType,
    itemId: String?,
    onBack: () -> Unit,
    onManageFolders: () -> Unit,
    onSaved: (String, ItemProtectionLevel) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onImagePicked(uri)
    }

    LaunchedEffect(type, itemId) {
        viewModel.load(type, itemId)
    }

    LaunchedEffect(state.savedItemId, state.savedProtectionLevel) {
        val savedItemId = state.savedItemId ?: return@LaunchedEffect
        onSaved(savedItemId, state.savedProtectionLevel)
    }

    EditorScreen(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onManageFolders = onManageFolders,
        onTypeSelected = viewModel::onTypeSelected,
        onTitleChanged = viewModel::onTitleChanged,
        onUrlChanged = viewModel::onUrlChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onContentChanged = viewModel::onContentChanged,
        onSourceChanged = viewModel::onSourceChanged,
        onQuoteAuthorChanged = viewModel::onQuoteAuthorChanged,
        onWebsiteUrlChanged = viewModel::onWebsiteUrlChanged,
        onUsernameChanged = viewModel::onUsernameChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onFolderSelected = viewModel::onFolderSelected,
        onToggleTag = viewModel::onToggleTag,
        onTogglePinned = viewModel::onTogglePinned,
        onToggleFavorite = viewModel::onToggleFavorite,
        onThemeSelected = viewModel::onThemeSelected,
        onProtectionLevelSelected = viewModel::onProtectionLevelSelected,
        onTitleVisibilityChanged = viewModel::onTitleVisibilityChanged,
        onPickImage = {
            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onManageFolders: () -> Unit,
    onTypeSelected: (ItemType) -> Unit,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSourceChanged: (String) -> Unit,
    onQuoteAuthorChanged: (String) -> Unit,
    onWebsiteUrlChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onToggleTag: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onProtectionLevelSelected: (ItemProtectionLevel) -> Unit,
    onTitleVisibilityChanged: (Boolean) -> Unit,
    onPickImage: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            VaultTopBar(
                title = state.screenTitle,
                subtitle = null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = VaultIcons.Back, contentDescription = null)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onSave()
                        },
                        enabled = !state.isSaving && !state.isLoading,
                    ) {
                        Text(text = if (state.isSaving) "保存中" else "保存")
                    }
                },
            )
        }
        item {
            state.error?.let { error ->
                VaultGlassCard {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (state.isLoading) {
            item {
                VaultGlassCard {
                    CircularProgressIndicator()
                    Text(
                        text = "正在读取内容...",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            if (state.itemId == null) {
                item {
                    EntryTypeSection(
                        selectedType = state.type,
                        onTypeSelected = onTypeSelected,
                    )
                }
            }
            item {
                when (state.type) {
                    ItemType.LINK -> LinkEditorSection(
                        state = state,
                        onTitleChanged = onTitleChanged,
                        onUrlChanged = onUrlChanged,
                        onNoteChanged = onNoteChanged,
                    )

                    ItemType.TEXT -> TextEditorSection(
                        state = state,
                        onTitleChanged = onTitleChanged,
                        onContentChanged = onContentChanged,
                        onSourceChanged = onSourceChanged,
                        onQuoteAuthorChanged = onQuoteAuthorChanged,
                    )

                    ItemType.IMAGE -> ImageEditorSection(
                        state = state,
                        onTitleChanged = onTitleChanged,
                        onNoteChanged = onNoteChanged,
                        onPickImage = onPickImage,
                    )

                    ItemType.CREDENTIAL -> CredentialEditorSection(
                        state = state,
                        onTitleChanged = onTitleChanged,
                        onWebsiteUrlChanged = onWebsiteUrlChanged,
                        onUsernameChanged = onUsernameChanged,
                        onEmailChanged = onEmailChanged,
                        onPasswordChanged = onPasswordChanged,
                        onNoteChanged = onNoteChanged,
                    )
                }
            }
            item {
                CompactMetaSection(
                    folders = state.folders,
                    selectedFolderId = state.folderId,
                    tags = state.tags,
                    selectedTagIds = state.tagIds,
                    selectedTheme = state.colorTheme,
                    isPinned = state.isPinned,
                    isFavorite = state.isFavorite,
                    onManageFolders = onManageFolders,
                    onFolderSelected = onFolderSelected,
                    onToggleTag = onToggleTag,
                    onThemeSelected = onThemeSelected,
                    onTogglePinned = onTogglePinned,
                    onToggleFavorite = onToggleFavorite,
                    protectionLevel = state.protectionLevel,
                    onProtectionLevelSelected = onProtectionLevelSelected,
                    titleVisibleWhenProtected = state.titleVisibleWhenProtected,
                    onTitleVisibilityChanged = onTitleVisibilityChanged,
                )
            }
        }
    }
}

@Composable
private fun EntryTypeSection(
    selectedType: ItemType,
    onTypeSelected: (ItemType) -> Unit,
) {
    VaultGlassCard {
        VaultSectionTitle(
            title = "类型",
            caption = null,
        )
        FlowRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ItemType.entries.forEach { type ->
                VaultChip(
                    label = type.editorLabel(),
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                )
            }
        }
    }
}

@Composable
private fun LinkEditorSection(
    state: EditorUiState,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
) {
    VaultGlassCard(containerColor = editorPanelColor(state.colorTheme)) {
        VaultSectionTitle(title = "链接", caption = null)
        VaultTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = "标题",
            placeholder = "标题",
            modifier = Modifier.padding(top = 16.dp),
        )
        VaultTextField(
            value = state.url,
            onValueChange = onUrlChanged,
            label = "URL",
            placeholder = "https://example.com",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultTextField(
            value = state.note,
            onValueChange = onNoteChanged,
            label = "备注",
            placeholder = "备注",
            modifier = Modifier.padding(top = 12.dp),
            minLines = 3,
        )
    }
}

@Composable
private fun TextEditorSection(
    state: EditorUiState,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSourceChanged: (String) -> Unit,
    onQuoteAuthorChanged: (String) -> Unit,
) {
    VaultGlassCard(containerColor = editorPanelColor(state.colorTheme)) {
        VaultSectionTitle(title = "便签", caption = null)
        VaultTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = "标题",
            placeholder = "标题",
            modifier = Modifier.padding(top = 16.dp),
        )
        VaultTextField(
            value = state.content,
            onValueChange = onContentChanged,
            label = "正文",
            placeholder = "写点什么",
            modifier = Modifier.padding(top = 12.dp),
            minLines = 6,
        )
        VaultTextField(
            value = state.source,
            onValueChange = onSourceChanged,
            label = "来源",
            placeholder = "来源",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultTextField(
            value = state.quoteAuthor,
            onValueChange = onQuoteAuthorChanged,
            label = "作者",
            placeholder = "作者",
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ImageEditorSection(
    state: EditorUiState,
    onTitleChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onPickImage: () -> Unit,
) {
    VaultGlassCard(containerColor = editorPanelColor(state.colorTheme)) {
        VaultSectionTitle(title = "图片", caption = null)
        Button(
            onClick = onPickImage,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = if (state.displayImageModel == null) "选择图片" else "重新选择")
        }
        state.displayImageModel?.let { imageModel ->
            AsyncImage(
                model = imageModel,
                contentDescription = state.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(top = 16.dp),
            )
        }
        VaultTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = "标题",
            placeholder = "标题",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultTextField(
            value = state.note,
            onValueChange = onNoteChanged,
            label = "备注",
            placeholder = "备注",
            modifier = Modifier.padding(top = 12.dp),
            minLines = 3,
        )
    }
}

@Composable
private fun CredentialEditorSection(
    state: EditorUiState,
    onTitleChanged: (String) -> Unit,
    onWebsiteUrlChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
) {
    VaultGlassCard(containerColor = editorPanelColor(state.colorTheme)) {
        VaultSectionTitle(title = "账号", caption = null)
        VaultTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = "网站 / 应用名称",
            placeholder = "网站 / 应用名称",
            modifier = Modifier.padding(top = 16.dp),
        )
        VaultTextField(
            value = state.websiteUrl,
            onValueChange = onWebsiteUrlChanged,
            label = "网址",
            placeholder = "网址",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultTextField(
            value = state.username,
            onValueChange = onUsernameChanged,
            label = "用户名",
            placeholder = "登录账号",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = "邮箱",
            placeholder = "邮箱",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultPasswordField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = "密码",
            placeholder = "密码",
            modifier = Modifier.padding(top = 12.dp),
        )
        VaultTextField(
            value = state.note,
            onValueChange = onNoteChanged,
            label = "备注",
            placeholder = "备注",
            modifier = Modifier.padding(top = 12.dp),
            minLines = 3,
        )
    }
}

@Composable
private fun CompactMetaSection(
    folders: List<Folder>,
    selectedFolderId: String?,
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    selectedTheme: String,
    isPinned: Boolean,
    isFavorite: Boolean,
    protectionLevel: ItemProtectionLevel,
    onManageFolders: () -> Unit,
    onFolderSelected: (String?) -> Unit,
    onToggleTag: (String) -> Unit,
    onThemeSelected: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit,
    onProtectionLevelSelected: (ItemProtectionLevel) -> Unit,
    titleVisibleWhenProtected: Boolean,
    onTitleVisibilityChanged: (Boolean) -> Unit,
) {
    VaultGlassCard(containerColor = editorPanelColor(selectedTheme)) {
        VaultSectionTitle(
            title = "更多",
            caption = null,
            action = {
                IconButton(onClick = onManageFolders) {
                    Icon(imageVector = VaultIcons.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
        Text(
            text = "收藏夹",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultChip(
                label = "未归类",
                selected = selectedFolderId == null,
                onClick = { onFolderSelected(null) },
            )
            folders.forEach { folder ->
                VaultChip(
                    label = folder.name,
                    selected = selectedFolderId == folder.id,
                    onClick = { onFolderSelected(folder.id.takeUnless { it == selectedFolderId }) },
                )
            }
        }
        Text(
            text = "标签",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tags.isEmpty()) {
                VaultChip(label = "无标签", selected = false, onClick = {})
            } else {
                tags.forEach { tag ->
                    VaultChip(
                        label = "#${tag.name}",
                        selected = selectedTagIds.contains(tag.id),
                        onClick = { onToggleTag(tag.id) },
                    )
                }
            }
        }
        Text(
            text = "主题",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            editorThemes.forEach { option ->
                VaultChip(
                    label = option.second,
                    selected = selectedTheme == option.first,
                    onClick = { onThemeSelected(option.first) },
                )
            }
        }
        Text(
            text = "保护",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ItemProtectionLevel.entries.forEach { level ->
                VaultChip(
                    label = level.editorLabel(),
                    selected = protectionLevel == level,
                    onClick = { onProtectionLevelSelected(level) },
                )
            }
        }
        if (protectionLevel != ItemProtectionLevel.NONE) {
            Text(
                text = "标题",
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VaultChip(
                    label = "标题公开",
                    selected = titleVisibleWhenProtected,
                    onClick = { onTitleVisibilityChanged(true) },
                )
                VaultChip(
                    label = "标题加密",
                    selected = !titleVisibleWhenProtected,
                    onClick = { onTitleVisibilityChanged(false) },
                )
            }
        }
        Text(
            text = "状态",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultChip(
                label = if (isPinned) "已置顶" else "设为置顶",
                selected = isPinned,
                onClick = onTogglePinned,
            )
            VaultChip(
                label = if (isFavorite) "已收藏" else "加入收藏",
                selected = isFavorite,
                onClick = onToggleFavorite,
            )
        }
    }
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    observeFoldersUseCase: ObserveFoldersUseCase,
    observeTagsUseCase: ObserveTagsUseCase,
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    private val getItemDetailUseCase: GetItemDetailUseCase,
    private val createLinkItemUseCase: CreateLinkItemUseCase,
    private val createTextItemUseCase: CreateTextItemUseCase,
    private val createImageItemUseCase: CreateImageItemUseCase,
    private val createCredentialItemUseCase: CreateCredentialItemUseCase,
    private val decryptCredentialUseCase: DecryptCredentialUseCase,
) : androidx.lifecycle.ViewModel() {
    private val form = MutableStateFlow(EditorFormState())
    private var lastLoadedKey: String? = null
    private var autoSaveJob: Job? = null

    val uiState: StateFlow<EditorUiState> = combine(
        form,
        observeFoldersUseCase(),
        observeTagsUseCase(),
        observeSecuritySettingsUseCase(),
    ) { currentForm, folders, tags, securitySettings ->
        currentForm.toUiState(
            folders = folders,
            tags = tags,
            securitySettings = securitySettings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditorUiState(),
    )

    fun load(type: ItemType, itemId: String?) {
        val key = "${type.name}:${itemId.orEmpty()}"
        if (lastLoadedKey == key) return
        lastLoadedKey = key
        form.update {
            EditorFormState(
                type = type,
                itemId = itemId,
                colorTheme = defaultThemeFor(type),
                coverStyle = defaultCoverStyleFor(type),
                isLoading = itemId != null,
            )
        }
        if (itemId == null) return
        viewModelScope.launch {
            val item = getItemDetailUseCase(itemId)
            if (item == null) {
                form.update { it.copy(isLoading = false, error = "未能读取要编辑的内容。") }
                return@launch
            }
            when (item) {
                is LinkVaultItem -> form.update {
                    it.copy(
                        type = ItemType.LINK,
                        isLoading = false,
                        title = item.title,
                        url = item.url,
                        note = item.note.orEmpty(),
                        previewTitle = item.previewTitle.orEmpty(),
                        previewDescription = item.previewDescription.orEmpty(),
                        protectionLevel = item.protectionLevel,
                        titleVisibleWhenProtected = item.titleVisibleWhenProtected,
                        folderId = item.folder?.id,
                        tagIds = item.tags.map(Tag::id).toSet(),
                        isPinned = item.isPinned,
                        isFavorite = item.isFavorite,
                        colorTheme = item.colorTheme ?: defaultThemeFor(ItemType.LINK),
                        coverStyle = item.coverStyle ?: defaultCoverStyleFor(ItemType.LINK),
                    )
                }

                is TextVaultItem -> form.update {
                    it.copy(
                        type = ItemType.TEXT,
                        isLoading = false,
                        title = item.title.orEmpty(),
                        content = item.content,
                        source = item.source.orEmpty(),
                        quoteAuthor = item.quoteAuthor.orEmpty(),
                        protectionLevel = item.protectionLevel,
                        titleVisibleWhenProtected = item.titleVisibleWhenProtected,
                        folderId = item.folder?.id,
                        tagIds = item.tags.map(Tag::id).toSet(),
                        isPinned = item.isPinned,
                        isFavorite = item.isFavorite,
                        colorTheme = item.colorTheme ?: defaultThemeFor(ItemType.TEXT),
                        coverStyle = item.coverStyle ?: defaultCoverStyleFor(ItemType.TEXT),
                    )
                }

                is ImageVaultItem -> form.update {
                    it.copy(
                        type = ItemType.IMAGE,
                        isLoading = false,
                        title = item.title.orEmpty(),
                        note = item.note.orEmpty(),
                        localImagePath = item.localImagePath,
                        thumbnailPath = item.thumbnailPath,
                        aspectRatio = item.aspectRatio,
                        protectionLevel = item.protectionLevel,
                        titleVisibleWhenProtected = item.titleVisibleWhenProtected,
                        folderId = item.folder?.id,
                        tagIds = item.tags.map(Tag::id).toSet(),
                        isPinned = item.isPinned,
                        isFavorite = item.isFavorite,
                        colorTheme = item.colorTheme ?: defaultThemeFor(ItemType.IMAGE),
                        coverStyle = item.coverStyle ?: defaultCoverStyleFor(ItemType.IMAGE),
                    )
                }

                is CredentialVaultItem -> {
                    val password = decodeStoredSecret(
                        storedValue = item.encryptedPassword,
                        decryptor = decryptCredentialUseCase::invoke,
                    ).getOrNull().orEmpty()
                    val note = item.encryptedNote?.let { storedValue ->
                        decodeStoredSecret(
                            storedValue = storedValue,
                            decryptor = decryptCredentialUseCase::invoke,
                        ).getOrNull().orEmpty()
                    }.orEmpty()
                    form.update {
                        it.copy(
                            type = ItemType.CREDENTIAL,
                            isLoading = false,
                            title = item.siteName,
                            websiteUrl = item.websiteUrl.orEmpty(),
                            username = item.username,
                            email = item.email.orEmpty(),
                            password = password,
                            note = note,
                            protectionLevel = item.protectionLevel,
                            titleVisibleWhenProtected = item.titleVisibleWhenProtected,
                            folderId = item.folder?.id,
                            tagIds = item.tags.map(Tag::id).toSet(),
                            isPinned = item.isPinned,
                            isFavorite = item.isFavorite,
                            colorTheme = item.colorTheme ?: defaultThemeFor(ItemType.CREDENTIAL),
                            coverStyle = item.coverStyle ?: defaultCoverStyleFor(ItemType.CREDENTIAL),
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(value: String) = updateForm { it.copy(title = value, error = null) }
    fun onUrlChanged(value: String) = updateForm { it.copy(url = value, error = null) }
    fun onNoteChanged(value: String) = updateForm { it.copy(note = value, error = null) }
    fun onPreviewTitleChanged(value: String) = updateForm { it.copy(previewTitle = value, error = null) }
    fun onPreviewDescriptionChanged(value: String) = updateForm { it.copy(previewDescription = value, error = null) }
    fun onContentChanged(value: String) = updateForm { it.copy(content = value, error = null) }
    fun onSourceChanged(value: String) = updateForm { it.copy(source = value, error = null) }
    fun onQuoteAuthorChanged(value: String) = updateForm { it.copy(quoteAuthor = value, error = null) }
    fun onWebsiteUrlChanged(value: String) = updateForm { it.copy(websiteUrl = value, error = null) }
    fun onUsernameChanged(value: String) = updateForm { it.copy(username = value, error = null) }
    fun onEmailChanged(value: String) = updateForm { it.copy(email = value, error = null) }
    fun onPasswordChanged(value: String) = updateForm { it.copy(password = value, error = null) }
    fun onTypeSelected(value: ItemType) = form.update {
        if (it.itemId != null) {
            it
        } else {
            it.copy(
                type = value,
                colorTheme = defaultThemeFor(value),
                coverStyle = defaultCoverStyleFor(value),
                error = null,
            )
        }
    }
    fun onFolderSelected(value: String?) = updateForm { it.copy(folderId = value) }
    fun onTogglePinned() = updateForm { it.copy(isPinned = !it.isPinned) }
    fun onToggleFavorite() = updateForm { it.copy(isFavorite = !it.isFavorite) }
    fun onThemeSelected(value: String) = updateForm { it.copy(colorTheme = value) }
    fun onProtectionLevelSelected(value: ItemProtectionLevel) = updateForm {
        it.copy(
            protectionLevel = value,
            titleVisibleWhenProtected = if (value == ItemProtectionLevel.NONE) true else it.titleVisibleWhenProtected,
            error = null,
        )
    }
    fun onTitleVisibilityChanged(value: Boolean) = updateForm { it.copy(titleVisibleWhenProtected = value, error = null) }

    fun onToggleTag(tagId: String) {
        updateForm {
            it.copy(
                tagIds = if (it.tagIds.contains(tagId)) {
                    it.tagIds - tagId
                } else {
                    it.tagIds + tagId
                },
            )
        }
    }

    fun onImagePicked(uri: Uri?) {
        updateForm { it.copy(pickedUri = uri, error = null) }
    }

    fun save() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persist(shouldNavigate = true, showSavingState = true)
        }
    }

    private fun updateForm(transform: (EditorFormState) -> EditorFormState) {
        form.update(transform)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        val current = form.value
        if (current.itemId == null || current.isLoading || current.isSaving) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600)
            persist(shouldNavigate = false, showSavingState = false)
        }
    }

    private suspend fun persist(
        shouldNavigate: Boolean,
        showSavingState: Boolean,
    ) {
        val current = form.value
        val error = current.validate(uiState.value.securitySettings)
        if (error != null) {
            if (shouldNavigate) {
                form.update { it.copy(error = error) }
            }
            return
        }
        if (showSavingState) {
            form.update { it.copy(isSaving = true, error = null) }
        }
        val savedItemId = when (current.type) {
                ItemType.LINK -> createLinkItemUseCase(
                    LinkDraft(
                        id = current.itemId,
                        title = current.title.trim().ifBlank {
                            VaultFormatters.hostFromUrl(current.url.trim()).ifBlank { "未命名链接" }
                        },
                        url = current.url.trim(),
                        note = current.note,
                        previewTitle = "",
                        previewDescription = "",
                        protectionLevel = current.protectionLevel,
                        titleVisibleWhenProtected = current.titleVisibleWhenProtected,
                        folderId = current.folderId,
                        tagIds = current.tagIds.toList(),
                        isPinned = current.isPinned,
                        isFavorite = current.isFavorite,
                        colorTheme = current.colorTheme,
                        coverStyle = current.coverStyle,
                    ),
                )

                ItemType.TEXT -> createTextItemUseCase(
                    TextDraft(
                        id = current.itemId,
                        title = current.title.ifBlank { null },
                        content = current.content,
                        source = current.source,
                        quoteAuthor = current.quoteAuthor,
                        protectionLevel = current.protectionLevel,
                        titleVisibleWhenProtected = current.titleVisibleWhenProtected,
                        folderId = current.folderId,
                        tagIds = current.tagIds.toList(),
                        isPinned = current.isPinned,
                        isFavorite = current.isFavorite,
                        colorTheme = current.colorTheme,
                        coverStyle = current.coverStyle,
                    ),
                )

                ItemType.IMAGE -> createImageItemUseCase(
                    ImageDraft(
                        id = current.itemId,
                        title = current.title.ifBlank { null },
                        note = current.note,
                        localImagePath = current.localImagePath,
                        thumbnailPath = current.thumbnailPath,
                        aspectRatio = current.aspectRatio,
                        pickedUri = current.pickedUri,
                        protectionLevel = current.protectionLevel,
                        titleVisibleWhenProtected = current.titleVisibleWhenProtected,
                        folderId = current.folderId,
                        tagIds = current.tagIds.toList(),
                        isPinned = current.isPinned,
                        isFavorite = current.isFavorite,
                        colorTheme = current.colorTheme,
                        coverStyle = current.coverStyle,
                    ),
                )

                ItemType.CREDENTIAL -> createCredentialItemUseCase(
                    CredentialDraft(
                        id = current.itemId,
                        title = current.title.trim().ifBlank {
                            current.websiteUrl.ifBlank { current.username.trim() }.ifBlank { "未命名账号" }
                        },
                        websiteUrl = current.websiteUrl,
                        username = current.username.trim(),
                        email = current.email,
                        password = current.password,
                        note = current.note,
                        secretsEncrypted = true,
                        protectionLevel = current.protectionLevel,
                        titleVisibleWhenProtected = current.titleVisibleWhenProtected,
                        folderId = current.folderId,
                        tagIds = current.tagIds.toList(),
                        isPinned = current.isPinned,
                        isFavorite = current.isFavorite,
                        colorTheme = current.colorTheme,
                        coverStyle = current.coverStyle,
                    ),
                )
        }
        form.update {
            it.copy(
                isSaving = false,
                error = null,
                savedItemId = if (shouldNavigate) savedItemId else it.savedItemId,
                savedProtectionLevel = if (shouldNavigate) current.protectionLevel else it.savedProtectionLevel,
            )
        }
    }
}

data class EditorUiState(
    val type: ItemType = ItemType.LINK,
    val itemId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val url: String = "",
    val note: String = "",
    val previewTitle: String = "",
    val previewDescription: String = "",
    val content: String = "",
    val source: String = "",
    val quoteAuthor: String = "",
    val localImagePath: String = "",
    val thumbnailPath: String = "",
    val aspectRatio: Float = 1f,
    val pickedUri: Uri? = null,
    val websiteUrl: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val protectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
    val titleVisibleWhenProtected: Boolean = true,
    val folderId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val colorTheme: String = "mist",
    val coverStyle: String = "glass",
    val securitySettings: SecuritySettings = SecuritySettings(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val error: String? = null,
    val savedItemId: String? = null,
    val savedProtectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
) {
    val screenTitle: String
        get() = if (itemId == null) "快速新建" else "编辑${type.screenTitleSuffix()}"

    val displayImageModel: Any?
        get() = pickedUri ?: thumbnailPath.takeIf { it.isNotBlank() } ?: localImagePath.takeIf { it.isNotBlank() }
}

private data class EditorFormState(
    val type: ItemType = ItemType.LINK,
    val itemId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val url: String = "",
    val note: String = "",
    val previewTitle: String = "",
    val previewDescription: String = "",
    val content: String = "",
    val source: String = "",
    val quoteAuthor: String = "",
    val localImagePath: String = "",
    val thumbnailPath: String = "",
    val aspectRatio: Float = 1f,
    val pickedUri: Uri? = null,
    val websiteUrl: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val protectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
    val titleVisibleWhenProtected: Boolean = true,
    val folderId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val colorTheme: String = "mist",
    val coverStyle: String = "glass",
    val securitySettings: SecuritySettings = SecuritySettings(),
    val error: String? = null,
    val savedItemId: String? = null,
    val savedProtectionLevel: ItemProtectionLevel = ItemProtectionLevel.NONE,
)

private fun EditorFormState.toUiState(
    folders: List<Folder>,
    tags: List<Tag>,
    securitySettings: SecuritySettings,
): EditorUiState = EditorUiState(
    type = type,
    itemId = itemId,
    isLoading = isLoading,
    isSaving = isSaving,
    title = title,
    url = url,
    note = note,
    previewTitle = previewTitle,
    previewDescription = previewDescription,
    content = content,
    source = source,
    quoteAuthor = quoteAuthor,
    localImagePath = localImagePath,
    thumbnailPath = thumbnailPath,
    aspectRatio = aspectRatio,
    pickedUri = pickedUri,
    websiteUrl = websiteUrl,
    username = username,
    email = email,
    password = password,
    protectionLevel = protectionLevel,
    titleVisibleWhenProtected = titleVisibleWhenProtected,
    folderId = folderId,
    tagIds = tagIds,
    isPinned = isPinned,
    isFavorite = isFavorite,
    colorTheme = colorTheme,
    coverStyle = coverStyle,
    securitySettings = securitySettings,
    folders = folders,
    tags = tags,
    error = error,
    savedItemId = savedItemId,
    savedProtectionLevel = savedProtectionLevel,
)

private fun EditorFormState.validate(securitySettings: SecuritySettings): String? = when (type) {
    ItemType.LINK -> when {
        !VaultFormatters.isValidUrl(url.trim()) -> "请输入有效的链接地址。"
        else -> null
    }

    ItemType.TEXT -> if (content.isBlank()) "正文不能为空。" else null
    ItemType.IMAGE -> if (pickedUri == null && localImagePath.isBlank()) "请先选择图片。" else null
    ItemType.CREDENTIAL -> when {
        username.isBlank() -> "用户名不能为空。"
        password.isBlank() -> "密码不能为空。"
        else -> null
    }
}.let { validationError ->
    if (validationError != null) {
        validationError
    } else if (protectionLevel != ItemProtectionLevel.NONE && !securitySettings.hasPin) {
        "请先在设置中配置主 PIN。"
    } else {
        null
    }
}

private fun defaultThemeFor(type: ItemType): String = when (type) {
    ItemType.LINK -> "mist"
    ItemType.TEXT -> "sage"
    ItemType.IMAGE -> "sunset"
    ItemType.CREDENTIAL -> "night"
}

private fun defaultCoverStyleFor(type: ItemType): String = when (type) {
    ItemType.LINK -> "glass"
    ItemType.TEXT -> "quote"
    ItemType.IMAGE -> "gallery"
    ItemType.CREDENTIAL -> "secure"
}

private val editorThemes = listOf(
    "mist" to "雾蓝",
    "sage" to "鼠尾草",
    "sunset" to "落日橙",
    "night" to "夜色",
)

private fun ItemType.screenTitleSuffix(): String = when (this) {
    ItemType.LINK -> "链接"
    ItemType.TEXT -> "便签"
    ItemType.IMAGE -> "图片"
    ItemType.CREDENTIAL -> "账号"
}

private fun ItemType.editorLabel(): String = when (this) {
    ItemType.LINK -> "链接"
    ItemType.TEXT -> "便签"
    ItemType.IMAGE -> "图片"
    ItemType.CREDENTIAL -> "账号"
}

private fun ItemProtectionLevel.editorLabel(): String = when (this) {
    ItemProtectionLevel.NONE -> "不加密"
    ItemProtectionLevel.STANDARD -> "一般加密"
    ItemProtectionLevel.SUPER -> "超级加密"
}

@Composable
private fun editorPanelColor(theme: String): androidx.compose.ui.graphics.Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (theme) {
        "sage" -> if (darkTheme) androidx.compose.ui.graphics.Color(0xFF1A211D) else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        "sunset" -> if (darkTheme) androidx.compose.ui.graphics.Color(0xFF241D19) else androidx.compose.ui.graphics.Color(0xFFFFFCFA)
        "night" -> if (darkTheme) androidx.compose.ui.graphics.Color(0xFF181B1F) else androidx.compose.ui.graphics.Color(0xFFF7F8FA)
        else -> if (darkTheme) androidx.compose.ui.graphics.Color(0xFF171A1D) else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    }
}
