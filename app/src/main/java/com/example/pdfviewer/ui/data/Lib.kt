package com.example.pdfviewer.ui.data

sealed class Libraries {

    data object AndroidX : Libraries()

    data object PdfRenderer : Libraries()

    data object PDFBox : Libraries()

    data object MuPDF : Libraries()

}

val listLib = mapOf(
    Pair( Libraries.AndroidX, "AndroidX" ),
    Pair( Libraries.PdfRenderer, "PdfRenderer" ),
    Pair( Libraries.PDFBox, "PDFBox" ),
    Pair( Libraries.MuPDF, "MuPDF" )
)