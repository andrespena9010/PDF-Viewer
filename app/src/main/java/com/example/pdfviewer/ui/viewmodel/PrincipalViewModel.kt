package com.example.pdfviewer.ui.viewmodel

import android.content.res.Resources
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.model.PdfPage
import com.example.pdfviewer.data.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.Executors

/**
 * ViewModel principal para la gestión de PDFs.
 *
 * @property repository Repositorio para la gestión de datos.
 */
open class PrincipalViewModel(
    private val repository: Repository = Repository
) : ViewModel() {

    private val deviceWidth = Resources.getSystem().displayMetrics.widthPixels

    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var pdfRenderer: PdfRenderer
    private val openPages = mutableMapOf<Int, PdfRenderer.Page>()

    private var pdfName = ""

    /**
     * Estado del PDF seleccionado.
     */
    private val _selectedPDF = MutableStateFlow(PDF())
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    /**
     * Estado de carga de las páginas del PDF.
     */
    private val _pdfPages = MutableStateFlow<List<PdfPage>>(listOf())
    val pdfPages: StateFlow<List<PdfPage>> = _pdfPages.asStateFlow()

    /**
     * Estado de carga general.
     */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val loadThreadPool = Executors.newFixedThreadPool(2)
    private val renderThreadPool = Executors.newFixedThreadPool(2)
    private var renderStat = LocalTime.now()
    private var renderAllComplete = false

    private var pagesCount = 0
    private var pagesRendered = 0

    private var visibleRange = 0 .. 5

    /**
     * Establece el PDF seleccionado y guarda una copia.
     *
     * Esta función actualiza el estado del PDF seleccionado y llama a [saveCopy] para guardar una copia del PDF.
     *
     * @param pdf PDF a seleccionar.
     */
    fun setSelectedPDF(pdf: PDF) {
        _selectedPDF.update { pdf }
        saveCopy()
    }

    /**
     * Guarda una copia del PDF seleccionado.
     *
     * Esta función verifica si el PDF ya existe en el repositorio. Si no existe, lo descarga y lo guarda.
     * Luego, abre el PDF con [pdfRenderer] y actualiza los estados de carga y páginas del PDF.
     */
    private fun saveCopy() {
        _loading.update { true }

        viewModelScope.launch {
            val uri = repository.exist(selectedPDF.value.fileName)

            if (uri == null) {
                val setUriResponse = async {
                    repository.downLoadDocument(
                        url = selectedPDF.value.url,
                        fileName = selectedPDF.value.fileName
                    )
                }.await()
                if (setUriResponse.savePDFResponse.uri != null) {
                    _selectedPDF.update { current ->
                        current.copy(
                            uri = setUriResponse.savePDFResponse.uri
                        )
                    }
                    val file = setUriResponse.savePDFResponse.uri!!.toFile()
                    parcelFileDescriptor = ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    pdfRenderer = PdfRenderer(parcelFileDescriptor)
                    pdfName = file.name
                }
            } else {
                _selectedPDF.update { current ->
                    current.copy(
                        uri = uri
                    )
                }
                val file = uri.toFile()
                parcelFileDescriptor = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                pdfRenderer = PdfRenderer(parcelFileDescriptor)
                pdfName = file.name
            }

            pagesCount = pdfRenderer.pageCount
            _pdfPages.update { List<PdfPage>( pagesCount ){ PdfPage( bitmap = null, pageLoading = true, cachedBitmap = false) } }

            _loading.update { false }

            renderDocument()

        }
    }

    /**
     * Carga un flujo de páginas del PDF.
     *
     * Esta función carga un rango de páginas del PDF alrededor del índice actual.
     * Cancela cualquier trabajo de flujo existente antes de iniciar uno nuevo.
     *
     * @param currentIndex Índice de la página actual.
     * @param nPages Número de páginas a cargar.
     */
    fun loadFlow(currentIndex: Int, nPages: Int) {

        val less = (nPages / 2)
        val plus = (nPages % 2) + (nPages / 2)
        val rang = ((currentIndex - less)..(currentIndex + plus))

        visibleRange = rang

        viewModelScope.launch(Dispatchers.Default) {

            if (rang.first - 1 >= 0) {
                dropRamPage(rang.first - 1)
            }

            for (index in rang) {
                if (index in 0..<pagesCount) {
                    loadBitmap(index)
                }
            }

            if (rang.last + 1 < pagesCount) {
                dropRamPage(rang.last + 1)
            }
        }

    }

    /**
     * Renderiza el documento PDF.
     *
     * Esta función renderiza todas las páginas del PDF y guarda los bitmaps en el repositorio.
     * Actualiza los estados de carga y páginas del PDF.
     */
    private fun renderDocument() {
        renderStat = LocalTime.now()
        Log.i("TIMEPDF", "Inicia renderizado del Documento $renderStat")

        pagesRendered = 0

        viewModelScope.launch {
            try {
                for (pageIndex in 0..<pagesCount) {

                    val bitmapName = "${selectedPDF.value.fileName}_$pageIndex.png"

                    if (repository.exist(bitmapName) == null) {

                        renderThreadPool.execute {

                            val it = LocalTime.now()
                            Log.i("TIMEPDF", "Inicia renderizado ->>> (RENDER PAGINA $pageIndex)")

                            _pdfPages.update { current ->
                                val list = current.toMutableList()
                                list[ pageIndex ] = list[ pageIndex ].copy( pageLoading = true )
                                list
                            }

                            val page = pdfRenderer.openPage( pageIndex )
                            openPages[ pageIndex ] = page
                            val size = fixImage(width = page.width, height = page.height, newWidth = deviceWidth)

                            val bitmap = createBitmap(size.first, size.second)

                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )

                            page.close()
                            openPages.remove(pageIndex)

                            CoroutineScope( Dispatchers.IO ).launch {
                                repository.saveCacheBitmap(bitmap, bitmapName)
                            }

                            if ( pageIndex in  visibleRange ){

                                _pdfPages.update { current ->
                                    val list = current.toMutableList()
                                    list[ pageIndex ] = list[ pageIndex ].copy( bitmap = bitmap )
                                    list
                                }

                            }

                            _pdfPages.update { current ->
                                val list = current.toMutableList()
                                list[ pageIndex ] = list[ pageIndex ].copy( pageLoading = false )
                                list
                            }

                            Log.i("TIMEPDF", "Termina renderizado ->>> (RENDER PAGINA $pageIndex) ${Duration.between(it, LocalTime.now()).toMillis()} Milisegundos\n")
                            pagesRendered++

                            if (pagesRendered == pagesCount) {
                                Log.i("TIMEPDF", "---- Termina renderizado TOTAL Tiempo total ---- : ${Duration.between(renderStat, LocalTime.now()).toMillis()} Milisegundos, pagina: $pagesCount")
                            }

                        }

                    } else {

                        if ( pageIndex in  visibleRange ){
                            loadBitmap( pageIndex )
                        }

                        Log.i("TIMEPDF", "La pagina $pageIndex ya esta guardada")
                        pagesRendered++

                        if (pagesRendered == pagesCount) {
                            Log.i("TIMEPDF", "---- Termina renderizado TOTAL Tiempo total ---- : ${Duration.between(renderStat, LocalTime.now()).toMillis()} Milisegundos, pagina: $pagesCount")
                        }
                    }

                }
            } catch (e: Exception) {
                Log.i("TIMEPDF", "Se cancela el renderizado... ${e.message}")
            }
        }
    }

    fun fixImage(width: Int, height: Int, newWidth: Int): Pair<Int, Int> {
        val newHeight = (height * newWidth) / width
        return Pair(newWidth, newHeight)
    }

    /**
     * Cancela la carga del documento.
     *
     * Esta función cancela el trabajo de renderizado y cierra el renderizador de PDF.
     */
    fun closeResources() {
        renderAllComplete = false
        openPages.forEach { it.value.close() }
        openPages.clear()
        parcelFileDescriptor.close()
        pdfRenderer.close()
    }

    /**
     * Libera una página de la memoria RAM.
     *
     * Esta función establece el bitmap de una página específica a null para liberar memoria.
     *
     * @param pageIndex Índice de la página a liberar.
     */
    private fun dropRamPage(pageIndex: Int) {
        _pdfPages.update { current ->
            val list = current.toMutableList()
            list[ pageIndex ] = list[ pageIndex ].copy( bitmap = null )
            list
        }
    }

    /**
     * Carga un bitmap de una página del PDF.
     *
     * Esta función carga el bitmap de una página específica desde el repositorio si no está ya cargado.
     *
     * @param pageIndex Índice de la página a cargar.
     */
    private fun loadBitmap(pageIndex: Int) {

        loadThreadPool.execute {
            CoroutineScope(Dispatchers.Default).launch {

                if ( pdfPages.value[pageIndex].bitmap == null) {

                    val bitmapName = "${selectedPDF.value.fileName}_$pageIndex.png"

                    if (repository.exist(bitmapName) != null) {

                        if ( pageIndex in visibleRange ){

                            val bitmap = repository.loadCacheBitmap(bitmapName)

                            _pdfPages.update { current ->
                                val list = current.toMutableList()
                                list[ pageIndex ] = list[ pageIndex ].copy( bitmap = bitmap )
                                list
                            }

                        }

                    }
                }

            }
        }

    }
}

/**
 * Objeto singleton para el ViewModel principal.
 */
object PViewModel : PrincipalViewModel()
