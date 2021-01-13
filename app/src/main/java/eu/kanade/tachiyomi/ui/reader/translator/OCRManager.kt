package eu.kanade.tachiyomi.ui.reader.translator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel.RIL_BLOCK
import java.io.*

class OCRManager(context: Context) {
    private val api: TessBaseAPI

    init {
        val dir = context.getExternalFilesDir(null)
        val tessdata = File(dir!!.path + "/tessdata")
        if (!tessdata.exists()) {
            tessdata.mkdirs()
            copyAssetFolderToFolder(context, "tessdata", tessdata)
        }
        api = TessBaseAPI()
        api.init(dir.path, "jpn_vert", TessBaseAPI.OEM_LSTM_ONLY)
    }

    private fun copyAssetFolderToFolder(activity: Context, assetsFolder: String, destinationFolder: File?) {
        var stream: InputStream?
        var output: OutputStream?
        try {
            for (fileName in activity.assets.list(assetsFolder)!!) {
                stream = activity.assets.open("$assetsFolder/$fileName")
                output = BufferedOutputStream(FileOutputStream(File(destinationFolder, fileName)))
                val data = ByteArray(1024)
                var count: Int
                while (stream.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }
                output.flush()
                output.close()
                stream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recognize(b: Bitmap): OCRResult {
        api.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT
        api.setImage(b)
        val result = OCRResult(api.utF8Text, ArrayList())
        val iterator = api.resultIterator
        while (iterator.next(RIL_BLOCK))
            result.blocks.add(OCRBlock(iterator.getUTF8Text(RIL_BLOCK), iterator.getBoundingRect(RIL_BLOCK)))
        return result
    }

    data class OCRBlock(val text: String, val box: Rect)
    data class OCRResult(val text: String, val blocks: ArrayList<OCRBlock>)
}
