package com.thermalprinter.app.printer

import android.graphics.Bitmap
import com.thermalprinter.app.domain.model.ReceiptTemplate
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import java.text.NumberFormat
import java.util.Locale

object ReceiptFormatter {
    private val idLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(idLocale)
        return "Rp ${formatter.format(amount)}"
    }

    fun buildEscPos(
        receipt: TransactionReceipt,
        settings: StoreSettings,
        logoBitmap: Bitmap? = null
    ): ByteArray {
        val driver = EscPosDriver(lineChars = 32).init()

        val copies = (settings.copies).coerceIn(1, 5)

        for (copyIndex in 1..copies) {
            val isCopy = copyIndex > 1 || receipt.isReprint
            val copyTag = if (isCopy) " (SALINAN #${copyIndex})" else ""

            when (receipt.template) {
                ReceiptTemplate.COMPACT -> formatCompact(driver, receipt, settings, logoBitmap, copyTag)
                ReceiptTemplate.DETAILED -> formatDetailed(driver, receipt, settings, logoBitmap, copyTag)
            }

            driver.feed(3)
            driver.cut()
        }

        return driver.build()
    }

    private fun formatCompact(
        driver: EscPosDriver,
        receipt: TransactionReceipt,
        settings: StoreSettings,
        logoBitmap: Bitmap?,
        copyTag: String
    ) {
        // Header
        driver.alignCenter()
        if (settings.showLogo && logoBitmap != null) {
            driver.image(logoBitmap, maxWidth = 300)
            driver.line()
        }
        driver.bold(true).doubleHeight(true).line(settings.storeName.ifBlank { "BUKTI TRANSAKSI" })
        driver.bold(false).doubleHeight(false)
        if (settings.phone.isNotBlank()) {
            driver.line("WA/Telp: ${settings.phone}")
        }
        if (copyTag.isNotEmpty()) {
            driver.line(copyTag)
        }

        driver.alignLeft()
        driver.divider('-')

        // Source & Transaction
        if (settings.showSource) {
            driver.twoColumn("Sumber", receipt.source.displayName.take(18))
        }
        driver.twoColumn("Jenis", receipt.transactionType.take(18))
        if (receipt.transferMethod.isNotBlank()) {
            driver.twoColumn("Metode", receipt.transferMethod.take(18))
        }
        if (receipt.receiverName.isNotBlank()) {
            driver.twoColumn("Penerima", receipt.receiverName.take(18))
        }

        driver.divider('-')

        // Nominal & Admin Fee (Split vs Combined)
        driver.twoColumn("Nominal", formatRupiah(receipt.transferAmount))

        if (receipt.splitAdminFee) {
            if (receipt.originalAdminFee > 0) {
                driver.twoColumn("Admin Bank", formatRupiah(receipt.originalAdminFee))
            }
            if (settings.showAdminFee && receipt.storeAdminFee > 0) {
                driver.twoColumn("Admin Toko", formatRupiah(receipt.storeAdminFee))
            }
        } else {
            if (settings.showAdminFee && receipt.totalAdminFee() > 0) {
                driver.twoColumn("Total Admin", formatRupiah(receipt.totalAdminFee()))
            }
        }

        driver.doubleDivider()

        // Total
        driver.bold(true).doubleSize(true).alignCenter()
        driver.line(formatRupiah(receipt.calculateTotal()))
        driver.bold(false).doubleSize(false).alignLeft()

        driver.divider('-')

        // Ref & Time
        if (settings.showReference && receipt.referenceNumber.isNotBlank()) {
            driver.twoColumn("No. Ref", receipt.referenceNumber.take(18))
        }
        val dateTime = "${receipt.transactionDate} ${receipt.transactionTime}".trim()
        if (dateTime.isNotBlank()) {
            driver.twoColumn("Waktu", dateTime)
        }
        if (receipt.notes.isNotBlank()) {
            driver.line("Ket: ${receipt.notes}")
        }

        // Footer
        if (settings.footer.isNotBlank()) {
            driver.line()
            driver.alignCenter()
            driver.line(settings.footer)
        }
        driver.line("~ ARBCN ~")
    }

    private fun formatDetailed(
        driver: EscPosDriver,
        receipt: TransactionReceipt,
        settings: StoreSettings,
        logoBitmap: Bitmap?,
        copyTag: String
    ) {
        // Header
        driver.alignCenter()
        if (settings.showLogo && logoBitmap != null) {
            driver.image(logoBitmap, maxWidth = 300)
            driver.line()
        }
        driver.bold(true).doubleHeight(true).line(settings.storeName.ifBlank { "BUKTI PEMBAYARAN" })
        driver.bold(false).doubleHeight(false)

        if (settings.address.isNotBlank()) {
            driver.line(settings.address)
        }
        if (settings.phone.isNotBlank()) {
            driver.line("WA/Telp: ${settings.phone}")
        }
        if (settings.ownerName.isNotBlank()) {
            driver.line("Kasir: ${settings.ownerName}")
        }
        if (copyTag.isNotEmpty()) {
            driver.line(copyTag)
        }

        driver.alignLeft()
        driver.doubleDivider()

        // Status & Source
        driver.twoColumn("Status", receipt.status)
        if (settings.showSource) {
            driver.twoColumn("Layanan", receipt.source.displayName.take(18))
        }
        driver.twoColumn("Transaksi", receipt.transactionType.take(18))
        if (receipt.transferMethod.isNotBlank()) {
            driver.twoColumn("Metode", receipt.transferMethod.take(18))
        }

        driver.divider('-')

        // Sender info
        if (receipt.senderName.isNotBlank()) {
            driver.twoColumn("Pengirim", receipt.senderName.take(18))
        }
        if (receipt.senderAccount.isNotBlank()) {
            driver.twoColumn("No. Sumber", receipt.senderAccount.take(18))
        }

        // Receiver info
        if (receipt.receiverName.isNotBlank()) {
            driver.twoColumn("Penerima", receipt.receiverName.take(18))
        }
        if (receipt.receiverBank.isNotBlank()) {
            driver.twoColumn("Bank Tujuan", receipt.receiverBank.take(18))
        }
        if (receipt.receiverAccount.isNotBlank()) {
            driver.twoColumn("No. Tujuan", receipt.receiverAccount.take(18))
        }

        driver.divider('-')

        // Amount calculation (Split vs Combined)
        driver.twoColumn("Nominal Transfer", formatRupiah(receipt.transferAmount))

        if (receipt.splitAdminFee) {
            if (receipt.originalAdminFee > 0) {
                driver.twoColumn("Biaya Admin Bank", formatRupiah(receipt.originalAdminFee))
            }
            if (settings.showAdminFee) {
                driver.twoColumn("Biaya Admin Toko", formatRupiah(receipt.storeAdminFee))
            }
        } else {
            if (settings.showAdminFee) {
                driver.twoColumn("Total Biaya Admin", formatRupiah(receipt.totalAdminFee()))
            }
        }

        driver.doubleDivider()

        // Total
        driver.bold(true).alignLeft()
        driver.twoColumn("TOTAL BAYAR", formatRupiah(receipt.calculateTotal()))
        driver.bold(false)

        driver.divider('-')

        // Reference & Date
        if (settings.showReference && receipt.referenceNumber.isNotBlank()) {
            driver.twoColumn("No. Referensi", receipt.referenceNumber.take(17))
        }
        if (receipt.transactionDate.isNotBlank()) {
            driver.twoColumn("Tanggal", receipt.transactionDate)
        }
        if (receipt.transactionTime.isNotBlank()) {
            driver.twoColumn("Jam", receipt.transactionTime)
        }

        if (receipt.notes.isNotBlank()) {
            driver.line("Catatan: ${receipt.notes}")
        }

        // Footer
        driver.line()
        driver.alignCenter()
        if (settings.footer.isNotBlank()) {
            driver.line(settings.footer)
        }
        driver.line("~ ARBCN Thermal Printer ~")
    }
}
