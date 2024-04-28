package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


@Document("sku_id")
data class Sku (
    @Id
    var id: String? = null,

    var skuId: Long? = null,
    var insertDate: Date? = null,
    var updateDate: Date? = null,
    var correctionDate: Date? = null,
    var adjusted: Boolean? = false,
    var corrections: MutableList<Eans> = mutableListOf()
) {
    constructor(skuId: Long) : this() {
        this.skuId = skuId
        this.insertDate = Date()
    }
}


data class Eans (
    var queryDate: Date? = null,
    var correctionDate: Date? = null,
    var eans: MutableList<String> = mutableListOf(),
    var eansCorrected: MutableList<String> = mutableListOf()
)
