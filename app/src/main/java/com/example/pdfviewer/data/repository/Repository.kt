package com.example.pdfviewer.data.repository

import android.net.Uri
import android.util.Log
import com.example.pdfviewer.data.local.LocalData
import com.example.pdfviewer.data.model.GetPDFResponse
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.model.SavePDFResponse
import com.example.pdfviewer.data.model.SetUriResponse
import com.example.pdfviewer.data.remote.Okhttp3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.PrintWriter

object Repository {

    private val supervisor = SupervisorJob()

    @OptIn(DelicateCoroutinesApi::class)
    private val scope = CoroutineScope(GlobalScope.coroutineContext + supervisor)

    private val web = Okhttp3

    private val local = LocalData

    suspend fun setUri ( url: String, fileName: String): SetUriResponse {
        var getResponse = GetPDFResponse()
        val deferredWeb = scope.async {
            getResponse = web.getPDF( url )
        }
        deferredWeb.join()

        var saveResponse = SavePDFResponse()
        val deferredLocal = scope.async {
            if ( getResponse.success ){
                saveResponse = local.savePDF( fileName, getResponse.data )
                getResponse.data = byteArrayOf()
            }
        }
        deferredLocal.join()

        try {
            deferredWeb.await()
            deferredLocal.await()
        } catch ( e: Exception ){
            val err = ""
            withContext( Dispatchers.IO ) {
                e.printStackTrace(PrintWriter(err))
            }
            Log.e("Repository.setUri() -> ", err)
        }

        return SetUriResponse( getPDFResponse = getResponse, savePDFResponse = saveResponse)
    }

    fun exist( fileName: String ): Uri? {
        return local.exist( fileName )
    }

}