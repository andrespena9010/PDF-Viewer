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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdfviewer.ui.viewmodel.PViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged


@Composable
fun PdfRendererContainer( viewModel: PViewModel = PViewModel ) {

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val pdfBitMaps by viewModel.pdfBitMaps.collectAsStateWithLifecycle()
    val renderAll by viewModel.renderAll.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    var previousPage by remember { mutableIntStateOf( 0 ) }

    LaunchedEffect( loading, state , renderAll) {

        if ( renderAll ){
            if ( !loading ){
                viewModel.renderRange( 0.. pdfBitMaps.size - 1)
            }
        }

        if ( !loading ){

            snapshotFlow { state.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collectLatest { first ->
                    viewModel.renderFlow( previousPage, first )
                    previousPage = first
                }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll( rememberScrollState() ),
            contentAlignment = Alignment.Center
        ){
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
                                .fillMaxWidth()
                                .padding(10.dp)
                                .shadow(20.dp)
                                .background( Color.White ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

}