package br.com.carrefour.catalogo.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


@Document("querys")
data class Query (
    @Id
    var id: String? = null,

    var name: String? = null,
    var insertDate: Date? = null,
    var updateDate: Date? = null,
) {
    constructor(name: String) : this() {
        this.name = name
        this.insertDate = Date()
    }
}
