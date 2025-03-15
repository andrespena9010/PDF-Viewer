package com.example.pdfviewer.ui.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.io.File

/**
 * Clase base para renderizar PDFs.
 */
open class PdfRendererMix {

    private val deviceWidth = Resources.getSystem().displayMetrics.widthPixels

    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var pdfRenderer: PdfRenderer
    private val openPages = mutableMapOf<Int, PdfRenderer.Page>()

    private var pdfName = ""

    /**
     * Abre un archivo PDF para renderizar.
     *
     * @param file Archivo PDF a abrir.
     */
    fun open(file: File) {
        parcelFileDescriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
        pdfName = file.name
    }

    /**
     * Obtiene el número de páginas del PDF.
     *
     * @return Número de páginas del PDF.
     */
    fun pageCount(): Int {
        return pdfRenderer.pageCount
    }

    /**
     * Renderiza una página del PDF como un Bitmap.
     *
     * @param indexPage Índice de la página a renderizar.
     * @return Bitmap de la página renderizada.
     */
    fun renderBitmap(indexPage: Int): Bitmap {
        var bitmap: Bitmap

        val page = pdfRenderer.openPage(indexPage)  // TODO: verificar el funcionamiento de este metodo en paralelo. condiciones de carrera
        openPages[indexPage] = page
        val size = fixImage(width = page.width, height = page.height, newWidth = deviceWidth)

        bitmap = createBitmap(size.first, size.second)

        page.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        page.close()
        openPages.remove(indexPage)

        return bitmap
    }

    /**
     * Ajusta las dimensiones de una imagen para que se adapte al ancho del dispositivo.
     *
     * @param width Ancho original de la imagen.
     * @param height Alto original de la imagen.
     * @param newWidth Nuevo ancho de la imagen.
     * @return Par de valores con el nuevo ancho y alto de la imagen.
     */
    fun fixImage(width: Int, height: Int, newWidth: Int): Pair<Int, Int> {
        val newHeight = (height * newWidth) / width
        return Pair(newWidth, newHeight)
    }

    /**
     * Cierra el renderizador de PDF y libera los recursos.
     */
    fun close() {
        openPages.forEach { it.value.close() }
        openPages.clear()
        parcelFileDescriptor.close()
        pdfRenderer.close()
    }
}

/**
 * Objeto singleton para el renderizador de PDF.
 */
object PdfRendererObject : PdfRendererMix()
