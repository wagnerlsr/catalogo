package br.com.carrefour.catalogo.vtex.controller

import br.com.carrefour.catalogo.core.exception.ExceptionResponse
import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuContextVO
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity


@Tag(name = "VTEX", description = "Acesso a base Vtex")
interface VtexController {

    @ApiOperation(value = "Obter Produto por Id",
        notes = "Obtem um produto pelo seu Id.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Produtos"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = ProductVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getProductById(id: Long): ResponseEntity<ProductVO>


    @ApiOperation(value = "Obter Produto por RefId",
        notes = "Obtem um produto pelo seu RefId (RMS).",
        authorizations = [Authorization(value = "JWT")],
        tags=["Produtos"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = ProductVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getProductByRefId(refId: String): ResponseEntity<ProductVO>


    @ApiOperation(value = "Obter SKU por Id",
        notes = "Obtem um SKU pelo seu skuId.",
        authorizations = [Authorization(value = "JWT")],
        tags=["SKUs"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = SkuContextVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getSkuBySkuId(skuId: Long): ResponseEntity<SkuContextVO>


    @ApiOperation(value = "Obter Contexto SKU",
        notes = "Obtem um contexto SKU pelo seu skuId.",
        authorizations = [Authorization(value = "JWT")],
        tags=["SKUs"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = SkuContextVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getSkuContextBySkuId(skuId: Long): ResponseEntity<SkuContextVO>


    @ApiOperation(value = "Obter EANs por SKU",
        notes = "Obtem todos os EANs pelo skuId.",
        authorizations = [Authorization(value = "JWT")],
        tags=["SKUs"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = String::class, responseContainer = "List"),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun getEansBySku(skuId: Long): ResponseEntity<List<String>>


    @ApiOperation(value = "Corrigir Produtos com campos contendo 1/9 por Id",
        notes = "Corrigir campos contendo 1/9 do Produto pelo Id do produto e do SKU.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Produtos"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = ProductVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun fixProductWith19ByProductIdAndSkuId(productId: Long, skuId: Long): ResponseEntity<ProductVO>


    @ApiOperation(value = "Corrigir todos Produtos com campos contendo 1/9",
        notes = "Corrigir campos contendo 1/9 de todos os Produtos importados.",
        authorizations = [Authorization(value = "JWT")],
        tags=["Produtos"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = ProductVO::class, responseContainer = "List"),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun fixProductsWith19(): ResponseEntity<List<ProductVO>>


    @ApiOperation(value = "Corrigir EANs com 1/9 por SKU",
        notes = "Corrigir todos EANS com 1/9 pelo skuId.",
        authorizations = [Authorization(value = "JWT")],
        tags=["SKUs"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = SkuContextVO::class),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun fixEansBySku(skuId: Long): ResponseEntity<SkuContextVO>


    @ApiOperation(value = "Corrigir EANs com 1/9 via exportação",
        notes = "Corrigir todos EANS com 1/9 capturados na exportação.",
        authorizations = [Authorization(value = "JWT")],
        tags=["SKUs"]
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Ok", response = SkuContextVO::class, responseContainer = "List"),
        ApiResponse(code = 400, message = "Bad Request", response = ExceptionResponse::class),
        ApiResponse(code = 401, message = "Unauthorized", response = ExceptionResponse::class),
        ApiResponse(code = 403, message = "Forbidden", response = ExceptionResponse::class),
        ApiResponse(code = 404, message = "Not Found", response = ExceptionResponse::class),
        ApiResponse(code = 500, message = "Internal Server Error", response = ExceptionResponse::class)
    ])
    fun fixEans(): ResponseEntity<List<SkuContextVO>>

}
