package br.com.carrefour.catalogo.core.repository

import br.com.carrefour.catalogo.core.model.Catalog
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface CatalogRepository : MongoRepository<Catalog, String?> {
    fun findByProductIdAndSkuId(productId: Long, skuId: Long): Optional<Catalog>
}
