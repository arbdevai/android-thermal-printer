package com.thermalprinter.app.printer

import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.InvoiceItem
import com.thermalprinter.app.domain.model.StoreSettings
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single canonical document generator for invoices.
 * Produces 32-character wrapped rows used across preview, plain text,
 * PNG image rendering, and ESC/POS thermal printing.
 */
object InvoiceDocument {

    const val LINE_WIDTH = 32

    private val idLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(idLocale)
        return "Rp ${formatter.format(amount)}"
    }

    /**
     * Splits and wraps a text into lines of at most [maxWidth] characters.
     * Preserves paragraph breaks and handles oversized words cleanly without loss.
     */
    fun wrapText(text: String, maxWidth: Int = LINE_WIDTH): List<String> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isEmpty()) {
                result.add("")
                continue
            }

            val words = trimmedParagraph.split(Regex("\\s+"))
            var currentLine = StringBuilder()

            for (word in words) {
                if (word.isEmpty()) continue

                if (word.length > maxWidth) {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                        currentLine = StringBuilder()
                    }
                    var start = 0
                    while (start < word.length) {
                        val end = (start + maxWidth).coerceAtMost(word.length)
                        val chunk = word.substring(start, end)
                        if (end < word.length) {
                            result.add(chunk)
                        } else {
                            currentLine.append(chunk)
                        }
                        start = end
                    }
                } else {
                    if (currentLine.isEmpty()) {
                        currentLine.append(word)
                    } else if (currentLine.length + 1 + word.length <= maxWidth) {
                        currentLine.append(" ").append(word)
                    } else {
                        result.add(currentLine.toString())
                        currentLine = StringBuilder(word)
                    }
                }
            }

            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }

        return result
    }

    /**
     * Centers text within [width] columns. If text exceeds [width], wraps first and centers each line.
     */
    fun center(text: String, width: Int = LINE_WIDTH): List<String> {
        val wrappedLines = wrapText(text, width)
        if (wrappedLines.isEmpty()) return emptyList()

        return wrappedLines.map { line ->
            if (line.length >= width) {
                line
            } else {
                val totalSpaces = width - line.length
                val leftSpaces = totalSpaces / 2
                val rightSpaces = totalSpaces - leftSpaces
                " ".repeat(leftSpaces) + line + " ".repeat(rightSpaces)
            }
        }
    }

    /**
     * Two-column key-value formatter.
     * When left and right fit on a single line, pads spaces between them.
     * When they exceed [width], wraps both sides without truncating, right-aligning values.
     */
    fun twoColumn(left: String, right: String, width: Int = LINE_WIDTH): List<String> {
        val trimmedLeft = left.trim()
        val trimmedRight = right.trim()

        if (trimmedLeft.isEmpty() && trimmedRight.isEmpty()) return emptyList()
        if (trimmedLeft.isEmpty()) {
            return wrapText(trimmedRight, width).map { r ->
                if (r.length >= width) r else " ".repeat(width - r.length) + r
            }
        }
        if (trimmedRight.isEmpty()) {
            return wrapText(trimmedLeft, width)
        }

        // Single line fit check
        if (trimmedLeft.length + 1 + trimmedRight.length <= width) {
            val spaces = width - trimmedLeft.length - trimmedRight.length
            return listOf(trimmedLeft + " ".repeat(spaces) + trimmedRight)
        }

        val result = mutableListOf<String>()
        val leftLines = wrapText(trimmedLeft, width)
        val rightLines = wrapText(trimmedRight, width)

        result.addAll(leftLines)
        for (r in rightLines) {
            if (r.length >= width) {
                result.add(r)
            } else {
                result.add(" ".repeat(width - r.length) + r)
            }
        }

        return result
    }

    fun divider(char: Char = '-', width: Int = LINE_WIDTH): String {
        return char.toString().repeat(width)
    }

    /**
     * Generates immutable canonical lines (rows) for the invoice and store settings snapshot.
     * Every line is guaranteed to be <= [LINE_WIDTH] characters.
     */
    fun lines(invoice: CustomInvoice, settings: StoreSettings): List<String> {
        val docLines = mutableListOf<String>()

        // 1. Header (Store Name, Address, Phone)
        val storeName = settings.storeName.ifBlank { "NOTA INVOICE" }
        docLines.addAll(center(storeName, LINE_WIDTH))

        if (settings.address.isNotBlank()) {
            docLines.addAll(center(settings.address, LINE_WIDTH))
        }
        if (settings.phone.isNotBlank()) {
            docLines.addAll(center("WA/Telp: ${settings.phone}", LINE_WIDTH))
        }

        docLines.add(divider('='))

        // 2. Invoice Metadata & Customer Information (Full wrapping, no truncation)
        val invoiceNum = invoice.invoiceNumber.ifBlank {
            "INV-${invoice.createdAt.toString().takeLast(6)}"
        }
        docLines.addAll(twoColumn("No. Invoice", invoiceNum))

        val dateStr = "${invoice.invoiceDate} ${invoice.invoiceTime}".trim()
        val formattedDate = if (dateStr.isNotBlank()) {
            dateStr
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", idLocale)
            val timestamp = if (invoice.createdAt > 0L) invoice.createdAt else System.currentTimeMillis()
            sdf.format(Date(timestamp))
        }
        docLines.addAll(twoColumn("Tanggal", formattedDate))

        if (invoice.customerName.isNotBlank()) {
            docLines.addAll(twoColumn("Pelanggan", invoice.customerName))
        }
        if (invoice.customerPhone.isNotBlank()) {
            docLines.addAll(twoColumn("No. HP", invoice.customerPhone))
        }
        if (invoice.vehiclePlateOrItemType.isNotBlank()) {
            docLines.addAll(twoColumn("Unit/Kendaraan", invoice.vehiclePlateOrItemType))
        }

        docLines.add(divider('-'))

        // 3. Items & Services List
        if (invoice.items.isEmpty()) {
            docLines.addAll(center("(Tidak ada item)", LINE_WIDTH))
        } else {
            for (item in invoice.items) {
                val itemName = item.name.ifBlank { "Item" }
                docLines.addAll(wrapText(itemName, LINE_WIDTH))

                val qtyPrice = "  ${item.quantity} x ${formatRupiah(item.price)}"
                val subtotalStr = formatRupiah(item.subtotal())

                if (qtyPrice.length + 1 + subtotalStr.length <= LINE_WIDTH) {
                    val spaces = LINE_WIDTH - qtyPrice.length - subtotalStr.length
                    docLines.add(qtyPrice + " ".repeat(spaces) + subtotalStr)
                } else {
                    docLines.add(qtyPrice)
                    val spaces = (LINE_WIDTH - subtotalStr.length).coerceAtLeast(0)
                    docLines.add(" ".repeat(spaces) + subtotalStr)
                }
            }
        }

        docLines.add(divider('-'))

        // 4. Financial Calculations
        val subtotal = invoice.calculateSubtotal()
        val total = invoice.calculateTotal()
        val discount = invoice.discountAmount

        docLines.addAll(twoColumn("Subtotal", formatRupiah(subtotal)))

        if (discount > 0L) {
            docLines.addAll(twoColumn("Diskon", "-${formatRupiah(discount)}"))
        }

        docLines.add(divider('='))

        docLines.addAll(twoColumn("TOTAL", formatRupiah(total)))

        docLines.add(divider('-'))

        // 5. Payment, Change, and Outstanding Balances
        docLines.addAll(twoColumn("Metode Bayar", invoice.paymentMethod.ifBlank { "Tunai" }))

        val paid = invoice.paidAmount
        if (paid > 0L) {
            docLines.addAll(twoColumn("Bayar", formatRupiah(paid)))
            if (paid >= total) {
                val change = paid - total
                docLines.addAll(twoColumn("Kembalian", formatRupiah(change)))
            } else {
                val outstanding = total - paid
                docLines.addAll(twoColumn("Sisa Tagihan", formatRupiah(outstanding)))
            }
        } else {
            // Avoid false paid total when paidAmount is zero
            if (total > 0L) {
                docLines.addAll(twoColumn("Sisa Tagihan", formatRupiah(total)))
            }
        }

        // 6. Warranty Notes & Footer
        if (invoice.warrantyOrNotes.isNotBlank()) {
            docLines.add(divider('-'))
            docLines.addAll(center(invoice.warrantyOrNotes, LINE_WIDTH))
        }

        if (settings.footer.isNotBlank()) {
            docLines.addAll(center(settings.footer, LINE_WIDTH))
        }

        docLines.addAll(center("~ ARBCN Invoice Pro ~", LINE_WIDTH))

        return docLines
    }

    /**
     * Formatted string of the canonical document lines joined by newline.
     */
    fun text(invoice: CustomInvoice, settings: StoreSettings): String {
        return lines(invoice, settings).joinToString("\n")
    }
}
