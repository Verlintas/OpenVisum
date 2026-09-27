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

@Database(
    entities = [
        MediaEntity::class,
        SafFolderEntity::class,
        NetworkSourceEntity::class,
        StreamHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class OpenVisumDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao

    abstract fun safFolderDao(): SafFolderDao

    abstract fun networkSourceDao(): NetworkSourceDao

    abstract fun streamHistoryDao(): StreamHistoryDao

    companion object {
        fun create(context: Context): OpenVisumDatabase =
            Room.databaseBuilder(context, OpenVisumDatabase::class.java, "openvisum.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
