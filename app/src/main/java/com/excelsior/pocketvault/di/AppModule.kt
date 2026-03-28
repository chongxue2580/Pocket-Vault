package com.excelsior.pocketvault.di

import android.content.Context
import androidx.room.Room
import com.excelsior.pocketvault.data.local.dao.VaultDao
import com.excelsior.pocketvault.data.local.db.PocketVaultDatabase
import com.excelsior.pocketvault.data.repository.CollectionRepositoryImpl
import com.excelsior.pocketvault.data.repository.SecurityRepositoryImpl
import com.excelsior.pocketvault.data.repository.SettingsRepositoryImpl
import com.excelsior.pocketvault.data.repository.VaultRepositoryImpl
import com.excelsior.pocketvault.data.storage.ImageStorageManager
import com.excelsior.pocketvault.domain.repository.FolderRepository
import com.excelsior.pocketvault.domain.repository.ImageStorageRepository
import com.excelsior.pocketvault.domain.repository.InsightRepository
import com.excelsior.pocketvault.domain.repository.ItemRepository
import com.excelsior.pocketvault.domain.repository.SecurityRepository
import com.excelsior.pocketvault.domain.repository.SettingsRepository
import com.excelsior.pocketvault.domain.repository.TagRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PocketVaultDatabase = Room.databaseBuilder(
        context,
        PocketVaultDatabase::class.java,
        "pocket_vault.db",
    ).addMigrations(
        PocketVaultDatabase.MIGRATION_1_2,
        PocketVaultDatabase.MIGRATION_2_3,
    ).build()

    @Provides
    @Singleton
    fun provideVaultDao(database: PocketVaultDatabase): VaultDao = database.vaultDao()

    @Provides
    @Singleton
    fun provideItemRepository(impl: VaultRepositoryImpl): ItemRepository = impl

    @Provides
    @Singleton
    fun provideInsightRepository(impl: VaultRepositoryImpl): InsightRepository = impl

    @Provides
    @Singleton
    fun provideFolderRepository(impl: CollectionRepositoryImpl): FolderRepository = impl

    @Provides
    @Singleton
    fun provideTagRepository(impl: CollectionRepositoryImpl): TagRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides
    @Singleton
    fun provideSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository = impl

    @Provides
    @Singleton
    fun provideImageStorageRepository(impl: ImageStorageManager): ImageStorageRepository = impl
}
