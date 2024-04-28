package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document("query_products")
data class QueryProduct (
    @Id
    var id: String? = null,

    var queryId: String? = null,
    var productId: Long? = null,
    var skuId: Long? = null,
    var fields: MutableMap<String, Any> = mutableMapOf(),
)
