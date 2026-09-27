/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

package verlintas.openvisum.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaEntity::class,
        SafFolderEntity::class,
        NetworkSourceEntity::class,
        StreamHistoryEntity::class,
        BookmarkEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class OpenVisumDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao

    abstract fun safFolderDao(): SafFolderDao

    abstract fun networkSourceDao(): NetworkSourceDao

    abstract fun streamHistoryDao(): StreamHistoryDao

    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): OpenVisumDatabase =
            Room.databaseBuilder(context, OpenVisumDatabase::class.java, "openvisum.db")
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookmarks` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`media_uri` TEXT NOT NULL, " +
                        "`position_ms` INTEGER NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_bookmarks_media_uri` ON `bookmarks` (`media_uri`)",
                )
            }
        }
    }
}
