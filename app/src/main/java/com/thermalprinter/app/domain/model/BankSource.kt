package com.thermalprinter.app.domain.model

enum class BankSource(
    val displayName: String,
    val brandColorHex: Long,
    val detectionKeywords: List<String>
) {
    BCA(
        displayName = "Bank Central Asia (BCA)",
        brandColorHex = 0xFF0060AF,
        detectionKeywords = listOf("bca", "bank central asia", "m-bca", "mybca", "klikbca")
    ),
    BRIMO(
        displayName = "BRI (BRImo)",
        brandColorHex = 0xFF00529C,
        detectionKeywords = listOf("brimo", "bank rakyat indonesia", "bri", "m-bri")
    ),
    LIVIN(
        displayName = "Mandiri (Livin')",
        brandColorHex = 0xFF003876,
        detectionKeywords = listOf("livin", "mandiri", "bank mandiri", "livin' by mandiri")
    ),
    DANA(
        displayName = "DANA Indonesia",
        brandColorHex = 0xFF118EEA,
        detectionKeywords = listOf("dana", "dana indonesia", "isi saldo dana", "kirim uang dana")
    ),
    GOPAY(
        displayName = "GoPay",
        brandColorHex = 0xFF00AA13,
        detectionKeywords = listOf("gopay", "gojek", "pt dompet anak bangsa")
    ),
    OVO(
        displayName = "OVO",
        brandColorHex = 0xFF4C3494,
        detectionKeywords = listOf("ovo", "pt visionet internasional")
    ),
    SHOPEEPAY(
        displayName = "ShopeePay",
        brandColorHex = 0xFFEE4D2D,
        detectionKeywords = listOf("shopeepay", "shopee", "pt airpay international indonesia")
    ),
    SEABANK(
        displayName = "SeaBank",
        brandColorHex = 0xFFFF5722,
        detectionKeywords = listOf("seabank", "sea bank", "pt bank seabank indonesia")
    ),
    OTHER(
        displayName = "Bank / E-Wallet Lain",
        brandColorHex = 0xFF38BDF8,
        detectionKeywords = emptyList()
    );

    companion object {
        fun detect(text: String): BankSource {
            val lower = text.lowercase()
            for (source in entries) {
                if (source == OTHER) continue
                if (source.detectionKeywords.any { lower.contains(it) }) {
                    return source
                }
            }
            return OTHER
        }
    }
}
