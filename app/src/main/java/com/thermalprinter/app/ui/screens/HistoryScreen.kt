package com.thermalprinter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.TransactionReceipt
import com.thermalprinter.app.printer.ReceiptFormatter
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.theme.*

@Composable
fun HistoryScreen(
    receipts: List<TransactionReceipt>,
    onSelectReceipt: (TransactionReceipt) -> Unit,
    onReprintReceipt: (TransactionReceipt) -> Unit,
    onDeleteReceipt: (Long) -> Unit,
    onClearAllReceipts: () -> Unit,
    onExportHistory: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBankFilter by remember { mutableStateOf<BankSource?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var receiptToDelete by remember { mutableStateOf<TransactionReceipt?>(null) }

    val filteredReceipts = remember(receipts, searchQuery, selectedBankFilter) {
        receipts.filter { receipt ->
            val matchSearch = searchQuery.isBlank() ||
                    receipt.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                    receipt.receiverName.contains(searchQuery, ignoreCase = true) ||
                    receipt.senderName.contains(searchQuery, ignoreCase = true) ||
                    receipt.transactionType.contains(searchQuery, ignoreCase = true)

            val matchBank = selectedBankFilter == null || receipt.source == selectedBankFilter

            matchSearch && matchBank
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Riwayat Nota",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "${receipts.size} transaksi tersimpan lokal di HP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (receipts.isNotEmpty()) {
                    IconButton(
                        onClick = onExportHistory,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(OrangeContainer)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Ekspor", tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ErrorContainer)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Semua", tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nama, nomor ref, atau jenis...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = LightSurface,
                unfocusedContainerColor = LightSurface
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Bank Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedBankFilter == null,
                onClick = { selectedBankFilter = null },
                label = { Text("Semua", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryOrange,
                    selectedLabelColor = Color.White,
                    containerColor = LightSurface,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            BankSource.entries.filter { it != BankSource.OTHER }.forEach { bank ->
                val isSelected = selectedBankFilter == bank
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedBankFilter = if (isSelected) null else bank },
                    label = { Text(bank.name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(bank.brandColorHex),
                        selectedLabelColor = Color.White,
                        containerColor = LightSurface,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Receipts List
        if (filteredReceipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(OrangeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedBankFilter != null)
                            "Tidak ada transaksi yang cocok"
                        else
                            "Belum ada riwayat transaksi",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredReceipts, key = { it.id }) { receipt ->
                    HistoryReceiptItemModern(
                        receipt = receipt,
                        onClick = { onSelectReceipt(receipt) },
                        onReprint = { onReprintReceipt(receipt) },
                        onDelete = { receiptToDelete = receipt }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Clear All
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = LightSurface,
            title = { Text("Hapus Semua Riwayat?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Semua catatan riwayat transaksi yang tersimpan di HP akan dihapus secara permanen.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllReceipts()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Hapus Semua", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation Dialog for Single Delete
    receiptToDelete?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToDelete = null },
            containerColor = LightSurface,
            title = { Text("Hapus Nota Ini?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Hapus transaksi ${receipt.source.displayName} sebesar ${ReceiptFormatter.formatRupiah(receipt.calculateTotal())}?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReceipt(receipt.id)
                        receiptToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun HistoryReceiptItemModern(
    receipt: TransactionReceipt,
    onClick: () -> Unit,
    onReprint: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(receipt.source.brandColorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = receipt.source.name.take(3),
                            color = Color(receipt.source.brandColorHex),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }

                    Column {
                        Text(
                            text = receipt.receiverName.ifBlank { receipt.transactionType },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${receipt.transactionDate} ${receipt.transactionTime}".trim(),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ReceiptFormatter.formatRupiah(receipt.calculateTotal()),
                        color = PrimaryOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    if (receipt.storeAdminFee > 0) {
                        Text(
                            text = "+Adm ${ReceiptFormatter.formatRupiah(receipt.storeAdminFee)}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (receipt.referenceNumber.isNotBlank()) {
                Text(
                    text = "Ref: ${receipt.referenceNumber}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Divider(color = BorderSubtle)

            // Bottom Actions inside card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (receipt.isReprint) OrangeContainer else LightSurfaceSecondary
                ) {
                    Text(
                        text = if (receipt.isReprint) "Pernah Dicetak" else "Tersimpan",
                        color = if (receipt.isReprint) PrimaryOrangeDark else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onReprint,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(OrangeContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Cetak Ulang",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ErrorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
