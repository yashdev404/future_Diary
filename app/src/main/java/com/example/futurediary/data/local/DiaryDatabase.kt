package com.example.futurediary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryImage
import com.example.futurediary.data.model.Promise
import com.example.futurediary.data.model.UserProfile

@Database(entities = [DiaryEntry::class, UserProfile::class, DiaryImage::class, Promise::class], version = 12, exportSchema = false)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promises ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promises ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN remoteUrl TEXT")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN mood TEXT")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN songLink TEXT")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN songTitle TEXT")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN songArtist TEXT")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN songThumbnailUrl TEXT")
            }
        }
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Update user_profiles
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN profileImageFileName TEXT")
                // Migrate data: extract filename from full path defensively
                db.execSQL("""
                    UPDATE user_profiles 
                    SET profileImageFileName = CASE 
                        WHEN profileImageUri LIKE '%/files/%' THEN substr(profileImageUri, instr(profileImageUri, '/files/') + 7)
                        ELSE profileImageUri 
                    END
                    WHERE profileImageUri IS NOT NULL
                """)
                
                // 2. Update diary_images
                db.execSQL("ALTER TABLE diary_images ADD COLUMN fileName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN mimeType TEXT NOT NULL DEFAULT 'image/jpeg'")
                db.execSQL("ALTER TABLE diary_images ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                
                // Migrate data for diary_images defensively
                db.execSQL("""
                    UPDATE diary_images 
                    SET fileName = CASE 
                        WHEN imageUri LIKE '%/files/%' THEN substr(imageUri, instr(imageUri, '/files/') + 7)
                        ELSE imageUri 
                    END
                    WHERE imageUri IS NOT NULL
                """)
                
                // Final fallback to ensure no empty filenames
                db.execSQL("UPDATE diary_images SET fileName = 'unknown_image' WHERE fileName = ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // UserProfile: userId (PK), name, profileImageFileName
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles_new` (`userId` TEXT NOT NULL, `name` TEXT NOT NULL DEFAULT 'Journal Owner', `profileImageFileName` TEXT, PRIMARY KEY(`userId`))")
                db.execSQL("INSERT INTO `user_profiles_new` (userId, name, profileImageFileName) SELECT userId, name, profileImageFileName FROM `user_profiles`")
                db.execSQL("DROP TABLE `user_profiles`")
                db.execSQL("ALTER TABLE `user_profiles_new` RENAME TO `user_profiles`")

                // DiaryImage: id (PK), entryId, fileName, mimeType, fileSize
                // 1. Create the new table with correct defaults to match the entity
                db.execSQL("CREATE TABLE IF NOT EXISTS `diary_images_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, `fileName` TEXT NOT NULL, `mimeType` TEXT NOT NULL DEFAULT 'image/jpeg', `fileSize` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`entryId`) REFERENCES `diary_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                
                // 2. Copy data from old table
                db.execSQL("INSERT INTO `diary_images_new` (id, entryId, fileName, mimeType, fileSize) SELECT id, entryId, fileName, mimeType, fileSize FROM `diary_images`")
                
                // 3. Drop old table and its indices
                db.execSQL("DROP TABLE `diary_images`")
                
                // 4. Rename new table to original name
                db.execSQL("ALTER TABLE `diary_images_new` RENAME TO `diary_images`")
                
                // 5. Create index on the final table name
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_images_entryId` ON `diary_images` (`entryId`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `promises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` TEXT NOT NULL, `date` INTEGER NOT NULL, `content` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL DEFAULT 0)")
            }
        }
    }
}
