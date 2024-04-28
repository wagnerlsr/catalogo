package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


@Document("corrections")
data class Correction (
    @Id
    var id: String? = null,

    var skuId: Long? = null,
    var productId: Long? = null,
    var hasCorrectionProduct: Boolean? = false,
    var hasCorrectionSku: Boolean? = false,
    var hasCorrectionEans: Boolean? = false,
    var insertDate: Date? = Date(),
    var updateDate: Date? = null,
    var products: MutableList<MutableMap<String, Any>> = mutableListOf(),
    var skus: MutableList<MutableMap<String, Any>> = mutableListOf(),
    var eans: MutableList<MutableMap<String, Any>> = mutableListOf()
) {

    constructor(productId: Long, skuId: Long) : this() {
        this.productId = productId
        this.skuId = skuId
    }

}
