package com.example.pdfviewer.ui.custom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ButtonPrincipal(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
){
    Button(
        modifier = modifier,
        onClick = onClick,
    ){
        Text(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun PreviewButton(){
    ButtonPrincipal( text = "Boton" ) { }
}