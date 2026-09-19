package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class SeabankParser : BankParserStrategy {
    override val supportedBank = BankSource.SEABANK

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("seabank") ||
                lower.contains("sea bank") ||
                lower.contains("pt bank seabank indonesia")
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("jumlah transfer", "nominal", "jumlah", "total transfer", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya transfer", "biaya admin", "biaya transaksi", "fee")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama penerima", "penerima", "tujuan", "kepada", "nama:")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama pengirim", "pengirim", "dari", "sumber dana")
        ) ?: ""

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("rekening penerima", "no. rekening tujuan", "ke rekening", "no. tujuan")
        )?.filter { it.isDigit() } ?: ParserUtils.accountNumberPattern.findAll(normalized).map { it.value }.lastOrNull().orEmpty()

        val senderAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("rekening pengirim", "rekening sumber", "dari rekening")
        )?.filter { it.isDigit() } ?: ""

        val bankTujuan = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank penerima", "bank tujuan", "ke bank", "bank")
        ) ?: "SeaBank"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("no. referensi", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("catatan", "keterangan", "pesan")
        ) ?: ""

        val method = when {
            normalized.contains("bi-fast", true) -> "BI-FAST"
            normalized.contains("online", true) -> "Transfer Online"
            else -> "Sesama SeaBank"
        }

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.SEABANK,
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
