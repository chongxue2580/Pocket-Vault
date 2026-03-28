package com.excelsior.pocketvault.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.excelsior.pocketvault.core.common.VaultFormatters
import com.excelsior.pocketvault.core.designsystem.icon.VaultIcons
import com.excelsior.pocketvault.domain.model.CredentialVaultItem
import com.excelsior.pocketvault.domain.model.ImageVaultItem
import com.excelsior.pocketvault.domain.model.ItemProtectionLevel
import com.excelsior.pocketvault.domain.model.LinkVaultItem
import com.excelsior.pocketvault.domain.model.TextVaultItem
import com.excelsior.pocketvault.domain.model.VaultItem

@Composable
fun VaultItemCard(item: VaultItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    when (item) {
        is LinkVaultItem -> LinkItemCard(item, modifier, onClick)
        is TextVaultItem -> TextItemCard(item, modifier, onClick)
        is ImageVaultItem -> ImageItemCard(item, modifier, onClick)
        is CredentialVaultItem -> CredentialItemCard(item, modifier, onClick)
    }
}

@Composable
fun LinkItemCard(item: LinkVaultItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    VaultGlassCard(modifier = modifier, containerColor = item.noteSurfaceColor(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VaultTypeIcon(type = item.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (item.protectionLevel == ItemProtectionLevel.NONE || item.titleVisibleWhenProtected) item.title else "加密链接",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.protectionLevel == ItemProtectionLevel.NONE) item.siteName else "已加密",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (item.protectionLevel == ItemProtectionLevel.NONE) {
                VaultFormatters.compactUrl(item.url)
            } else {
                "输入密码后查看"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.protectionLevel == ItemProtectionLevel.NONE && !item.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = item.note, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        item.tags.takeIf { it.isNotEmpty() }?.let {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))
            VaultTagRow(tags = item.tags)
        }
    }
}

@Composable
fun TextItemCard(item: TextVaultItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    VaultGlassCard(modifier = modifier, containerColor = item.noteSurfaceColor(), onClick = onClick) {
        Text(
            text = if (item.protectionLevel == ItemProtectionLevel.NONE) {
                item.title?.takeIf { it.isNotBlank() } ?: item.content.take(12)
            } else if (item.titleVisibleWhenProtected) {
                item.title?.takeIf { it.isNotBlank() } ?: item.content.take(12)
            } else {
                "加密便签"
            },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (item.protectionLevel == ItemProtectionLevel.NONE) item.content else "输入密码后查看",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (item.protectionLevel == ItemProtectionLevel.NONE) {
            item.quoteAuthor?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
            }
            item.source?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        item.tags.takeIf { it.isNotEmpty() }?.let {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))
            VaultTagRow(tags = item.tags)
        }
    }
}

@Composable
fun ImageItemCard(item: ImageVaultItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    VaultGlassCard(modifier = modifier, containerColor = item.noteSurfaceColor(), onClick = onClick) {
        if (item.protectionLevel == ItemProtectionLevel.NONE) {
            AsyncImage(
                model = item.thumbnailPath.ifBlank { item.localImagePath },
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(item.noteAccentColor().copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = VaultIcons.Lock,
                    contentDescription = null,
                    tint = item.noteAccentColor(),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (item.protectionLevel == ItemProtectionLevel.NONE || item.titleVisibleWhenProtected) {
                item.title ?: "未命名图片"
            } else {
                "加密图片"
            },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.protectionLevel == ItemProtectionLevel.NONE) {
            item.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "输入密码后查看", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        item.tags.takeIf { it.isNotEmpty() }?.let {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))
            VaultTagRow(tags = item.tags)
        }
    }
}

@Composable
fun CredentialItemCard(item: CredentialVaultItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    VaultGlassCard(modifier = modifier, containerColor = item.noteSurfaceColor(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VaultTypeIcon(type = item.type, tint = item.noteAccentColor())
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (item.protectionLevel == ItemProtectionLevel.NONE || item.titleVisibleWhenProtected) item.siteName else "加密账号",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (item.protectionLevel == ItemProtectionLevel.NONE) item.username else "输入密码后查看",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(item.noteAccentColor().copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "••••••••••••",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (item.protectionLevel == ItemProtectionLevel.NONE) {
                item.websiteUrl ?: "仅限本地查看"
            } else {
                "输入密码后查看"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.tags.takeIf { it.isNotEmpty() }?.let {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))
            VaultTagRow(tags = item.tags)
        }
    }
}

@Composable
private fun VaultItem.noteSurfaceColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (colorTheme) {
        "sage" -> if (darkTheme) Color(0xFF1A211D) else Color(0xFFFBFCFA)
        "sunset" -> if (darkTheme) Color(0xFF241D19) else Color(0xFFFFFAF6)
        "night" -> if (darkTheme) Color(0xFF181B1F) else Color(0xFFF7F7F8)
        else -> if (darkTheme) Color(0xFF171A1D) else Color(0xFFFFFFFF)
    }
}

@Composable
private fun VaultItem.noteAccentColor(): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (colorTheme) {
        "sage" -> if (darkTheme) Color(0xFFB8D0BA) else Color(0xFF5D7962)
        "sunset" -> if (darkTheme) Color(0xFFF2B17A) else Color(0xFFB96F3C)
        "night" -> if (darkTheme) Color(0xFFD9E1EC) else Color(0xFF526173)
        else -> if (darkTheme) Color(0xFFD6DFEB) else Color(0xFF4E6178)
    }
}
