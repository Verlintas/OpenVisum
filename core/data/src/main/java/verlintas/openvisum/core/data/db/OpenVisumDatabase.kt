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
