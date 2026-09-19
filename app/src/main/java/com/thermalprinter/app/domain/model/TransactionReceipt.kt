package com.thermalprinter.app.domain.model

data class TransactionReceipt(
    val id: Long = 0L,
    val source: BankSource = BankSource.OTHER,
    val transactionType: String = "Transfer Bank",
    val transferAmount: Long = 0L,
    val originalAdminFee: Long = 0L,
    val storeAdminFee: Long = 2000L,
    val totalAmount: Long = 2000L,
    val senderName: String = "",
    val senderAccount: String = "",
    val receiverName: String = "",
    val receiverAccount: String = "",
    val receiverBank: String = "",
    val referenceNumber: String = "",
    val transactionDate: String = "",
    val transactionTime: String = "",
    val status: String = "BELUM DIVERIFIKASI",
    val notes: String = "",
    val template: ReceiptTemplate = ReceiptTemplate.COMPACT,
    val isReprint: Boolean = false,
    val printCount: Int = 0,
    val rawExtractedText: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculateTotal(): Long {
        return transferAmount + storeAdminFee
    }
}
