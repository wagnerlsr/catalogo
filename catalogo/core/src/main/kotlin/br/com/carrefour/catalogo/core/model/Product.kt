package br.com.carrefour.catalogo.core.model

import java.util.*


data class Product (
    var date: Date = Date(),
    var productId: Long? = null,
    var description: String? = null,
    var vtexFields: MutableMap<String, Any> = mutableMapOf(),
    var queryFields: MutableMap<String, Any> = mutableMapOf(),
    var updatedFields: MutableMap<String, Any> = mutableMapOf()
) {

    constructor(productId: Long) : this() {
        this.productId = productId
    }
}
