package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Product
import br.com.carrefour.catalogo.core.model.RevisionProduct
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface RevisionProductRepository : MongoRepository<RevisionProduct, String?> {
    fun findByProductId(id: Long): Optional<RevisionProduct>
}
