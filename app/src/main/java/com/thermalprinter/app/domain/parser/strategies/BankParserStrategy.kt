package com.thermalprinter.app.domain.parser.strategies

import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt

interface BankParserStrategy {
    val supportedBank: BankSource
    fun canHandle(text: String): Boolean
    fun parse(text: String, defaultStoreFee: Long, splitAdminFee: Boolean): TransactionReceipt
}
