package com.thermalprinter.app.domain.model

data class StoreSettings(
    val storeName: String = "TOKO ANDA",
    val address: String = "",
    val phone: String = "",
    val ownerName: String = "",
    val footer: String = "Terima kasih atas kunjungan Anda",
    val logoPath: String = "",
    val defaultAdminFee: Long = 2000L,
    val showLogo: Boolean = true,
    val showReference: Boolean = true,
    val showSource: Boolean = true,
    val showAdminFee: Boolean = true,
    val splitAdminFee: Boolean = true, // default dipisah: tampilkan Admin Bank & Admin Toko terpisah
    val quickFeePresets: String = "0,1000,2000,2500,3000,5000", // comma-separated custom quick fees
    val copies: Int = 1,
    val template: ReceiptTemplate = ReceiptTemplate.COMPACT,
    val printerAddress: String = "",
    val printerName: String = ""
) {
    fun getQuickFeeList(): List<Long> {
        val list = quickFeePresets.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it >= 0 }
        return if (list.isEmpty()) listOf(0L, 2000L, 3000L, 5000L) else list
    }
}
