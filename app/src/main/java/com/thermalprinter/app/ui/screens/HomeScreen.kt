package com.thermalprinter.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.domain.model.BankSource
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import com.thermalprinter.app.printer.PrinterConnectionStatus
import com.thermalprinter.app.printer.ReceiptFormatter
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.theme.*

@Composable
fun HomeScreen(
    settings: StoreSettings,
    recentReceipts: List<TransactionReceipt>,
    printerStatus: PrinterConnectionStatus,
    onImageSelected: (Uri) -> Unit,
    onManualInputText: (String) -> Unit,
    onSelectReceipt: (TransactionReceipt) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onQuickTestPrint: () -> Unit
) {
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = settings.storeName.ifBlank { "Thermal Printer" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Bukti Transaksi Bank & E-Wallet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Bluetooth status badge
                val (statusColor, statusText) = when (printerStatus) {
                    is PrinterConnectionStatus.Connected -> SuccessGreen to "Terhubung"
                    is PrinterConnectionStatus.Connecting -> WarningAmber to "Menghubungkan..."
                    is PrinterConnectionStatus.Error -> ErrorRed to "Error"
                    PrinterConnectionStatus.Disconnected -> TextMuted to "Offline"
                }

                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = DarkPanel,
                    onClick = onNavigateToSettings
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkPanel
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val totalTransactions = recentReceipts.size
                    val totalNominal = recentReceipts.sumOf { it.transferAmount }

                    StatItem(
                        label = "Total Transaksi",
                        value = "$totalTransactions",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = AccentBlue
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = GlassBorder
                    )
                    StatItem(
                        label = "Total Terproses",
                        value = ReceiptFormatter.formatRupiah(totalNominal),
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = SuccessGreen
                    )
                }
            }
        }

        // Quick Action Buttons Grid
        item {
            Text(
                text = "Aksi Cepat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionTile(
                    title = "Scan / Galeri",
                    subtitle = "Unggah Gambar Bukti",
                    icon = Icons.Default.Image,
                    iconTint = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
                ActionTile(
                    title = "Input Teks",
                    subtitle = "Tempel / Tulis Manual",
                    icon = Icons.Default.EditNote,
                    iconTint = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { showManualInputDialog = true }
                )
            }
        }

        // Second Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionTile(
                    title = "Uji Printer",
                    subtitle = "Cetak Nota Tes 58mm",
                    icon = Icons.Default.Print,
                    iconTint = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onQuickTestPrint
                )
                ActionTile(
                    title = "Pengaturan",
                    subtitle = "Toko, Admin & Printer",
                    icon = Icons.Default.Settings,
                    iconTint = AccentPurple,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings
                )
            }
        }

        // Supported Bank/E-Wallet Horizontal Badges
        item {
            Text(
                text = "Mendukung 8 Bank & E-Wallet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val supported = listOf(
                    BankSource.BCA,
                    BankSource.BRIMO,
                    BankSource.LIVIN,
                    BankSource.DANA,
                    BankSource.GOPAY,
                    BankSource.OVO,
                    BankSource.SHOPEEPAY,
                    BankSource.SEABANK
                )
                supported.take(4).forEach { bank ->
                    BankBadgeItem(bank = bank)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val supported = listOf(
                    BankSource.BCA,
                    BankSource.BRIMO,
                    BankSource.LIVIN,
                    BankSource.DANA,
                    BankSource.GOPAY,
                    BankSource.OVO,
                    BankSource.SHOPEEPAY,
                    BankSource.SEABANK
                )
                supported.drop(4).forEach { bank ->
                    BankBadgeItem(bank = bank)
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (recentReceipts.isNotEmpty()) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text(text = "Lihat Semua", color = AccentBlue, fontSize = 13.sp)
                    }
                }
            }
        }

        // Recent Transactions List
        if (recentReceipts.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Riwayat Transaksi",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Gunakan tombol Bagikan dari BCA, DANA, dll. atau pilih menu di atas.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(recentReceipts.take(5)) { receipt ->
                RecentReceiptCard(
                    receipt = receipt,
                    onClick = { onSelectReceipt(receipt) }
                )
            }
        }
    }

    // Manual Input Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(text = "Input / Tempel Teks Transaksi", color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    placeholder = {
                        Text(
                            text = "Contoh: Transfer Berhasil\nNominal: Rp 50.000\nKe: Budi Santoso\nRef: 20240919123456",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DarkPanel,
                        unfocusedContainerColor = DarkPanel
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualText.isNotBlank()) {
                            onManualInputText(manualText)
                            showManualInputDialog = false
                            manualText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Proses Nota", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BankBadgeItem(bank: BankSource) {
    GlassCard(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = DarkPanel
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bank.name,
                color = Color(bank.brandColorHex),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RecentReceiptCard(
    receipt: TransactionReceipt,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(receipt.source.brandColorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = receipt.source.name.take(3),
                        color = Color(receipt.source.brandColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Column {
                    Text(
                        text = receipt.receiverName.ifBlank { receipt.transactionType },
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${receipt.transactionDate} ${receipt.transactionTime}".trim(),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ReceiptFormatter.formatRupiah(receipt.calculateTotal()),
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (receipt.storeAdminFee > 0) {
                    Text(
                        text = "+Adm ${ReceiptFormatter.formatRupiah(receipt.storeAdminFee)}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
