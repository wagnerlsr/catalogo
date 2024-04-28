package br.com.carrefour.catalogo.vtex.service.vtex

import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuContextVO

interface VtexService {

    fun getProductById(id: Long): ProductVO
    fun getProductByRefId(refId: String): ProductVO
    fun getSkuBySkuId(skuId: Long): SkuContextVO
    fun getSkuContextBySkuId(skuId: Long): SkuContextVO
    fun fixProductWith19ByProductIdAndSkuId(productId: Long, skuId: Long): ProductVO
    fun fixProductsWith19(): List<ProductVO>
    fun fixEans(): List<SkuContextVO>
    fun fixEansBySku(skuId: Long): SkuContextVO
    fun getEansBySku(skuId: Long): List<String>

}
