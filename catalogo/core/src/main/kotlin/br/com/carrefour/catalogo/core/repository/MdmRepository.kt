package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Mdm
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface MdmRepository : MongoRepository<Mdm, String?> {

    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<Mdm>
    fun findByProductId(productId: Long): Optional<Mdm>

}
