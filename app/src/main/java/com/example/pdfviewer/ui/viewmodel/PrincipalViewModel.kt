package com.example.pdfviewer.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.repository.Repository
import com.example.pdfviewer.ui.data.PdfRendererObject
import kotlinx.coroutines.CoroutineScope
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

/**
 * ViewModel principal para la gestión de PDFs.
 *
 * @property repository Repositorio para la gestión de datos.
 */
open class PrincipalViewModel(
    private val repository: Repository = Repository
) : ViewModel() {

    private var renderJob: Job? = null
    private var flowJob: Job? = null

    /**
     * Estado del PDF seleccionado.
     */
    private val _selectedPDF = MutableStateFlow(PDF())
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    /**
     * Estado de carga de las páginas del PDF.
     */
    private val _pdfPagesLoading = MutableStateFlow<List<Boolean>>(listOf())
    val pdfPagesLoading: StateFlow<List<Boolean>> = _pdfPagesLoading.asStateFlow()

    /**
     * Bitmaps de las páginas del PDF.
     */
    private val _pdfBitMaps = MutableStateFlow<List<Bitmap?>>(listOf())
    val pdfBitMaps: StateFlow<List<Bitmap?>> = _pdfBitMaps.asStateFlow()

    /**
     * Estado de carga general.
     */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val pdfRenderer = PdfRendererObject

    private val loadThreadPool = Executors.newFixedThreadPool(10)
    private var renderStat = LocalTime.now()
    private var renderAllComplete = false

    private var pdfPagesCount = 0
    private var pagesRendered = 0

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
    fun saveCopy() {
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
                    pdfRenderer.open(setUriResponse.savePDFResponse.uri!!.toFile())
                }
            } else {
                _selectedPDF.update { current ->
                    current.copy(
                        uri = uri
                    )
                }
                pdfRenderer.open(uri.toFile())
            }

            pdfPagesCount = pdfRenderer.pageCount()
            _pdfBitMaps.update { List<Bitmap?>(pdfPagesCount) { null } }
            _pdfPagesLoading.update { List<Boolean>(pdfPagesCount) { false } }
            renderDocument()
            _loading.update { false }
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
        flowJob?.let { job ->
            if (job.isActive) {
                job.cancel()
            }
        }

        flowJob = viewModelScope.launch(Dispatchers.Default) {
            val less = (nPages / 2)
            val plus = (nPages % 2) + (nPages / 2)
            val rang = ((currentIndex - less)..(currentIndex + plus))

            if (rang.first - 1 >= 0) {
                dropRamPage(rang.first - 1)
            }

            for (index in rang) {
                if (index in 0..pdfPagesCount - 1) {
                    loadBitmap(index)
                }
            }

            if (rang.last + 1 < pdfPagesCount) {
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
    fun renderDocument() {
        renderStat = LocalTime.now()
        Log.i("TIMEPDF", "Inicia renderizado del Documento $renderStat")

        pagesRendered = 0

        renderJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    for (pageIndex in 0..pdfPagesCount - 1) {
                        val it = LocalTime.now()
                        Log.i("TIMEPDF", "Inicia renderizado ->>> (RENDER PAGINA $pageIndex)")

                        val bitmapName = "${selectedPDF.value.fileName}_$pageIndex"

                        renderJob?.let {
                            if (renderJob!!.isActive) {
                                if (repository.exist(bitmapName) == null) {
                                    _pdfPagesLoading.update { current ->
                                        val list = current.toMutableList()
                                        list[pageIndex] = true
                                        list
                                    }

                                    ensureActive()

                                    val bitmap = pdfRenderer.renderBitmap(pageIndex)
                                    repository.saveCacheBitmap(bitmap, bitmapName)

                                    _pdfPagesLoading.update { current ->
                                        val list = current.toMutableList()
                                        list[pageIndex] = false
                                        list
                                    }
                                }
                            }
                        }

                        Log.i("TIMEPDF", "Termina renderizado ->>> (RENDER PAGINA $pageIndex) ${Duration.between(it, LocalTime.now()).toMillis()} Milisegundos\n")
                        pagesRendered++

                        if (pagesRendered == pdfPagesCount) {
                            Log.i("TIMEPDF", "---- Termina renderizado TOTAL Tiempo total ---- : ${Duration.between(renderStat, LocalTime.now()).toMillis()} Milisegundos, pagina: $pdfPagesCount")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.i("TIMEPDF", "Se cancela el renderizado... ${e.message}")
            }
        }
    }

    /**
     * Cancela la carga del documento.
     *
     * Esta función cancela el trabajo de renderizado y cierra el renderizador de PDF.
     */
    fun cancelLoad() {
        renderAllComplete = false
        renderJob?.cancel()
        pdfRenderer.close()
    }

    /**
     * Libera una página de la memoria RAM.
     *
     * Esta función establece el bitmap de una página específica a null para liberar memoria.
     *
     * @param pageIndex Índice de la página a liberar.
     */
    fun dropRamPage(pageIndex: Int) {
        _pdfBitMaps.update { current ->
            val list = current.toMutableList()
            list[pageIndex] = null
            list.toList()
        }
    }

    /**
     * Carga un bitmap de una página del PDF.
     *
     * Esta función carga el bitmap de una página específica desde el repositorio si no está ya cargado.
     *
     * @param pageIndex Índice de la página a cargar.
     */
    fun loadBitmap(pageIndex: Int) {
        if (pdfBitMaps.value[pageIndex] == null) {
            val bitmapName = "${selectedPDF.value.fileName}_$pageIndex"

            if (repository.exist(bitmapName) != null) {
                loadThreadPool.execute {
                    CoroutineScope(Dispatchers.Default).launch {
                        val bitmap = repository.loadCacheBitmap(bitmapName) ?: createBitmap(100, 100)

                        _pdfBitMaps.update { current ->
                            val list = pdfBitMaps.value.toMutableList()
                            list[pageIndex] = bitmap
                            list.toList()
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
