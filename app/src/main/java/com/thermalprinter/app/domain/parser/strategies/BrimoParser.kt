package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class BrimoParser : BankParserStrategy {
    override val supportedBank = BankSource.BRIMO

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("brimo") ||
                lower.contains("bank rakyat indonesia") ||
                lower.contains("bri") ||
                lower.contains("m-bri")
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("nominal", "jumlah", "total transfer", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya admin", "biaya transaksi", "admin bank", "biaya transfer")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama penerima", "tujuan", "penerima", "kepada", "nama:")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("sumber dana", "rekening sumber", "nama pengirim", "pengirim", "dari")
        ) ?: ""

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("rekening tujuan", "no. rekening tujuan", "ke rekening", "no. tujuan")
        )?.filter { it.isDigit() } ?: ""

        val senderAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("rekening sumber", "dari rekening", "sumber dana")
        )?.filter { it.isDigit() } ?: ""

        val bankTujuan = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "ke bank", "bank")
        ) ?: "BRI"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("nomor referensi", true) || it.contains("no. ref", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("keterangan", "catatan", "berita", "pesan")
        ) ?: ""

        val method = when {
            normalized.contains("bi-fast", true) -> "BI-FAST"
            normalized.contains("online", true) || normalized.contains("realtime", true) -> "Realtime Online"
            else -> "Sesama BRI"
        }

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.BRIMO,
            transactionType = "Transfer $method",
            transferMethod = method,
            transferAmount = transferAmount,
            originalAdminFee = originalFee,
            storeAdminFee = defaultStoreFee,
            splitAdminFee = splitAdminFee,
            totalAmount = transferAmount + defaultStoreFee + originalFee,
            senderName = senderName,
            senderAccount = senderAccount,
            receiverName = receiverName,
            receiverAccount = receiverAccount,
            receiverBank = bankTujuan,
            referenceNumber = refNumber,
            transactionDate = date,
            transactionTime = time,
            status = ParserUtils.detectStatus(normalized),
            notes = notes,
            rawExtractedText = normalized
        )
    }
}
