package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Affiliate1
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface Affiliate1Repository : MongoRepository<Affiliate1, String?> {
    fun findByProductId(productId: Long): Optional<Affiliate1>

//    @Query(fields = "{'skuId':  1}")
//    fun findByType(type: Int): Optional<List<Affiliate1>>
}
