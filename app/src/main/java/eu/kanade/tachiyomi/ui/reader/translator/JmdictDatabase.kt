package eu.kanade.tachiyomi.ui.reader.translator

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntryOptimized::class], version = 1)
abstract class JmdictDatabase : RoomDatabase() {
    abstract fun entryOptimizedDao(): EntryOptimizedDao
}
