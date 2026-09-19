package com.thermalprinter.app

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.parser.ReceiptParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserEngineTest {

    @Test
    fun testParseBcaTransfer() {
        val raw = """
            m-Transfer: BERHASIL
            19/09/2026 14:35:12
            DARI REKENING: 1234567890
            KE REKENING: 0987654321
            NAMA: BUDI SANTOSO
            JUMLAH: Rp 150.000,00
            BERITA: Bayar pulsa
            NO. REFERENSI: 2026091914351234
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L)
        assertEquals(BankSource.BCA, receipt.source)
        assertEquals(150000L, receipt.transferAmount)
        assertEquals(2000L, receipt.storeAdminFee)
        assertEquals(152000L, receipt.calculateTotal())
        assertEquals("BUDI SANTOSO", receipt.receiverName)
        assertEquals("2026091914351234", receipt.referenceNumber)
    }

    @Test
    fun testParseDanaTransfer() {
        val raw = """
            DANA Indonesia
            Kirim Uang Berhasil
            19 Sep 2026 10:15 WIB
            Total Bayar
            Rp50.000
            Penerima: SITI AMINAH
            No. Tujuan: 081234567890
            ID Transaksi: DANA987654321
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 3000L)
        assertEquals(BankSource.DANA, receipt.source)
        assertEquals(50000L, receipt.transferAmount)
        assertEquals(3000L, receipt.storeAdminFee)
        assertEquals(53000L, receipt.calculateTotal())
        assertEquals("SITI AMINAH", receipt.receiverName)
        assertEquals("DANA987654321", receipt.referenceNumber)
    }

    @Test
    fun testParseBrimoTransfer() {
        val raw = """
            BRImo
            TRANSAKSI BERHASIL
            Nominal: Rp 250.000
            Biaya Admin: Rp 2.500
            Tujuan: ANDI PRATAMA
            Nomor Referensi: BRI20260919999
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L)
        assertEquals(BankSource.BRIMO, receipt.source)
        assertEquals(250000L, receipt.transferAmount)
        assertEquals(2000L, receipt.storeAdminFee)
        assertEquals(252000L, receipt.calculateTotal())
        assertEquals("ANDI PRATAMA", receipt.receiverName)
    }

    @Test
    fun testParseAmountFormatting() {
        assertEquals(50000L, ReceiptParserEngine.parseAmount("50.000"))
        assertEquals(1500000L, ReceiptParserEngine.parseAmount("1.500.000,00"))
        assertEquals(2500L, ReceiptParserEngine.parseAmount("2.500"))
    }
}
