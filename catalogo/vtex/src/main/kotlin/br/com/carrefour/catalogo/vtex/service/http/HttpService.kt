package br.com.carrefour.catalogo.vtex.service.http

import br.com.carrefour.catalogo.core.exception.ResourceNotFoundException
import br.com.carrefour.catalogo.vtex.data.vo.v1.ProductVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuContextVO
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture


@Service
object HttpService {
    private val client = HttpClient.newBuilder().build()
    private const val HOST = "https://carrefourbr.vtexcommercestable.com.br"
    private const val HOST1 = "https://mercado.carrefour.com.br"
    private const val HOST2 = "https://api.vtex.com"

    private const val HOSTNC = "https://nacional2.vtexcommercestable.com.br"
    private const val HOSTBP = "https://bompreco2.vtexcommercestable.com.br"
    private const val HOSTTD = "https://tododia2.vtexcommercestable.com.br"

    private val headersnc = mapOf<String, String>(
        "Accept" to "application/json",
        "Content-Type" to "application/json",
        "X-VTEX-API-AppKey" to "vtexappkey-nacional2-TDEDYV",
        "X-VTEX-API-AppToken" to "WBNJSNVMHUOOHRTPGUVQFGWRSNHFQDPCJWIEFPAKZKKRCRJEDDKOXPGOVTUTXYPFOLGTSCFPUWGQZQHLRAFHLBMHHWIUDSLRXGLLFWAFXAAJEMSGDHPDVWMFDLPTPHSU"
    )

    private val headersbp = mapOf<String, String>(
        "Accept" to "application/json",
        "Content-Type" to "application/json",
        "X-VTEX-API-AppKey" to "vtexappkey-bompreco2-TKMNER",
        "X-VTEX-API-AppToken" to "QVHHAZUGUGRWBAMXSHXKEBVTDOPQKSUQBVGPPPEYAFGAIGGTAPMQKWAOIZXOASKDEWVCZCRQVRHDHJXMEFECITKPWVMLNIHHUSHHTGDHYJLPOQRRPPQSSOOSJPXLDYTZ"
    )

    private val headerstd = mapOf<String, String>(
        "Accept" to "application/json",
        "Content-Type" to "application/json",
        "X-VTEX-API-AppKey" to "vtexappkey-tododia2-WXSMLJ",
        "X-VTEX-API-AppToken" to "UPIEQYXAXHJUMMUWQHHYUUMZJJPZEWMTRZDXPGDAHFJNBWSKAZARBUTFZVFROETVHIAAGEAXPHFZSLSWVXAENTSGYDFNBQIUWPNOYSAZEQISGJWFKOHIARXOUAIVQUDV"
    )

    private val headers = mapOf<String, String>(
        "Accept" to "application/json",
        "Content-Type" to "application/json",
//        "Accept-Encoding" to "gzip, deflate, br",
//        "X-VTEX-API-AppKey" to "vtexappkey-carrefourbr-IDRNEI",
//        "X-VTEX-API-AppToken" to "HYPSFWSYAVVIGWJRYSTHNZLDFBPJEHEYHUEGYLKSJJYWYKRLCHQVJELINMNDGITSRXBMSCSNIAXDAPEUJEHXQIOCIWTMXGLBQFFBJFTZODNJHIIXHSUCBWCIIZDWJGPB"
//        "X-VTEX-API-AppKey" to "vtexappkey-carrefourbr-PDFRLF",
//        "X-VTEX-API-AppToken" to "SFIFQPCHYBPQTRWNPAGHIKYTBTHLZYMMAVISRHUBSJQJSPHRRVRATVNIHPDWHQIAPCQXIEVHFFPZUOAMMPUEGMSEHALCEYTBAPUQZJJNZKUOFONTORKRWEUDJLPHSTDH"
//        "X-VTEX-API-AppKey" to "vtexappkey-carrefourbr-MYPJVA",
//        "X-VTEX-API-AppToken" to "TDELEGMZOTAYMMPRPRYMBFYJNHTQTEIBYFXSHEYVODVESVSHABTUUDMCGKOQERJSWGUKDMQXVWOMTWUTZZDDWTWPMHTLTFUXOVVJNQLBWHYTMMBOGJDUNAGQLPYUTCQJ"
//        "X-VTEX-API-AppKey" to "vtexappkey-carrefourbr-BTBMBW",
//        "X-VTEX-API-AppToken" to "BGUVXGCFRZZYOOKAOKZUBTCUCOKPPTZMDUVASASAVWKSHSHWZNENDDUBVYTUQZPEHPJUUMYAUPLRKRUXXRUXBAGRQYDBJDSGDLQBVFBTVPBWZIASAIVZDANCMYDSJEEN"
        "X-VTEX-API-AppKey" to "vtexappkey-carrefourbr-OZARLV",
        "X-VTEX-API-AppToken" to "MOAFIRHKDYGRYPCHARBAKSGZJLAGJPMXURAPNEXWIVFXVFYVJKIBJGWTHAVWRBBNLIWHOMTVUESPNJDEMEWZHLNOUHHMJAFHURLMIRGWQMHFNHGYXQKMEBGAVHNHFGMT"
    )

