package com.example.pdfviewer.ui.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.createBitmap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File

sealed class Libraries {

    data object PdfRenderer : Libraries()

    data object PDFBox : Libraries()

}

val listLib = mapOf(
    Pair(Libraries.PdfRenderer, "PdfRenderer"),
    Pair(Libraries.PDFBox, "PDFBox")
)

open class PdfRendererMix() {

    private val deviceWidth = Resources.getSystem().displayMetrics.widthPixels

    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var pdfRenderer: PdfRenderer
    private val openPages = mutableMapOf<Int,PdfRenderer.Page>()

    private lateinit var pdfBoxDocument: PDDocument
    private lateinit var pdfBoxRenderer: PDFRenderer

    private var pdfName = ""

    private var native = true

    fun init(library: Libraries) {

        native = when (library) {
            Libraries.PDFBox -> {
                false
            }

            Libraries.PdfRenderer -> {
                true
            }
        }

    }

    fun open(file: File) {

        if (native) {

            parcelFileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            pdfRenderer = PdfRenderer(parcelFileDescriptor)

            pdfName = file.name

        } else {

            pdfBoxDocument = PDDocument.load(file)
            pdfBoxRenderer = PDFRenderer(pdfBoxDocument)

        }

    }

    fun pageCount(): Int {

        return if (native) {
            pdfRenderer.pageCount
        } else {
            pdfBoxDocument.numberOfPages
        }

    }

    fun renderBitmap( indexPage: Int ): Bitmap {

        var bitmap: Bitmap

        if (native) {

            val page = pdfRenderer.openPage( indexPage )
            openPages[ indexPage ]= page
            val size = fixImage(width = page.width, height = page.height, newWidth = deviceWidth)

            bitmap = createBitmap(size.first, size.second)

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            page.close()
            openPages.remove( indexPage )

        } else {

            val page = pdfBoxDocument.getPage(indexPage)
            val size = fixImage(
                width = page.mediaBox.width.toInt(),
                height = page.mediaBox.height.toInt(),
                newWidth = deviceWidth
            )

            try {
                bitmap = pdfBoxRenderer.renderImage(0)
            } catch (e: Exception) {
                bitmap = createBitmap(100, 100)
                Log.i("TIMEPDF", "Error al renderizar con pdfbox ${e.message}")
            }

        }

        return bitmap

    }

    fun fixImage(width: Int, height: Int, newWidth: Int): Pair<Int, Int> {
        val newHeight = (height * newWidth) / width
        return Pair(newWidth, newHeight)
    }

    fun close() {

        if (native) {

            openPages.forEach { it.value.close() }
            openPages.clear()
            parcelFileDescriptor.close()
            pdfRenderer.close()

        } else {
            pdfBoxDocument.close()
        }

    }

}

object PdfRendererObject : PdfRendererMix()