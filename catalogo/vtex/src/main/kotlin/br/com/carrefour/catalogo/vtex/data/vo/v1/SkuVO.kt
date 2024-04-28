package br.com.carrefour.catalogo.vtex.data.vo.v1

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.util.*


@JsonPropertyOrder("skuId", "adjusted", "insertDate", "updateDate", "correctionDate")
data class SkuVO (
    @field:JsonProperty("id")
    var skuId: Long? = null,
    var insertDate: Date? = null,
    var updateDate: Date? = null,
    var correctionDate: Date? = null,
    var adjusted: Boolean? = false
)
