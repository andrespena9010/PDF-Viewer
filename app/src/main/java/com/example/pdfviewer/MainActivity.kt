package com.example.pdfviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pdfviewer.data.local.LocalData
import com.example.pdfviewer.navigation.Navigation

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalData.setFilesDir( this.cacheDir )
        enableEdgeToEdge()
        setContent {
            Navigation()
        }
    }

}