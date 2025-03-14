package com.example.pdfviewer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.view.Libraries
import com.example.pdfviewer.ui.view.PDFList
import com.example.pdfviewer.ui.view.PDFViewer
import kotlinx.serialization.Serializable

interface NoParamView

@Serializable
sealed class Views {

    @Serializable
    object Libraries : NoParamView

    @Serializable
    object PDFList : NoParamView

    @Serializable
    object PDFViewer : NoParamView

}

@Composable
fun Navigation(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Views.Libraries
    ){

        composable<Views.Libraries>{
            PDFViewerTheme {
                Libraries( nav = navController )
            }
        }

        composable<Views.PDFList>{
            PDFViewerTheme {
                PDFList( nav = navController )
            }
        }

        composable<Views.PDFViewer>{
            PDFViewerTheme {
                PDFViewer( nav = navController )
            }
        }

    }

}