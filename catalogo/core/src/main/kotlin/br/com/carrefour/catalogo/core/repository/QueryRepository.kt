package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Query
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface QueryRepository : MongoRepository<Query, String?> {
    fun findByName(name: String): Optional<Query>
//    fun findByAdjustedEquals(adjusted: Boolean): List<Product>
}
