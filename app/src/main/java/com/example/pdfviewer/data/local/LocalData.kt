package com.example.pdfviewer.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.pdfviewer.data.model.SavePDFResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * Objeto singleton para gestionar datos locales de PDFs y bitmaps.
 */
object LocalData {

    private lateinit var filesDir: File
    private lateinit var dir: File

    private val mutexMap = mutableMapOf<String, Mutex>()

    /**
     * Establece el directorio de archivos para almacenar PDFs y bitmaps.
     *
     * @param filesDir Directorio de archivos.
     */
    fun setFilesDir(filesDir: File) {
        this.filesDir = filesDir
        dir = File(LocalData.filesDir, "pdf")
        if (!dir.exists()) dir.mkdir()
    }

    /**
     * Guarda un PDF en el almacenamiento local.
     *
     * @param name Nombre del archivo PDF.
     * @param pdfData Datos del PDF en bytes.
     * @return Respuesta con el resultado de la operación.
     */
    suspend fun savePDF(
        name: String,
        pdfData: ByteArray
    ): SavePDFResponse {
        val response = SavePDFResponse()
        val pdf = File(dir, name)
        if (pdf.exists()) {
            response.success = false
            response.message = "The PDF is already exists."
            response.uri = pdf.toUri()
        } else {
            withContext(Dispatchers.IO) {
                try {
                    if (pdf.createNewFile()) {
                        pdf.writeBytes(pdfData)
                        response.success = true
                        response.message = "PDF created."
                        response.uri = pdf.toUri()
                    } else {
                        response.success = false
                        response.message = "Error creating the PDF."
                    }
                } catch (e: Exception) {
                    response.success = false
                    response.message = "Error"
                    response.exceptions.add(e)
                    val err = ""
                    e.printStackTrace(PrintStream(err))
                    Log.e("LocalData.savePDF() -> ", err)
                }
            }
        }
        return response
    }

    /**
     * Verifica si un archivo PDF existe en el almacenamiento local.
     *
     * @param fileName Nombre del archivo PDF.
     * @return Uri del archivo si existe, null en caso contrario.
     */
    fun exist(fileName: String): Uri? {
        val file = File(dir, fileName)
        return if (file.exists()) file.toUri() else null
    }

    /**
     * Guarda un bitmap en la caché local.
     *
     * @param bitmap Bitmap a guardar.
     * @param bitmapName Nombre del archivo bitmap.
     */
    suspend fun saveCacheBitmap(bitmap: Bitmap, bitmapName: String) {
        mutexMap.getOrPut(bitmapName) { Mutex() }.withLock {
            val file = File(dir, bitmapName)
            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Carga un bitmap desde la caché local.
     *
     * @param bitmapName Nombre del archivo bitmap.
     * @return Bitmap cargado, o null si no existe.
     */
    suspend fun loadCacheBitmap(bitmapName: String): Bitmap? {
        mutexMap.getOrPut(bitmapName) { Mutex() }.withLock {
            val file = File(dir, bitmapName)
            return if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }
    }
}