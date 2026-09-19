package com.thermalprinter.app.domain.model

data class InvoiceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val price: Long = 0L,
    val quantity: Int = 1
) {
    fun subtotal(): Long = price * quantity
}

data class CustomInvoice(
    val id: Long = 0L,
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val invoiceTime: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val vehiclePlateOrItemType: String = "", // e.g. "B 1234 ABC / Vario 150" or "iPhone 13 / LCD Service"
    val businessType: String = "Bengkel / Servis", // "Bengkel Motor/Mobil", "Jasa / Service", "Toko / Penjualan"
    val items: List<InvoiceItem> = emptyList(),
    val discountAmount: Long = 0L,
    val paidAmount: Long = 0L,
    val paymentMethod: String = "Tunai", // "Tunai", "Transfer Bank", "QRIS"
    val warrantyOrNotes: String = "Garansi servis 7 hari. Terima kasih atas kepercayaan Anda.",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculateSubtotal(): Long = items.sumOf { it.subtotal() }
    fun calculateTotal(): Long = (calculateSubtotal() - discountAmount).coerceAtLeast(0L)
    fun calculateChange(): Long = (paidAmount - calculateTotal()).coerceAtLeast(0L)
}
