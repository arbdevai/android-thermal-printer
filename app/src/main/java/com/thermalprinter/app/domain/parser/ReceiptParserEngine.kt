package com.thermalprinter.app.domain.parser

import com.thermalprinter.app.domain.model.TransactionReceipt
import com.thermalprinter.app.domain.parser.strategies.*

/**
 * High-precision Orchestrator for Indonesian Banks & E-Wallets.
 * Uses dedicated strategy parsers for BCA, BRImo, Livin Mandiri, DANA, GoPay, OVO, ShopeePay, SeaBank,
 * and a robust fallback parser.
 */
object ReceiptParserEngine {
    private val strategies: List<BankParserStrategy> = listOf(
        BcaParser(),
        BrimoParser(),
        LivinParser(),
        DanaParser(),
        GopayParser(),
        OvoParser(),
        ShopeePayParser(),
        SeabankParser()
    )

    private val fallback = FallbackGenericParser()

    fun parse(
        text: String,
        defaultStoreFee: Long = 2000L,
        splitAdminFee: Boolean = true
    ): TransactionReceipt {
        val strategy = strategies.firstOrNull { it.canHandle(text) } ?: fallback
        return strategy.parse(text, defaultStoreFee, splitAdminFee)
    }

    fun parseAmount(raw: String): Long? {
        return ParserUtils.parseAmount(raw)
    }
}
