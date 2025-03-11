package com.example.pdfviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.pdfviewer.data.local.LocalData
import com.example.pdfviewer.navigation.Navigation
import com.example.pdfviewer.ui.viewmodel.PViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalData.setFilesDir( this.filesDir )
        enableEdgeToEdge()
        setContent {
            Navigation()
        }
    }

}