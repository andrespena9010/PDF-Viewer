package com.example.pdfviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.model.SetUriResponse
import com.example.pdfviewer.data.repository.Repository
import com.example.pdfviewer.ui.data.Libraries
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class PrincipalViewModel(
    private val repository: Repository = Repository
): ViewModel() {

    private val _cached = MutableStateFlow(false)
    val cached: StateFlow<Boolean> = _cached.asStateFlow()

    private val _library = MutableStateFlow<Libraries>( Libraries.PdfRenderer )
    val library: StateFlow<Libraries> = _library.asStateFlow()

    private val _selectedPDF = MutableStateFlow( PDF() )
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    fun setCache( cache: Boolean ){
        _cached.update { cache }
    }

    fun setLibrary( library: Libraries ){
        _library.update { library }
    }

    fun setSelectedPDF ( pdf: PDF ){
        _selectedPDF.update { pdf }
    }

    fun saveCopy(){
        viewModelScope.launch {
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
            }
        }
    }

}

object PViewModel : PrincipalViewModel()