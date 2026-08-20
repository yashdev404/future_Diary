package com.example.futurediary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryImage
import com.example.futurediary.data.model.UserProfile

@Database(entities = [DiaryEntry::class, UserProfile::class, DiaryImage::class], version = 6, exportSchema = false)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN isVaultItem INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN unlockDate INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`userId` TEXT NOT NULL, `name` TEXT NOT NULL, `profileImageUri` TEXT, PRIMARY KEY(`userId`))")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new diary_images table
                db.execSQL("CREATE TABLE IF NOT EXISTS `diary_images` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, `imageUri` TEXT NOT NULL, FOREIGN KEY(`entryId`) REFERENCES `diary_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_images_entryId` ON `diary_images` (`entryId`)")
                
                // 2. Migrate existing imageUri from diary_entries to diary_images
                db.execSQL("INSERT INTO diary_images (entryId, imageUri) SELECT id, imageUri FROM diary_entries WHERE imageUri IS NOT NULL")
                
                // Note: We're not dropping the imageUri column from diary_entries yet to keep the migration simple.
                // Room entities will just ignore that column in the code.
            }
        }
    }
}
