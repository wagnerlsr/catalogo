package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Affiliate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface AffiliateRepository : MongoRepository<Affiliate, String?> {
    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<Affiliate>
    fun findByProductId(productId: Long): Optional<List<Affiliate>>

    @Query(fields = "{'skuId':  1}")
    fun findByType(type: Int): Optional<List<Affiliate>>
}
