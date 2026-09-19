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

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L, splitAdminFee = true)
        assertEquals(BankSource.BCA, receipt.source)
        assertEquals(150000L, receipt.transferAmount)
        assertEquals(2000L, receipt.storeAdminFee)
        assertEquals(152000L, receipt.calculateTotal())
        assertEquals("BUDI SANTOSO", receipt.receiverName)
        assertEquals("2026091914351234", receipt.referenceNumber)
        assertEquals("Bayar pulsa", receipt.notes)
    }

    @Test
    fun testParseBrimoTransfer() {
        val raw = """
            BRImo
            TRANSAKSI BERHASIL
            Nominal: Rp 250.000
            Biaya Admin: Rp 2.500
            Tujuan: ANDI PRATAMA
            Rekening Tujuan: 012301098765501
            Nomor Referensi: BRI20260919999
            Keterangan: Tagihan listrik
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L, splitAdminFee = true)
        assertEquals(BankSource.BRIMO, receipt.source)
        assertEquals(250000L, receipt.transferAmount)
        assertEquals(2500L, receipt.originalAdminFee)
        assertEquals(2000L, receipt.storeAdminFee)
        // Total = 250000 + 2000 (store) + 2500 (bank) = 254500
        assertEquals(254500L, receipt.calculateTotal())
        assertEquals("ANDI PRATAMA", receipt.receiverName)
        assertEquals("BRI20260919999", receipt.referenceNumber)
        assertEquals("Tagihan listrik", receipt.notes)
    }

    @Test
    fun testParseLivinTransfer() {
        val raw = """
            Livin' by Mandiri
            Status: Berhasil
            Nominal Transfer: Rp 100.000
            Biaya Transaksi: Rp 2.500
            Kepada: HENDRA WIJAYA
            Rekening Tujuan: 1370012345678
            Bank Tujuan: BCA
            No. Resi: 20260919LIVIN777
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 3000L, splitAdminFee = true)
        assertEquals(BankSource.LIVIN, receipt.source)
        assertEquals(100000L, receipt.transferAmount)
        assertEquals(2500L, receipt.originalAdminFee)
        assertEquals(3000L, receipt.storeAdminFee)
        assertEquals(105500L, receipt.calculateTotal())
        assertEquals("HENDRA WIJAYA", receipt.receiverName)
        assertEquals("20260919LIVIN777", receipt.referenceNumber)
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

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 3000L, splitAdminFee = true)
        assertEquals(BankSource.DANA, receipt.source)
        assertEquals(50000L, receipt.transferAmount)
        assertEquals(3000L, receipt.storeAdminFee)
        assertEquals(53000L, receipt.calculateTotal())
        assertEquals("SITI AMINAH", receipt.receiverName)
        assertEquals("DANA987654321", receipt.referenceNumber)
    }

    @Test
    fun testParseGopayTransfer() {
        val raw = """
            GoPay
            Transfer Berhasil
            Nominal: Rp 75.000
            Biaya Layanan: Rp 1.000
            Penerima: RIZKY MAULANA
            No. Ponsel: 085712345678
            Order ID: GOPAY-2026-999
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L)
        assertEquals(BankSource.GOPAY, receipt.source)
        assertEquals(75000L, receipt.transferAmount)
        assertEquals(1000L, receipt.originalAdminFee)
        assertEquals(2000L, receipt.storeAdminFee)
        assertEquals(78000L, receipt.calculateTotal())
        assertEquals("RIZKY MAULANA", receipt.receiverName)
        assertEquals("GOPAY-2026-999", receipt.referenceNumber)
    }

    @Test
    fun testParseOvoTransfer() {
        val raw = """
            OVO
            Transfer Berhasil
            Nominal: Rp 45.000
            Biaya Transaksi: Rp 2.500
            Penerima: DEWI LESTARI
            No. Referensi: OVO20260919111
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L)
        assertEquals(BankSource.OVO, receipt.source)
        assertEquals(45000L, receipt.transferAmount)
        assertEquals(2500L, receipt.originalAdminFee)
        assertEquals(49500L, receipt.calculateTotal())
        assertEquals("DEWI LESTARI", receipt.receiverName)
    }

    @Test
    fun testParseShopeePayTransfer() {
        val raw = """
            ShopeePay
            Rincian Transfer
            Total Pembayaran: Rp 80.000
            Transfer ke: AGUS SETIAWAN
            No. Handphone: 089912345678
            No. Pesanan: SPP20260919888
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L)
        assertEquals(BankSource.SHOPEEPAY, receipt.source)
        assertEquals(80000L, receipt.transferAmount)
        assertEquals(82000L, receipt.calculateTotal())
        assertEquals("AGUS SETIAWAN", receipt.receiverName)
    }

    @Test
    fun testParseSeabankTransfer() {
        val raw = """
            SeaBank
            Transfer Berhasil
            Jumlah Transfer: Rp 120.000
            Biaya Transfer: Rp 0
            Nama Penerima: RINA KUSUMA
            Rekening Penerima: 901234567890
            No. Referensi: SEA20260919333
        """.trimIndent()

        val receipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L)
        assertEquals(BankSource.SEABANK, receipt.source)
        assertEquals(120000L, receipt.transferAmount)
        assertEquals(0L, receipt.originalAdminFee)
        assertEquals(122000L, receipt.calculateTotal())
        assertEquals("RINA KUSUMA", receipt.receiverName)
        assertEquals("SEA20260919333", receipt.referenceNumber)
    }

    @Test
    fun testSplitAndCombineAdminFee() {
        val raw = """
            BRImo
            Nominal: Rp 100.000
            Biaya Admin: Rp 2.500
            Tujuan: TESTING
        """.trimIndent()

        val splitReceipt = ReceiptParserEngine.parse(raw, defaultStoreFee = 2000L, splitAdminFee = true)
        assertTrue(splitReceipt.splitAdminFee)
        assertEquals(2500L, splitReceipt.originalAdminFee)
        assertEquals(2000L, splitReceipt.storeAdminFee)
        assertEquals(4500L, splitReceipt.totalAdminFee())
        assertEquals(104500L, splitReceipt.calculateTotal())

        val combinedReceipt = splitReceipt.copy(splitAdminFee = false)
        assertEquals(4500L, combinedReceipt.totalAdminFee())
        assertEquals(104500L, combinedReceipt.calculateTotal())
    }
}
