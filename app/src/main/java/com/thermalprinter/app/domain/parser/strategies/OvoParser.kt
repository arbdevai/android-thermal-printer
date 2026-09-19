package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class OvoParser : BankParserStrategy {
    override val supportedBank = BankSource.OVO

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("ovo") ||
                lower.contains("pt visionet internasional")
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("nominal", "transfer berhasil", "total", "jumlah", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya transaksi", "biaya admin", "fee", "biaya layanan")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("ke", "penerima", "nama penerima", "tujuan", "nama:")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("dari", "pengirim", "sumber dana")
        ) ?: "OVO Cash"

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("no. ponsel", "nomor ponsel", "no. tujuan", "no. rek")
        )?.filter { it.isDigit() } ?: ParserUtils.accountNumberPattern.findAll(normalized).map { it.value }.lastOrNull().orEmpty()

        val bankTujuan = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "bank")
        ) ?: "OVO"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("no. referensi", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("pesan", "catatan", "keterangan")
        ) ?: ""

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.OVO,
            transactionType = "Transfer OVO",
            transferMethod = "OVO Cash",
            transferAmount = transferAmount,
            originalAdminFee = originalFee,
            storeAdminFee = defaultStoreFee,
            splitAdminFee = splitAdminFee,
            totalAmount = transferAmount + defaultStoreFee + originalFee,
            senderName = senderName,
            senderAccount = "",
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
