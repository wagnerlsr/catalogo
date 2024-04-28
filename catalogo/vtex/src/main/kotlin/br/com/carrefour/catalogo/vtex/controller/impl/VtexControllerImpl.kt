package br.com.carrefour.catalogo.vtex.controller.impl

import br.com.carrefour.catalogo.vtex.controller.VtexController
import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuContextVO
import br.com.carrefour.catalogo.vtex.service.vtex.VtexService
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController


@Validated
@RestController
@RequiredArgsConstructor
class VtexControllerImpl(val vtexService: VtexService): VtexController {

    @GetMapping("/v1/vtex/products/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getProductById(@PathVariable(value = "id", required = true) id: Long): ResponseEntity<ProductVO> =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.getProductById(id))

    @GetMapping("/v1/vtex/products/refid/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getProductByRefId(@PathVariable(value = "id", required = true) refId: String) =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.getProductByRefId(refId))

    @GetMapping("/v1/vtex/skus/{skuId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getSkuBySkuId(@PathVariable(value = "skuId", required = true) skuId: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.getSkuBySkuId(skuId))

    @GetMapping("/v1/vtex/skus/context/{skuId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getSkuContextBySkuId(@PathVariable(value = "skuId", required = true) skuId: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.getSkuContextBySkuId(skuId))

    @GetMapping("/v1/vtex/skus/eans/{skuId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getEansBySku(@PathVariable(value = "skuId", required = true) skuId: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.getEansBySku(skuId))

    @PutMapping("/v1/vtex/products/fix/fields19/product/{productId}/sku/{skuId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun fixProductWith19ByProductIdAndSkuId(@PathVariable(value = "productId", required = true) productId: Long,
                                                     @PathVariable(value = "skuId", required = true) skuId: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.fixProductWith19ByProductIdAndSkuId(productId, skuId))

    @PutMapping("/v1/vtex/products/fix/fields19", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun fixProductsWith19() =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.fixProductsWith19())

    @PutMapping("/v1/vtex/skus/fix/eans/{skuId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun fixEansBySku(@PathVariable(value = "skuId", required = true) skuId: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.fixEansBySku(skuId))

    @PutMapping("/v1/vtex/skus/fix/eans", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun fixEans(): ResponseEntity<List<SkuContextVO>> =
        ResponseEntity.status(HttpStatus.OK).body(vtexService.fixEans())

}
