package com.example.pdfviewer.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.navigation.Views
import com.example.pdfviewer.ui.custom.ButtonPrincipal
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.ui.viewmodel.PViewModel

@Composable
fun Principal(
    viewModel: PViewModel = PViewModel,
    nav: NavController = rememberNavController()
){
    Scaffold(

    ){ innerPaddings ->
        Column(
            modifier = Modifier
                .padding( innerPaddings )
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            ButtonPrincipal(
                text = "Renderizando total",
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxWidth(0.7f)
            ) {
                viewModel.setRenderAll( true )
                nav.navigate( Views.Libraries )
            }
            ButtonPrincipal(
                text = "Renderizado parcial",
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxWidth(0.7f)
            ) {
                viewModel.setRenderAll( false )
                nav.navigate( Views.Libraries )
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
        Principal()
    }
}
