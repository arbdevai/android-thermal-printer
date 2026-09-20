package com.thermalprinter.app.printer

import android.graphics.Bitmap
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.StoreSettings

/**
 * ESC/POS thermal printer byte generator for invoices.
 * Consumes the single canonical document rows from [InvoiceDocument].
 */
object InvoiceFormatter {

    fun formatRupiah(amount: Long): String {
        return InvoiceDocument.formatRupiah(amount)
    }

    /**
     * Builds ESC/POS thermal printer byte array for the given invoice.
     * Uses canonical document lines, includes logo if enabled and provided, followed by feed and cut.
     */
    fun buildEscPos(
        invoice: CustomInvoice,
        settings: StoreSettings,
        logoBitmap: Bitmap? = null
    ): ByteArray {
        val driver = EscPosDriver(lineChars = InvoiceDocument.LINE_WIDTH).init()

        // 1. Store Logo if enabled
        if (settings.showLogo && logoBitmap != null) {
            driver.alignCenter()
            driver.image(logoBitmap, maxWidth = 300)
            driver.line()
        }

        // 2. Canonical Document Rows
        driver.alignLeft()
        val lines = InvoiceDocument.lines(invoice, settings)
        for (line in lines) {
            driver.line(line)
        }

        // 3. Feed and Cut
        driver.feed(3)
        driver.cut()

        return driver.build()
    }
}
