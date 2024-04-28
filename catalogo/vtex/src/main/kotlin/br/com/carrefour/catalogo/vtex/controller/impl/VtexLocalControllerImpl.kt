package br.com.carrefour.catalogo.vtex.controller.impl

import br.com.carrefour.catalogo.vtex.controller.VtexLocalController
import br.com.carrefour.catalogo.vtex.data.vo.v1.DownloadFilesVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.service.vtex.VtexLocalService
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@Validated
@RestController
@RequiredArgsConstructor
class VtexLocalControllerImpl(val vtexLocalService: VtexLocalService): VtexLocalController {

    @GetMapping("/v1/vtex/local/skus/{skuId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getSku(@PathVariable(value = "skuId", required = true) skuId: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.findBySkuId(skuId))


    @GetMapping("/v1/vtex/local/skus/{skuIdStart}/{skuIdEnd}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getSkusRange(@PathVariable(value = "skuIdStart", required = true) skuIdStart: Long,
                              @PathVariable(value = "skuIdEnd", required = true) skuIdEnd: Long) =
        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.findBySkuIdRange(skuIdStart, skuIdEnd))

    @PostMapping("/v1/vtex/local/download/files", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun downloadFiles(@RequestBody downloadFiles: DownloadFilesVO) =
        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.downloadFiles(downloadFiles))

    @PostMapping("/v1/vtex/local/products/before", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getProductsBeforeImport(@RequestBody skus: Map<String, Any>) =
        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getProductsBeforeAfterImport(skus, true))

    @PostMapping("/v1/vtex/local/products/after", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getProductsAfterImport(@RequestBody skus: Map<String, Any>) =
        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getProductsBeforeAfterImport(skus, false))

    @GetMapping("/v1/vtex/local/products/import/fields19", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun importProductsWithFields19() = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importProductsWithFields19())

    @GetMapping("/v1/vtex/local/products/import/similar", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun importProductsWithSimilar() = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importProductsWithSimilares())

    @GetMapping("/v1/vtex/local/products/variant", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getVariants(): ResponseEntity<String> = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getVariants())

    @GetMapping("/v1/vtex/local/products/variant3p", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getVariants3P(): ResponseEntity<String> = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getVariants3P())

    @GetMapping("/v1/vtex/local/products/report/similar", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun reportProductsWithSimilar(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.reportProductsWithSimilar())

    @PostMapping("/v1/vtex/local/products/select/poc", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun selectProductsPOC(@RequestBody params: Map<String, Any>) =
    override fun selectProductsPOC(@RequestBody params: Map<String, Any>): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.selectProductsPOC(params))

    @GetMapping("/v1/vtex/local/products/affiliate/db", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun importDbAffiliate(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getAffiliateDb())



//    @GetMapping("/v1/vtex/local/products/affiliate/vtex/{page}", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun importAffiliate(@PathVariable(value = "page", required = true) page: Int): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getAffiliateVtex(page))

    @GetMapping("/v1/vtex/local/products/affiliate/vtex", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun importAffiliate(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getAffiliateVtexList())



    @GetMapping("/v1/vtex/local/products/affiliate/export", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun exportAffiliate(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getAffiliateExport())

    @GetMapping("/v1/vtex/local/products/affiliate/price", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getPriceAffiliate(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getAffiliatePrice())


    @GetMapping("/v1/vtex/local/products/bandeiras", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun putBandeiras(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.putFlagsBandeiras())

    @GetMapping("/v1/vtex/local/products/mdm/import", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun importMdm(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importMdm())

    @GetMapping("/v1/vtex/local/products/mdm/vtex", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun getVtexMdmFD(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.getMdmVtex())

    @GetMapping("/v1/vtex/local/products/mdm/export", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun exportMdm(): ResponseEntity<String>  = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.exportMdm())


//    @PostMapping("/v1/vtex/local/products/import", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
//    override fun importProducts(@RequestBody importProducts: ImportProductsVO) =
//        ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importProducts(importProducts))

//    @GetMapping("/v1/vtex/local/products/import/ean19", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun importProductsWithEan19() = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importProductsWithEan19())

//    @GetMapping("/v1/vtex/local/products/import/ref19", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun importProductsWithRefId19() = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importProductsWithRefId19())

//    @GetMapping("/v1/vtex/local/skus/import/refid19", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun importSkusWithRefId19() = ResponseEntity.status(HttpStatus.OK).body(vtexLocalService.importSkusWithEan19())

}
