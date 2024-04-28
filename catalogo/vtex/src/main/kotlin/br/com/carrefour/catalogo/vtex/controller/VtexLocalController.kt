package br.com.carrefour.catalogo.vtex.controller

import br.com.carrefour.catalogo.core.exception.ExceptionResponse
import br.com.carrefour.catalogo.vtex.data.vo.v1.DownloadFilesVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuVO
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity


@Tag(name = "VTEX", description = "Acesso a base Vtex")
interface VtexLocalController {

    @ApiOperation(value = "Obter SKU",
        notes = "Obtem o SKU da base local.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = SkuVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getSku(skuId: Long): ResponseEntity<SkuVO>


    @ApiOperation(value = "Obter um range SKUs",
        notes = "Obtem um range de SKUs da base local.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = SkuVO::class, responseContainer = "List"),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getSkusRange(skuIdStart: Long, skuIdEnd: Long): ResponseEntity<List<SkuVO>>


    @ApiOperation(value = "Baixar arquivos de exportação",
        notes = "Baixa todos arquivos exportados pela VTEX.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = String::class, responseContainer = "List"),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun downloadFiles(downloadFiles: DownloadFilesVO): ResponseEntity<List<String>>


    @ApiOperation(value = "Obter produtos antes da importação",
        notes = "Obter produtos antes da importação",
        authorizations = [Authorization(value = "JWT")],
        tags=["Catalogo"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = String::class),
    ])
    fun getProductsBeforeImport(skus: Map<String, Any>): ResponseEntity<String>


    @ApiOperation(value = "Obter produtos antes da importação",
        notes = "Obter produtos antes da importação",
        authorizations = [Authorization(value = "JWT")],
        tags=["Catalogo"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = String::class),
    ])
    fun getProductsAfterImport(skus: Map<String, Any>): ResponseEntity<String>


//    @ApiOperation(value = "Importar Produtos",
//        notes = "Importa todos produtos dos arquivos baixados.",
//        authorizations = [Authorization(value = "JWT")],
//        tags=["Base Local"]
//    )
//    @ApiResponses(value = [
//        ApiResponse(code = 200, message = "Ok", response = Long::class),
//        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
//        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
//        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
//        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
//        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
//    ])
//    fun importProducts(importProducts: ImportProductsVO): ResponseEntity<Int>

    @ApiOperation(value = "Importar Produtos com campos contendo 1/9",
        notes = "Importa todos Produtos que contenham 1/9 em algum campo dos arquivos exportados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun importProductsWithFields19(): ResponseEntity<Map<String, Any>>

    @ApiOperation(value = "Importar Produtos com Similares",
        notes = "Importa todos Produtos de uma categoria.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun importProductsWithSimilar(): ResponseEntity<Map<String, Any>>

    @ApiOperation(value = "Obter Variantes de Produtos com Similares",
        notes = "Obtem as variantes de todos produtos importados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getVariants(): ResponseEntity<String>

    @ApiOperation(value = "Obter Variantes de Produtos com Similares 3P",
        notes = "Obtem as variantes de todos produtos importados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getVariants3P(): ResponseEntity<String>

    @ApiOperation(value = "Emitir Relatorio de Produtos com Similares",
        notes = "Emiti relatorio de todos produtos importados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun reportProductsWithSimilar(): ResponseEntity<String>

    @ApiOperation(value = "Selecionar Produtos - POC",
        notes = "Selecionar Produtos - POC.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Base Local"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok"),
//        ApiResponse(code = 200, message = "Ok", response = Map::class, responseContainer = "List"),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun selectProductsPOC(params: Map<String, Any>): ResponseEntity<Any>

    @ApiOperation(value = "Importar Produtos DB para Afiliados",
        notes = "Importa todos Produtos para o banco de dados de afiliados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Afiliados"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun importDbAffiliate(): ResponseEntity<String>

    @ApiOperation(value = "Importar Produtos VTEX para Afiliados",
        notes = "Importa todos Produtos da VTEX para afiliados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Afiliados"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
//    fun importAffiliate(page: Int): ResponseEntity<String>
    fun importAffiliate(): ResponseEntity<String>

    @ApiOperation(value = "Exportar Produtos VTEX para Afiliados",
        notes = "Exportar todos Produtos da VTEX para afiliados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Afiliados"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun exportAffiliate(): ResponseEntity<String>

    @ApiOperation(value = "Obter preços VTEX para Afiliados",
        notes = "Obter preços VTEX para Afiliados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Afiliados"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getPriceAffiliate(): ResponseEntity<String>

    @ApiOperation(value = "Gravar flags para bandeiras",
        notes = "Obter dados VTEX para bandeiras.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Bandeiras"]
    )
    @ApiResponses(value = [
    ])
    fun putBandeiras(): ResponseEntity<String>

    @ApiOperation(value = "Importar Produtos MDM",
        notes = "Importar Produtos MDM.",
        authorizations = [Authorization(value = "JWT")],
        tags=["MDM"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
    ])
    fun importMdm(): ResponseEntity<String>

    @ApiOperation(value = "Obtem Produtos VTEX MDM",
        notes = "Obtem Produtos VTEX MDM.",
        authorizations = [Authorization(value = "JWT")],
        tags=["MDM"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
    ])
    fun getVtexMdmFD(): ResponseEntity<String>

    @ApiOperation(value = "Exportar Produtos VTEX para MDM",
        notes = "Exportar todos Produtos da VTEX para MDM.",
        authorizations = [Authorization(value = "JWT")],
        tags=["MDM"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = Map::class),
    ])
    fun exportMdm(): ResponseEntity<String>

//    fun selectProductsPOC(params: Map<String, Any>): ResponseEntity<List<Map<String, Any>>>

//    @ApiOperation(value = "Importar Produtos com EAN 1/9",
//        notes = "Importa todos Produtos com EAN 1/9 dos arquivos exportados.",
//        authorizations = [Authorization(value = "JWT")],
//        tags=["Base Local"]
//    )
//    @ApiResponses(value = [
//        ApiResponse(code = 200, message = "Ok", response = Long::class, responseContainer = "List"),
//        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
//        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
//        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
//        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
//        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
//    ])
//    fun importProductsWithEan19(): ResponseEntity<Set<Long>>

//    @ApiOperation(value = "Importar Produtos com RefId 1/9",
//        notes = "Importa todos Produtos com RefId 1/9 dos arquivos exportados.",
//        authorizations = [Authorization(value = "JWT")],
//        tags=["Base Local"]
//    )
//    @ApiResponses(value = [
//        ApiResponse(code = 200, message = "Ok", response = Long::class, responseContainer = "List"),
//        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
//        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
//        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
//        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
//        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
//    ])
//    fun importProductsWithRefId19(): ResponseEntity<Set<Long>>

//    @ApiOperation(value = "Importar SKUs com RefId 1/9",
//        notes = "Importa todos SKUs com RefId 1/9 dos arquivos exportados.",
//        authorizations = [Authorization(value = "JWT")],
//        tags=["Base Local"]
//    )
//    @ApiResponses(value = [
//        ApiResponse(code = 200, message = "Ok", response = Long::class, responseContainer = "List"),
//        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
//        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
//        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
//        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
//        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
//    ])
//    fun importSkusWithRefId19(): ResponseEntity<Set<Long>>

}
