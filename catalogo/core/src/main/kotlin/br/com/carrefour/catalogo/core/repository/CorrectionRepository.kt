package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Correction
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface CorrectionRepository : MongoRepository<Correction, String?> {
    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<Correction>

    @Query("{\$or: [{'hasCorrectionProduct': true}, {'hasCorrectionSku': true}, {'hasCorrectionEans': true}]}")
    fun findCorrections(): List<Correction>
}
