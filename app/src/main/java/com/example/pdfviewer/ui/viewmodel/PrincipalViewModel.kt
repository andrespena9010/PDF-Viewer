package com.example.pdfviewer.ui.viewmodel

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.currentComposer
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

    private val _cached = MutableStateFlow(false)
    val cached: StateFlow<Boolean> = _cached.asStateFlow()

    private val _library = MutableStateFlow<Libraries>( Libraries.PdfRenderer )
    val library: StateFlow<Libraries> = _library.asStateFlow()

    private val _selectedPDF = MutableStateFlow( PDF() )
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    private val _pdfBitMaps = MutableStateFlow<List<Pair<Bitmap?, Boolean>>>( listOf() )
    val pdfBitMaps: StateFlow<List<Pair<Bitmap?, Boolean>>> = _pdfBitMaps.asStateFlow()

    private val _loading = MutableStateFlow( false )
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var pdfRenderer: PdfRenderer
    private val density = Resources.getSystem().displayMetrics.density

    fun setCache( cache: Boolean ){
        _cached.update { cache }
    }

    fun setLibrary( library: Libraries ){
        _library.update { library }
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

            if ( uri == null || !cached.value ){

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

    fun renderPage( pageIndex: Int ){

        viewModelScope.launch {
            withContext( Dispatchers.Default ){

                val it = LocalTime.now()
                Log.i("TIMEPDF", "Inicia renderizado pagina $it")

                if ( pdfBitMaps.value[ pageIndex ].first == null ){ // Renderiza solo los que no estan renderizados anteriormente

                    _pdfBitMaps.update { current ->
                        val list = current.toMutableList()
                        list[ pageIndex ] = Pair( null, true )
                        list.toList()
                    }

                    val page = pdfRenderer.openPage( pageIndex )
                    val bitmap = createBitmap( ( page.width * density ).toInt(), ( page.height * density ).toInt() )

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    page.close()

                    _pdfBitMaps.update { current ->
                        val list = current.toMutableList()
                        list[ pageIndex ] = Pair( bitmap, false )
                        list.toList()
                    }

                } else {
                    Log.i("TIMEPDF", "Bitmap reutilizado\n")
                }

                Log.i("TIMEPDF", "Termina renderizado pagina ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")

            }
        }
    }

    fun renderRange ( range: IntRange ){

        renderJob = viewModelScope.launch {

            withContext( Dispatchers.Default ){

                val it = LocalTime.now()
                Log.i("TIMEPDF", "Inicia renderizado rango $it")

                for ( pageIndex in range ){

                    if ( pdfBitMaps.value[ pageIndex ].first == null ){ // Renderiza solo los que no estan renderizados anteriormente

                        _pdfBitMaps.update { current ->
                            val list = current.toMutableList()
                            list[ pageIndex ] = Pair( null, true )
                            list.toList()
                        }

                        val page = pdfRenderer.openPage( pageIndex )
                        val bitmap = createBitmap( ( page.width * density ).toInt(), ( page.height * density ).toInt() )

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        page.close()

                        _pdfBitMaps.update { current ->
                            val list = current.toMutableList()
                            list[ pageIndex ] = Pair( bitmap, false )
                            list.toList()
                        }

                    } else {
                        Log.i("TIMEPDF", "Bitmap reutilizado\n")
                    }
                }

                Log.i("TIMEPDF", "Termina renderizado rango ${Duration.between( it, LocalTime.now()).toMillis()} Milisegundos\n")

            }
        }

    }

}

object PViewModel : PrincipalViewModel()