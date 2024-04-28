package br.com.carrefour.catalogo.vtex.service.vtex

import br.com.carrefour.catalogo.vtex.data.vo.v1.DownloadFilesVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.ImportProductsVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuVO

interface VtexLocalService {

    fun findBySkuId(skuId: Long): SkuVO
    fun findBySkuIdRange(skuIdStart: Long, skuIdEnd: Long): List<SkuVO>
    fun downloadFiles(downloadFiles: DownloadFilesVO): List<String>
//    fun importProducts(importProducts: ImportProductsVO): Int
    fun importProductsWithFields19(): Map<String, Any>
    fun importProductsWithSimilares(): Map<String, Any>
    fun getVariants(): String
    fun getVariants3P(): String
    fun getAffiliateDb(): String
    fun getAffiliateVtex(page: Int): String
    fun getAffiliateVtexList(): String
    fun getAffiliateExport(): String
    fun getAffiliatePrice(): String
    fun reportProductsWithSimilar(): String
    fun getProductsBeforeAfterImport(skus: Map<String, Any>, before: Boolean): String
    fun selectProductsPOC(params: Map<String, Any>): Any //List<Map<String, Any>>
//    fun importProductsWithEan19(): Set<Long>
//    fun importProductsWithRefId19(): Set<Long>
//    fun importSkusWithEan19(): Set<Long>
//    fun importSkusWithRefId19(): Set<Long>
    fun putFlagsBandeiras(): String
    fun importMdm(): String
    fun exportMdm(): String
    fun getMdmVtex(): String

}
