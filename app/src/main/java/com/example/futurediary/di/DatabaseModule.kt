package com.example.futurediary.di

import android.content.Context
import androidx.room.Room
import com.example.futurediary.data.local.DiaryDao
import com.example.futurediary.data.local.DiaryDatabase
import com.example.futurediary.ui.util.SecurityUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton



@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiaryDatabase {
        // SQLCipher requirement: Load native libraries
        SQLiteDatabase.loadLibs(context)
        
        // Get the hardware-backed secure passphrase
        val passphrase = SecurityUtils.getDatabasePassphrase(context)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            DiaryDatabase::class.java,
            "future_diary_secure.db"
        )
        .openHelperFactory(factory)
        .addMigrations(
            DiaryDatabase.MIGRATION_2_3, 
            DiaryDatabase.MIGRATION_3_4,
            DiaryDatabase.MIGRATION_4_5,
            DiaryDatabase.MIGRATION_5_6,
            DiaryDatabase.MIGRATION_6_7,
            DiaryDatabase.MIGRATION_7_8,
            DiaryDatabase.MIGRATION_8_9,
            DiaryDatabase.MIGRATION_9_10,
            DiaryDatabase.MIGRATION_10_11,
            DiaryDatabase.MIGRATION_11_12
        )
        .build()
    }

    @Provides
    fun provideDiaryDao(database: DiaryDatabase): DiaryDao {
        return database.diaryDao()
    }
}