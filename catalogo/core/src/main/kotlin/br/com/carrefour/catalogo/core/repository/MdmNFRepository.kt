package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.MdmNF
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface MdmNFRepository : MongoRepository<MdmNF, String?> {

    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<MdmNF>
    fun findByProductId(productId: Long): Optional<MdmNF>
    fun findAllByType(type: Int, pageRequest: PageRequest): List<MdmNF>

}
