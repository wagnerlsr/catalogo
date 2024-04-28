package br.com.carrefour.catalogo.vtex.service.util

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


@Service
object UtilService {

//    fun longToLocalDateTime(time: Long) = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun longToTimeStr(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = seconds / 3600

        return "$hours:${minutes - (hours*60)}:${seconds - (hours*60) - (minutes*60)}"
    }

    fun contains19(str: String?) = str?.contains("1/9") ?: false

    fun eansContains19(eans: List<String>): Boolean {
        eans.forEach { if (contains19(it)) return true }
        return false
    }

    fun fix19(str: String) = str.replace("1/9", "").trim()

    fun fixEans(eans: List<String>): Set<String>
    {
        val fEans = mutableSetOf<String>()
        eans.forEach { fEans.add(fix19(it)) }
        return fEans;
    }

}