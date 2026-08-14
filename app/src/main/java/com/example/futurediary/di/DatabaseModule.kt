package com.example.futurediary.di

import android.content.Context
import androidx.room.Room
import com.example.futurediary.data.local.DiaryDao
import com.example.futurediary.data.local.DiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton



@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiaryDatabase {
        return Room.databaseBuilder(
            context,
            DiaryDatabase::class.java,
            "diary_db"
        )
        .addMigrations(DiaryDatabase.MIGRATION_2_3, DiaryDatabase.MIGRATION_3_4)
        .build()
    }

    @Provides
    fun provideDiaryDao(database: DiaryDatabase): DiaryDao {
        return database.diaryDao()
    }
}