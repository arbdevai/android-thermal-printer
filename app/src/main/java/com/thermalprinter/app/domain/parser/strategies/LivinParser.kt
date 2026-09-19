package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class LivinParser : BankParserStrategy {
    override val supportedBank = BankSource.LIVIN

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("livin") ||
                lower.contains("mandiri") ||
                lower.contains("bank mandiri") ||
                lower.contains("livin' by mandiri")
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("nominal transfer", "total transfer", "nominal", "jumlah", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya transaksi", "biaya transfer", "biaya admin", "fee")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama penerima", "kepada", "penerima", "tujuan", "nama:")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("rekening sumber", "nama pengirim", "pengirim", "dari")
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
        ) ?: "Mandiri"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("no. resi", true) || it.contains("no. referensi", true) || it.contains("resi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("keterangan", "catatan", "berita", "pesan")
        ) ?: ""

        val method = when {
            normalized.contains("bi-fast", true) -> "BI-FAST"
            normalized.contains("online", true) || normalized.contains("realtime", true) -> "Transfer Online"
            else -> "Sesama Mandiri"
        }

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.LIVIN,
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
