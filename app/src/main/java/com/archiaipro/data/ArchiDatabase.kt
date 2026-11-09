package com.archiaipro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class, DesignEntity::class], version = 2, exportSchema = false)
abstract class ArchiDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun designDao(): DesignDao

    companion object {
        @Volatile private var INSTANCE: ArchiDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS designs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "projectId INTEGER NOT NULL, kind TEXT NOT NULL, " +
                        "title TEXT NOT NULL, payloadJson TEXT NOT NULL, updatedAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_designs_projectId_kind ON designs(projectId, kind)")
            }
        }

        fun get(context: Context): ArchiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ArchiDatabase::class.java,
                    "archiai-pro.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
