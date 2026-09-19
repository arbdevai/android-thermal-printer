package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class ShopeePayParser : BankParserStrategy {
    override val supportedBank = BankSource.SHOPEEPAY

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("shopeepay") ||
                lower.contains("shopee") ||
                lower.contains("pt airpay international indonesia")
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("total pembayaran", "nominal", "jumlah", "total transfer", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya penanganan", "biaya layanan", "biaya admin", "fee")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("transfer ke", "nama penerima", "penerima", "tujuan", "nama:")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("dari", "pengirim", "sumber dana")
        ) ?: "ShopeePay"

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("no. handphone", "no. ponsel", "nomor tujuan", "no. rek")
        )?.filter { it.isDigit() } ?: ParserUtils.accountNumberPattern.findAll(normalized).map { it.value }.lastOrNull().orEmpty()

        val bankTujuan = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "bank")
        ) ?: "ShopeePay"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("no. pesanan", true) || it.contains("id transaksi", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("catatan", "keterangan", "pesan")
        ) ?: ""

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.SHOPEEPAY,
            transactionType = "Transfer ShopeePay",
            transferMethod = "Saldo ShopeePay",
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
