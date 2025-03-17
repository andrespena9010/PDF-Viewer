package com.example.pdfviewer.ui.render

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.File

class PDFRenderer {

    private val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
    private var parcelFileDescriptor: ParcelFileDescriptor
    private var pdfRenderer: PdfRenderer

    constructor( file: File ){

        parcelFileDescriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        pdfRenderer = PdfRenderer( parcelFileDescriptor )

    }

    fun pageCount(): Int {
        return pdfRenderer.pageCount
    }

    fun getBitmapPage(pageIndex: Int ): Bitmap? {

        var bitmap: Bitmap? = null

        try {

            pdfRenderer.openPage( pageIndex ).use { page ->

                val size = fixImage(width = page.width, height = page.height, newWidth = deviceWidth)

                bitmap = createBitmap(size.first, size.second)

                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

            }

        } catch ( e: Exception ){
            Log.e("PDFRenderer", "Error de renderizado en pagina $pageIndex ${e.message}")
        }

        return bitmap

    }

    fun fixImage(width: Int, height: Int, newWidth: Int): Pair<Int, Int> {
        val newHeight = (height * newWidth) / width
        return Pair(newWidth, newHeight)
    }

}