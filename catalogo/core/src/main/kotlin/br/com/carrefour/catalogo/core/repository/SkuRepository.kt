package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Sku
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface SkuRepository : MongoRepository<Sku, String?> {
    @Query("{'skuId': {\$gt: ?0, \$lte: ?1}}")
    fun findBySkuIdRange(skuIdStart: Long, skuIdEnd: Long): List<Sku>

    fun findBySkuId(skuId: Long): Optional<Sku>
    fun findByAdjustedEquals(adjusted: Boolean): List<Sku>
}
