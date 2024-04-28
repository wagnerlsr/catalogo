package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document("catalogs")
data class Catalog (
    @Id
    var id: String? = null,

    var skuId: Long? = null,
    var productId: Long? = null,
    var before: MutableMap<String, Any> = mutableMapOf(),
    var after: MutableMap<String, Any> = mutableMapOf()
) {

    constructor(productId: Long, skuId: Long) : this() {
        this.productId = productId
        this.skuId = skuId
    }

}
