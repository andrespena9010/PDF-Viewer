package com.example.pdfviewer.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.custom.TopAppBarBack
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.custom.PdfRendererContainer
import com.example.pdfviewer.ui.viewmodel.PViewModel

/**
 * Composable que muestra el visor de PDFs.
 *
 * @param viewModel ViewModel para la gestión de PDFs.
 * @param nav Controlador de navegación para la vista.
 */
@Composable
fun PDFViewer(
    viewModel: PViewModel = PViewModel,
    nav: NavController = rememberNavController()
) {
    // Observa el PDF seleccionado del ViewModel
    val pdf by viewModel.selectedPDF.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            // Barra superior con título y botón de retroceso
            TopAppBarBack(
                title = pdf.name,
                onCkick = {
                    nav.popBackStack()
                }
            )
        }
    ) { innerPaddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPaddings)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            // Contenedor para renderizar el PDF
            PdfRendererContainer()
        }
    }
}

/**
 * Vista previa del composable PDFViewer en un dispositivo Pixel XL.
 */
@Preview(
    device = Devices.PIXEL_XL,
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewPixel() {
    PDFViewerTheme {
        PDFViewer()
    }
}
