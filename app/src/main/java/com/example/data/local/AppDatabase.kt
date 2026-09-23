package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        KhataEntryEntity::class,
        DiseaseScanEntity::class,
        UserEntity::class,
        PendingProofEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun khataDao(): KhataDao
    abstract fun diseaseScanDao(): DiseaseScanDao
    abstract fun userDao(): UserDao
    abstract fun pendingProofDao(): PendingProofDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Adds the profile photo column.
         *
         * There were no migrations at all before this, so any schema change would
         * have crashed existing installs on launch. The destructive fallback below
         * is a safety net for databases from unreleased builds; this migration is
         * what actually preserves a real user's data.
         */
        /**
         * Adds a sync flag to disease scans.
         *
         * Without it the retry pass had no way to tell a synced scan from an
         * unsynced one, so it re-uploaded the fifty most recent scans on every
         * single app launch. Harmless to the data, because the upsert is
         * idempotent, but fifty sequential network calls made startup crawl.
         */
        /**
         * Offline queue for contractor evidence captured without a signal.
         * Column types and nullability match PendingProofEntity exactly, which
         * Room verifies on open.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_proofs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workOrderId TEXT NOT NULL,
                        stage TEXT NOT NULL,
                        imagePath TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        accuracyM REAL,
                        capturedAtIso TEXT NOT NULL,
                        isMockLocation INTEGER NOT NULL,
                        attempts INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE disease_scans ADD COLUMN isSyncedCloud INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN profilePhotoPath TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farmify_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
