package com.example.pdfviewer.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.navigation.Views
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.viewmodel.PViewModel
import com.example.pdfviewer.utils.pdfList

/**
 * Composable que muestra una lista de PDFs.
 *
 * @param viewModel ViewModel para la gestión de PDFs.
 * @param nav Controlador de navegación para la vista.
 */
@Composable
fun PDFList(
    viewModel: PViewModel = PViewModel,
    nav: NavController = rememberNavController()
) {
    Scaffold { innerPaddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPaddings),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pdfList.forEach { pdf ->
                Text(
                    text = pdf.fileName,
                    modifier = Modifier
                        .padding(5.dp)
                        .height(40.dp)
                        .clickable {
                            viewModel.setSelectedPDF(pdf)
                            nav.navigate(Views.PDFViewer)
                        },
                    color = Color.Blue,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

/**
 * Vista previa del composable PDFList en un dispositivo Pixel XL.
 */
@Preview(
    device = Devices.PIXEL_XL,
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewPixel() {
    PDFViewerTheme {
        PDFList()
    }
}
