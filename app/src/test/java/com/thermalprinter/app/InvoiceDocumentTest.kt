package com.thermalprinter.app

import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.InvoiceItem
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.printer.InvoiceDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceDocumentTest {

    @Test
    fun testLineWidthConstraint_everyLineNeverExceeds32Chars() {
        val invoice = CustomInvoice(
            id = 101L,
            invoiceNumber = "INV-20260920-EXTREMELY-LONG-INVOICE-REF-NUMBER-9999",
            invoiceDate = "20/09/2026",
            invoiceTime = "14:30:45",
            customerName = "Muhammad Rizky Pratama Kusuma Atmadja bin Syarifuddin",
            customerPhone = "+6281234567890123456",
            vehiclePlateOrItemType = "B 9876 XYZ / Honda CBR250RR SP Quick Shifter Repsol",
            businessType = "Bengkel Motor & Mobil Sport",
            items = listOf(
                InvoiceItem(
                    name = "Jasa Servis Turun Mesin & Porting Polish Head Silinder",
                    price = 450000L,
                    quantity = 1
                ),
                InvoiceItem(
                    name = "Oli Mesin Motul 300V Factory Line 10W-40 1 Liter",
                    price = 375000L,
                    quantity = 2
                ),
                InvoiceItem(
                    name = "Busi Iridium Laser NGK Racing Spark Plug CPR9EAIX-9",
                    price = 125000L,
                    quantity = 2
                )
            ),
            discountAmount = 50000L,
            paidAmount = 1500000L,
            paymentMethod = "Transfer Bank Mandiri Virtual Account",
            warrantyOrNotes = "Garansi servis mesin 30 hari atau 1000 km. Harap simpan nota ini untuk klaim garansi resmi."
        )

        val settings = StoreSettings(
            storeName = "BENGKEL RESMI MOTOR DAN MOBIL ARBCN SPEED PERFORMANCE JAKARTA",
            address = "Jl. Raya Industri Otomotif Kavling 88 Blok C2 No. 15-16, Jakarta Pusat",
            phone = "081299887766 / 021-5554321",
            footer = "Terima kasih atas kunjungan dan kepercayaan Anda. Layanan darurat 24 jam hubungi WA kami."
        )

        val lines = InvoiceDocument.lines(invoice, settings)

        assertTrue("Generated document lines should not be empty", lines.isNotEmpty())

        for ((index, line) in lines.withIndex()) {
            assertTrue(
                "Line $index ('$line') length ${line.length} exceeds 32 characters limit",
                line.length <= 32
            )
        }
    }

    @Test
    fun testFullFieldsIncludedWithoutTruncation() {
        val invoice = CustomInvoice(
            invoiceNumber = "INV-778899",
            invoiceDate = "20/09/2026",
            invoiceTime = "10:15",
            customerName = "Budi Santoso",
            customerPhone = "081234567890",
            vehiclePlateOrItemType = "B 1234 ABC / Vario 150",
            items = listOf(
                InvoiceItem(name = "Oli MPX2", price = 55000L, quantity = 1),
                InvoiceItem(name = "Kampas Rem Depan", price = 45000L, quantity = 2)
            ),
            discountAmount = 10000L,
            paidAmount = 150000L,
            paymentMethod = "QRIS",
            warrantyOrNotes = "Garansi toko 7 hari"
        )

        val settings = StoreSettings(
            storeName = "TOKO ANDA JAYA",
            address = "Jl. Sudirman 10",
            phone = "0811223344",
            footer = "Terima kasih banyak"
        )

        val text = InvoiceDocument.text(invoice, settings)

        // Store headers
        assertTrue(text.contains("TOKO ANDA JAYA"))
        assertTrue(text.contains("Jl. Sudirman 10"))
        assertTrue(text.contains("0811223344"))

        // Invoice metadata & customer
        assertTrue(text.contains("INV-778899"))
        assertTrue(text.contains("20/09/2026 10:15"))
        assertTrue(text.contains("Budi Santoso"))
        assertTrue(text.contains("081234567890"))
        assertTrue(text.contains("B 1234 ABC / Vario 150"))

        // Items: name, quantity, unit price, subtotal
        assertTrue(text.contains("Oli MPX2"))
        assertTrue(text.contains("1 x Rp 55.000"))
        assertTrue(text.contains("Kampas Rem Depan"))
        assertTrue(text.contains("2 x Rp 45.000"))

        // Financials
        // Subtotal = 55000 + 90000 = 145000
        assertTrue(text.contains("Subtotal"))
        assertTrue(text.contains("Rp 145.000"))
        assertTrue(text.contains("Diskon"))
        assertTrue(text.contains("-Rp 10.000"))
        // Total = 135000
        assertTrue(text.contains("TOTAL"))
        assertTrue(text.contains("Rp 135.000"))

        // Payment
        assertTrue(text.contains("QRIS"))
        assertTrue(text.contains("Bayar"))
        assertTrue(text.contains("Rp 150.000"))
        // Change = 150000 - 135000 = 15000
        assertTrue(text.contains("Kembalian"))
        assertTrue(text.contains("Rp 15.000"))

        // Notes & Footer
        assertTrue(text.contains("Garansi toko 7 hari"))
        assertTrue(text.contains("Terima kasih banyak"))
        assertTrue(text.contains("~ ARBCN Invoice Pro ~"))
    }

    @Test
    fun testNoFalsePaidTotalWhenZero_showsOutstandingInstead() {
        val invoice = CustomInvoice(
            invoiceNumber = "INV-UNPAID-01",
            customerName = "Pak Heri",
            items = listOf(
                InvoiceItem(name = "Servis Komputer", price = 100000L, quantity = 1)
            ),
            paidAmount = 0L // Unpaid / pending payment
        )

        val settings = StoreSettings(storeName = "Klinik Komputer")

        val text = InvoiceDocument.text(invoice, settings)
        val lines = InvoiceDocument.lines(invoice, settings)

        // Must NOT print false payment information like "Bayar: Rp 0" or change
        assertFalse("Should not contain false paid line 'Bayar'", lines.any { it.trim().startsWith("Bayar") })
        assertFalse("Should not contain 'Kembalian' when unpaid", text.contains("Kembalian"))

        // Must show outstanding balance
        assertTrue("Should display outstanding balance", text.contains("Sisa Tagihan"))
        assertTrue("Outstanding balance should equal total Rp 100.000", text.contains("Rp 100.000"))
    }

    @Test
    fun testPartialPayment_showsPaidAndOutstanding() {
        val invoice = CustomInvoice(
            invoiceNumber = "INV-PARTIAL-02",
            items = listOf(
                InvoiceItem(name = "Barang A", price = 200000L, quantity = 1)
            ),
            paidAmount = 75000L // Partial payment
        )

        val settings = StoreSettings()
        val text = InvoiceDocument.text(invoice, settings)

        assertTrue("Should show partial payment amount", text.contains("Rp 75.000"))
        // Outstanding balance = 200000 - 75000 = 125000
        assertTrue("Should show Sisa Tagihan", text.contains("Sisa Tagihan"))
        assertTrue("Should show remaining 125000", text.contains("Rp 125.000"))
        assertFalse("Should not show Kembalian when underpaid", text.contains("Kembalian"))
    }

    @Test
    fun testExactPayment_showsZeroChange() {
        val invoice = CustomInvoice(
            invoiceNumber = "INV-EXACT-03",
            items = listOf(
                InvoiceItem(name = "Barang B", price = 50000L, quantity = 1)
            ),
            paidAmount = 50000L
        )

        val settings = StoreSettings()
        val text = InvoiceDocument.text(invoice, settings)

        assertTrue(text.contains("Bayar"))
        assertTrue(text.contains("Kembalian"))
        assertTrue(text.contains("Rp 0"))
        assertFalse("Should not show Sisa Tagihan when fully paid", text.contains("Sisa Tagihan"))
    }

    @Test
    fun testDiscountFormatting_onlyPresentWhenGreaterThanZero() {
        val invoiceWithDiscount = CustomInvoice(
            items = listOf(InvoiceItem(name = "Item", price = 10000L, quantity = 1)),
            discountAmount = 2000L
        )
        val textWithDiscount = InvoiceDocument.text(invoiceWithDiscount, StoreSettings())
        assertTrue("Should contain discount", textWithDiscount.contains("Diskon"))
        assertTrue("Should format discount with minus", textWithDiscount.contains("-Rp 2.000"))

        val invoiceWithoutDiscount = CustomInvoice(
            items = listOf(InvoiceItem(name = "Item", price = 10000L, quantity = 1)),
            discountAmount = 0L
        )
        val textWithoutDiscount = InvoiceDocument.text(invoiceWithoutDiscount, StoreSettings())
        assertFalse("Should not contain discount when zero", textWithoutDiscount.contains("Diskon"))
    }

    @Test
    fun testTextApiMatchesLinesApi() {
        val invoice = CustomInvoice(
            invoiceNumber = "INV-API-TEST",
            items = listOf(InvoiceItem(name = "Jasa Test", price = 25000L, quantity = 1))
        )
        val settings = StoreSettings()

        val lines = InvoiceDocument.lines(invoice, settings)
        val text = InvoiceDocument.text(invoice, settings)

        assertEquals(lines.joinToString("\n"), text)
    }

    @Test
    fun testWrapTextPreservesLongWordContent() {
        val longWord = "SUPEREXTREMELYSUPERLONGWORDEXCEEDINGTHIRTYTWOCHARACTERSWITHOUTANYSPACES"
        val wrapped = InvoiceDocument.wrapText(longWord, 32)

        assertTrue("Should break into multiple lines", wrapped.size > 1)
        for (w in wrapped) {
            assertTrue("Chunk length must be <= 32", w.length <= 32)
        }
        assertEquals(longWord, wrapped.joinToString(""))
    }

    @Test
    fun testTwoColumnFormatting() {
        val singleLine = InvoiceDocument.twoColumn("Label", "Value", 32)
        assertEquals(1, singleLine.size)
        assertEquals(32, singleLine[0].length)
        assertTrue(singleLine[0].startsWith("Label"))
        assertTrue(singleLine[0].endsWith("Value"))

        val longLine = InvoiceDocument.twoColumn(
            "Unit/Kendaraan",
            "B 1234 ABC / Toyota Kijang Innova Reborn Diesel 2.4V",
            32
        )
        for (line in longLine) {
            assertTrue("Line '$line' must be <= 32 chars", line.length <= 32)
        }
        assertTrue("Contains label", longLine.any { it.contains("Unit/Kendaraan") })
        assertTrue("Contains value part 1", longLine.any { it.contains("Toyota") })
        assertTrue("Contains value part 2", longLine.any { it.contains("Diesel") })
    }

    @Test
    fun testEmptyItemsInvoice_doesNotCrash() {
        val invoice = CustomInvoice(items = emptyList())
        val lines = InvoiceDocument.lines(invoice, StoreSettings())

        assertTrue(lines.isNotEmpty())
        assertTrue(lines.any { it.contains("(Tidak ada item)") })
        for (line in lines) {
            assertTrue(line.length <= 32)
        }
    }
}
