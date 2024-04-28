package br.com.carrefour.catalogo.vtex.service.vtex.impl

import br.com.carrefour.catalogo.core.model.*
import br.com.carrefour.catalogo.core.repository.*
import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuContextVO
import br.com.carrefour.catalogo.vtex.service.http.HttpService
import br.com.carrefour.catalogo.vtex.service.util.UtilService
import br.com.carrefour.catalogo.vtex.service.vtex.VtexService
import br.com.carrefour.catalogo.vtex.utils.Constants
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.util.*


@Service
class VtexServiceImpl(private val skuRepository: SkuRepository,
                      private val catalogRepository: CatalogRepository,
                      private val correctionRepository: CorrectionRepository,
                      private val revisionProductRepository: RevisionProductRepository,
                      private val queryProductRepository: QueryProductRepository,
                      private val queryRepository: QueryRepository) : VtexService {

    private val logger = LogManager.getLogger()

    override fun getProductById(id: Long) =
        ProductVO(HttpService.getProductById(id)?.let { jacksonObjectMapper().readValue(it) })

    override fun getProductByRefId(refId: String) = HttpService.getProductByRefId(refId)

    override fun getSkuBySkuId(skuId: Long): SkuContextVO =
        SkuContextVO(HttpService.getSkuBySkuId(skuId)?.let { jacksonObjectMapper().readValue(it) })

    override fun getSkuContextBySkuId(skuId: Long): SkuContextVO = HttpService.getSkuContextBySkuId(skuId)


    override fun fixProductWith19ByProductIdAndSkuId(productId: Long, skuId: Long): ProductVO {
        val productVO = ProductVO()

        val correction =
            correctionRepository.findByProductIdAndSkuId(productId, skuId).orElse(Correction(productId, skuId))

        ////// PRODUCT CORRETION \\\\\\
        val vtexProduct: MutableMap<String, Any>? = HttpService.getProductById(productId)?.let {
            jacksonObjectMapper().readValue(it)
        }

//        if (vtexProduct == null) {
//            correction.hasCorrectionProduct = false
//        } else {
//            if (UtilService.contains19(vtexProduct["RefId"] as String?)) {
//                vtexProduct["RefId"] = UtilService.fix19(vtexProduct["RefId"] as String)
//
//                val product = mutableMapOf<String, Any>(
//                    "Correção" to Constants.DESCRIPTION_FIELDS_19,
//                    "Data" to Date(),
//                    "Vtex" to vtexProduct
//                )
//
//                correction.updateDate = Date()
//                correction.products.add(product)
//                correctionRepository.save(correction)
//
//                val res = HttpService.putProductById(productId, jacksonObjectMapper().writeValueAsString(vtexProduct))
//
//                if (res.statusCode() == 200) correction.hasCorrectionProduct = false else product["Erro"] = res.body()
//
//                logger.info(">> P >> $productId > $skuId >> ${res.statusCode()}")
//            } else {
//                correction.hasCorrectionProduct = false
//            }
//        }
//
//        ////// SKU CORRETION \\\\\\
//        val vtexSku: MutableMap<String, Any>? = HttpService.getSkuBySkuId(skuId)?.let {
//            jacksonObjectMapper().readValue(it)
//        }
//
//        if (vtexSku == null) {
//            correction.hasCorrectionSku = false
//        } else {
//            if (UtilService.contains19(vtexSku["RefId"] as String?)) {
//                vtexSku["RefId"] = UtilService.fix19(vtexSku["RefId"] as String)
//
//                val product = mutableMapOf<String, Any>(
//                    "Correção" to Constants.DESCRIPTION_FIELDS_19,
//                    "Data" to Date(),
//                    "Vtex" to vtexSku
//                )
//
//                correction.updateDate = Date()
//                correction.skus.add(product)
//                correctionRepository.save(correction)
//
//                val res = HttpService.putSkuById(skuId, jacksonObjectMapper().writeValueAsString(vtexSku))
//
//                if (res.statusCode() == 200) correction.hasCorrectionSku = false else product["Erro"] = res.body()
//
//                logger.info(">> S >> $productId > $skuId >> ${res.statusCode()}")
//            } else {
//                correction.hasCorrectionSku = false
//            }
//        }

        ////// EANs CORRETION \\\\\\
        var eans = getEansBySku(skuId).toMutableList()

        if (eans.isEmpty()) {
            correction.hasCorrectionEans = false
        } else {
            if (UtilService.eansContains19(eans)) {
                eans =  UtilService.fixEans(eans).toMutableList()

                val product = mutableMapOf<String, Any>(
                    "Correção" to Constants.DESCRIPTION_FIELDS_19,
                    "Data" to Date(),
                    "Vtex" to eans
                )

                correction.updateDate = Date()
                correction.eans.add(product)
                correctionRepository.save(correction)

                if (HttpService.deleteEansBySku(skuId)) {
                    correction.hasCorrectionEans = false
                    eans.forEach { if (!HttpService.insertEanBySku(skuId, it)) correction.hasCorrectionEans = true }
                }

                logger.info(">> E >> $productId > $skuId")
            } else {
                correction.hasCorrectionEans = false
            }
        }

        correctionRepository.save(correction)

        productVO.product = mutableMapOf("Produto" to correction)

        return productVO
    }

    override fun fixProductsWith19(): List<ProductVO> {
        val productsVO = mutableListOf<ProductVO>()

//        val catalogs = catalogRepository.findAll()
//
//        catalogs.forEach {catalog ->
//            catalog.productId?.let {productId ->
//                catalog.skuId?.let { skuId ->
//                    productsVO.add(fixProductWith19ByProductIdAndSkuId(productId, skuId))
//                }
//            }
//        }

        val corrections = correctionRepository.findCorrections()

        corrections.forEach {correction ->
            correction.productId?.let {productId ->
                correction.skuId?.let { skuId ->
//                    if (correction.hasCorrectionProduct == true)
//                        logger.info(">> P >> $productId > $skuId >> ${correction.products[correction.products.size-1]["Erro"]}")
//                    if (correction.hasCorrectionSku == true)
//                        logger.info(">> S >> $productId > $skuId >> ${correction.skus[correction.skus.size-1]["Erro"]}")
                    productsVO.add(fixProductWith19ByProductIdAndSkuId(productId, skuId))
                }
            }
        }

        return productsVO
    }

    override fun fixEansBySku(skuId: Long): SkuContextVO {
        val skuContextVO = SkuContextVO()
        val sku = skuRepository.findBySkuId(skuId).orElse(Sku(skuId))

        val eans = Eans().apply {
            queryDate = Date()
            eans = getEansBySku(skuId).toMutableList()
        }

        sku.corrections.add(eans)
        skuRepository.save(sku);

        if (UtilService.eansContains19(eans.eans)) {
            eans.eansCorrected = UtilService.fixEans(eans.eans).toMutableList()

            if (HttpService.deleteEansBySku(skuId)) {
                eans.eansCorrected.forEach {
                    val res = HttpService.insertEanBySku(skuId, it)
                    logger.info(">>>>>>>> $skuId >>> $it >>> $res")
                }

                with(sku) {
                    adjusted = true
                    updateDate = Date()
                    correctionDate = Date()
                }

                eans.correctionDate = Date()
            }
        } else {
            with(sku) {
                adjusted = true
                updateDate = Date()
            }

            logger.info(">>>>>>>> $skuId")
        }

        skuRepository.save(sku);
        skuContextVO.map?.set("SKU", sku)

        return skuContextVO
    }


    override fun getEansBySku(skuId: Long) = Gson().fromJson(HttpService.getEansByskuId(skuId), Array<String>::class.java).asList()


    override fun fixEans(): List<SkuContextVO> {
        val skusContextVO = mutableListOf<SkuContextVO>()
        val query = queryRepository.findByName(Constants.QUERY_NAME_EANS19).orElse(null)

        if (query != null) {
            val products = query.id?.let { queryProductRepository.findByQueryId(it) }

            products?.forEach { it.skuId?.let { skuId -> skusContextVO.add(fixEansBySku(skuId)) } }
        }

        return skusContextVO
    }

}
