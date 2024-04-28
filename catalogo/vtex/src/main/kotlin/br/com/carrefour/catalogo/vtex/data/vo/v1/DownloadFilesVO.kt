package br.com.carrefour.catalogo.vtex.data.vo.v1


data class DownloadFilesVO (
    var directory: String = "/home/wagner/work/Carrefour/vtex/exports/",
    var links: List<String> = mutableListOf()
)
