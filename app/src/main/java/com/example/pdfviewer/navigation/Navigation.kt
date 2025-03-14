package com.example.pdfviewer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.view.PDFList
import com.example.pdfviewer.ui.view.PDFViewer
import kotlinx.serialization.Serializable

/**
 * Interfaz que representa una vista sin parámetros.
 */
interface NoParamView

/**
 * Sellado de clases que representan las diferentes vistas de la aplicación.
 *
 * @property PDFList Vista que muestra la lista de PDFs.
 * @property PDFViewer Vista que muestra el visor de PDFs.
 */
@Serializable
sealed class Views {

    /**
     * Vista que muestra la lista de PDFs.
     */
    @Serializable
    object PDFList : NoParamView

    /**
     * Vista que muestra el visor de PDFs.
     */
    @Serializable
    object PDFViewer : NoParamView

}

/**
 * Composable principal para la navegación de la aplicación.
 *
 * Este composable configura el controlador de navegación y define las rutas de navegación para las diferentes vistas de la aplicación.
 */
@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Views.PDFList
    ) {

        /**
         * Composable para la vista de la lista de PDFs.
         *
         * @param -navController Controlador de navegación para la vista.
         */
        composable<Views.PDFList> {
            PDFViewerTheme {
                PDFList(nav = navController)
            }
        }

        /**
         * Composable para la vista del visor de PDFs.
         *
         * @param -navController Controlador de navegación para la vista.
         */
        composable<Views.PDFViewer> {
            PDFViewerTheme {
                PDFViewer(nav = navController)
            }
        }

    }
}
