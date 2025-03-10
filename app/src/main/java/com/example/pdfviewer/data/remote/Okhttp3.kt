package com.example.pdfviewer.data.remote

import android.util.Log
import com.example.pdfviewer.data.model.GetPDFResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.PrintStream

object Okhttp3 {

    private val client = OkHttpClient()

    suspend fun getPDF( url: String ): GetPDFResponse {

        val request = Request.Builder().url( url ).build()
        val res = GetPDFResponse()

        withContext( Dispatchers.IO ){
            try {
                client.newCall( request ).execute().use { response ->
                    if ( response.isSuccessful ) {
                        if ( response.body != null ) {
                            res.data = response.body!!.bytes()
                            res.success = true
                        } else {
                            res.success = false
                            res.message = "Error on network."
                        }
                    }
                }
            } catch ( e: Exception ){
                res.success = false
                res.message = "Error"
                res.exceptions.add( e )
                val err = ""
                e.printStackTrace( PrintStream( err ) )
                Log.e("Okhttp3.getPDF() -> ", err)
            }
        }

        return res
    }

}