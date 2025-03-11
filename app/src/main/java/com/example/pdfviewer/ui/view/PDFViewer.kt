package com.example.pdfviewer.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.custom.TopAppBarBack
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.data.Libraries
import com.example.pdfviewer.ui.custom.PdfRendererContainer
import com.example.pdfviewer.ui.viewmodel.PViewModel

@Composable
fun PDFViewer(
    viewModel: PViewModel = PViewModel,
    nav: NavController = rememberNavController()
) {

    val cached by viewModel.cached.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()

    Scaffold (
        topBar = {
            TopAppBarBack(
                title = "Viewer",
                nav = nav
            )
        }
    ){ innerPaddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding( innerPaddings )
        ){
            when ( library ) {
                Libraries.PdfRenderer -> {
                    if ( cached ) {
                        viewModel.saveCopy()
                        PdfRendererContainer()
                    } else {
                        // solicitar por partes
                    }
                }
                Libraries.PDFBox -> {

                }
                Libraries.MuPDF -> {

                }
            }
        }
    }
}

@Preview(
    device = Devices.PIXEL_XL,
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewPixel(){
    PDFViewerTheme {
        PDFViewer()
    }
}