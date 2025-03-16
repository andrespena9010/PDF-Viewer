package com.example.pdfviewer.ui.custom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.pdfviewer.ui.viewmodel.PViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Composable que contiene el renderizador de PDF.
 *
 * @param viewModel ViewModel para la gestión de PDFs.
 */
@Composable
fun PdfRendererContainer(viewModel: PViewModel = PViewModel) {
    // Observa los estados de carga y bitmaps del PDF del ViewModel
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val pdfPages by viewModel.pdfPages.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    var lastVisibleItem = 0

    val totalRange = 10

    // Efecto lanzado cuando el estado de la lista cambia
    LaunchedEffect( pdfPages, state ) {

        if (!loading) {

            snapshotFlow { state.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collectLatest { first ->

                    if ( lastVisibleItem != first ){

                        viewModel.viewModelScope.launch {
                            withContext(Dispatchers.Default) {

                                viewModel.loadFlow(first, totalRange)
                                lastVisibleItem = first

                            }
                        }

                    }

                }

        }
    }

    // Muestra un indicador de carga mientras se cargan los PDFs
    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(150.dp),
                strokeWidth = 10.dp,
                color = Color.White
            )
        }
    } else {
        // Muestra las páginas del PDF en una lista desplazable
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items( pdfPages ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (page.bitmap != null) {
                        // Muestra la imagen de la página del PDF
                        Image(
                            bitmap = page.bitmap.asImageBitmap(),
                            contentDescription = "PDF Page",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .shadow(20.dp)
                                .background(Color.White),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Muestra un indicador de carga mientras se carga la página del PDF
                        Box(
                            modifier = Modifier
                                .size(500.dp)
                                .padding(10.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (page.pageLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size((LocalView.current.width * 0.3).dp),
                                    strokeWidth = 20.dp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
