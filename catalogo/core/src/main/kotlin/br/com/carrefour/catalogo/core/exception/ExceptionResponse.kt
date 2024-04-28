package br.com.carrefour.catalogo.core.exception

import java.util.*


class ExceptionResponse(
    val timestamp: Date,
    val message: String?,
    val details: String
)
