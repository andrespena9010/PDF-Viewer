package com.example.pdfviewer.data.local

import android.net.Uri
import com.example.pdfviewer.data.model.SavePDFResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log
import androidx.core.net.toUri
import java.io.PrintStream

object LocalData {

    private lateinit var filesDir: File
    private lateinit var dir: File

    fun setFilesDir( filesDir: File ){
        this.filesDir = filesDir
        dir = File(LocalData.filesDir, "pdf" )
        if ( !dir.exists() ) dir.mkdir()
    }

    suspend fun savePDF (
        name: String,
        pdfData: ByteArray
    ): SavePDFResponse {
        val response = SavePDFResponse()
        val pdf = File( dir, name )
        if ( pdf.exists() ){
            response.success = false
            response.message = "The PDF is already exists."
            response.uri = pdf.toUri()
        } else {
            withContext( Dispatchers.IO ) {
                try {
                    if ( pdf.createNewFile() ) {
                        pdf.writeBytes( pdfData )
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
                    response.exceptions.add( e )
                    val err = ""
                    e.printStackTrace( PrintStream( err ) )
                    Log.e("LocalData.savePDF() -> ", err)
                }
            }
        }
        return response
    }

    fun exist( fileName: String ): Uri? {
        val file = File( dir, fileName )
        return if ( file.exists() ) file.toUri() else null
    }

}