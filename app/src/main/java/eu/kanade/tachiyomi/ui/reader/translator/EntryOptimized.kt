package eu.kanade.tachiyomi.ui.reader.translator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entryoptimized")
data class EntryOptimized(
    @PrimaryKey val id: Int?,
    val dictionary: String?,
    val kanji: String?,
    val readings: String?,
    val meanings: String?,
    val pos: String?,
    val priorities: String?,
    val primaryEntry: Boolean?
)
