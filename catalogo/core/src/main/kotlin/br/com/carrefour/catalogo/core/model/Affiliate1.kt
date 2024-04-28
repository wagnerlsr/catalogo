package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document("affiliateFood")
data class Affiliate1 (
    @Id
    var id: String? = null,

    var productId: Long? = null,
    var skuId: Long? = null,
    var productRefId: String? = null,
    var type: Int? = 0,
    var product: Any? = null,
    var price: Double? = 0.0
    ) {

    constructor(productId: Long) : this() {
        this.productId = productId
    }

}
