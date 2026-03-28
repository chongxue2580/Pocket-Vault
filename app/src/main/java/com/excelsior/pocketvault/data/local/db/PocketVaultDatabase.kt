package com.excelsior.pocketvault.data.local.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.excelsior.pocketvault.data.local.dao.VaultDao
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

@Database(
    entities = [
        ItemEntity::class,
        ItemSecurityEntity::class,
        LinkItemEntity::class,
        TextItemEntity::class,
        ImageItemEntity::class,
        CredentialItemEntity::class,
        FolderEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        SettingEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class PocketVaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `item_security` (
                        `itemId` TEXT NOT NULL,
                        `protectionLevel` TEXT NOT NULL,
                        PRIMARY KEY(`itemId`),
                        FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `item_security`
                    ADD COLUMN `titleVisible` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent(),
                )
            }
        }
    }
}
