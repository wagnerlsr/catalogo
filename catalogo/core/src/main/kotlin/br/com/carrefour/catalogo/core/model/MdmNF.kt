package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document("mdmnf")
data class MdmNF (
    @Id
    var id: String? = null,

    var skuId: Long? = null,
    var productId: Long? = null,
    var type: Int? = 0,
    var food: Boolean? = false,
    var product: Any? = null
    ) {

    constructor(productId: Long, skuId: Long) : this() {
        this.productId = productId
        this.skuId = skuId
    }

}
