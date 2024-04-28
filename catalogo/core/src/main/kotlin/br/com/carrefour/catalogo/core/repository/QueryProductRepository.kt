package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.QueryProduct
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository


@Repository
interface QueryProductRepository : MongoRepository<QueryProduct, String?> {
    fun findByQueryId(id: String): MutableList<QueryProduct>
    fun deleteByQueryId(id: String): Any
}
