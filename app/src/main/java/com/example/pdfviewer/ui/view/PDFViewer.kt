package com.example.pdfviewer.ui.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.custom.TopAppBarBack
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.viewmodel.PrincipalViewModel

@Composable
fun PDFViewer(
    viewModel: PrincipalViewModel = viewModel(),
    nav: NavController = rememberNavController()
) {
    Scaffold (
        topBar = {
            TopAppBarBack(
                title = "Viewer",
                nav = nav
            )
        }
    ){ innerPaddings ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding( innerPaddings )
        ){

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