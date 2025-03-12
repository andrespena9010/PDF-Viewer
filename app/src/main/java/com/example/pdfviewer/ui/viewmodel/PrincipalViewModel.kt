package com.example.pdfviewer.ui.viewmodel

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.repository.Repository
import com.example.pdfviewer.ui.data.Libraries
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

open class PrincipalViewModel(
    private val repository: Repository = Repository
): ViewModel() {

    private var renderJob: Job? = null

    private val _renderAll = MutableStateFlow(false)
    val renderAll: StateFlow<Boolean> = _renderAll.asStateFlow()

    private val _library = MutableStateFlow<Libraries>( Libraries.PdfRenderer )
    val library: StateFlow<Libraries> = _library.asStateFlow()

    private val _selectedPDF = MutableStateFlow( PDF() )
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    private val _pdfBitMaps = MutableStateFlow<List<Pair<Bitmap?, Boolean>>>( listOf() )
    val pdfBitMaps: StateFlow<List<Pair<Bitmap?, Boolean>>> = _pdfBitMaps.asStateFlow()

    private val _loading = MutableStateFlow( false )
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var pdfRenderer: PdfRenderer // sub clase hereda
    private val deviceWidth = Resources.getSystem().displayMetrics.widthPixels

    fun setRenderAll(renderAll: Boolean ){
        _renderAll.update { renderAll }
    }

    fun setLibrary( library: Libraries ){
        _library.update { library }
        // configurar renderer con una sub clase que implemente las demas.
    }

    fun setSelectedPDF ( pdf: PDF ){
        _pdfBitMaps.update { listOf() }
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
                    parcelFileDescriptor = ParcelFileDescriptor.open(
                        setUriResponse.savePDFResponse.uri!!.toFile(),
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    pdfRenderer = PdfRenderer( parcelFileDescriptor )
                    _pdfBitMaps.update {
                        val list: MutableList<Pair<Bitmap?, Boolean>> = mutableListOf()
                        repeat( pdfRenderer.pageCount ){
                            list.add( Pair(null, false) )
                        }
                        list.toList()
                    }
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
                parcelFileDescriptor = ParcelFileDescriptor.open(
                    uri.toFile(),
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                pdfRenderer = PdfRenderer( parcelFileDescriptor )
                _pdfBitMaps.update {
                    val list: MutableList<Pair<Bitmap?, Boolean>> = mutableListOf()
                    repeat( pdfRenderer.pageCount ){
                        list.add( Pair(null, false) )
                    }
                    list.toList()
                }

                Log.i("TIMEPDF", "Termina carga de almacenamiento ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")
            }

            _loading.update { false }

        }
    }

    fun renderFlow(previousIndex: Int, currentIndex: Int ){

        val range = 6
        val forward = currentIndex >= previousIndex
        val dropRamRenderPage: Int? = if ( forward ){
            if ( currentIndex - range > 0 ){
                currentIndex - range
            } else {
                null
            }
        } else {
            if ( currentIndex + range < pdfBitMaps.value.size ){
                currentIndex + range
            } else {
                null
            }
        }
        val nextRenderPage: Int? = if ( !forward ){
            if ( currentIndex - range > 0 ){
                currentIndex - range
            } else {
                null
            }
        } else {
            if ( currentIndex + range < pdfBitMaps.value.size ){
                currentIndex + range
            } else {
                null
            }
        }

        viewModelScope.launch {
            withContext( Dispatchers.Default ){

                if ( currentIndex == 0 && forward ){
                    for ( p in 0..range){
                        if ( p < pdfBitMaps.value.size ){
                            renderPage( p, true)
                        }
                    }
                } else {
                    if ( nextRenderPage != null ) renderPage( nextRenderPage, true)
                    if ( dropRamRenderPage != null ) dropRamPage( dropRamRenderPage )
                }

            }
        }
    }

    fun gcd( width: Int, height: Int): Int {
        return if (height == 0) width else gcd(height, width % height)
    }

    fun fixImage( width: Int, height: Int, newWidth: Int): Pair<Int, Int> {
        val newHeight = (height * newWidth) / width
        return Pair( newWidth, newHeight )
    }

    fun renderRange ( range: IntRange ){

        renderJob = viewModelScope.launch {

            try {
                withContext( Dispatchers.Default ){

                    val it = LocalTime.now()
                    Log.i("TIMEPDF", "Inicia renderizado rango $it")

                    for ( pageIndex in range ){
                        ensureActive()
                        renderPage( pageIndex, false )
                    }

                    Log.i("TIMEPDF", "Termina renderizado rango ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")

                }
            } catch ( e: Exception ){
                Log.i("TIMEPDF", "Se cancela el renderizado... ${e.message}")
            }
        }

    }

    fun cancelLoad(){
        renderJob?.cancel()
    }

    fun renderPage( pageIndex: Int, ram: Boolean){

        if ( ram ){

            _pdfBitMaps.update { current ->
                val list = current.toMutableList()
                list[ pageIndex ] = Pair( list[ pageIndex ].first, true )
                list.toList()
            }

            val bitmap = renderBitmap( pageIndex )

            _pdfBitMaps.update { current ->
                val list = current.toMutableList()
                list[ pageIndex ] = Pair( bitmap, false )
                list.toList()
            }

        } else {
            renderBitmap( pageIndex )
        }

    }

    fun dropRamPage( pageIndex: Int ){
        _pdfBitMaps.update { current ->
            val list = current.toMutableList()
            list[ pageIndex ] = Pair( null, false )
            list.toList()
        }
    }

    fun renderBitmap( pageIndex: Int ): Bitmap {

        val it = LocalTime.now()
        Log.i("TIMEPDF", "Inicia renderizado de la pagina $pageIndex")

        val page = pdfRenderer.openPage( pageIndex )
        val gcd = gcd( width = page.width, height = page.height )
        val aspectRatio = Pair( first = page.width/gcd, second = page.height/gcd )
        val size = fixImage( width = page.width, height = page.height, newWidth = deviceWidth)
        val bitmapName = "${selectedPDF.value.fileName}_${pageIndex}_$aspectRatio"
        var bitmap: Bitmap

        if ( repository.exist( bitmapName ) != null ){

            bitmap = repository.loadCacheBitmap( bitmapName ) ?: createBitmap( 100, 100 )

            Log.i("TIMEPDF", "Pagina $bitmapName reutilizada tiempo de carga: ${
                Duration.between(
                    it,
                    LocalTime.now()
                ).toMillis()
            } Milisegundos\n")

        } else {

            bitmap = createBitmap( size.first, size.second )

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            repository.saveCacheBitmap( bitmap, bitmapName )

        }

        page.close()

        Log.i("TIMEPDF", "Termina renderizado pagina ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")

        return bitmap
    }

}

object PViewModel : PrincipalViewModel()