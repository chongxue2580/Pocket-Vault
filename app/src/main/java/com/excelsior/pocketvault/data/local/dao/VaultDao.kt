package com.excelsior.pocketvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.excelsior.pocketvault.data.local.entity.CredentialItemEntity
import com.excelsior.pocketvault.data.local.entity.FolderEntity
import com.excelsior.pocketvault.data.local.entity.ImageItemEntity
import com.excelsior.pocketvault.data.local.entity.ItemEntity
import com.excelsior.pocketvault.data.local.entity.ItemSecurityEntity
import com.excelsior.pocketvault.data.local.entity.ItemTagCrossRef
import com.excelsior.pocketvault.data.local.entity.LinkItemEntity
import com.excelsior.pocketvault.data.local.entity.SettingEntity
import com.excelsior.pocketvault.data.local.entity.TagEntity
import com.excelsior.pocketvault.data.local.entity.TextItemEntity
import com.excelsior.pocketvault.data.local.entity.VaultItemBundle
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Transaction
    @Query("SELECT * FROM items ORDER BY isPinned DESC, updatedAt DESC")
    fun observeItemBundles(): Flow<List<VaultItemBundle>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    fun observeItemBundle(itemId: String): Flow<VaultItemBundle?>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemBundle(itemId: String): VaultItemBundle?

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemEntity(itemId: String): ItemEntity?

    @Query("SELECT COUNT(*) FROM items")
    suspend fun itemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)

    @Query("UPDATE items SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updatePinned(itemId: String, isPinned: Boolean, updatedAt: Long)

    @Query("UPDATE items SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateFavorite(itemId: String, isFavorite: Boolean, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemSecurity(itemSecurity: ItemSecurityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinkItem(item: LinkItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTextItem(item: TextItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImageItem(item: ImageItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredentialItem(item: CredentialItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemTags(itemTags: List<ItemTagCrossRef>)

    @Query("DELETE FROM link_items WHERE itemId = :itemId")
    suspend fun deleteLinkItem(itemId: String)

    @Query("DELETE FROM text_items WHERE itemId = :itemId")
    suspend fun deleteTextItem(itemId: String)

    @Query("DELETE FROM image_items WHERE itemId = :itemId")
    suspend fun deleteImageItem(itemId: String)

    @Query("DELETE FROM credential_items WHERE itemId = :itemId")
    suspend fun deleteCredentialItem(itemId: String)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun deleteTagsForItem(itemId: String)

    @Query("DELETE FROM item_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteItemLinksForTag(tagId: String)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    suspend fun getFolders(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE items SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun migrateFolderItems(oldFolderId: String, newFolderId: String?)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String)

    @Query("SELECT * FROM tags ORDER BY createdAt ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY createdAt ASC")
    suspend fun getTags(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: String)

    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings")
    suspend fun getSettings(): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSetting(key: String)
}
