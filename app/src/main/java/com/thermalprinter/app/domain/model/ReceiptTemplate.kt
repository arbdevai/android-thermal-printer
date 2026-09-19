package com.thermalprinter.app.domain.model

enum class ReceiptTemplate(
    val id: String,
    val title: String,
    val description: String
) {
    COMPACT(
        id = "compact",
        title = "Ringkas (Hemat Kertas)",
        description = "Format simpel fokus pada total dan nominal transfer, cocok untuk transaksi cepat."
    ),
    DETAILED(
        id = "detailed",
        title = "Detail Lengkap",
        description = "Format lengkap dengan identitas pengirim, penerima, nomor referensi, dan detail status."
    );

    companion object {
        fun fromId(id: String?): ReceiptTemplate {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: COMPACT
        }
    }
}
