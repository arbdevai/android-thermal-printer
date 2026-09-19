package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class GopayParser : BankParserStrategy {
    override val supportedBank = BankSource.GOPAY

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("gopay") ||
                lower.contains("gojek") ||
                lower.contains("pt dompet anak bangsa")
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("nominal", "total", "jumlah", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya transaksi", "biaya layanan", "biaya admin", "fee")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("ke", "penerima", "nama penerima", "tujuan", "nama:")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("dari", "pengirim", "nama pengirim", "sumber dana")
        ) ?: "GoPay"

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("no. ponsel", "no. handphone", "no. rek", "nomor tujuan", "no. tujuan")
        )?.filter { it.isDigit() } ?: ParserUtils.accountNumberPattern.findAll(normalized).map { it.value }.lastOrNull().orEmpty()

        val bankTujuan = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "bank")
        ) ?: "GoPay Saldo"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("order id", true) || it.contains("id transaksi", true) || it.contains("no. transaksi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("catatan", "keterangan", "pesan")
        ) ?: ""

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.GOPAY,
            transactionType = "Transfer GoPay",
            transferMethod = "Saldo GoPay",
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
