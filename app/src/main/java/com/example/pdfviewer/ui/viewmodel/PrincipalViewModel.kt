package com.example.pdfviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pdfviewer.data.model.PDF
import com.example.pdfviewer.data.repository.Repository
import com.example.pdfviewer.ui.data.Libraries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PrincipalViewModel(
    private val repository: Repository = Repository
): ViewModel() {

    private val _cacheOption = MutableStateFlow(false)
    val cacheOption: StateFlow<Boolean> = _cacheOption.asStateFlow()

    private val _library = MutableStateFlow<Libraries>( Libraries.AndroidX )
    val library: StateFlow<Libraries> = _library.asStateFlow()

    private val _selectedPDF = MutableStateFlow( PDF() )
    val selectedPDF: StateFlow<PDF> = _selectedPDF.asStateFlow()

    fun setCache( cache: Boolean ){
        _cacheOption.update { cache }
    }

    fun setLibrary( library: Libraries ){
        _library.update { library }
    }

    fun setSelectedPDF ( pdf: PDF ){
        _selectedPDF.update { pdf }
    }

}