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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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


@Composable
fun PdfRendererContainer( viewModel: PViewModel = PViewModel ) {

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val pdfBitMaps by viewModel.pdfBitMaps.collectAsStateWithLifecycle()
    val pdfPagesLoading by viewModel.pdfPagesLoading.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    var pdfSize by remember { mutableStateOf( Pair(0,0) ) }

    LaunchedEffect( loading, state ) {

        if ( !loading ){

            viewModel.viewModelScope.launch {
                withContext ( Dispatchers.Default ){
                    snapshotFlow { state.firstVisibleItemIndex }
                        .distinctUntilChanged()
                        .collectLatest { first ->
                            viewModel.renderFlow( first, 10)
                        }
                }
            }

        }

    }

    if ( loading ){

        Box (
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll( rememberScrollState() ),
            contentAlignment = Alignment.Center
        ) {
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
            itemsIndexed ( pdfBitMaps ){ index, bitmap ->

                Box (
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll( rememberScrollState() ),
                    contentAlignment = Alignment.Center
                ){

                    if ( bitmap != null  ){

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .shadow(20.dp)
                                .background( Color.White ),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Box(
                            modifier = Modifier
                                .size( (pdfSize.first * 0.5).dp, (pdfSize.second * 0.5).dp )
                                .background(Color.White),
                            contentAlignment = Alignment.Center

                        ){

                            if ( pdfPagesLoading[ index ] ){

                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(
                                            (LocalView.current.width * 0.3).dp
                                        ),
                                    strokeWidth = 50.dp,
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