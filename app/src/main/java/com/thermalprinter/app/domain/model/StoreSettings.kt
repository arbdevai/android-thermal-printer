package com.thermalprinter.app.domain.model

data class StoreSettings(
    val storeName: String = "TOKO ANDA",
    val address: String = "",
    val phone: String = "",
    val ownerName: String = "",
    val footer: String = "Terima kasih atas kunjungan Anda",
    val logoPath: String = "",
    val defaultAdminFee: Long = 0,
    val showLogo: Boolean = true,
    val showReference: Boolean = true,
    val showSource: Boolean = true,
    val showAdminFee: Boolean = true,
    val copies: Int = 1,
    val template: ReceiptTemplate = ReceiptTemplate.COMPACT,
    val printerAddress: String = "",
    val printerName: String = ""
)
