package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Similar
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface SimilarRepository : MongoRepository<Similar, String?> {
    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<Similar>
}
