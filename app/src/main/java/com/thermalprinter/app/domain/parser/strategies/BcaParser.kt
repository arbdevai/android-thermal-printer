package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

class BcaParser : BankParserStrategy {
    override val supportedBank = BankSource.BCA

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        // Match source-app markers only. A plain "BCA" may be the recipient bank
        // on another app's receipt (for example Livin -> BCA).
        return lower.contains("m-transfer") ||
                lower.contains("m-bca") ||
                lower.contains("bca mobile") ||
                lower.contains("mybca") ||
                lower.contains("klikbca") ||
                lower.contains("bank central asia")
                || lower.lines().take(3).any { it.trim().equals("bca", ignoreCase = true) }
    }

    override fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val transferAmount = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("jumlah", "nominal", "total transfer", "total bayar", "amount")
        ) ?: ParserUtils.explicitAmountPattern.findAll(normalized).mapNotNull { ParserUtils.parseAmount(it.groupValues[1]) }.firstOrNull { it >= 100 } ?: 0L

        val originalFee = ParserUtils.findAmountAfterLabels(
            lines,
            listOf("biaya admin", "biaya transaksi", "fee", "biaya transfer")
        ) ?: 0L

        val receiverName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama penerima", "nama tujuan", "nama:", "penerima", "kepada", "tujuan")
        ) ?: ""

        val senderName = ParserUtils.findValueAfterLabels(
            lines,
            listOf("nama pengirim", "pengirim", "sumber dana", "dari")
        ) ?: ""

        val receiverAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("ke rekening", "rekening tujuan", "no. rekening tujuan", "no. rek tujuan")
        )?.filter { it.isDigit() } ?: ""

        val senderAccount = ParserUtils.findValueAfterLabels(
            lines,
            listOf("dari rekening", "rekening sumber", "rekening asal")
        )?.filter { it.isDigit() } ?: ""

        val bankTujuan = ParserUtils.findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "ke bank", "bank")
        ) ?: "BCA"

        val refNumber = ParserUtils.referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("no. referensi", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val notes = ParserUtils.findValueAfterLabels(
            lines,
            listOf("berita", "catatan", "keterangan", "pesan")
        ) ?: ""

        val method = when {
            normalized.contains("bi-fast", true) -> "BI-FAST"
            normalized.contains("antar bank", true) || normalized.contains("online", true) -> "Transfer Online"
            else -> "Sesama BCA"
        }

        val date = ParserUtils.datePattern.find(normalized)?.value.orEmpty().ifBlank { ParserUtils.defaultDate() }
        val time = ParserUtils.timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty().ifBlank { ParserUtils.defaultTime() }

        return TransactionReceipt(
            source = BankSource.BCA,
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
