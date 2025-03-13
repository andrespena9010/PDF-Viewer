package com.example.pdfviewer.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.repository.Repository
import com.example.pdfviewer.ui.data.Libraries
import com.example.pdfviewer.ui.data.PdfRendererObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.Executors

open class PrincipalViewModel(
    private val repository: Repository = Repository
): ViewModel() {

    private var renderJob: Job? = null
    private var flowJob: Job? = null

    private val _renderAll = MutableStateFlow(false)
    val renderAll: StateFlow<Boolean> = _renderAll.asStateFlow()

    private val _library = MutableStateFlow<Libraries>( Libraries.PdfRenderer )
    val library: StateFlow<Libraries> = _library.asStateFlow()

    private val _selectedPDF = MutableStateFlow( PDF() )
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    private val _pdfBitMaps = MutableStateFlow<List<Bitmap?>>( listOf() )
    val pdfBitMaps: StateFlow<List<Bitmap?>> = _pdfBitMaps.asStateFlow()

    private val _loading = MutableStateFlow( false )
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val pdfRenderer = PdfRendererObject

    private val threadPool = Executors.newFixedThreadPool( 10 )
    private var renderStat = LocalTime.now()

    private var pdfPagesCount = 0

    fun getSize(): Pair<Int, Int>{
        return pdfRenderer.getPageSize()
    }

    fun setRenderAll(renderAll: Boolean ){
        _renderAll.update { renderAll }
    }

    fun setLibrary( library: Libraries ){
        _library.update { library }
        pdfRenderer.init( library )
    }

    fun setSelectedPDF ( pdf: PDF ){
        _selectedPDF.update { pdf }
        saveCopy()
    }

    fun saveCopy(){

        _loading.update { true }

        viewModelScope.launch {

            val uri = repository.exist( selectedPDF.value.fileName )

            if ( uri == null ){

                val it = LocalTime.now()
                Log.i("TIMEPDF", "Inicia carga de internet $it")

                val setUriResponse = async {
                    repository.setUri(
                        url = selectedPDF.value.url,
                        fileName = selectedPDF.value.fileName
                    )
                }.await()
                if ( setUriResponse.savePDFResponse.uri != null ){
                    _selectedPDF.update { current ->
                        current.copy(
                            uri = setUriResponse.savePDFResponse.uri
                        )
                    }

                    pdfRenderer.open( setUriResponse.savePDFResponse.uri!!.toFile() )
                }

                Log.i("TIMEPDF", "Termina carga de internet ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")

            } else {

                val it = LocalTime.now()
                Log.i("TIMEPDF", "Inicia carga almacenamiento $it")

                _selectedPDF.update { current ->
                    current.copy(
                        uri = uri
                    )
                }

                pdfRenderer.open( uri.toFile() )

                Log.i("TIMEPDF", "Termina carga de almacenamiento ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")
            }

            pdfPagesCount = pdfRenderer.pageCount()
            _pdfBitMaps.update { List<Bitmap?>( 1 ) { null } }

            _loading.update { false }

        }
    }

    fun renderFlow( previousIndex: Int, currentIndex: Int, nPages: Int ){

        if ( renderAll.value ){

            renderRange( 0 .. pdfPagesCount -1 , 0..nPages)

        } else {

            if ( currentIndex == 0 ){
                for ( p in 0..nPages - 1){
                    if ( p < pdfPagesCount ){
                        renderBitmap( p, true)
                    }
                }
            }

        }

        if ( pdfBitMaps.value.size > 5 ) {

            val forward = currentIndex >= previousIndex
            val dropRamRenderPage: Int? = if ( forward ){
                if ( currentIndex - nPages >= 0 ){
                    currentIndex - nPages
                } else {
                    null
                }
            } else { // va hacia atras
                if ( currentIndex + nPages < pdfPagesCount ){
                    currentIndex + nPages
                } else {
                    null
                }
            }
            val nextRenderPage: Int? = if ( !forward ){
                if ( currentIndex - nPages > 0 ){
                    currentIndex - nPages
                } else {
                    null
                }
            } else { // va hacia adelante
                if ( currentIndex + nPages < pdfPagesCount ){
                    currentIndex + nPages
                } else {
                    null
                }
            }

            if ( nextRenderPage != null ) renderBitmap( nextRenderPage, true)
            if ( dropRamRenderPage != null ) dropRamPage( dropRamRenderPage )

        }

    }

    fun renderRange (range: IntRange, ramRange: IntRange ){

        renderStat = LocalTime.now()
        Log.i("TIMEPDF", "Inicia renderizado rango $renderStat")

        renderJob = viewModelScope.launch {

            try {
                withContext( Dispatchers.Default ){

                    for ( pageIndex in range ){
                        ensureActive()
                        if ( pageIndex in ramRange ){
                            renderBitmap( pageIndex, true )
                        } else {
                            renderBitmap( pageIndex, false )
                        }
                    }

                }
            } catch ( e: Exception ){
                Log.i("TIMEPDF", "Se cancela el renderizado... ${e.message}")
            }
        }


    }

    fun cancelLoad(){
        renderJob?.cancel()
        pdfRenderer.close()
    }

    fun dropRamPage( pageIndex: Int ){
        _pdfBitMaps.update { current ->
            val list = current.toMutableList()
            list[ pageIndex ] = null
            list.toList()
        }
    }

    fun renderBitmap( pageIndex: Int , ram: Boolean ) {

        val bitmapName = pdfRenderer.getBitmapName( selectedPDF.value.fileName, pageIndex )

        if ( repository.exist( bitmapName ) != null ){

            val it = LocalTime.now()
            Log.i("TIMEPDF", "Carga de ---> (CACHE PAGINA $pageIndex)")

            val bitmap = repository.loadCacheBitmap( bitmapName ) ?: createBitmap( 100, 100 )

            if ( ram ) {

                _pdfBitMaps.update { current ->
                    val list = current.toMutableList()
                    if ( pageIndex < list.size ){
                        list[ pageIndex ] = bitmap
                    } else {
                        list.add( bitmap )
                    }
                    list.toList()
                }

            }

            Log.i("TIMEPDF", "Pagina $bitmapName reutilizada de ---> (CACHE PAGINA $pageIndex), tiempo de carga: ${
                Duration.between(
                    it,
                    LocalTime.now()
                ).toMillis()
            } Milisegundos\n")

            if ( pageIndex == pdfPagesCount - 1 ){
                Log.i("TIMEPDF", "Termina renderizado rango Tiempo total: ${Duration.between( renderStat, LocalTime.now()).toMillis()} Milisegundos, pagina: $pageIndex\n")
            }

        } else {

            _pdfBitMaps.update { current ->
                val list = current.toMutableList()
                if ( pageIndex < list.size ){
                    list[ pageIndex ] = null
                } else {
                    list.add( null )
                }

                list.toList()
            }


            threadPool.execute {

                // TODO: este bloque irira en hilos

                val it = LocalTime.now()
                Log.i("TIMEPDF", "Inicia renderizado ->>> (RENDER PAGINA $pageIndex)")

                val bitmap = pdfRenderer.renderBitmap( pageIndex )
                repository.saveCacheBitmap( bitmap, bitmapName )

                _pdfBitMaps.update { current ->
                    val list = current.toMutableList()
                    list[ pageIndex ] = bitmap
                    list.toList()
                }

                Log.i("TIMEPDF", "Termina renderizado ->>> (RENDER PAGINA $pageIndex) ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")

                if ( pageIndex == pdfPagesCount - 1 ){
                    Log.i("TIMEPDF", "Termina renderizado rango Tiempo total: ${Duration.between( renderStat, LocalTime.now()).toMillis()} Milisegundos, pagina: $pageIndex\n")
                }

                // TODO: este bloque irira en hilos

            }

        }

    }

}

object PViewModel : PrincipalViewModel()