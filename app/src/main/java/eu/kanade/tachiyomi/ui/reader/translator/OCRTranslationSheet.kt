package eu.kanade.tachiyomi.ui.reader.translator

import android.app.Activity
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DictionaryEntryBinding
import eu.kanade.tachiyomi.databinding.OcrTranslationSheetBinding
import eu.kanade.tachiyomi.util.lang.launchUI

class OCRTranslationSheet(activity: Activity, ocrResult: OCRManager.OCRResult) : BottomSheetDialog(activity) {
    private val binding = OcrTranslationSheetBinding.inflate(layoutInflater, null, false)
    private var editable = false
    private val db: JmdictDatabase

    init {
        setContentView(binding.root)
        db = Room.databaseBuilder(context, JmdictDatabase::class.java, "DB_KakuDict-02-16-2019").createFromAsset("DB_KakuDict-02-16-2019.db").build()
        val entry = DictionaryEntryBinding.inflate(layoutInflater, binding.entriesLayout, true)
        entry.dictionaryWord.text = "Recontra pelotudísimo"
        entry.dictionaryMeaning.text = "Dícese de aquel que no es muy inteligente. 頭があまり良くない人"
        binding.ocrResultText.setHorizontallyScrolling(false)
        binding.ocrResultText.ellipsize = null
        binding.ocrResultText.setText(ocrResult.text.filter { !it.isWhitespace() })
        binding.editOCRResult.setOnClickListener { toggleEditable() }
        binding.searchOCRResult.setOnClickListener { launchUI { searchText() } }
    }

    private suspend fun searchText() {
        val result = db.entryOptimizedDao().findByName(binding.ocrResultText.text.toString())
        populateResults(result)
    }

    private fun populateResults(results: List<EntryOptimized>) {
        for (result: EntryOptimized in results) {
            val entry = DictionaryEntryBinding.inflate(layoutInflater, binding.entriesLayout, true)
            entry.dictionaryWord.text = result.kanji
            entry.dictionaryMeaning.text = result.meanings
        }
    }

    private fun toggleEditable() {
        editable = !editable
        binding.ocrResultText.isEnabled = editable
        if (editable) {
            binding.editOCRResult.setImageDrawable(context.getDrawable(R.drawable.ic_done_24dp))
            binding.ocrResultText.requestFocus()
        } else {
            binding.editOCRResult.setImageDrawable(context.getDrawable(R.drawable.ic_edit_24dp))
        }
    }
}
