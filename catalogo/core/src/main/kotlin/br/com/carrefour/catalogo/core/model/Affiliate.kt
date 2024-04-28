package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


@Document("affiliate")
data class Affiliate (
    @Id
    var id: String? = null,

    var skuId: Long? = null,
    var productId: Long? = null,
    var productRefId: String? = null,
    var skuRefId: String? = null,
    var type: Int? = 0,
    var updated: Boolean? = false,
    var insertDate: Date? = Date(),
    var product: MutableMap<String, Any>? = null
    ) {

    constructor(productId: Long, skuId: Long) : this() {
        this.productId = productId
        this.skuId = skuId
    }

}
