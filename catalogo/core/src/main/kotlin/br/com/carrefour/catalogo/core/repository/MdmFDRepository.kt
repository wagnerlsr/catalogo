package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Affiliate
import br.com.carrefour.catalogo.core.model.MdmFD
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface MdmFDRepository : MongoRepository<MdmFD, String?> {

    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<MdmFD>
    fun findByProductId(productId: Long): Optional<MdmFD>
    fun findAllByType(type: Int, pageRequest: PageRequest): List<MdmFD>

}
