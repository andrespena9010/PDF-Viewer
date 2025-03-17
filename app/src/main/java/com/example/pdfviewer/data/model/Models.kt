package com.example.pdfviewer.data.model

import android.graphics.Bitmap
import android.net.Uri

data class PDF (
    var fileName: String = "",
    val url: String,
    val uri: Uri? = null
){
    init {
        fileName = url.split("/").last()
    }
}

data class PdfPage(
    val bitmap: Bitmap?,
    val pageLoading: Boolean,
    val cachedBitmap: Boolean
)

data class SavePDFResponse (
    var success: Boolean = false,
    var message: String = "",
    var uri: Uri? = null,
    val exceptions: MutableList<Throwable> = mutableListOf()
)

data class GetPDFResponse (
    var success: Boolean = false,
    var message: String = "",
    var data: ByteArray = byteArrayOf(),
    val exceptions: MutableList<Throwable> = mutableListOf()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GetPDFResponse

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

data class SetUriResponse (
    val getPDFResponse: GetPDFResponse,
    val savePDFResponse: SavePDFResponse
)