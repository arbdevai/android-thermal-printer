package com.thermalprinter.app.domain.model

data class TransactionReceipt(
    val id: Long = 0L,
    val source: BankSource = BankSource.OTHER,
    val transactionType: String = "Transfer Bank",
    val transferMethod: String = "", // e.g. BI-FAST, Realtime Online, Sesama Bank
    val transferAmount: Long = 0L,
    val originalAdminFee: Long = 0L, // Admin fee from bank/e-wallet source
    val storeAdminFee: Long = 2000L, // Admin fee charged by the store/counter
    val splitAdminFee: Boolean = true, // true: dipisah, false: digabung
    val totalAmount: Long = 2000L,
    val senderName: String = "",
    val senderAccount: String = "",
    val receiverName: String = "",
    val receiverAccount: String = "",
    val receiverBank: String = "",
    val referenceNumber: String = "",
    val transactionDate: String = "",
    val transactionTime: String = "",
    val status: String = "BERHASIL",
    val notes: String = "",
    val template: ReceiptTemplate = ReceiptTemplate.COMPACT,
    val isReprint: Boolean = false,
    val printCount: Int = 0,
    val rawExtractedText: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Total calculated amount:
     * Total = Transfer Amount + Store Admin Fee + Original Bank Fee (if applicable)
     */
    fun calculateTotal(): Long {
        return transferAmount + storeAdminFee + originalAdminFee
    }

    /**
     * Combined admin fee total (Store Admin Fee + Original Bank Fee)
     */
    fun totalAdminFee(): Long {
        return storeAdminFee + originalAdminFee
    }
}
