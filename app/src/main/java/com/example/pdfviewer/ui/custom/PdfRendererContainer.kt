package com.example.pdfviewer.ui.custom

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdfviewer.ui.viewmodel.PViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged


@Composable
fun PdfRendererContainer( viewModel: PViewModel = PViewModel ) {

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val pdfBitMaps by viewModel.pdfBitMaps.collectAsStateWithLifecycle()
    val state = rememberLazyListState()

    val pageRender = true

    LaunchedEffect( loading, state ) {

        if ( pageRender ){
            val last = pdfBitMaps.size - 1
            val firstPages = if ( last >= 5 ) 5 else last
            if ( !loading ){
                if ( last >= 0 ){
                    viewModel.renderRange( 0..firstPages)
                    snapshotFlow { state.firstVisibleItemIndex }
                        .distinctUntilChanged()
                        .collectLatest { first ->
                            val next = first + firstPages + 1
                            if ( next < last ){
                                viewModel.renderPage( next )
                            }
                        }
                }
            }
        } else {
            viewModel.renderRange( 0.. pdfBitMaps.size - 1)
        }
    }

    if ( loading ){
        Box {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(150.dp),
                strokeWidth = 10.dp,
                color = Color.White
            )
        }
    } else {
        LazyColumn (
            modifier = Modifier
                .fillMaxSize(),
            state = state,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            items( pdfBitMaps ){ bitmap ->

                if ( bitmap.second == true ){
                    CircularProgressIndicator(
                        strokeWidth = 100.dp
                    )
                } else if ( bitmap.first != null ){
                    Image(
                        bitmap = bitmap.first!!.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .shadow(20.dp)
                            .background( Color.White )
                    )
                }
            }
        }
    }

}