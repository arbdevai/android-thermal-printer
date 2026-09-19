package com.thermalprinter.app.domain.parser

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.ReceiptTemplate
import com.thermalprinter.app.domain.model.TransactionReceipt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline parser for transaction text shared by bank/e-wallet apps or produced by ML Kit OCR.
 * Supports: BCA, BRImo, Livin Mandiri, DANA, GoPay, OVO, ShopeePay, SeaBank, and generic formats.
 */
object ReceiptParserEngine {
    private val amountPattern = Regex("(?i)(?:rp\\.?|idr)\\s*([0-9][0-9.\\s]*(?:,[0-9]{1,2})?)")
    private val referencePattern = Regex("(?i)(?:no\\.?\\s*(?:referensi|ref|transaksi|pesanan)|reference(?:\\s*number)?|id\\s*transaksi)\\s*[:#-]?\\s*([A-Z0-9./_-]{5,})")
    private val datePattern = Regex("\\b(?:[0-3]?\\d[/-][01]?\\d[/-](?:20)?\\d{2}|[0-3]?\\d\\s+(?:jan|feb|mar|apr|mei|may|jun|jul|agu|aug|sep|okt|oct|nov|des|dec)[a-z]*\\s+(?:20)?\\d{2})\\b", RegexOption.IGNORE_CASE)
    private val timePattern = Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?(?:\\s*(?:wib|wita|wit))?\\b", RegexOption.IGNORE_CASE)
    private val accountNumberPattern = Regex("\\b(?:\\d{4,}[-\\s]?\\d{4,}|08\\d{8,11})\\b")

    fun parse(text: String, defaultStoreFee: Long = 0L): TransactionReceipt {
        val normalized = text.replace("\r", "").trim()
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        val source = BankSource.detect(normalized)

        val amounts = amountPattern.findAll(normalized).mapNotNull { parseAmount(it.groupValues[1]) }.toList()

        val transferAmount = findAmountAfterLabels(
            lines,
            listOf("nominal", "jumlah", "amount", "total transfer", "total bayar", "dikirim", "kirim", "dana keluar")
        ) ?: amounts.firstOrNull { it > 0 } ?: 0L

        val originalFee = findAmountAfterLabels(
            lines,
            listOf("biaya admin", "biaya transaksi", "fee", "admin bank", "biaya layanan")
        ) ?: 0L

        val receiver = findValueAfterLabels(
            lines,
            listOf("penerima", "kepada", "tujuan", "nama tujuan", "nama penerima", "ke rekening", "ke")
        )

        val sender = findValueAfterLabels(
            lines,
            listOf("pengirim", "dari", "sumber", "dari rekening", "sumber dana", "nama pengirim")
        )

        val bank = findValueAfterLabels(
            lines,
            listOf("bank tujuan", "bank penerima", "bank", "metode transfer")
        ) ?: ""

        val ref = referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?: lines.firstOrNull { it.contains("ref", true) || it.contains("referensi", true) }
                ?.substringAfterLast(":")?.trim().orEmpty()

        val date = datePattern.find(normalized)?.value.orEmpty()
        val time = timePattern.find(normalized)?.groupValues?.getOrNull(0).orEmpty()

        val status = when {
            normalized.contains("gagal", true) || normalized.contains("failed", true) -> "GAGAL"
            normalized.contains("pending", true) || normalized.contains("diproses", true) -> "DIPROSES"
            normalized.contains("berhasil", true) || normalized.contains("sukses", true) || normalized.contains("success", true) -> "BERHASIL"
            else -> "BERHASIL"
        }

        // Find potential account numbers
        val numbers = accountNumberPattern.findAll(normalized).map { it.value }.toList()
        val receiverAccount = numbers.lastOrNull().orEmpty()
        val senderAccount = if (numbers.size > 1) numbers.first() else ""

        return TransactionReceipt(
            source = source,
            transactionType = detectType(normalized),
            transferAmount = transferAmount,
            originalAdminFee = originalFee,
            storeAdminFee = defaultStoreFee,
            totalAmount = transferAmount + defaultStoreFee,
            senderName = sender.orEmpty(),
            senderAccount = senderAccount,
            receiverName = receiver.orEmpty(),
            receiverAccount = receiverAccount,
            receiverBank = bank,
            referenceNumber = ref,
            transactionDate = date.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
            transactionTime = time.ifBlank { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) },
            status = status,
            rawExtractedText = normalized
        )
    }

    fun parseAmount(raw: String): Long? {
        val value = raw.trim().replace(" ", "")
        if (value.isEmpty()) return null
        val integerPart = value.substringBefore(',').replace(".", "").filter(Char::isDigit)
        return integerPart.toLongOrNull()
    }

    private fun findAmountAfterLabels(lines: List<String>, labels: List<String>): Long? {
        for (index in lines.indices) {
            val line = lines[index]
            if (labels.any { line.contains(it, true) }) {
                amountPattern.find(line)?.groupValues?.getOrNull(1)?.let {
                    parseAmount(it)?.let { amount -> return amount }
                }
                if (index + 1 < lines.size) {
                    amountPattern.find(lines[index + 1])?.groupValues?.getOrNull(1)?.let {
                        parseAmount(it)?.let { amount -> return amount }
                    }
                }
            }
        }
        return null
    }

    private fun findValueAfterLabels(lines: List<String>, labels: List<String>): String? {
        for (index in lines.indices) {
            val line = lines[index]
            val label = labels.firstOrNull { line.startsWith(it, true) || line.contains(it, true) } ?: continue
            val sameLine = line.substringAfter(label, "").trim().trimStart(':', '-', '#').trim()
            if (sameLine.isNotBlank() && !amountPattern.containsMatchIn(sameLine)) {
                return cleanValue(sameLine)
            }
            if (index + 1 < lines.size) {
                val next = lines[index + 1].trim().trimStart(':', '-', '#').trim()
                if (next.isNotBlank() && !amountPattern.containsMatchIn(next)) {
                    return cleanValue(next)
                }
            }
        }
        return null
    }

    private fun cleanValue(value: String): String {
        return value.replace(Regex("(?i)^(rp|idr)\\s*"), "")
            .trim()
            .take(30)
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
