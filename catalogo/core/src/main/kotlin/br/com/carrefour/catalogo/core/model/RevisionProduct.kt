package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


@Document("revision_products")
data class RevisionProduct (
    @Id
    var id: String? = null,

    var productId: Long? = null,
    var insertDate: Date? = null,
    var updateDate: Date? = null,
    var revisions: MutableList<Product> = mutableListOf()
) {

    constructor(productId: Long) : this() {
        this.productId = productId
        this.insertDate = Date()
    }

    fun insertRevision(product: Product) {
        this.revisions.add(product)
        this.updateDate = Date()
    }

}
