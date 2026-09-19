package com.thermalprinter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.ReceiptTemplate
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import com.thermalprinter.app.printer.PrinterConnectionStatus
import com.thermalprinter.app.printer.ReceiptFormatter
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.components.ReceiptPaperPreview
import com.thermalprinter.app.ui.theme.*

@Composable
fun PreviewScreen(
    receipt: TransactionReceipt,
    settings: StoreSettings,
    printerStatus: PrinterConnectionStatus,
    onUpdateReceipt: (TransactionReceipt) -> Unit,
    onPrintReceipt: (TransactionReceipt) -> Unit,
    onSaveReceipt: (TransactionReceipt) -> Unit,
    onShareText: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var editableReceipt by remember(receipt) { mutableStateOf(receipt) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Preview, 1: Form Edit

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Pratinjau Nota 58mm",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "${editableReceipt.source.displayName} • ${editableReceipt.transactionType}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryOrange
                )
            }

            // Quick Share Button
            IconButton(
                onClick = {
                    val summaryText = buildString {
                        appendLine("=== ${settings.storeName} ===")
                        appendLine("Layanan: ${editableReceipt.source.displayName}")
                        appendLine("Nominal: ${ReceiptFormatter.formatRupiah(editableReceipt.transferAmount)}")
                        if (editableReceipt.storeAdminFee > 0) {
                            appendLine("Biaya Admin: ${ReceiptFormatter.formatRupiah(editableReceipt.storeAdminFee)}")
                        }
                        appendLine("Total: ${ReceiptFormatter.formatRupiah(editableReceipt.calculateTotal())}")
                        if (editableReceipt.receiverName.isNotBlank()) {
                            appendLine("Penerima: ${editableReceipt.receiverName}")
                        }
                        if (editableReceipt.referenceNumber.isNotBlank()) {
                            appendLine("No. Ref: ${editableReceipt.referenceNumber}")
                        }
                        appendLine("Waktu: ${editableReceipt.transactionDate} ${editableReceipt.transactionTime}")
                    }
                    onShareText(summaryText)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Bagikan",
                    tint = PrimaryOrange
                )
            }
        }

        // Tab Selector (Pratinjau Kertas vs Edit Data)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Pratinjau Nota Kertas", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryOrange,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = LightSurface,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Edit & Sesuaikan Data", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryOrange,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = LightSurface,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Main Scrollable Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Template & Source Selector Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilih Bank / Sumber",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    // Horizontal Bank Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BankSource.entries.forEach { bank ->
                            val isSelected = editableReceipt.source == bank
                            val bankColor = Color(bank.brandColorHex)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) bankColor else LightSurfaceSecondary,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.padding(vertical = 2.dp),
                                onClick = {
                                    editableReceipt = editableReceipt.copy(source = bank)
                                    onUpdateReceipt(editableReceipt)
                                }
                            ) {
                                Text(
                                    text = bank.name,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Divider(color = BorderSubtle)

                    // Template Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Format Template",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReceiptTemplate.entries.forEach { tpl ->
                                val isSelected = editableReceipt.template == tpl
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        editableReceipt = editableReceipt.copy(template = tpl)
                                        onUpdateReceipt(editableReceipt)
                                    },
                                    label = { Text(if (tpl == ReceiptTemplate.COMPACT) "Ringkas" else "Detail", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangeContainer,
                                        selectedLabelColor = PrimaryOrangeDark,
                                        containerColor = LightSurfaceSecondary,
                                        labelColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (activeTab == 0) {
                // Paper Preview
                ReceiptPaperPreview(
                    receipt = editableReceipt,
                    settings = settings
                )
            } else {
                // Form Edit Fields
                EditFieldsForm(
                    receipt = editableReceipt,
                    onReceiptChanged = {
                        editableReceipt = it
                        onUpdateReceipt(it)
                    }
                )
            }

            // Quick Admin Fee Override Bar
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Biaya Admin Toko Cepat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val feePresets = listOf(0L, 2000L, 3000L, 5000L)
                        feePresets.forEach { fee ->
                            val isSelected = editableReceipt.storeAdminFee == fee
                            Button(
                                onClick = {
                                    editableReceipt = editableReceipt.copy(storeAdminFee = fee)
                                    onUpdateReceipt(editableReceipt)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) PrimaryOrange else LightSurfaceSecondary,
                                    contentColor = if (isSelected) Color.White else TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (fee == 0L) "Gratis" else "Rp ${fee / 1000}k",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Bottom Fixed Print Action Bar
        Surface(
            color = LightSurface,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onSaveReceipt(editableReceipt) },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onPrintReceipt(editableReceipt) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CETAK NOTA",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EditFieldsForm(
    receipt: TransactionReceipt,
    onReceiptChanged: (TransactionReceipt) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Detail Transaksi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Nominal Transfer
            OutlinedTextField(
                value = receipt.transferAmount.toString(),
                onValueChange = {
                    val amount = it.filter(Char::isDigit).toLongOrNull() ?: 0L
                    onReceiptChanged(receipt.copy(transferAmount = amount))
                },
                label = { Text("Nominal Transfer (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = modernTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Admin Fee Toko
            OutlinedTextField(
                value = receipt.storeAdminFee.toString(),
                onValueChange = {
                    val fee = it.filter(Char::isDigit).toLongOrNull() ?: 0L
                    onReceiptChanged(receipt.copy(storeAdminFee = fee))
                },
                label = { Text("Biaya Admin Toko (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = modernTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Nama Penerima
            OutlinedTextField(
                value = receipt.receiverName,
                onValueChange = { onReceiptChanged(receipt.copy(receiverName = it)) },
                label = { Text("Nama Penerima") },
                modifier = Modifier.fillMaxWidth(),
                colors = modernTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Nama Pengirim
            OutlinedTextField(
                value = receipt.senderName,
                onValueChange = { onReceiptChanged(receipt.copy(senderName = it)) },
                label = { Text("Nama Pengirim") },
                modifier = Modifier.fillMaxWidth(),
                colors = modernTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Bank / Akun Penerima
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = receipt.receiverBank,
                    onValueChange = { onReceiptChanged(receipt.copy(receiverBank = it)) },
                    label = { Text("Bank Tujuan") },
                    modifier = Modifier.weight(1f),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = receipt.receiverAccount,
                    onValueChange = { onReceiptChanged(receipt.copy(receiverAccount = it)) },
                    label = { Text("No. Rek/HP") },
                    modifier = Modifier.weight(1f),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Nomor Referensi
            OutlinedTextField(
                value = receipt.referenceNumber,
                onValueChange = { onReceiptChanged(receipt.copy(referenceNumber = it)) },
                label = { Text("No. Referensi / ID Transaksi") },
                modifier = Modifier.fillMaxWidth(),
                colors = modernTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Tanggal & Waktu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = receipt.transactionDate,
                    onValueChange = { onReceiptChanged(receipt.copy(transactionDate = it)) },
                    label = { Text("Tanggal") },
                    modifier = Modifier.weight(1f),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = receipt.transactionTime,
                    onValueChange = { onReceiptChanged(receipt.copy(transactionTime = it)) },
                    label = { Text("Waktu") },
                    modifier = Modifier.weight(1f),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Catatan
            OutlinedTextField(
                value = receipt.notes,
                onValueChange = { onReceiptChanged(receipt.copy(notes = it)) },
                label = { Text("Catatan Tambahan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = modernTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun modernTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryOrange,
    unfocusedBorderColor = BorderLight,
    focusedLabelColor = PrimaryOrange,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = LightSurface,
    unfocusedContainerColor = LightSurface
)
