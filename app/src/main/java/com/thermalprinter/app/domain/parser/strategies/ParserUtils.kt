package com.thermalprinter.app.domain.parser.strategies

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ParserUtils {
    val explicitAmountPattern = Regex("(?i)(?:rp\\.?|idr)\\s*([0-9][0-9.\\s]*(?:,[0-9]{1,2})?)")
    val standardAmountPattern = Regex("(?i)(?:rp\\.?|idr)?\\s*([0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]{1,2})?|[0-9]{4,}(?:,[0-9]{1,2})?)")
    val referencePattern = Regex("(?i)(?:no\\.?\\s*(?:referensi|ref|transaksi|pesanan|resi)|reference(?:\\s*number)?|id\\s*transaksi|order\\s*id)\\s*[:#-]?\\s*([A-Z0-9./_-]{5,})")
    val datePattern = Regex("\\b(?:[0-3]?\\d[/-][01]?\\d[/-](?:20)?\\d{2}|[0-3]?\\d\\s+(?:jan|feb|mar|apr|mei|may|jun|jul|agu|aug|sep|okt|oct|nov|des|dec)[a-z]*\\s+(?:20)?\\d{2})\\b", RegexOption.IGNORE_CASE)
    val timePattern = Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?(?:\\s*(?:wib|wita|wit))?\\b", RegexOption.IGNORE_CASE)
    val accountNumberPattern = Regex("\\b(?:\\d{4,}[-\\s]?\\d{4,}|08\\d{8,11})\\b")

    fun parseAmount(raw: String): Long? {
        val value = raw.trim().replace(" ", "")
        if (value.isEmpty()) return null
        val integerPart = value.substringBefore(',').replace(".", "").filter(Char::isDigit)
        return integerPart.toLongOrNull()
    }

    fun findAmountAfterLabels(lines: List<String>, labels: List<String>): Long? {
        for (index in lines.indices) {
            val line = lines[index]
            for (label in labels) {
                if (line.contains(label, ignoreCase = true)) {
                    val sameLine = explicitAmountPattern.find(line)?.groupValues?.getOrNull(1)
                        ?: standardAmountPattern.find(line)?.groupValues?.getOrNull(1)

                    if (sameLine != null) {
                        parseAmount(sameLine)?.let { if (it >= 100) return it }
                    }

                    if (index + 1 < lines.size) {
                        val nextLine = lines[index + 1]
                        if (!datePattern.containsMatchIn(nextLine)) {
                            val nextAmount = explicitAmountPattern.find(nextLine)?.groupValues?.getOrNull(1)
                                ?: standardAmountPattern.find(nextLine)?.groupValues?.getOrNull(1)
                            if (nextAmount != null) {
                                parseAmount(nextAmount)?.let { if (it >= 100) return it }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    fun findValueAfterLabels(lines: List<String>, labels: List<String>): String? {
        for (index in lines.indices) {
            val line = lines[index]
            for (label in labels) {
                val idx = line.indexOf(label, ignoreCase = true)
                if (idx >= 0) {
                    val after = line.substring(idx + label.length).trim().trimStart(':', '-', '#').trim()
                    if (after.isNotBlank() && !isPureAmount(after)) {
                        return cleanValue(after)
                    }
                    if (index + 1 < lines.size) {
                        val next = lines[index + 1].trim().trimStart(':', '-', '#').trim()
                        if (next.isNotBlank() && !isPureAmount(next) && !datePattern.containsMatchIn(next)) {
                            return cleanValue(next)
                        }
                    }
                }
            }
        }
        return null
    }

    fun isPureAmount(str: String): Boolean {
        val cleaned = str.replace(Regex("(?i)^(rp|idr)\\.?\\s*"), "").replace(".", "").replace(",", "").trim()
        return cleaned.all { it.isDigit() } && cleaned.isNotEmpty()
    }

    fun cleanValue(value: String): String {
        return value.replace(Regex("(?i)^(rp|idr|nama|name)[:\\s-]*"), "")
            .trim()
            .take(30)
    }

    fun detectStatus(text: String): String = when {
        text.contains("gagal", true) || text.contains("failed", true) -> "GAGAL"
        text.contains("pending", true) || text.contains("diproses", true) -> "DIPROSES"
        text.contains("berhasil", true) || text.contains("sukses", true) || text.contains("success", true) -> "BERHASIL"
        else -> "BERHASIL"
    }

    fun defaultDate(): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    fun defaultTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
