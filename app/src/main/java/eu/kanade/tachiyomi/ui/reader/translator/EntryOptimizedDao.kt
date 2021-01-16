package eu.kanade.tachiyomi.ui.reader.translator

import androidx.room.Dao
import androidx.room.Query

@Dao
interface EntryOptimizedDao {
    @Query("SELECT * FROM entryoptimized WHERE kanji LIKE :kanji")
    suspend fun findByName(kanji: String): List<EntryOptimized>
}
