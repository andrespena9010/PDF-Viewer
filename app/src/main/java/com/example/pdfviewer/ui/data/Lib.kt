package com.example.pdfviewer.ui.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
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

    private lateinit var pdfBoxDocument: PDDocument
    private lateinit var pdfBoxRenderer: PDFRenderer

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

    fun getPageSize(): Pair<Int, Int> {

        return if (native) {
            val page = pdfRenderer.openPage(0)
            val size = Pair(page.width, page.height)
            page.close()
            fixImage(width = size.first, height = size.second, newWidth = deviceWidth)
        } else {
            val page = pdfBoxDocument.getPage(0)
            fixImage(
                width = page.mediaBox.width.toInt(),
                height = page.mediaBox.height.toInt(),
                newWidth = deviceWidth
            )
        }

    }

    fun renderBitmap(indexPage: Int): Bitmap { // TODO: colocar mutex

        var bitmap: Bitmap

        if (native) {

            val page = pdfRenderer.openPage(indexPage)
            val size = fixImage(width = page.width, height = page.height, newWidth = deviceWidth)

            bitmap = createBitmap(size.first, size.second)

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            page.close()

        } else {

            val page = pdfBoxDocument.getPage(indexPage)
            val size = fixImage(
                width = page.mediaBox.width.toInt(),
                height = page.mediaBox.height.toInt(),
                newWidth = deviceWidth
            )
            // validar el tamaño y calidad
            try {
                bitmap = pdfBoxRenderer.renderImage(0)
            } catch (e: Exception) {
                bitmap = createBitmap(100, 100)
                Log.i("TIMEPDF", "Inicia renderizado de la pagina ${e.message}")
            }

        }

        return bitmap

    }

    fun getBitmapName(fileName: String, pageIndex: Int): String {

        return if (native) {

            val page = pdfRenderer.openPage(pageIndex)
            val gcd = gcd(width = page.width, height = page.height)
            val aspectRatio = Pair(first = page.width / gcd, second = page.height / gcd)
            page.close()
            "${fileName}_${pageIndex}_${aspectRatio}_A"

        } else {
            val page = pdfBoxDocument.getPage(pageIndex)
            val gcd =
                gcd(width = page.mediaBox.width.toInt(), height = page.mediaBox.height.toInt())
            val aspectRatio = Pair(
                first = page.mediaBox.width.toInt() / gcd,
                second = page.mediaBox.height.toInt() / gcd
            )
            "${fileName}_${pageIndex}_$aspectRatio"
        }

    }

    fun gcd(width: Int, height: Int): Int {
        return if (height == 0) width else gcd(height, width % height)
    }

    fun fixImage(width: Int, height: Int, newWidth: Int): Pair<Int, Int> {
        val newHeight = (height * newWidth) / width
        return Pair(newWidth, newHeight)
    }

    fun close() {

        if (native) {
            parcelFileDescriptor.close()
            pdfRenderer.close()
        } else {
            pdfBoxDocument.close()
        }

    }

}

object PdfRendererObject : PdfRendererMix()