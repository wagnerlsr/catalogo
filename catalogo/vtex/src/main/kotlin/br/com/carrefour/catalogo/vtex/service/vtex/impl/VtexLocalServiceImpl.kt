package br.com.carrefour.catalogo.vtex.service.vtex.impl

import br.com.carrefour.catalogo.core.exception.ResourceNotFoundException
import br.com.carrefour.catalogo.core.mapper.DozerMapper
import br.com.carrefour.catalogo.core.model.*
import br.com.carrefour.catalogo.core.repository.*
import br.com.carrefour.catalogo.vtex.data.vo.v1.DownloadFilesVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuContextVO
import br.com.carrefour.catalogo.vtex.data.vo.v1.SkuVO
import br.com.carrefour.catalogo.vtex.service.http.HttpService
import br.com.carrefour.catalogo.vtex.service.util.UtilService
import br.com.carrefour.catalogo.vtex.service.vtex.VtexLocalService
import br.com.carrefour.catalogo.vtex.utils.Constants
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import org.apache.logging.log4j.LogManager
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jsoup.Jsoup
import org.springframework.data.domain.PageRequest
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.math.RoundingMode
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*


@Service
class VtexLocalServiceImpl(private val skuRepository: SkuRepository,
                           private val correctionRepository: CorrectionRepository,
                           private val affiliateRepository: AffiliateRepository,
                           private val affiliate1Repository: Affiliate1Repository,
                           private val mdmRepository: MdmRepository,
                           private val mdmFDRepository: MdmFDRepository,
                           private val mdmNFRepository: MdmNFRepository,
                           private val similarRepository: SimilarRepository,
                           private val catalogRepository: CatalogRepository,
                           private val jmsTemplate: JmsTemplate,
                           private val productRepository: QueryProductRepository) : VtexLocalService {

    private val logger = LogManager.getLogger()


    override fun findBySkuId(skuId: Long): SkuVO =
        DozerMapper.parseObject(
            skuRepository.findBySkuId(skuId).orElseThrow { ResourceNotFoundException("SKU não encontrado") },
            SkuVO::class.java
        )

    override fun findBySkuIdRange(skuIdStart: Long, skuIdEnd: Long): List<SkuVO> =
        DozerMapper.parseListObject(skuRepository.findBySkuIdRange(skuIdStart, skuIdEnd), SkuVO::class.java)

    override fun downloadFiles(downloadFiles: DownloadFilesVO): List<String> = getLinksAndDownloadFiles(downloadFiles)

//    override fun importSkusWithEan19(): Set<Long> {
//        val skus = HashSet<Long>()
//
//        try {
//            val fileList = HashSet<String>()
//
//            Files.walk(Paths.get(exportDir)).use {
//                    paths -> paths.filter { Files.isRegularFile(it) }
//                .forEach { fileList.add(it.fileName.toString()) }
//            }
//
//            fileList.forEach { file ->
//                try {
//                    val fis = FileInputStream("$exportDir/$file")
//                    val xlWb = WorkbookFactory.create(fis)
//                    val xlWs = xlWb.getSheetAt(0)
//
//                    xlWs.forEach {
//                        try {
//                            val skuId = it.getCell(0).numericCellValue.toLong()
//                            val ean = it.getCell(2).stringCellValue
//
//                            if (UtilService.eanContains19(ean)) {
//                                val sku = skuRepository.findBySkuId(skuId).orElse(Sku(skuId));
//
//                                if (sku.id != null) {
//                                    sku.updateDate = Date();
//                                    sku.correctionDate = null;
//                                    sku.adjusted = false;
//                                }
//
//                                skuRepository.save(sku);
//                                skus.add(skuId)
//                            }
//                        }
//                        catch(_: Exception) {}
//                    }
//
//                    logger.info(">>>>> readSkusXml [$file] [${skus.size}]");
//
//                    fis.close();
//                } catch(e: Exception) {
//                    logger.error(String.format(">>>>> Erro readSkusXml [%s] [%s]", file, e))
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        return skus
//    }

//    override fun importProductsWithEan19() = importProducts19(true)
//
//    override fun importProductsWithRefId19() = importProducts19(false)


    override fun importProductsWithFields19(): Map<String, Any> {
        val imports = mutableMapOf<String, Any>()

        try {
            val fileList = HashSet<String>()
            var total = 0

            Files.walk(Paths.get(Constants.EXPORT_DIRECTORY)).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .forEach { fileList.add(it.fileName.toString()) }
            }

            fileList.forEach { file ->
                try {
                    logger.info(">>>>> $file");

                    val fis = FileInputStream("${Constants.EXPORT_DIRECTORY}/$file")
                    val xlWb = WorkbookFactory.create(fis)
                    val xlWs = xlWb.getSheetAt(0)

                    val headers: MutableMap<Int, String> = mutableMapOf()

                    xlWs.forEach { row ->
                        try {
                            if (row.rowNum == 0) {
                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
                            } else {
                                val fields: MutableMap<String, Any> = mutableMapOf()

                                row.forEach {
                                    when (it.cellType.name) {
                                        "NUMERIC" ->
                                            fields[headers[it.columnIndex].toString()] =
                                                it.numericCellValue.toLong()

                                        "STRING" ->
                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue

                                        else -> {
                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
                                        }
                                    }
                                }

                                if (fields["eansku"].toString().contains("1/9") ||
                                    fields["codigoreferenciaproduto"].toString().contains("1/9") ||
                                    fields["codigoreferenciasku"].toString().contains("1/9")
                                ) {
                                    val productId = fields["idproduto"] as Long?
                                    val skuId = fields["idsku"] as Long?

                                    productId?.let { prodId ->
                                        if (skuId != null) {
                                            val correction = correctionRepository.findByProductIdAndSkuId(prodId, skuId)
                                                .orElse(Correction(prodId, skuId))

                                            correction.apply {
                                                updateDate = Date()

                                                if (fields["codigoreferenciaproduto"].toString().contains("1/9"))
                                                    hasCorrectionProduct = true

                                                if (fields["codigoreferenciasku"].toString().contains("1/9"))
                                                    hasCorrectionSku = true

                                                if (fields["eansku"].toString().contains("1/9"))
                                                    hasCorrectionEans = true

                                                if (fields["eansku"].toString().contains("1/9"))
                                                    logger.info("Sku [ ${fields["idsku"].toString()} ]   RMS [ ${fields["codigoreferenciaproduto"].toString()} ]");
                                            }

                                            correctionRepository.save(correction)

                                            total = total.inc()

                                            val ids = "$total >> SkuId [${fields["idsku"].toString()}] ProductId [${fields["idproduto"].toString()}] >> SkuEan: ${fields["eansku"].toString()} >> RefIdProd: ${fields["codigoreferenciaproduto"].toString()} >> RefIdSku: ${fields["codigoreferenciasku"].toString()}"
                                            imports[total.toString()] = ids

//                                            logger.info(ids)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error(">>>>> importProductsWithFields19 (3) [$file] [$e]")
                        }
                    }

//                    logger.info(">>>>> readSkusXml [$file]");

                    fis.close();

                } catch (e: Exception) {
                    logger.error(">>>>> importProductsWithFields19 (2) [$file] [$e]")
                }
            }
        } catch (e: Exception) {
            logger.error(">>>>> importProductsWithFields19 (1) [$e]")
        }

        return imports
    }


    override fun importProductsWithSimilares(): Map<String, Any> {
        val imports = mutableMapOf<String, Any>()

        try {
            val fileList = HashSet<String>()
            var total = 0

            Files.walk(Paths.get(Constants.SIMILAR_DIRECTORY)).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .forEach { fileList.add(it.fileName.toString()) }
            }

            fileList.forEach { file ->
                try {
                    val fis = FileInputStream("${Constants.SIMILAR_DIRECTORY}/$file")
                    val xlWb = WorkbookFactory.create(fis)
                    val xlWs = xlWb.getSheetAt(0)

                    val headers: MutableMap<Int, String> = mutableMapOf()

                    xlWs.forEach { row ->
                        try {
                            if (row.rowNum == 0) {
                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
                            } else {
                                val fields: MutableMap<String, Any> = mutableMapOf()

                                row.forEach {
                                    when (it.cellType.name) {
                                        "NUMERIC" ->
                                            fields[headers[it.columnIndex].toString()] =
                                                it.numericCellValue.toLong()

                                        "STRING" ->
                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue

                                        else -> {
                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
                                        }
                                    }
                                }

                                if (!(fields["codigoreferenciaproduto"].toString().lowercase().contains("mp") ||
                                            fields["codigoreferenciaproduto"].toString().lowercase().contains("mv"))
                                ) {
                                    val productId = fields["idproduto"] as Long?
                                    val skuId = fields["idsku"] as Long?

                                    productId?.let { prodId ->
                                        if (skuId != null) {
                                            val s = similarRepository.findByProductIdAndSkuId(prodId, skuId)
                                                .orElse(Similar(prodId, skuId))

                                            val sim = fields["similares"] as String?

                                            s.apply {
                                                updateDate = Date()

                                                productRefId = fields["codigoreferenciaproduto"] as String?
                                                productName = fields["nomeproduto"] as String?
                                                categoryName = fields["nomecategoria"] as String?
                                                similar = if (sim == null || sim.trim() == "") listOf() else getSimilar(sim)
                                            }

                                            similarRepository.save(s)

//                                            total = total.inc()
//
//                                            val ids = "$total >> SkuId [${fields["idsku"].toString()}] ProductId [${fields["idproduto"].toString()}] >> SkuEan: ${fields["eansku"].toString()} >> RefIdProd: ${fields["codigoreferenciaproduto"].toString()} >> RefIdSku: ${fields["codigoreferenciasku"].toString()}"
//                                            imports[total.toString()] = ids

//                                            logger.info(ids)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error(">>>>> importProductsWithSimilares (3) [$file] [$e]")
                        }
                    }

                    logger.info(">>>>> readSkusXml [$file]");

                    fis.close();

                } catch (e: Exception) {
                    logger.error(">>>>> importProductsWithSimilares (2) [$file] [$e]")
                }
            }
        } catch (e: Exception) {
            logger.error(">>>>> importProductsWithSimilares (1) [$e]")
        }

        return imports
    }


    override fun exportMdm(): String {
        val time1 = System.currentTimeMillis()
        var total = 0
        var totale = 0
        val fileName = "${Constants.MDM_DIRECTORY}mdm.json"

        logger.info(">>> Lendo banco...")

        try {
            val page = 0
            val produtos = mdmFDRepository.findAllByType(3, PageRequest.of(page, 100000))

            logger.info(">>> Gerando arquivo  $fileName")

            FileWriter(fileName, true).use { out -> out.append("[\n")}

            produtos.forEach { produto ->
                total++

                if (produto.product != null) {
                    try {
                        val prod: Map<String, *>? = try {
                            jacksonObjectMapper().readValue<Map<String, *>>("${produto.product}")
                        } catch (e: Exception) {
                            try {
                                jacksonObjectMapper().readValue<Map<String, *>>(jacksonObjectMapper().writeValueAsString(produto.product))
                            } catch (_: Exception) {
                                null
                            }
                        }

                        FileWriter(fileName, true).use { out -> out.append(jacksonObjectMapper().writeValueAsString(prod)+",\n")}
                    } catch (e: Exception) {
                        totale++
                        logger.error("ERROR exportMdm file [ $page ] [ $total ] [ $totale ] [ ${produto.productId} ] [$e]")
                    }
                }

                if ((total % 1000) == 0)
                    logger.info(
                        "$total / ${produtos.size} >> $totale >> ${
                            UtilService.longToTimeStr(
                                System.currentTimeMillis() - time1
                            )
                        }"
                    )
            }
        } catch (e: Exception) {
            logger.error("ERROR exportMdm [$e]")
        }

        FileWriter(fileName, true).use { out -> out.append("]\n")}
        FileWriter(fileName, true).close()

        logger.info("Finalizado >> $total >> $totale >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }

    override fun importMdm(): String {
        val time1 = System.currentTimeMillis()
        var total = 0
        var tot1p = 0
        var tot3p = 0
        var error = 0

        try {
            val fileList = HashSet<String>()

            Files.walk(Paths.get(Constants.EXPORT_DIRECTORY)).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .forEach { fileList.add(it.fileName.toString()) }
            }

            fileList.sorted().forEach { file ->
                try {
                    logger.info("FILE: $file");

                    val fis = FileInputStream("${Constants.EXPORT_DIRECTORY}/$file")
                    val xlWb = WorkbookFactory.create(fis)
                    val xlWs = xlWb.getSheetAt(0)

                    val headers: MutableMap<Int, String> = mutableMapOf()

                    xlWs.forEach { row ->
                        try {
                            if (row.rowNum == 0) {
                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
                            } else {
                                val fields: MutableMap<String, Any> = mutableMapOf()

                                row.forEach {
                                    when (it.cellType.name) {
                                        "NUMERIC" ->
                                            fields[headers[it.columnIndex].toString()] =
                                                it.numericCellValue.toLong()

                                        "STRING" ->
                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue

                                        else -> {
                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
                                        }
                                    }
                                }

                                val productId = fields["idproduto"] as Long
                                val skuId = fields["idsku"] as Long
                                val type = if (fields["condicaocomercial"].toString().uppercase().startsWith("1P")) 1 else 3

                                if (type == 1) tot1p++ else tot3p++

//                                if (mdmRepository.findByProductIdAndSkuId(productId, skuId).isEmpty) {
                                    val mdm = Mdm(productId, skuId)

                                    mdm.skuId = skuId
                                    mdm.productId = productId
                                    mdm.type = type

                                    mdmRepository.save(mdm)
//                                }

                                total++

                                if ((total % 1000) == 0)
                                    logger.info("$total > $tot1p > $tot3p > ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
                            }
                        } catch (e: Exception) {
                            error++
                            logger.error("ERROR importMdm xml [$file] [$e]")
                        }
                    }

                    logger.info("$total > $tot1p > $tot3p > ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

                    fis.close();

                } catch (e: Exception) {
                    logger.error("ERROR importMdm file [$file] [$e]")
                }
            }
        } catch (e: Exception) {
            logger.error("ERROR importMdm [$e]")
        }

        logger.info("Finalizado > $total > $tot1p > $tot3p > ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }

    override fun getMdmVtex(): String {
        var time1 = 0L
        var total = 0
        var error = 0
        var calls = 0

        val fqs = mutableListOf<String>()
        val ids = mutableMapOf<Long, Any>()

        logger.info("Lendo banco...")

        try {
            val page = 5
            val produtos = mdmRepository.findAll(PageRequest.of(page, 5000000))  // 2000000

            logger.info("Inicio processo...")

            time1 = System.currentTimeMillis()

            produtos.forEach { produto ->
                total++

                if (produto.productId != null) {
                    fqs.add("productId:${produto.productId}")
                    ids[produto.productId!!] = "${produto.skuId}:${produto.type}"
                }

                if (fqs.size == 5) {
                    try {
                        var params = ""

                        for (fq in fqs) params += "fq=${fq}&"

                        val result = HttpService.getProductsListAsync(params.dropLast(1))    ///// NF

                        result.thenApply { response: HttpResponse<*> ->
                            if (response.statusCode() == 200) {
                                val prods: List<MutableMap<String, Any>?>? =
                                    jacksonObjectMapper().readValue(response.body().toString())

                                prods?.forEach { prod ->
                                    val pid = prod?.get("productId").toString().toLong()
                                    val values = ids[pid].toString().split(":")

                                    val mdm = MdmNF(pid, values[0].toLong())    ///// NF
                                    mdm.type = values[1].toInt()
                                    mdm.food = false;   ///// NF
                                    mdm.product = jacksonObjectMapper().writeValueAsString(prod)

                                    try {
                                        mdmNFRepository.save(mdm)    ///// NF
                                    } catch (e: Exception) {
                                        logger.error("ERROR SAVE [ $page ] [ $total ] [ $error ] [ $params ] [ ${e.message} ]")
                                    }
                                }
                            }
                        } ?.exceptionally {
                            error++
                            logger.error("Error Context [ $page ] [ $total ] [ $error ] [ $params ] [ ${it.message} ]")
                        }

                        calls++

                        if (calls >= 50) {
                            calls = 0
                            try {
                                Thread.sleep(2000)
                            } catch (ignored: Exception) {
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error  [ $page ] [${produtos.size}] [$e]")
                    }

                    fqs.clear()
                }

                if ((total % 10000) == 0)
                    logger.info("$page > $total / ${produtos.size} > $error > ${UtilService.longToTimeStr(System.currentTimeMillis() - time1)}")
            }
        } catch (e: Exception) {
            logger.error("Error [$e]")
        }

        logger.info("Finalizado > $total > $error > ${UtilService.longToTimeStr(System.currentTimeMillis() - time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }


    override fun getVariants3P(): String {
        val time1 = System.currentTimeMillis()
        var total = 0
        var totas = 0
        var error = 0
        var calls = 0

        try {
            val fileList = HashSet<String>()

            Files.walk(Paths.get(Constants.SIMILAR_DIRECTORY)).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .forEach { fileList.add(it.fileName.toString()) }
            }

            fileList.sorted().forEach { file ->
                try {
                    logger.info(">>>>> $file");

                    val fis = FileInputStream("${Constants.SIMILAR_DIRECTORY}/$file")
                    val xlWb = WorkbookFactory.create(fis)
                    val xlWs = xlWb.getSheetAt(0)

                    val headers: MutableMap<Int, String> = mutableMapOf()

                    xlWs.forEach { row ->
                        try {
                            if (row.rowNum == 0) {
                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
                            } else {
                                val fields: MutableMap<String, Any> = mutableMapOf()

                                row.forEach {
                                    when (it.cellType.name) {
                                        "NUMERIC" ->
                                            fields[headers[it.columnIndex].toString()] =
                                                it.numericCellValue.toLong()

                                        "STRING" ->
                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue

                                        else -> {
                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
                                        }
                                    }
                                }

                                //////////////////  1P / 3P  \\\\\\\\\\\\\\\\\\\\\
                                if ((fields["codigoreferenciaproduto"].toString().lowercase().contains("mp") ||
                                            fields["codigoreferenciaproduto"].toString().lowercase().contains("mv"))
                                ) {
                                    val productId = fields["idproduto"] as Long?
                                    val skuId = fields["idsku"] as Long?

                                    productId?.let { prodId ->
                                        if (skuId != null) {
//                                            val s = similarRepository.findByProductIdAndSkuId(prodId, skuId)
//                                                .orElse(Similar(prodId, skuId))

                                            val s = Similar(prodId, skuId)

                                            val sim = fields["similares"] as String?

                                            s.apply {
                                                updateDate = Date()

                                                productRefId = fields["codigoreferenciaproduto"] as String?
                                                productName = fields["nomeproduto"] as String?
                                                categoryName = fields["nomecategoria"] as String?
                                                similar = if (sim == null || sim.trim() == "") listOf() else getSimilar(sim)
                                            }

                                            if (!s.variant) {
                                                try {
                                                    val result = s.skuId?.let { skuId -> HttpService.getSkuContextBySkuIdAsync(skuId) }

                                                    result?.thenApply { obj: HttpResponse<*> -> obj.body() }
                                                        ?.thenAccept { body ->
                                                            val context = SkuContextVO(jacksonObjectMapper().readValue(body.toString()))
                                                            val specifications = context?.map?.get("ProductSpecifications") as List<Map<String, Any>>

                                                            s.variants.clear()

                                                            specifications.forEach {
                                                                if ((it["FieldGroupName"] as String).lowercase().trim() == "variantes") {
                                                                    s.variants.add("${it["FieldName"]},${it["FieldValues"].toString().dropLast(1).drop(1)}")
                                                                }
                                                            }

                                                            s.variant = true

                                                            similarRepository.save(s)

                                                            totas++

                                                        }
                                                        ?.exceptionally {
                                                            error++
                                                            logger.error(">>>>> Error Context getVariants3P [ $error ] [ $total ] [ $totas ] [ ${it.message} ]")
                                                            null
                                                        }

                                                    calls++

                                                    if (calls >= 50) {
                                                        calls = 0
//                                                        logger.info(">>> $total / ${xlWs.lastRowNum} [ $totas ] >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
                                                        try {
                                                            Thread.sleep(2000)
                                                        } catch (ignored: Exception) {
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    logger.error(">>>>> getVariants3P (4) [${s.skuId}] [$e]")
                                                }
                                            }

//                                            jmsTemplate.convertAndSend("carrefour_vtex_queue", Gson().toJson(s))

                                            total = total.inc()

                                            if ((total % 1000) == 0)
                                                logger.info(">>> $total / ${xlWs.lastRowNum} [ $totas ] >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} >> ${s.skuId} >> ${s.productRefId}")

//                                            if (total > 1000)  return "$total / ${xlWs.lastRowNum} [${System.currentTimeMillis()-time1}] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error(">>>>> getVariants3P (3) [$file] [$e]")
                        }
                    }

                    logger.info(">>> $total / ${xlWs.lastRowNum} >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
//                    logger.info("<<<<<<<<<<<<<<< ${System.currentTimeMillis() - time1} >>>>>>>>>>>>>>>")

                    fis.close();

                } catch (e: Exception) {
                    logger.error(">>>>> getVariants3P (2) [$file] [$e]")
                }
            }
        } catch (e: Exception) {
            logger.error(">>>>> getVariants3P (1) [$e]")
        }

        logger.info(">>> Finalizado >> $total >> $totas >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }


    override fun getAffiliateDb(): String {
        val time1 = System.currentTimeMillis()
        var total = 0
        var totaf = 0
        var totag = 0

        try {
            val fileList = HashSet<String>()

            Files.walk(Paths.get(Constants.EXPORT_DIRECTORY)).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .forEach { fileList.add(it.fileName.toString()) }
            }

            fileList.sorted().forEach { file ->
                try {
                    logger.info(">>> $totaf - $file");

                    val fis = FileInputStream("${Constants.EXPORT_DIRECTORY}/$file")
                    val xlWb = WorkbookFactory.create(fis)
                    val xlWs = xlWb.getSheetAt(0)

                    val headers: MutableMap<Int, String> = mutableMapOf()

                    totag += xlWs.lastRowNum
                    totaf++

                    xlWs.forEach { row ->
                        try {
                            if (row.rowNum == 0) {
                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
                            } else {
                                val fields: MutableMap<String, Any> = mutableMapOf()

                                row.forEach {
                                    when (it.cellType.name) {
                                        "NUMERIC" ->
                                            fields[headers[it.columnIndex].toString()] =
                                                it.numericCellValue.toLong()

                                        "STRING" ->
                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue

                                        else -> {
                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
                                        }
                                    }
                                }

                                val productId = fields["idproduto"] as Long?
                                val skuId = fields["idsku"] as Long?

                                productId?.let { prodId ->
                                    if (skuId != null) {
//                                        val affiliate = affiliateRepository.findByProductIdAndSkuId(prodId, skuId).orElse(Afilliate(prodId, skuId))
                                        val affiliate = Affiliate(prodId, skuId)

                                        affiliate.apply {
                                            productRefId = fields["codigoreferenciaproduto"] as String?
                                            skuRefId = fields["codigoreferenciasku"] as String?
                                        }

                                        affiliateRepository.save(affiliate)

                                        total = total.inc()

                                        if ((total % 10000) == 0)
                                            logger.info(">>> $total / $totag >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error(">>>>> getAffiliateDb (3) [$file] [$e]")
                        }
                    }

                    logger.info(">>> $total / ${xlWs.lastRowNum} [ $totag ] >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

                    fis.close();

                } catch (e: Exception) {
                    logger.error(">>>>> getAffiliateDb (2) [$file] [$e]")
                }
            }
        } catch (e: Exception) {
            logger.error(">>>>> getAffiliateDb (1) [$e]")
        }

        logger.info(">>> Finalizado >> $total >> $totag >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }

    override fun getAffiliateVtexList(): String {
        var time1 = 0L
        var total = 0
        var totin = 0
        var tot1p = 0
        var tot3i = 0
        var tot3o = 0
        var tot99 = 0
        var error = 0
        var evtex = 0
        var calls = 0

        val fqs = mutableListOf<String>()
        val errfqs = mutableListOf<String>()

        logger.info(">>> Lendo banco...")

        try {
            val page = 0
            val produtos = affiliate1Repository.findAll(PageRequest.of(page, 200000))  // 5000000

            logger.info(">>> Inicio processo...")

            time1 = System.currentTimeMillis()

            produtos.forEach { produto ->
                total++

                if ((produto.productRefId?.lowercase()?.contains("mp") == true || produto.productRefId?.lowercase()?.contains("mv") == true))
                    tot3i++

//                if (produto.skuId != null) fqs.add("skuId:${produto.skuId}")
                if (produto.productId != null) fqs.add("productId:${produto.productId}")

                if (fqs.size == 5) {
                    try {
                        var params = ""

                        for (fq in fqs) params += "fq=${fq}&"

                        val result = HttpService.getProductsListAsync(params.dropLast(1))

                        result.thenApply { response: HttpResponse<*> ->
                            if (response.statusCode() == 200) {
                                val prods: List<MutableMap<String, Any>?>? =
                                    jacksonObjectMapper().readValue(response.body().toString())

                                prods?.forEach { prod ->
                                    var tip = 11
                                    val pid = prod?.get("productId").toString().toLong()
                                    var rid = ""

                                    try {
                                        rid =
                                            ((((prod?.get("items") as List<*>)[0] as MutableMap<*, *>?)?.get("referenceId") as List<*>)[0] as MutableMap<*, *>?)?.get(
                                                "Value"
                                            ).toString()
                                    } catch (_: Exception) {
                                    }

                                    if (rid.lowercase().contains("mp") || rid.lowercase().contains("mv")) {
                                        tip = 13
                                        tot3o++
                                    }

//                                    if (pid == 986L)
//                                        tip = 99

                                    val affiliate = affiliate1Repository.findByProductId(pid).orElse(Affiliate1(pid))
                                    affiliate.productRefId = rid
                                    affiliate.product = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(prod)
                                    affiliate.type = tip

                                    try {
                                        affiliate1Repository.save(affiliate)
                                    } catch (e: Exception) {
                                        tot99++
                                        affiliate.type = 99
//                                        affiliate.product = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(prod)
                                        affiliate1Repository.save(affiliate)
                                        logger.info(">>> DB ERROR [ $page ] [ $total ] [ $totin ] [ $tot3i ] [ $tot3o ] [ $evtex ] [ $error ] [ $pid ] [ $e ]")
                                    }

                                    totin++
                                }

//                                logger.info(">>> VTEX [ $page ] [ $total ] [ $totin ] [ $tot3i ] [ $tot3o ] [ $evtex ] [ $error ]")
                            } else {
                                evtex++
                                logger.info(">>> VTEX Not found [ $page ] [ $total ] [ $totin ] [ $tot3o ] [ $tot99 ] [ $evtex ] [ $error ] [ $params ]")
                            }
                        } ?.exceptionally {
                            error++
                            logger.error(">>>>> Error Context getAffiliateVtex [ $page ] [ $total ] [ $totin ] [ $tot3o ] [ $tot99 ] [ $evtex ] [ $error ] [ $params ] [ ${it.message} ]")
                        }

                        calls++

                        if (calls >= 50) {
                            calls = 0
                            try {
                                Thread.sleep(2000)
                            } catch (ignored: Exception) {
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(">>>>> getAffiliateVtex (2) [ $page ] [${produtos.size}] [$e]")
                    }

                    fqs.clear()
                }

                if ((total % 10000) == 0)
                    logger.info(
                        ">>> $page >> $total / ${produtos.size} >> $totin >> $tot3i >> $tot3o >> $tot99 >> $evtex >> $error >> ${
                            UtilService.longToTimeStr(
                                System.currentTimeMillis() - time1
                            )
                        }"
                    )
            }
        } catch (e: Exception) {
            logger.error(">>>>> getAffiliateVtex (1) [$e]")
        }

        logger.info(">>> Finalizado >> $total >> $totin >> $tot3i >> $tot3o >> $evtex >> $error >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }


    override fun getAffiliateVtex(page: Int): String {
        val time1 = System.currentTimeMillis()
        var total = 0
        var calls = 0

        logger.info(">>> Lendo banco...")

        try {
//            val page = 0
            val produtos = affiliateRepository.findAll(PageRequest.of(page, 2000))

            logger.info(">>> Inicio processo...")

            produtos.forEach { produto ->
                total++

                    try {
                        val result =
                            produto.skuId?.let { skuId -> HttpService.getSkuContextBySkuIdAsync(skuId) }

                        result?.thenApply { response: HttpResponse<*> ->
                            if (response.statusCode() == 200) {
                                val p: MutableMap<String, Any>? =
                                    jacksonObjectMapper().readValue(response.body().toString())

                                val pid = produto.productId
                                val affiliate = pid?.let { affiliate1Repository.findByProductId(it).orElse(Affiliate1(pid)) }

                                if (affiliate != null) {
                                    affiliate.productId = pid
                                    affiliate.skuId = produto.skuId
                                    affiliate.type = 0
                                    affiliate.product = p

                                    affiliate1Repository.save(affiliate)
                                }
                          } else {
                                logger.info(">>> Not found [ $page ] [ ${produto.skuId} ] [ $total ]")
                            }
                        }
                        ?.exceptionally {
                            logger.error(">>> Error Context getAffiliateVtex [ $page ] [ ${produto.skuId} ] [ $total ] [ [ ${it.message} ]")
                        }

                        calls++

                        if (calls >= 50) {
                            calls = 0
                            try {
                                Thread.sleep(2000)
                            } catch (ignored: Exception) {
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(">>>>> getAffiliateVtex (2) [ $page ] [${produtos.size}] [$e]")
                    }

                if ((total % 1000) == 0)
                    logger.info(
                        ">>> $page >> $total / ${produtos.size} >> ${
                            UtilService.longToTimeStr(
                                System.currentTimeMillis() - time1
                            )
                        }"
                    )
            }
        } catch (e: Exception) {
            logger.error(">>>>> getAffiliateVtex (1) [$e]")
        }

        logger.info(">>> Finalizado >> $total >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [$page] [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }

//    override fun getAffiliateVtex(): String {
//        return ""
////        val time1 = System.currentTimeMillis()
////        var total = 0
////        var totaf = 0
////        var totan = 0
////        var tot13 = 0
////        var error = 0
////        var calls = 0
////        val pageS = 8
////        val pageE = 9
////
////        logger.info(">>> Lendo banco...")
////
////        try {
////            val page = 6
//////            for (page in pageS..pageE) {
////            val produtos = affiliateRepository.findAll(PageRequest.of(page, 5000000))
////
////            logger.info(">>> Inicio processo...")
////
////            produtos.forEach { produto ->
////                total++
////
////                if (!(produto.productRefId?.lowercase()?.contains("mp") == true ||
////                            produto.productRefId?.lowercase()?.contains("mv") == true) && produto.updated == false
////                ) {
////                    tot13++
////
////                    try {
////                        val result =
////                            produto.skuId?.let { skuId -> HttpService.getProductsAsync(listOf("skuId:$skuId")) }
////
////                        result?.thenApply { response: HttpResponse<*> ->
////                            if (response.statusCode() == 200) {
////                                val p: List<MutableMap<String, Any>?>? =
////                                    jacksonObjectMapper().readValue(response.body().toString())
////                                produto.type = 1
////                                produto.updated = true
////                                produto.product = if (p?.size!! > 0) p[0] else null
////
////                                affiliateRepository.save(produto)
////
////                                totaf++
////                            } else {
////                                totan++
////                                logger.info(">>> Not found [ $page ] [ ${produto.skuId} ] [ $error ] [ $total ] [ $totaf ] [ $tot13 ] [ $totan ]")
////                            }
////                        }
////                            ?.exceptionally {
////                                error++
////                                logger.error(">>>>> Error Context getAffiliateVtex [ $page ] [ ${produto.skuId} ] [ $error ] [ $total ] [ $totaf ] [ $tot13 ] [ $totan ] [ ${it.message} ]")
////                                null
////                            }
////
////                        calls++
////
////                        if (calls >= 50) {
////                            calls = 0
////                            try {
////                                Thread.sleep(2000)
////                            } catch (ignored: Exception) {
////                            }
////                        }
////                    } catch (e: Exception) {
////                        logger.error(">>>>> getAffiliateVtex (2) [ $page ] [${produtos.size}] [$e]")
////                    }
////                }
////
////                if ((total % 100000) == 0)
////                    logger.info(
////                        ">>> $page >> $total / ${produtos.size} >> $totaf >> $tot13 >> $totan >> ${
////                            UtilService.longToTimeStr(
////                                System.currentTimeMillis() - time1
////                            )
////                        }"
////                    )
////            }
//////            }  ////// for
////        } catch (e: Exception) {
////            logger.error(">>>>> getAffiliateVtex (1) [$e]")
////        }
////
////        logger.info(">>> Finalizado >> $total >> $totaf >> $tot13 >> $totan >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
////
////        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//    }


    override fun putFlagsBandeiras(): String {
        val bandeira = "tododia"

        val time1 = System.currentTimeMillis()
        var calls = 0
        var total = 0
        var totalg = 0
        var totalp = 0
        var totaeg = 0
        var totale = 0
        var totlin = 0

        try {
            logger.info(">>> Iniciando processo [ $bandeira ]")

            totlin = File("/home/wagner/work/Carrefour/develop/ativacao/catalogo/vtex/src/main/resources/$bandeira.csv").readLines().size

            File("/home/wagner/work/Carrefour/develop/ativacao/catalogo/vtex/src/main/resources/$bandeira.csv").forEachLine { pid ->
                total++

                try {
                    val result = HttpService.getCatalogProductAsync(pid, bandeira)

                    if (result != null) {
                        result.thenApply { response: HttpResponse<*> ->
                            if (response.statusCode() == 200) {
                                totalg++

                                val payload: MutableMap<String, Any>? = jacksonObjectMapper().readValue(response.body().toString())

                                if (payload != null) {
                                    if (!(payload["ShowWithoutStock"]?.toString().toBoolean() ?: false)) {
                                        payload["ShowWithoutStock"] = true
                                        var res = HttpService.putCatalogProductAsync(pid, jacksonObjectMapper().writeValueAsString(payload), bandeira)
                                        totalp++
                                    }
                                }
                            }
                        } ?.exceptionally {
                            totaeg++
                            logger.error("Error GET putFlagsBandeiras [ $total / $totlin ] [ $totalg ] [ $totalp ] [ $totaeg ] [ $totale ] [ ${it.message} ]")
                        }
                    }

                    if ((total % 100) == 0)
                        logger.info(
                            ">>> [ $total / $totlin ] [ $totalg ] [ $totalp ] [ $totaeg ] [ $totale ] >> ${
                                UtilService.longToTimeStr(System.currentTimeMillis() - time1)
                            }")

                    calls++

                    if (calls >= 50) {
                        calls = 0
                        try {
                            Thread.sleep(2000)
                        } catch (ignored: Exception) {
                        }
                    }
                } catch (e: Exception) {
                    totale++
                    logger.error("Error putFlagsBandeiras [ $total / $totlin ] [ $totalg ] [ $totalp ] [ $totaeg ] [ $totale ] [ ${e.message} ]")
                }
            }
        } catch (e: Exception) {
            logger.error("Error putFlagsBandeiras (1) [$e]")
        }

        logger.info("Finalizado [ $bandeira ] >> [ $total / $totlin ] [ $totalg ] [ $totalp ] [ $totaeg ] [ $totale ]  >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "[ $bandeira ]  $total / $totlin [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }


    override fun getAffiliatePrice(): String {
        val time1 = System.currentTimeMillis()
        var calls = 0
        var total = 0
        var totalp = 0
        var total0 = 0
        var total1 = 0
        var totale = 0
        var error = 0

        logger.info(">>> Lendo banco...")

        try {
            val page = 0
            val produtos = affiliate1Repository.findAll(PageRequest.of(page, 100000))

            logger.info(">>> Consultando...")

            produtos.forEach { produto ->
                total++

                if (produto.product != null) {
                    try {
                        val result = produto.skuId?.let { HttpService.getPriceAsync(it, "carrefourbrfood149") }

                        if (result != null) {
                            result.thenApply { response: HttpResponse<*> ->
                                if (response.statusCode() == 200) {
                                    totalp++

                                    val price: MutableMap<String, Any>? = jacksonObjectMapper().readValue(response.body().toString())

                                    var pricep = convertToDouble(price?.get("sellingPrice") as Any)

                                    if (pricep == null) {
                                        total1++
                                        pricep = 0.0
                                    }

                                    produto.price = pricep

                                    if (produto.price == 0.0) total0++

                                    affiliate1Repository.save(produto)
                                }
                            } ?.exceptionally {
                                error++
                                logger.error(">>>>> Error Context getAffiliateVtex [ $page ] [ $total ] [ $totalp ] [ $total0 ] [ $total1 ] [ $totale ] [ $error ] [ ${it.message} ]")
                            }
                        }

                        calls++

                        if (calls >= 50) {
                            calls = 0
                            try {
                                Thread.sleep(2000)
                            } catch (ignored: Exception) {
                            }
                        }

                    } catch (e: Exception) {
                        totale++
                        logger.error(">>>>> getAffiliateExport (2) [ $page ] [$total] [$totalp] [ $total0 ] [ $total1 ] [$totale] [${produtos.size}] [$e]")
                    }
                }

                if ((total % 1000) == 0)
                    logger.info(
                        ">>> $page >> $total / ${produtos.size} >> $totalp >> $total0 >> $total1 >> $totale >> ${
                            UtilService.longToTimeStr(
                                System.currentTimeMillis() - time1
                            )
                        }"
                    )
            }
        } catch (e: Exception) {
            logger.error(">>>>> getAffiliateExport (1) [$e]")
        }

        logger.info(">>> Finalizado >> $total >> $totalp >> $total0 >> $total1 >> $totale >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }


//    override fun getAffiliateExport(): String {
//        val time1 = System.currentTimeMillis()
//        var total = 150
//        val fileName = "${Constants.AFFILIATE_DIRECTORY}productsup.json"
//
//        logger.info(">>> Lendo banco...")
//
//        try {
//            val page = 0
//            val produtos = affiliate1Repository.findAll(PageRequest.of(page, 70000))
//
//            logger.info(">>> Gerando arquivo  $fileName")
//
//            FileWriter(fileName, true).use { out -> out.append("[\n")}
//
//            produtos.forEach { produto ->
//                total++
//
//                if (produto.product != null) {
//                    try {
//                        var prod = ""
//
//                        try {
//                            prod = "${produto.product}"
//                            jacksonObjectMapper().readTree(prod)
//                        } catch (e: Exception) {
//                            try {
//                                prod = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(produto.product)
//                            } catch (e: Exception) {
//                            }
//                        }
//
//                        FileWriter(fileName, true).use { out -> out.append("$prod,\n")}
//                    } catch (e: Exception) {
//                        logger.error(">>>>> getAffiliateExport (2) [ $page ] [${produtos.size}] [$e]")
//                    }
//                }
//
//                if ((total % 10000) == 0)
//                    logger.info(
//                        ">>> $page >> $total / ${produtos.size} >> ${
//                            UtilService.longToTimeStr(
//                                System.currentTimeMillis() - time1
//                            )
//                        }"
//                    )
//            }
//        } catch (e: Exception) {
//            logger.error(">>>>> getAffiliateExport (1) [$e]")
//        }
//
//        FileWriter(fileName, true).use { out -> out.append("]\n")}
//        FileWriter(fileName, true).close()
//
//        logger.info(">>> Finalizado >> $total >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
//
//        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//    }



    fun convertToDouble(vararg any: Any) : Double? {
        return when(val tmp = any.first()) {
            is Number -> tmp.toDouble()
            else -> null
        }
    }

    fun getPrice(price: Double?, priceb: Double?): Double {
        if (price != null && price > 0) return price
        if (priceb != null && priceb > 0) return priceb
        return 0.0
    }

    fun getAvailability(availability: Boolean): String {
        return if (availability) "in_stock" else "out_of_stock"
    }

    fun formatUrl(url: String?): String? {
        if (url == null || url.trim() == "") return null

        var tmp = if ("${url.first()}" == "/") url else "/$url"
        tmp = if ("${url.last()}" == "/") "${tmp}p" else "$tmp/p"

        return "https://mercado.carrefour.com.br$tmp"
    }

    fun getCategory(desc: String): String {
        return try {
            return desc.trim().split("/").last { it != "" }.replace(",", "")
        } catch (e: Exception) {
            "' '"
        }
    }

    override fun getAffiliateExport(): String {
        val time1 = System.currentTimeMillis()
        var total = 0
        var totalp = 0
        var total0 = 0
        var totall = 0
        var totale = 0
        val fileName = "${Constants.AFFILIATE_DIRECTORY}affiliate.json"
        val fileNameCsv = "${Constants.AFFILIATE_DIRECTORY}affiliate.csv"

        logger.info(">>> Lendo banco...")

        try {
            val page = 0
            val produtos = affiliate1Repository.findAll(PageRequest.of(page, 100000))

            logger.info(">>> Gerando arquivo  $fileName")

            FileWriter(fileName, true).use { out -> out.append("[\n")}
            FileWriter(fileNameCsv, true).use { out -> out.append("id,title,description,link,imageLink,availability,availability_date,price,brand_name,gtin,mpn,condition,adult,multipack,bundle,age_group,color,gender,material,pattern,size,item_group_id,category_id,category_name,region_id\r\n")}

            produtos.forEach { produto ->
                total++

                if (produto.product != null) {
                    try {
                        val prod: Map<String, *>? = try {
                            jacksonObjectMapper().readValue<Map<String, *>>("${produto.product}")
                        } catch (e: Exception) {
                            try {
                                jacksonObjectMapper().readValue<Map<String, *>>(jacksonObjectMapper().writeValueAsString(produto.product))
                            } catch (_: Exception) {
                                null
                            }
                        }

                        val obj = jacksonObjectMapper().valueToTree<JsonNode>(prod)

                        val priceb: Any = try { obj.get("items").get(0).get("sellers").get(0).get("commertialOffer").get("Price").asText("0.0") } catch (e: Exception) { "0.0" }
                        val price = getPrice(produto.price, convertToDouble(priceb))

                        val productTitle = try { "'${obj.get("productTitle").asText("' '")}'" } catch (e: Exception) { "' '" }
                        val description  = try { "'${obj.get("description").asText("' '")}'" } catch (e: Exception) { "' '" }

                        val link = formatUrl(try { obj.get("linkText").asText(null) } catch (e: Exception) { null })
                        val availability = getAvailability(try { obj.get("items").get(0).get("sellers").get(0).get("commertialOffer").get("IsAvailable").asBoolean(false) } catch (e: Exception) { false })
                        val categoryName = getCategory(try { obj.get("categories").get(0).asText("") } catch (e: Exception) { "" })

                        val mpn = try { obj.get("productReference").asText("") } catch (e: Exception) { "" }

                        if (price > 0.0 ) {
                            if (link != null) {
//                                if (mpn.contains("-")) {
                                    val product = mapOf<String, Any>(
                                        "id" to obj.get("productId").asLong(),
                                        "title" to try { obj.get("productTitle").asText("") } catch (e: Exception) { "" },
                                        "description" to try { obj.get("description").asText("") } catch (e: Exception) { "" },
                                        "link" to (link ?: ""),
                                        "imageLink" to try { obj.get("items").get(0).get("images").get(0).get("imageUrl").asText("") } catch (e: Exception) { "" },
                                        "availability" to availability,
                                        "availability_date" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).replace(" ", "T"),
                                        "price" to price,
                                        "brand_name" to try { obj.get("brand").asText("") } catch (e: Exception) { "" },
                                        "gtin" to try { obj.get("items").get(0).get("ean").asText("") } catch (e: Exception) { "" },
                                        "mpn" to try { obj.get("productReference").asText("") } catch (e: Exception) { "" },
                                        "condition" to "New",  //???????????????
                                        "adult" to try { if (obj.get("categoriesIds").asText().contains("/369")) "yes" else "no" } catch (e: Exception) { "no" }, //??????????????????????
                                        "multipack" to try { obj.get("items").get(0).get("unitMultiplier").asDouble(0.0) } catch (e: Exception) { 0.0 },
                                        "bundle" to try { obj.get("items").get(0).get("isKit").asBoolean(false) } catch (e: Exception) { false },
                                        "age_group" to try { obj.get("Fase Indicada").get(0).asText("") } catch (e: Exception) { "" },
                                        "color" to try { obj.get("Cor do Produto").get(0).asText("") } catch (e: Exception) { "" }, ////
                                        "gender" to try { obj.get("Gênero").get(0).asText("") } catch (e: Exception) { "" }, ////
                                        "material" to try { obj.get("Material").get(0).asText("") } catch (e: Exception) { "" },
                                        "pattern" to try { obj.get("SUBCATEGORIA_KEY").get(0).asText("") } catch (e: Exception) { "" },
                                        "size" to try { obj.get("Tamanho").get(0).asText("") } catch (e: Exception) { "" },
                                        "item_group_id" to try { obj.get("SUBCATEGORIA_KEY").get(0).asText("") } catch (e: Exception) { "" },
                                        "category_id" to try { obj.get("categoryId").asText("") } catch (e: Exception) { "" },
                                        "category_name" to categoryName,
                                        "region_id" to "27"
                                    )

                                    var prodcsv = obj.get("productId").asText() + ","
                                    prodcsv += "'${productTitle.replace(", ", "").replace("'", "")}',"
                                    prodcsv += "'${description.replace(",", "").replace("'", "")}',"
                                    prodcsv += "'$link',"
                                    prodcsv += try { "'${obj.get("items").get(0).get("images").get(0).get("imageUrl").asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += "$availability,"
                                    prodcsv += LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).replace(" ", "T") + ","
                                    prodcsv += "$price,"
                                    prodcsv += try { "'${obj.get("brand").asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { "'${obj.get("items").get(0).get("ean").asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { "'${obj.get("productReference").asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += "New,"
                                    prodcsv += try { if (obj.get("categoriesIds").asText().contains("/369")) "yes" else "no" } catch (e: Exception) { "no" } + ","
                                    prodcsv += try { obj.get("items").get(0).get("unitMultiplier").asText("0.0") } catch (e: Exception) { "0.0" } + ","
                                    prodcsv += try { obj.get("items").get(0).get("isKit").asText("false") } catch (e: Exception) { "false" } + ","
                                    prodcsv += try { "'${obj.get("Fase Indicada").get(0).asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { "'${obj.get("Cor do Produto").get(0).asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { "'${obj.get("Gênero").get(0).asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { "'${obj.get("Material").get(0).asText("' '")}'" } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { obj.get("SUBCATEGORIA_KEY").get(0).asText("' '") } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { obj.get("Tamanho").get(0).asText("' '") } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { obj.get("SUBCATEGORIA_KEY").get(0).asText("' '") } catch (e: Exception) { "' '" } + ","
                                    prodcsv += try { obj.get("categoryId").asText("' '") } catch (e: Exception) { "' '" } + ","
                                    prodcsv += "$categoryName,"
                                    prodcsv += "27\r\n"

                                    FileWriter(fileName, true).use { out -> out.append(jacksonObjectMapper().writeValueAsString(product)+",\n")}
                                    FileWriter(fileNameCsv, true).use { out -> out.append(prodcsv)}

                                    totalp++
//                                }   /// MPN
                            } else {    /// LINK
                                totall++
                            }
                        } else {
                            total0++
                        }
                    } catch (e: Exception) {
                        totale++
                        logger.error(">>>>> getAffiliateExport (2) [ $page ] [$total] [$totalp] [$total0] [$totall] [$totale] [${produtos.size}] [$e]")
                    }
                }

                if ((total % 1000) == 0)
                    logger.info(
                        ">>> $page >> $total / ${produtos.size} >> $totalp >> $total0 >> $totall >> $totale >> ${
                            UtilService.longToTimeStr(
                                System.currentTimeMillis() - time1
                            )
                        }"
                    )
            }
        } catch (e: Exception) {
            logger.error(">>>>> getAffiliateExport (1) [$e]")
        }

        FileWriter(fileName, true).use { out -> out.append("]\n")}

        FileWriter(fileName, true).close()
        FileWriter(fileNameCsv, true).close()

        logger.info(">>> Finalizado >> $total >> $totalp >> $total0 >> $totall >> $totale >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")

        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
    }


//    override fun getAffiliateExport(): String {
//        val time1 = System.currentTimeMillis()
//        var total = 0
//        var tota0 = 0
//        var tota1 = 0
//        var totan = 0
//        var totae = 0
//        var totee = 0
//        var error = 0
//        val fileName = "${Constants.AFFILIATE_DIRECTORY}affiliate.json"
//
//        logger.info(">>> Lendo banco...")
//
//        try {
//            val page = 0
//            val produtos = affiliate1Repository.findAll(PageRequest.of(page, 200000))
//
//            logger.info(">>> Gerando arquivo  $fileName")
//
//            FileWriter(fileName, true).use { out -> out.append("[\n")}
//
//            produtos.forEach { produto ->
//                total++
//
//                if (produto.product != null) {
//                    try {
//                        var prod: Map<String, *>?
//
//                        try {
//                            prod = jacksonObjectMapper().readValue<Map<String, *>>("${produto.product}")
//                        } catch (e: Exception) {
//                            try {
//                                prod = jacksonObjectMapper().readValue<Map<String, *>>(jacksonObjectMapper().writeValueAsString(produto.product))
//                                totae++
//                            } catch (e: Exception) {
//                                prod = null
//                                totee++
//                            }
//                        }
//
//                        val obj = jacksonObjectMapper().valueToTree<JsonNode>(prod)
//
//                        val product = mapOf<String, Any>(
//                            "id" to obj.get("productId").asLong(),
//                            "title" to try { obj.get("productTitle").asText("") } catch (e: Exception) { "" },
//                            "description" to try { obj.get("description").asText("") } catch (e: Exception) { "" },
//                            "link" to try { obj.get("linkText").asText("") } catch (e: Exception) { "" },
//                            "imageLink" to try { obj.get("items").get(0).get("images").get(0).get("imageUrl").asText("") } catch (e: Exception) { "" },
//                            "availability" to try { obj.get("items").get(0).get("sellers").get(0).get("commertialOffer").get("IsAvailable").asBoolean(false) } catch (e: Exception) { false },
//                            "availability_date" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).replace(" ", "T"),
//                            "price" to try { obj.get("items").get(0).get("sellers").get(0).get("commertialOffer").get("Price").asDouble(0.0) } catch (e: Exception) { 0.0 },
//                            "brand_name" to try { obj.get("brand").asText("") } catch (e: Exception) { "" },
//                            "gtin" to try { obj.get("items").get(0).get("ean").asText("") } catch (e: Exception) { "" },
//                            "mpn" to try { obj.get("productReference").asText("") } catch (e: Exception) { "" },
//                            "condition" to "New",  //???????????????
//                            "adult" to try { if (obj.get("categoriesIds").asText().contains("/369")) "yes" else "no" } catch (e: Exception) { "no" }, //??????????????????????
//                            "multipack" to try { obj.get("items").get(0).get("unitMultiplier").asDouble(0.0) } catch (e: Exception) { 0.0 },
//                            "bundle" to try { obj.get("items").get(0).get("isKit").asBoolean(false) } catch (e: Exception) { false },
//                            "age_group" to try { obj.get("Fase Indicada").get(0).asText("") } catch (e: Exception) { "" },
//                            "color" to try { obj.get("Cor do Produto").get(0).asText("") } catch (e: Exception) { "" }, ////
//                            "gender" to try { obj.get("Gênero").get(0).asText("") } catch (e: Exception) { "" }, ////
//                            "material" to try { obj.get("Material").get(0).asText("") } catch (e: Exception) { "" },
//                            "pattern" to try { obj.get("SUBCATEGORIA_KEY").get(0).asText("") } catch (e: Exception) { "" },
//                            "size" to try { obj.get("Tamanho").get(0).asText("") } catch (e: Exception) { "" },
//                            "item_group_id" to try { obj.get("SUBCATEGORIA_KEY").get(0).asText("") } catch (e: Exception) { "" },
//                            "shipping" to "", ////////
//                            "tax" to try { obj.get("items").get(0).get("sellers").get(0).get("commertialOffer").get("Tax").asDouble(0.0) } catch (e: Exception) { 0.0 },
//                            "region_id" to "27"
//                        )
//
//                        FileWriter(fileName, true).use { out -> out.append(jacksonObjectMapper().writeValueAsString(product)+",\n")}
//                        tota0++
//                    } catch (e: Exception) {
//                        tota1++
//                        logger.error(">>>>> getAffiliateExport (2) [ $page ] [${produtos.size}] [$e]")
//                    }
//                } else {
//                    totan++
//                }
//
//                if ((total % 10000) == 0)
//                    logger.info(
//                        ">>> $page >> $total / ${produtos.size} >> $tota0 >> $tota1 >> $totan >> $totae >> $totee >> ${
//                            UtilService.longToTimeStr(
//                                System.currentTimeMillis() - time1
//                            )
//                        }"
//                    )
//            }
//        } catch (e: Exception) {
//            logger.error(">>>>> getAffiliateExport (1) [$e]")
//        }
//
//        FileWriter(fileName, true).use { out -> out.append("]\n")}
//        FileWriter(fileName, true).close()
//
//        logger.info(">>> Finalizado >> $total >> $tota0 >> $tota0 >> $tota1 >> $totan >> $totae >> $totee >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
//
//        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//    }





//    override fun getAffiliateExport(): String {
//        val time1 = System.currentTimeMillis()
//        var total = 0
//        var tota0 = 0
//        var tota1 = 0
//        var tota3 = 0
//        var totan = 0
//        var error = 0
//        val fileName = "${Constants.AFFILIATE_DIRECTORY}affiliate.json"
//
//        logger.info(">>> Lendo banco...")
//
//        try {
//            val page = 6
//            val produtos = affiliateRepository.findAll(PageRequest.of(page, 5000000))
//
//            logger.info(">>> Gerando arquivo  $fileName")
//
//            produtos.forEach { produto ->
//                total++
//
//                if (produto.type == 1) {
//                    tota1++
//
//                    if (produto.product != null) {
//
//                        try {
//                            val prod = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(produto.product)
//
//                            if (prod != null)
//                                FileWriter(fileName, true).use { out -> out.append("$prod,\n")}
//                            else
//                                tota0++
//                        } catch (e: Exception) {
//                            logger.error(">>>>> getAffiliateExport (2) [ $page ] [${produtos.size}] [$e]")
//                        }
//                    } else {
//                        totan++
//                    }
//                } else {
//                    tota3++
//                }
//
//                if ((total % 100000) == 0)
//                    logger.info(
//                        ">>> $page >> $total / ${produtos.size} >> $tota1 >> $tota3 >> $totan >> ${
//                            UtilService.longToTimeStr(
//                                System.currentTimeMillis() - time1
//                            )
//                        }"
//                    )
//            }
//        } catch (e: Exception) {
//            logger.error(">>>>> getAffiliateExport (1) [$e]")
//        }
//
//        FileWriter(fileName, true).close()
//
//        logger.info(">>> Finalizado >> $total >> $tota0 >> $tota1 >> $tota3 >> $totan >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
//
//        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//    }
//


//    override fun getVariants3P(): String {
//        val time1 = System.currentTimeMillis()
//        var total = 0
//        var total1 = 0
//
//        try {
//            val fileList = HashSet<String>()
//            var similarList = mutableListOf<Similar>()
//
//            Files.walk(Paths.get(Constants.SIMILAR_DIRECTORY)).use { paths ->
//                paths.filter { Files.isRegularFile(it) }
//                    .forEach { fileList.add(it.fileName.toString()) }
//            }
//
//            fileList.forEach { file ->
//                try {
//                    logger.info(">>>>> $file");
//
//                    val fis = FileInputStream("${Constants.SIMILAR_DIRECTORY}/$file")
//                    val xlWb = WorkbookFactory.create(fis)
//                    val xlWs = xlWb.getSheetAt(0)
//
//                    val headers: MutableMap<Int, String> = mutableMapOf()
//
//                    xlWs.forEach { row ->
//                        try {
////                            total1++
////
////                            if ((total1 % 100) == 0)
////                                logger.info(">>> $total1 / ${xlWs.lastRowNum}")
//
//                            if (row.rowNum == 0) {
//                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
//                            } else {
//                                val fields: MutableMap<String, Any> = mutableMapOf()
//
//                                row.forEach {
//                                    when (it.cellType.name) {
//                                        "NUMERIC" ->
//                                            fields[headers[it.columnIndex].toString()] =
//                                                it.numericCellValue.toLong()
//
//                                        "STRING" ->
//                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue
//
//                                        else -> {
//                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
//                                        }
//                                    }
//                                }
//
//                                if ((fields["codigoreferenciaproduto"].toString().lowercase().contains("mp") ||
//                                            fields["codigoreferenciaproduto"].toString().lowercase().contains("mv"))
//                                ) {
//                                    val productId = fields["idproduto"] as Long?
//                                    val skuId = fields["idsku"] as Long?
//
//                                    productId?.let { prodId ->
//                                        if (skuId != null) {
////                                            val s = similarRepository.findByProductIdAndSkuId(prodId, skuId)
////                                                .orElse(Similar(prodId, skuId))
//                                            val s = Similar(prodId, skuId)
//
//                                            val sim = fields["similares"] as String?
//
//                                            s.apply {
//                                                updateDate = Date()
//
//                                                productRefId = fields["codigoreferenciaproduto"] as String?
//                                                productName = fields["nomeproduto"] as String?
//                                                categoryName = fields["nomecategoria"] as String?
//                                                similar = if (sim == null || sim.trim() == "") listOf() else getSimilar(sim)
//                                            }
//
//                                            if (!s.variant) {
//                                                try {
//                                                    val context = s.skuId?.let { skuId -> HttpService.getSkuContextBySkuId(skuId) }
//                                                    val specifications = context?.map?.get("ProductSpecifications") as List<Map<String, Any>>
//
//                                                    s.variants.clear()
//
//                                                    specifications.forEach {
//                                                        if ((it["FieldGroupName"] as String).lowercase().trim() == "variantes") {
//                                                            s.variants.add("${it["FieldName"]},${it["FieldValues"].toString().dropLast(1).drop(1)}")
//                                                        }
//                                                    }
//
//                                                    s.variant = true
//                                                } catch (e: Exception) {
//                                                    logger.error(">>>>> getVariants3P (4) [${s.skuId}] [$e]")
//                                                }
//                                            }
//
////                                            similarList.add(s)
//
////                                            jmsTemplate.convertAndSend("carrefour_vtex_queue", Gson().toJson(s))
//
////                                            if (similarList.size >= 100) {
////                                                similarRepository.saveAll(similarList)
////                                                similarList.clear()
////                                            }
//
//                                            total = total.inc()
//
//                                            if ((total % 100) == 0)
//                                                logger.info(">>> $total / ${xlWs.lastRowNum} >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} >> ${s.skuId} >> ${s.productRefId}")
//
////                                            if (total > 1000)  return "$total / ${xlWs.lastRowNum} [${System.currentTimeMillis()-time1}] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//                                        }
//                                    }
//                                }
//                            }
//                        } catch (e: Exception) {
//                            logger.error(">>>>> getVariants3P (3) [$file] [$e]")
//                        }
//                    }
//
//                    logger.info(">>> $total / ${xlWs.lastRowNum} >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)}")
////                    logger.info("<<<<<<<<<<<<<<< ${System.currentTimeMillis() - time1} >>>>>>>>>>>>>>>")
//
//                    fis.close();
//
//                } catch (e: Exception) {
//                    logger.error(">>>>> getVariants3P (2) [$file] [$e]")
//                }
//            }
//        } catch (e: Exception) {
//            logger.error(">>>>> getVariants3P (1) [$e]")
//        }
//
//        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//    }


//    override fun getVariants3P(): String {
//        val time1 = System.currentTimeMillis()
//        var total = 0
//        var total1 = 0
//
//        try {
//            val fileList = HashSet<String>()
//
//            Files.walk(Paths.get(Constants.SIMILAR_DIRECTORY)).use { paths ->
//                paths.filter { Files.isRegularFile(it) }
//                    .forEach { fileList.add(it.fileName.toString()) }
//            }
//
//            fileList.forEach { file ->
//                try {
//                    logger.info(">>>>> $file");
//
//                    val fis = FileInputStream("${Constants.SIMILAR_DIRECTORY}/$file")
//                    val xlWb = WorkbookFactory.create(fis)
//                    val xlWs = xlWb.getSheetAt(0)
//
//                    val headers: MutableMap<Int, String> = mutableMapOf()
//
//                    xlWs.forEach { row ->
//                        try {
////                            total1++
////
////                            if ((total1 % 100) == 0)
////                                logger.info(">>> $total1 / ${xlWs.lastRowNum}")
//
//                            if (row.rowNum == 0) {
//                                row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
//                            } else {
//                                val fields: MutableMap<String, Any> = mutableMapOf()
//
//                                row.forEach {
//                                    when (it.cellType.name) {
//                                        "NUMERIC" ->
//                                            fields[headers[it.columnIndex].toString()] =
//                                                it.numericCellValue.toLong()
//
//                                        "STRING" ->
//                                            fields[headers[it.columnIndex].toString()] = it.stringCellValue
//
//                                        else -> {
//                                            logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
//                                        }
//                                    }
//                                }
//
//                                if ((fields["codigoreferenciaproduto"].toString().lowercase().contains("mp") ||
//                                            fields["codigoreferenciaproduto"].toString().lowercase().contains("mv"))
//                                ) {
//                                    val productId = fields["idproduto"] as Long?
//                                    val skuId = fields["idsku"] as Long?
//
//                                    productId?.let { prodId ->
//                                        if (skuId != null) {
//                                            val s = similarRepository.findByProductIdAndSkuId(prodId, skuId)
//                                                .orElse(Similar(prodId, skuId))
//
//                                            val sim = fields["similares"] as String?
//
//                                            s.apply {
//                                                updateDate = Date()
//
//                                                productRefId = fields["codigoreferenciaproduto"] as String?
//                                                productName = fields["nomeproduto"] as String?
//                                                categoryName = fields["nomecategoria"] as String?
//                                                similar = if (sim == null || sim.trim() == "") listOf() else getSimilar(sim)
//                                            }
//
//                                            if (!s.variant) {
//                                                try {
//                                                    val context = s.skuId?.let { skuId -> HttpService.getSkuContextBySkuId(skuId) }
//                                                    val specifications = context?.map?.get("ProductSpecifications") as List<Map<String, Any>>
//
//                                                    s.variants.clear()
//
//                                                    specifications.forEach {
//                                                        if ((it["FieldGroupName"] as String).lowercase().trim() == "variantes") {
//                                                            s.variants.add("${it["FieldName"]},${it["FieldValues"].toString().dropLast(1).drop(1)}")
//                                                        }
//                                                    }
//
//                                                    s.variant = true
//                                                } catch (e: Exception) {
//                                                    logger.error(">>>>> getVariants3P (4) [${s.skuId}] [$e]")
//                                                }
//                                            }
//
//                                            similarRepository.save(s)
//                                            total = total.inc()
//
//                                            if ((total % 1000) == 0)
//                                                logger.info(">>> $total / ${xlWs.lastRowNum} >> ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} >> ${s.skuId} >> ${s.productRefId}")
//
////                                            if (total > 1000)  return "$total / ${xlWs.lastRowNum} [${System.currentTimeMillis()-time1}] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//                                        }
//                                    }
//                                }
//                            }
//                        } catch (e: Exception) {
//                            logger.error(">>>>> getVariants3P (3) [$file] [$e]")
//                        }
//                    }
//
////                    logger.info("<<<<<<<<<<<<<<< ${System.currentTimeMillis() - time1} >>>>>>>>>>>>>>>")
//
//                    fis.close();
//
//                } catch (e: Exception) {
//                    logger.error(">>>>> getVariants3P (2) [$file] [$e]")
//                }
//            }
//        } catch (e: Exception) {
//            logger.error(">>>>> getVariants3P (1) [$e]")
//        }
//
//        return "$total [ ${System.currentTimeMillis()-time1} ] [ ${UtilService.longToTimeStr(System.currentTimeMillis()-time1)} ]"
//    }


    override fun getVariants(): String {
        val products = similarRepository.findAll()
        var total = 0;

        products.forEach { similar ->
            if (similar.variant) {
                try {
                    val context = similar.skuId?.let { skuId -> HttpService.getSkuContextBySkuId(skuId) }
                    val specifications = context?.map?.get("ProductSpecifications") as List<Map<String, Any>>

                    similar.variants.clear()

                    specifications.forEach {
                        if ((it["FieldGroupName"] as String).lowercase().trim() == "variantes") {
                            similar.variants.add("${it["FieldName"]},${it["FieldValues"].toString().dropLast(1).drop(1)}")
                        }
                    }

                    similar.variant = true

                    similarRepository.save(similar)
                    total++

                    logger.info(">>> ${similar.skuId} >> $total / ${products.size}")
                } catch (e: Exception) {
                    logger.error(">>>>> getVariants [${similar.skuId}] [$e]")
                }
            }
        }

        return  ""
    }


    override fun reportProductsWithSimilar(): String {
//        val products = similarRepository.findAll(Sort.by("productName").and(Sort.by("skuId")))
        val similars = similarRepository.findAll()
        var header = "Nome do Produto|Id Produto|RefId Produto|Nome Categoria|Id SKU|Similares";
        var indice = 1
        var total = 0
        val variantes = mutableSetOf<String>()
        val vardata = mutableMapOf<String, String>()
        val catName = "eletroportateis_3p_"  /////////// NOME CATEGORIA \\\\\\\\\\\\\
        var fileName = "${Constants.SIMILAR_FILE_DIRECTORY}$catName$indice.csv"

        val products = similars.sortedWith(compareBy<Similar?> { it?.productName }.thenBy { it?.skuId })

        products.forEach { similar ->
            similar.variants.forEach { variante ->
                variantes.add(variante.toString().split(",")[0].trim())
            }
        }

        variantes.sorted().forEach { header += "|$it" }

        FileWriter(fileName, true).use { out -> out.append("$header\n")}
        logger.info(">>> Gerando arquivo  ${Constants.SIMILAR_FILE_DIRECTORY}similares_$indice.csv")

        products.forEach { similar ->
            try {
                var line = "${similar.productName}|${similar.productId}|${similar.productRefId}|${similar.categoryName}|${similar.skuId}|${similar.similar.toString().dropLast(1).drop(1)}"

                variantes.sorted().forEach { vardata[it] = "" }

                similar.variants.forEach { variante ->
                    val v = variante.toString().split(",")
                    vardata[v[0].trim()] = v[1].trim()
                }

                vardata.forEach { line += "|${it.value}" }

                FileWriter(fileName, true).use {out -> out.append("$line\n")}
                total++

                if ((total % 200000) == 0) {
                    FileWriter(fileName, true).close()
                    indice++
                    fileName = "${Constants.SIMILAR_FILE_DIRECTORY}$catName$indice.csv"
                    FileWriter(fileName, true).use { out -> out.append("$header\n")}
                    logger.info(">>> Gerando arquivo  $fileName")
                }

                if ((total % 10000) == 0)
                    logger.info(">>> $total / ${products.size} >> $indice")
            } catch (e: Exception) {
                logger.error(">>>>> reportProductsWithSimilar [$indice] [$total] [${similar.skuId}] [$e]")
            }
        }

        FileWriter(fileName, true).close()
        logger.info(">>> Relatorio finalizado...")

        return  "$total / ${products.size} >> $indice"
    }


    override fun getProductsBeforeAfterImport(skus: Map<String, Any>, before: Boolean): String {
        var skus19 = ""

        try {
            val ids = skus["ids"] as List<Long>

            ids.forEach {
                try {
                    val sku: MutableMap<String, Any>? = HttpService.getSkuBySkuId(it)?.let { map ->
                        jacksonObjectMapper().readValue(map)
                    }

                    if (sku != null) {
                        val productId = (sku["ProductId"] as Int).toLong()
                        val catalog = catalogRepository.findByProductIdAndSkuId(productId, it).orElse(Catalog(productId, it))
                        val fields = mutableMapOf<String, Any>("SkuRefId" to sku["RefId"] as String)

                        val context = HttpService.getSkuContextBySkuId(it)

                        fields["ProductName"] = context.map?.get("ProductName") as String
                        fields["ProductRefId"] = context.map?.get("ProductRefId") as String
                        fields["AlternateIds"] = context.map?.get("AlternateIds") as Any
                        fields["AlternateIdValues"] = context.map?.get("AlternateIdValues") as Any

                        fields["EANs"] = Gson().fromJson(HttpService.getEansByskuId(it), Array<String>::class.java).asList()

                        if (before)
                            catalog.before = fields
                        else
                            catalog.after = fields

                        catalogRepository.save(catalog)

                        if (UtilService.contains19(fields["ProductRefId"] as String?) ||
                            UtilService.contains19(fields["SkuRefId"] as String?) ||
                            UtilService.eansContains19(fields["EANs"] as List<String>)) {
                            skus19 += "$it,\n"
                        }
                    }

                    logger.info(">>>>>>> $it")
                } catch (e: Exception) {
                    logger.error(">>>>> getProductsBeforeAfterImport List [$it] [$e]")
                }
            }
        } catch (e: Exception) {
            logger.error(">>>>> getProductsBeforeAfterImport [$e]")
            return "{\"Status\": \"Lista invalida!\"}"
        }

        return "{\"Status\": \"Ok\", \"Skus\": [$skus19]}"
    }


    override fun selectProductsPOC(params: Map<String, Any>): Any { //List<Map<String, Any>> {
        val products = mutableListOf<Map<String, Any>>()

        try {
            // productId:6575  C:26
            //{"type": "xml", "products": []}

            var xml = "<?xml version=\"1.0\"?>\n<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:g=\"http://base.google.com/ns/1.0\">\n\t<title>POC</title>\n\t<updated>2022-12-15T15:00:00Z</updated>"
            var csv = "item_id, store_id, pickup_method, pickup_sla, availability, quantity, price${System.getProperty("line.separator")}"
            val type = params["type"] ?: "json"

            (params["products"] as List<*>).forEach {
                val sku = HttpService.getSkuByRMS(it as String)

                if (sku != null) {
                    HttpService.getProducts(listOf("skuId:$sku", "carrefourbr154"))
                        ?.let<String, List<MutableMap<String, Any>?>?>
                        { map -> jacksonObjectMapper().readValue(map) }?.forEach { prod ->
                            val item = (prod?.get("items") as List<*>)[0] as MutableMap<*, *>
                            val commertialOffer =
                                ((item["sellers"] as List<*>)[0] as MutableMap<*, *>)["commertialOffer"] as MutableMap<*, *>

                            val product = mapOf<String, Any>(
                                "productName" to prod["productName"] as String,
                                "productId" to prod["productId"] as String,
                                "productReference" to prod["productReference"] as String,
                                "itemId" to item["itemId"] as String,
                                "measurementUnit" to item["measurementUnit"] as String,
                                "unitMultiplier" to item["unitMultiplier"] as Double,
                                "price" to commertialOffer["Price"] as Double,
                                "availableQuantity" to commertialOffer["AvailableQuantity"] as Int
                            )

                            val price = (product["unitMultiplier"] as Double) * (product["price"] as Double)

//                    if (price > 0) {
                            products.add(product)

                            csv += "${product["itemId"]}, carrefourbr154, ship to store, multiweek, out of stock, 0, ${
                                price.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
                            }${System.getProperty("line.separator")}"

                            xml += "\n\t<entry>"
                            xml += "\n\t\t<g:id>${product["itemId"]}</g:id>"
                            xml += "\n\t\t<g:store_code>carrefourbr154</g:store_code>"
                            xml += "\n\t\t<g:pickup_method>ship_to_store</g:pickup_method>"
                            xml += "\n\t\t<g:pickup_sla>multi-week</g:pickup_sla>"
                            xml += "\n\t\t<g:availability>out_of_stock</g:availability>"
                            xml += "\n\t\t<g:quantity>0</g:quantity>"
                            xml += "\n\t\t<g:price>${price.toString().replace(".", ",")} BRL</g:price>"
                            xml += "\n\t</entry>"

                            logger.info(">>>>>> $it")
//                    }
                        }
                }
            }

            xml += "\n</feed>"

            return if (type == "csv") csv else (if (type == "xml") xml else products)
        } catch (e: Exception) {
            logger.error(">>>>> selectProductsPOC [$e]")
        }

        return products
    }


//    override fun importProducts(importProducts: ImportProductsVO): Int {
//        val query = queryRepository.findByName(importProducts.name).orElse(Query(importProducts.name))
//
//        if (query.id == null) queryRepository.save(query)
//
//        query.id?.let {
//            productRepository.deleteByQueryId(it)
//
//            try {
//                val dir = if (importProducts.directory.trim() == "") exportDir else importProducts.directory
//                val fileList = HashSet<String>()
//
//                Files.walk(Paths.get(exportDir)).use { paths ->
//                    paths.filter { Files.isRegularFile(it) }
//                        .forEach { fileList.add(it.fileName.toString()) }
//                }
//
//                fileList.forEach { file ->
//                    try {
//                        val fis = FileInputStream("$dir/$file")
//                        val xlWb = WorkbookFactory.create(fis)
//                        val xlWs = xlWb.getSheetAt(0)
//
//                        val headers: MutableMap<Int, String> = mutableMapOf()
//
//                        xlWs.forEach { row ->
//                            try {
//                                if (row.rowNum == 0) {
//                                    row.forEach { headers[it.columnIndex] = normalizeName(it.stringCellValue) }
//                                } else {
//                                    val fields: MutableMap<String, Any> = mutableMapOf()
//
//                                    row.forEach {
//                                        when (it.cellType.name) {
//                                            "NUMERIC" ->
//                                                fields[headers[it.columnIndex].toString()] =
//                                                    it.numericCellValue.toLong()
//
//                                            "STRING" ->
//                                                fields[headers[it.columnIndex].toString()] = it.stringCellValue
//
//                                            else -> {
//                                                logger.info(">>>>>>>>>>>>>>>>>> ${it.cellType.name} >>> ${headers[it.columnIndex].toString()}")
//                                            }
//                                        }
//                                    }
//
//                                    if (fields["skuEan"].toString().contains("1/9")
////                                        fields["codigoReferenciaSKU"].toString().contains("1/9") ||
////                                        fields["codigoReferenciaProduto"].toString().contains("1/9")
//                                    ) {
//                                        val product = Product().apply {
//                                            this.queryId = query.id
//                                            this.productId = fields["idProduto"] as Long?
//                                            this.skuId = fields["skuId"] as Long?
//                                            this.fields = fields
//                                        }
//
//                                        logger.info(">>>>>>>>>>>>> ${fields["skuId"].toString()} >> ${fields["skuEan"].toString()} >> ${fields["codigoReferenciaSKU"].toString()} >> ${fields["codigoReferenciaProduto"].toString()}")
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                logger.error(">>>>> importProducts(3) [$file] [$e]")
//                            }
//                        }
//
//                        logger.info(">>>>> readSkusXml [$file]");
//
//                        fis.close();
//                    } catch (e: Exception) {
//                        logger.error(">>>>> importProducts(2) [$file] [$e]")
//                    }
//                }
//            } catch (e: Exception) {
//                logger.error(">>>>> importProducts(1) [$e]")
//            }
//        }
//
//
//        return query.products.size;
//    }


//    private fun normalizeName(name: String): String =
//        name.split(" ")[0].replace("_", "").replaceFirstChar {
//                c -> c.toString().lowercase(Locale.getDefault()) }

    private fun normalizeName(name: String): String =
        name.split(" ")[0].replace("_", "").lowercase(Locale.getDefault()).trim()

    private fun getSimilar(similar: String): List<Long> =
        similar.split(",").map { id -> id.trim().toLong() }

    private fun getLinksAndDownloadFiles(downloadFiles: DownloadFilesVO): List<String> {
        val links = arrayListOf<String>()

//        if (url == null) return  links

//        val html = getSource(url)

        val l = html.split("target=\"_blank\"")

        l.forEach {
            val mm = it.split("<a href=")
            if (mm.size > 1) links.add(mm[1].trim().substring(1, mm[1].length-2).replace("&amp;", "&"))
        }

        val xmls = arrayListOf<String>()
        var idx = 1

        val dir = if (downloadFiles.directory.trim() == "") Constants.EXPORT_DIRECTORY else downloadFiles.directory

        links.forEach {
            try {
                val path = HttpService.getXml(
                    it,
                    "$dir${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}-${idx++}.xls"
                )

                if (path == null)
                    xmls.add("ERRO: ${idx-1}")
                else
                    xmls.add(path.toString());
            } catch (e: Exception) {
                xmls.add("ERRO: [${idx-1}] ${e.message}")
            }
        }

        return xmls
    }


    private fun getSource(url: String): String {
        try {
            val doc = Jsoup.connect(url).get()

            val urls = mutableSetOf<String>()

            doc.select("a").forEach {
                val u = it.attr("href")
                urls.add(u)
            }

            return doc.html()

        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }
}



private val html = """

""".trimIndent()