    private fun getApi(url: String): HttpResponse<String>? {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .apply { headers.forEach {(key, value) -> header(key, value)} }
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            return response
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    private fun getApiAsync(url: String): CompletableFuture<HttpResponse<String>> {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .apply { headers.forEach {(key, value) -> header(key, value)} }
                .GET()
                .build()

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    private fun getApiAsync(url: String, headers: Map<String, String>): CompletableFuture<HttpResponse<String>> {
        try {
            val request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .apply { headers.forEach {(key, value) -> header(key, value)} }
                .build()

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    private fun getApi1(url: String): HttpResponse<ByteArray>? {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .apply { headers.forEach {(key, value) -> header(key, value)} }
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

            return response
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    fun deleteEansBySku(skuId: Long): Boolean {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$HOST/api/catalog/pvt/stockkeepingunit/$skuId/ean"))
                .apply { headers.forEach {(key, value) -> header(key, value)} }
                .DELETE()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            return response.statusCode() == 200
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    fun insertEanBySku(skuId: Long, ean: String): Boolean {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$HOST/api/catalog/pvt/stockkeepingunit/$skuId/ean/$ean"))
                .apply { headers.forEach {(key, value) -> header(key, value)} }
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            return response.statusCode() == 200
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    fun getXml(url: String, filePath: String): Path? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers("Accept", "application/xml")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(Paths.get(filePath)))

        if (response.statusCode() == 200)
            return response.body()

        return null
    }

    fun putProductById(productId: Long, body: String): HttpResponse<String> {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$HOST/api/catalog/pvt/product/$productId"))
                .apply {
                    header("Accept", "application/json")
                    header("Content-Type", "application/json")
                    headers.forEach {(key, value) -> header(key, value)}
                }
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build()

            return client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    fun putSkuById(skuId: Long, body: String): HttpResponse<String> {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$HOST/api/catalog/pvt/stockkeepingunit/$skuId"))
                .apply {
                    header("Accept", "application/json")
                    header("Content-Type", "application/json")
                    headers.forEach {(key, value) -> header(key, value)}
                }
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build()

            return client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    fun getProductById(productId: Long): String? {
        val response = getApi("$HOST/api/catalog/pvt/product/$productId")

        if (response?.statusCode() == 200)
            return response.body()

        return null
    }

    fun getSkuBySkuId(skuId: Long): String? {
        val response = getApi("$HOST/api/catalog/pvt/stockkeepingunit/$skuId")

        if (response?.statusCode() == 200)
            return response.body()

        return null
    }

    fun getPrice(skuId: Long, seller: String): String? {
        val response = getApi("$HOST2/$seller/pricing/prices/$skuId/computed/2")

        if (response?.statusCode() == 200)
            return response.body()

        return null
    }

    fun getPriceAsync(skuId: Long, seller: String): CompletableFuture<HttpResponse<String>> {
        return getApiAsync("$HOST2/$seller/pricing/prices/$skuId/computed/2")
    }

    fun getCatalogProductAsync(productId: String, bandeira: String): CompletableFuture<HttpResponse<String>>? {
        return when (bandeira) {
            "nacional" -> getApiAsync("$HOSTNC/api/catalog/pvt/product/$productId", headersnc)
            "bompreco" -> getApiAsync("$HOSTBP/api/catalog/pvt/product/$productId", headersbp)
            "tododia" -> getApiAsync("$HOSTTD/api/catalog/pvt/product/$productId", headerstd)
            else -> null
        }
    }

    fun putCatalogProductAsync(productId: String, body: String, bandeira: String): HttpResponse<String>? {
        val host: String
        val headers:  Map<String, String>

        when (bandeira) {
            "nacional" -> {
                host = HOSTNC
                headers = headersnc
            }
            "bompreco" -> {
                host = HOSTBP
                headers = headersbp
            }
            "tododia" -> {
                host = HOSTTD
                headers = headerstd
            }
            else -> return null
        }

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/api/catalog/pvt/product/$productId"))
                .apply {headers.forEach {(key, value) -> header(key, value)}}
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build()

            return client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    fun getProducts(fqs: List<String>): String? {
        var params = ""

        for (fq in fqs) params += "fq=${fq}&"

        val response = getApi("$HOST1/api/catalog_system/pub/products/search?${params.dropLast(1)}")

        if (response?.statusCode() == 200)
            return response.body()

        return null
    }

    fun getProductsAsync(fqs: List<String>): CompletableFuture<HttpResponse<String>> {
        var params = ""

        for (fq in fqs) params += "fq=${fq}&"

        return getApiAsync("$HOST1/io/api/catalog_system/pub/products/search?$params")
    }

    fun getProductsListAsync(fqs: String): CompletableFuture<HttpResponse<String>> {
//        return getApiAsync("$HOST1/api/catalog_system/pub/products/search?${fqs.dropLast(1)}")
        return getApiAsync("https://carrefourbr.vtexcommercestable.com.br/api/catalog_system/pub/products/search?$fqs")
//        return getApiAsync("https://carrefourbrfood.vtexcommercestable.com.br/api/catalog_system/pub/products/search?$fqs")
//        return getApiAsync("https://carrefourbr.vtexcommercestable.com.br/api/catalog_system/pub/products/search?$fqs")
    }

    fun getSkuByRMS(rms: String): String? {
        val response = getApi("$HOST/api/catalog_system/pvt/sku/stockkeepingunitidbyrefid/$rms")

        if (response?.statusCode() == 200)
            return response.body().replace("\"", "")

        return null
    }

    fun getProductByRefId(refId: String) =
        ProductVO(getApi("$HOST/api/catalog_system/pvt/products/productgetbyrefid/$refId")?.body()?.let {
            jacksonObjectMapper().readValue(it)
        })

    fun getSkuContextBySkuId(skuId: Long) =
        SkuContextVO(getApi("$HOST/api/catalog_system/pvt/sku/stockkeepingunitbyid/$skuId")?.body()?.let {
            jacksonObjectMapper().readValue(it)
        })

    fun getSkuContextBySkuIdAsync(skuId: Long) =
        getApiAsync("$HOST/api/catalog_system/pvt/sku/stockkeepingunitbyid/$skuId")

    fun getEansByskuId(skuId: Long) =
        getApi("$HOST/api/catalog/pvt/stockkeepingunit/$skuId/ean")?.body()
}
