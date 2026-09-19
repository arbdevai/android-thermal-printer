package com.thermalprinter.app.printer

import android.graphics.Bitmap
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.StoreSettings
import java.text.NumberFormat
import java.util.Locale

object InvoiceFormatter {
    private val idLocale = Locale("id", "ID")

    private fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(idLocale)
        return "Rp ${formatter.format(amount)}"
    }

    fun buildEscPos(
        invoice: CustomInvoice,
        settings: StoreSettings,
        logoBitmap: Bitmap? = null
    ): ByteArray {
        val driver = EscPosDriver(lineChars = 32).init()

        // 1. Header (Logo & Store Info)
        driver.alignCenter()
        if (settings.showLogo && logoBitmap != null) {
            driver.image(logoBitmap, maxWidth = 300)
            driver.line()
        }
        driver.bold(true).doubleHeight(true).line(settings.storeName.ifBlank { "NOTA INVOICE" })
        driver.bold(false).doubleHeight(false)

        if (settings.address.isNotBlank()) {
            driver.line(settings.address)
        }
        if (settings.phone.isNotBlank()) {
            driver.line("WA/Telp: ${settings.phone}")
        }

        driver.doubleDivider()

        // 2. Invoice Meta & Customer
        driver.alignLeft()
        driver.twoColumn("No. Invoice", invoice.invoiceNumber.ifBlank { "INV-${invoice.createdAt.toString().takeLast(6)}" })
        driver.twoColumn("Tanggal", "${invoice.invoiceDate} ${invoice.invoiceTime}".trim())
        if (invoice.customerName.isNotBlank()) {
            driver.twoColumn("Pelanggan", invoice.customerName.take(18))
        }
        if (invoice.vehiclePlateOrItemType.isNotBlank()) {
            driver.twoColumn("Unit/Kendaraan", invoice.vehiclePlateOrItemType.take(16))
        }

        driver.divider('-')

        // 3. Items & Services List
        driver.alignLeft()
        for (item in invoice.items) {
            val itemName = if (item.quantity > 1) "${item.name} (${item.quantity}x)" else item.name
            driver.twoColumn(itemName.take(20), formatRupiah(item.subtotal()))
        }

        driver.divider('-')

        // 4. Financial Calculations
        driver.twoColumn("Subtotal", formatRupiah(invoice.calculateSubtotal()))
        if (invoice.discountAmount > 0) {
            driver.twoColumn("Diskon", "-${formatRupiah(invoice.discountAmount)}")
        }

        driver.doubleDivider()

        // 5. Total Highlight
        driver.bold(true).doubleSize(true).alignCenter()
        driver.line(formatRupiah(invoice.calculateTotal()))
        driver.bold(false).doubleSize(false).alignLeft()

        driver.divider('-')

        // 6. Payment & Change
        driver.twoColumn("Metode Bayar", invoice.paymentMethod)
        if (invoice.paidAmount > 0) {
            driver.twoColumn("Bayar", formatRupiah(invoice.paidAmount))
            val change = invoice.calculateChange()
            driver.twoColumn("Kembalian", formatRupiah(change))
        }

        // 7. Footer / Warranty Notes
        if (invoice.warrantyOrNotes.isNotBlank()) {
            driver.divider('-')
            driver.alignCenter()
            driver.line(invoice.warrantyOrNotes)
        }

        if (settings.footer.isNotBlank()) {
            driver.line(settings.footer)
        }
        driver.line("~ ARBCN Invoice Pro ~")

        driver.feed(3)
        driver.cut()

        return driver.build()
    }
}
