package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


@Document("similares_3p_eletroportateis")
data class Similar (
    @Id
    var id: String? = null,

    var insertDate: Date? = Date(),
    var updateDate: Date? = null,
    var skuId: Long? = null,
    var productId: Long? = null,
    var productRefId: String? = null,
    var productName: String? = null,
    var categoryName: String? = null,
    var similar: List<Long> = mutableListOf(),
    var variants: MutableList<String> = mutableListOf(),
    var variant: Boolean = false
    ) {

    constructor(productId: Long, skuId: Long) : this() {
        this.productId = productId
        this.skuId = skuId
    }

}
