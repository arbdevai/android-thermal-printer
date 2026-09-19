package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class FallbackGenericParser : BankParserStrategy {
    override val supportedBank = BankSource.OTHER

    override fun canHandle(text: String): Boolean = true

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        val source = BankSource.detect(normalized)

        val explicitAmounts = ParserUtils.explicitAmountPattern.findAll(normalized)
            .mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }
            .filter { it >= 100 }
            .toList()

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("nominal transfer", "nominal", "jumlah transfer", "jumlah", "total transfer", "total bayar", "total", "amount", "dana keluar")
        ) ?: explicitAmounts.firstOrNull() ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya admin", "biaya transaksi", "admin bank", "biaya layanan")
        ) ?: 0L

        val receiver = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama penerima", "nama tujuan", "nama:", "penerima", "kepada", "tujuan")
        ) ?: ""

        val sender = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama pengirim", "pengirim", "sumber dana", "dari")
        ) ?: ""

        val bank = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "bank", "metode transfer")
        ) ?: ""

        val ref = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("ref", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("ke rekening", "rekening tujuan", "no. tujuan", "no tujuan")
        )?.filter { it.isDigit() } ?: ParserUtils.accountNumberPattern.findAll(normalized).map { it.value }.lastOrNull().orEmpty()

        val senderAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("dari rekening", "rekening sumber")
        )?.filter { it.isDigit() } ?: ""

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("catatan", "berita", "keterangan", "pesan")
        ) ?: ""

        return TransactionReceipt(
            source = source,
            transactionType = detectType(normalized),
            transferMethod = "Transfer",
            transferAmount = transferAmount,
            originalAdminFee = originalFee,
            storeAdminFee = defaultStoreFee,
            splitAdminFee = splitAdminFee,
            totalAmount = transferAmount + defaultStoreFee + originalFee,
            senderName = sender,
            senderAccount = senderAccount,
            receiverName = receiver,
            receiverAccount = receiverAccount,
            receiverBank = bank,
            referenceNumber = ref,
            transactionDate = date,
            transactionTime = time,
            status = ParserUtils.detectStatus(normalized),
            notes = notes,
            rawExtractedText = normalized
        )
    }

    private fun detectType(text: String): String = when {
        text.contains("top up", true) || text.contains("isi saldo", true) -> "Top Up"
        text.contains("qris", true) -> "Pembayaran QRIS"
        text.contains("tarik", true) -> "Tarik Tunai"
        text.contains("kirim uang", true) || text.contains("kirim", true) || text.contains("transfer", true) -> "Transfer"
        text.contains("pembayaran", true) || text.contains("bayar", true) -> "Pembayaran"
        else -> "Transfer"
    }
}
