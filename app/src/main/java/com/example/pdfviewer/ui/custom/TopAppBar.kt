package com.example.pdfviewer.ui.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.theme.PDFViewerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarBack(
    title: String,
    nav: NavController = rememberNavController()
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ){
                Text( title )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    nav.popBackStack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = ""
                )
            }
        }
    )
}

@Preview
@Composable
private fun TopAppBarBackPreview(){
    PDFViewerTheme {
        TopAppBarBack (
            title = "Titulo"
        )
    }
}