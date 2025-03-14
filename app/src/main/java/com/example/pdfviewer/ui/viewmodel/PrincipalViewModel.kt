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

    private val _library = MutableStateFlow<Libraries>( Libraries.PdfRenderer )
    val library: StateFlow<Libraries> = _library.asStateFlow()

    private val _selectedPDF = MutableStateFlow( PDF() )
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    private val _pdfPagesLoading = MutableStateFlow<List<Boolean>>( listOf() )
    val pdfPagesLoading: StateFlow<List<Boolean>> = _pdfPagesLoading.asStateFlow()

    private val _pdfBitMaps = MutableStateFlow<List<Bitmap?>>( listOf() )
    val pdfBitMaps: StateFlow<List<Bitmap?>> = _pdfBitMaps.asStateFlow()

    private val _loading = MutableStateFlow( false )
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val pdfRenderer = PdfRendererObject

    private val threadPool = Executors.newFixedThreadPool( 10 )
    private var renderStat = LocalTime.now()
    private var renderAllComplete = false

    private var pdfPagesCount = 0
    private var pagesRendered = 0

    fun getSize(): Pair<Int, Int>{
        return pdfRenderer.getPageSize()
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

            _pdfBitMaps.update { List<Bitmap?>( pdfPagesCount ){ null } }
            _pdfPagesLoading.update { List<Boolean>( pdfPagesCount ){ false } }
            renderDocument()

            _loading.update { false }

        }
    }

    fun loadFlow(currentIndex: Int, nPages: Int ){

        flowJob?.let { job ->
            if (job.isActive) {
                job.cancel()
            }
        }

        flowJob = viewModelScope.launch {

            val less = ( nPages / 2 )
            val plus = ( nPages % 2 ) + ( nPages / 2 )
            val rang = ( ( currentIndex - less ) .. ( currentIndex + plus ) )

            if ( rang.first - 1 >= 0 ){
                dropRamPage( rang.first - 1 )
            }

            for ( index in rang ){
                if ( index in 0 .. pdfPagesCount -1 ){
                    loadBitmap( index )
                }
            }

            if ( rang.last + 1 < pdfPagesCount ){
                dropRamPage( rang.last + 1 )
            }

        }

    }

    fun renderDocument(){

        renderStat = LocalTime.now()
        Log.i("TIMEPDF", "Inicia renderizado del Documento $renderStat")

        pagesRendered = 0

        renderJob = viewModelScope.launch {

            try {
                withContext( Dispatchers.Default ){

                    for ( pageIndex in 0 .. pdfPagesCount - 1 ){

                        ensureActive()

                        //threadPool.execute { // TODO: hacer funcionar la concurrencia

                            val it = LocalTime.now()
                            Log.i("TIMEPDF", "Inicia renderizado ->>> (RENDER PAGINA $pageIndex)")

                            val bitmapName = "${selectedPDF.value.fileName}_$pageIndex"

                            renderJob?.let{

                                if ( renderJob!!.isActive ){

                                    if ( repository.exist( bitmapName ) == null ){

                                        _pdfPagesLoading.update { current ->
                                            val list = current.toMutableList()
                                            list[ pageIndex ] = true
                                            list
                                        }

                                        val bitmap = pdfRenderer.renderBitmap( pageIndex )
                                        repository.saveCacheBitmap( bitmap, bitmapName )

                                        _pdfPagesLoading.update { current ->
                                            val list = current.toMutableList()
                                            list[ pageIndex ] = false
                                            list
                                        }

                                    }

                                }

                            }

                            Log.i("TIMEPDF", "Termina renderizado ->>> (RENDER PAGINA $pageIndex) ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")
                            pagesRendered++

                            if ( pagesRendered == pdfPagesCount ){
                                Log.i("TIMEPDF", "---- Termina renderizado TOTAL Tiempo total ---- : ${Duration.between( renderStat, LocalTime.now()).toMillis()} Milisegundos, pagina: $pdfPagesCount")
                            }
                        //}

                    }

                }
            } catch ( e: Exception ){
                Log.i("TIMEPDF", "Se cancela el renderizado... ${e.message}")
            }
        }

    }

    fun cancelLoad(){
        renderAllComplete = false
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

    suspend fun loadBitmap(pageIndex: Int ) {

        if ( pdfBitMaps.value[ pageIndex ] == null ){

            val bitmapName = "${selectedPDF.value.fileName}_$pageIndex"

            if ( repository.exist( bitmapName ) != null ){

                val it = LocalTime.now()
                Log.i("TIMEPDF", "Carga ---> (CACHE PAGINA $pageIndex)")

                val bitmap = repository.loadCacheBitmap( bitmapName ) ?: createBitmap( 100, 100 )

                _pdfBitMaps.update { current ->
                    val list = pdfBitMaps.value.toMutableList()
                    list[ pageIndex ] = bitmap
                    list.toList()
                }

                Log.i("TIMEPDF", "Pagina $bitmapName cargada ---> (CACHE PAGINA $pageIndex), tiempo de carga: ${
                    Duration.between(
                        it,
                        LocalTime.now()
                    ).toMillis()
                } Milisegundos\n")

            }

        }

    }

}

object PViewModel : PrincipalViewModel()