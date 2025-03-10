package com.example.pdfviewer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.view.Principal
import kotlinx.serialization.Serializable

interface NoParamView

@Serializable
sealed class Views {

    @Serializable
    object Principal : NoParamView
}

@Composable
fun Navigation(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Views.Principal
    ){
        composable<Views.Principal>{
            PDFViewerTheme {
                Principal()
            }
        }
    }

}