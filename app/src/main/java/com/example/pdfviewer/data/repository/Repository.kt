package com.example.pdfviewer.data.repository

import com.example.pdfviewer.data.local.LocalData
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.model.SavePDFResponse
import com.example.pdfviewer.data.model.SetUriResponse
import com.example.pdfviewer.data.remote.Okhttp3

object Repository {

    private val web = Okhttp3

    private val local = LocalData

    suspend fun setUri ( pdf: PDF ): SetUriResponse {
        val getResponse = web.getPDF( pdf.url )
        var saveResponse = SavePDFResponse()

        if ( getResponse.success ){
            saveResponse = local.savePDF( pdf.name, getResponse.data )
            getResponse.data = byteArrayOf()
        }
        return SetUriResponse( getPDFResponse = getResponse, savePDFResponse = saveResponse)
    }

}