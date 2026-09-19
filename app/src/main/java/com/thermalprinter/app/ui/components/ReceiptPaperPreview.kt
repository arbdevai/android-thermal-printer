package com.thermalprinter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.domain.model.ReceiptTemplate
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import com.thermalprinter.app.printer.ReceiptFormatter
import com.thermalprinter.app.ui.theme.*

@Composable
fun ReceiptPaperPreview(
    receipt: TransactionReceipt,
    settings: StoreSettings,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(4.dp))
                .background(PaperBackground, RoundedCornerShape(4.dp))
                .border(1.dp, PaperBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = settings.storeName.ifBlank { "BUKTI TRANSAKSI" },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PaperText,
                textAlign = TextAlign.Center
            )

            if (settings.phone.isNotBlank()) {
                Text(
                    text = "WA/Telp: ${settings.phone}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PaperTextMuted,
                    textAlign = TextAlign.Center
                )
            }

            if (settings.address.isNotBlank() && receipt.template == ReceiptTemplate.DETAILED) {
                Text(
                    text = settings.address,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PaperTextMuted,
                    textAlign = TextAlign.Center
                )
            }

            if (receipt.isReprint) {
                Text(
                    text = "*** SALINAN / DUPLIKAT ***",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PaperText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Body content depending on template
            when (receipt.template) {
                ReceiptTemplate.COMPACT -> CompactContent(receipt, settings)
                ReceiptTemplate.DETAILED -> DetailedContent(receipt, settings)
            }

            Spacer(modifier = Modifier.height(8.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            if (settings.footer.isNotBlank()) {
                Text(
                    text = settings.footer,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PaperTextMuted,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = "~ Terima Kasih ~",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = PaperTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun CompactContent(receipt: TransactionReceipt, settings: StoreSettings) {
    if (settings.showSource) {
        ReceiptRow(label = "Sumber", value = receipt.source.displayName.take(18))
    }
    ReceiptRow(label = "Jenis", value = receipt.transactionType.take(18))
    if (receipt.receiverName.isNotBlank()) {
        ReceiptRow(label = "Penerima", value = receipt.receiverName.take(18))
    }

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    ReceiptRow(label = "Nominal", value = ReceiptFormatter.formatRupiah(receipt.transferAmount))
    if (settings.showAdminFee && receipt.storeAdminFee > 0) {
        ReceiptRow(label = "Biaya Admin", value = ReceiptFormatter.formatRupiah(receipt.storeAdminFee))
    }

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    // Total highlight
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TOTAL",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PaperText
        )
        Text(
            text = ReceiptFormatter.formatRupiah(receipt.calculateTotal()),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PaperText
        )
    }

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    if (settings.showReference && receipt.referenceNumber.isNotBlank()) {
        ReceiptRow(label = "No. Ref", value = receipt.referenceNumber.take(18))
    }
    val dateTime = "${receipt.transactionDate} ${receipt.transactionTime}".trim()
    if (dateTime.isNotBlank()) {
        ReceiptRow(label = "Waktu", value = dateTime)
    }
}

@Composable
private fun DetailedContent(receipt: TransactionReceipt, settings: StoreSettings) {
    ReceiptRow(label = "Status", value = receipt.status)
    if (settings.showSource) {
        ReceiptRow(label = "Layanan", value = receipt.source.displayName.take(18))
    }
    ReceiptRow(label = "Transaksi", value = receipt.transactionType.take(18))

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    if (receipt.senderName.isNotBlank()) {
        ReceiptRow(label = "Pengirim", value = receipt.senderName.take(18))
    }
    if (receipt.receiverName.isNotBlank()) {
        ReceiptRow(label = "Penerima", value = receipt.receiverName.take(18))
    }
    if (receipt.receiverBank.isNotBlank()) {
        ReceiptRow(label = "Bank Tujuan", value = receipt.receiverBank.take(18))
    }
    if (receipt.receiverAccount.isNotBlank()) {
        ReceiptRow(label = "No. Rek/HP", value = receipt.receiverAccount.take(18))
    }

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    ReceiptRow(label = "Nominal", value = ReceiptFormatter.formatRupiah(receipt.transferAmount))
    if (settings.showAdminFee) {
        ReceiptRow(label = "Admin Toko", value = ReceiptFormatter.formatRupiah(receipt.storeAdminFee))
    }

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TOTAL BAYAR",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = PaperText
        )
        Text(
            text = ReceiptFormatter.formatRupiah(receipt.calculateTotal()),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = PaperText
        )
    }

    Spacer(modifier = Modifier.height(6.dp))
    ReceiptDashedDivider()
    Spacer(modifier = Modifier.height(6.dp))

    if (settings.showReference && receipt.referenceNumber.isNotBlank()) {
        ReceiptRow(label = "No. Ref", value = receipt.referenceNumber.take(18))
    }
    if (receipt.transactionDate.isNotBlank()) {
        ReceiptRow(label = "Tanggal", value = receipt.transactionDate)
    }
    if (receipt.transactionTime.isNotBlank()) {
        ReceiptRow(label = "Jam", value = receipt.transactionTime)
    }
    if (receipt.notes.isNotBlank()) {
        ReceiptRow(label = "Catatan", value = receipt.notes.take(18))
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = PaperTextMuted
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = PaperText,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ReceiptDashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - -",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = PaperDivider,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
