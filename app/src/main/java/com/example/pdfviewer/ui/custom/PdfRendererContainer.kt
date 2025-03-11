package com.example.pdfviewer.ui.custom

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pdfviewer.ui.viewmodel.PViewModel
import java.io.File
import java.net.URI


@Composable
fun PdfRendererContainer(
    viewModel: PViewModel = PViewModel,
    nav: NavController = rememberNavController()
) {

    val pdf by viewModel.selectedPDF.collectAsStateWithLifecycle()

    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect( pdf ) {
        pdf.uri?.let {
            val pdfFile = File( URI( pdf.uri.toString() ) )
            val parcelFileDescriptor = ParcelFileDescriptor.open( pdfFile , ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer( parcelFileDescriptor )
            val page = pdfRenderer.openPage( 0 )

            val renderBitmap = createBitmap( page.width, page.height )
            page.render(renderBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            bitmap.value = renderBitmap
            page.close()
            pdfRenderer.close()
            parcelFileDescriptor.close()
        }
    }

    bitmap.value?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "PDF Page",
            modifier = Modifier.fillMaxSize()
        )
    }
}