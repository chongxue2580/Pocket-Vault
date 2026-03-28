package com.excelsior.pocketvault.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "items", indices = [Index("folderId"), Index("type")])
data class ItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String?,
    val summary: String?,
    val folderId: String?,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val colorTheme: String?,
    val coverStyle: String?,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "item_security",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ItemSecurityEntity(
    @PrimaryKey val itemId: String,
    val protectionLevel: String,
    val titleVisible: Boolean,
)

@Entity(
    tableName = "link_items",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LinkItemEntity(
    @PrimaryKey val itemId: String,
    val url: String,
    val siteName: String,
    val note: String?,
    val previewTitle: String?,
    val previewDescription: String?,
)

@Entity(
    tableName = "text_items",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TextItemEntity(
    @PrimaryKey val itemId: String,
    val content: String,
    val source: String?,
    val quoteAuthor: String?,
)

@Entity(
    tableName = "image_items",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ImageItemEntity(
    @PrimaryKey val itemId: String,
    val localImagePath: String,
    val thumbnailPath: String,
    val note: String?,
    val aspectRatio: Float,
)

@Entity(
    tableName = "credential_items",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CredentialItemEntity(
    @PrimaryKey val itemId: String,
    val siteName: String,
    val websiteUrl: String?,
    val username: String,
    val email: String?,
    val passwordEncrypted: String,
    val noteEncrypted: String?,
    val lastUsedAt: Long?,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val createdAt: Long,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val createdAt: Long,
)

@Entity(
    tableName = "item_tag_cross_ref",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class ItemTagCrossRef(
    val itemId: String,
    val tagId: String,
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

data class VaultItemBundle(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val security: ItemSecurityEntity?,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val link: LinkItemEntity?,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val text: TextItemEntity?,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val image: ImageItemEntity?,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val credential: CredentialItemEntity?,
    @Relation(
        parentColumn = "id",
        entity = TagEntity::class,
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "folderId", entityColumn = "id")
    val folder: FolderEntity?,
)
