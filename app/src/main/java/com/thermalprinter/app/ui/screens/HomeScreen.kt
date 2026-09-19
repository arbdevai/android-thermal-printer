package com.thermalprinter.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
    subscriptionState: com.thermalprinter.app.subscription.SubscriptionManager.SubscriptionState,
    onOpenSubscriptionDialog: () -> Unit,
    onNavigateToCustomInvoice: () -> Unit,
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
            .background(LightBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar / Brand Header
        item {
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryOrange, PrimaryOrangeDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ARBCN",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryOrange,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Thermal Printer Pro 2026",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                // Bluetooth status pill badge
                val (statusColor, statusBg, statusText) = when (printerStatus) {
                    is PrinterConnectionStatus.Connected -> Triple(SuccessGreen, SuccessContainer, "Terhubung")
                    is PrinterConnectionStatus.Connecting -> Triple(WarningAmber, WarningContainer, "Menghubungkan...")
                    is PrinterConnectionStatus.Error -> Triple(ErrorRed, ErrorContainer, "Error")
                    PrinterConnectionStatus.Disconnected -> Triple(TextSecondary, LightSurfaceSecondary, "Offline")
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                    onClick = onNavigateToSettings
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }
        }

        // Hero Card 2026 (Orange Gradient with dynamic stats)
        item {
            val totalTransactions = recentReceipts.size
            val totalNominal = recentReceipts.sumOf { it.transferAmount }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = PrimaryOrange.copy(alpha = 0.2f),
                        spotColor = PrimaryOrange.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryOrange, PrimaryOrangeDark)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = settings.storeName.ifBlank { "TOKO ANDA" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (settings.phone.isNotBlank()) "WA: ${settings.phone}" else "Siap Cetak Nota 58mm",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                onClick = onOpenSubscriptionDialog
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (subscriptionState.isPro) Icons.Default.Verified else Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (subscriptionState.isPro) "PRO: ${subscriptionState.plan}" else "Gratis (${subscriptionState.remaining}/7)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Hero Metrics Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Transaksi",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "$totalTransactions",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Terproses",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = ReceiptFormatter.formatRupiah(totalNominal),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Quota Indicator Card (Free tier warning/pro badge)
        if (!subscriptionState.isPro) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OrangeContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSubscriptionDialog
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = "Sisa Kuota Cetak Gratis: ${subscriptionState.remaining}/7 Hari Ini",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Direset tiap 00:00. Upgrade ke Pro (Rp 15rb/bln) via DANA",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = PrimaryOrange)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BannerCard(
                    title = "Bagi Langsung dari Bank",
                    subtitle = "Klik 'Bagikan' di BCA, DANA, BRImo langsung terisi otomatis.",
                    tag = "Auto OCR",
                    icon = Icons.Default.Share,
                    bgColor = OrangeContainer,
                    accentColor = PrimaryOrange
                )
                BannerCard(
                    title = "Logo & Struk Kustom",
                    subtitle = "Unggah logo toko & atur biaya admin per transaksi.",
                    tag = "58mm Presisi",
                    icon = Icons.Default.Image,
                    bgColor = InfoContainer,
                    accentColor = InfoBlue
                )
                BannerCard(
                    title = "100% Offline & Aman",
                    subtitle = "Tanpa server, riwayat tersimpan aman di HP Anda.",
                    tag = "Privasi Terjaga",
                    icon = Icons.Default.Shield,
                    bgColor = SuccessContainer,
                    accentColor = SuccessGreen
                )
            }
        }

        // Action Grid Rows
        item {
            Text(
                text = "Fitur & Aksi Cepat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionPillCard(
                    title = "Pindai Gambar",
                    subtitle = "Screenshot Bukti",
                    icon = Icons.Default.PhotoCamera,
                    iconBg = OrangeContainer,
                    iconColor = PrimaryOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
                ActionPillCard(
                    title = "Nota Bengkel / Toko",
                    subtitle = "Invoice Jasa (PLUS)",
                    icon = Icons.Default.PostAdd,
                    iconBg = SuccessContainer,
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCustomInvoice
                )
            }
        }

        // Second Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionPillCard(
                    title = "Input Teks",
                    subtitle = "Tempel / Manual",
                    icon = Icons.Default.EditNote,
                    iconBg = InfoContainer,
                    iconColor = InfoBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { showManualInputDialog = true }
                )
                ActionPillCard(
                    title = "Pengaturan",
                    subtitle = "Logo, Toko & Admin",
                    icon = Icons.Default.Settings,
                    iconBg = WarningContainer,
                    iconColor = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings
                )
            }
        }

        // Supported Bank/E-Wallet Badges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mendukung 8 Bank & E-Wallet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Auto-Detect",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryOrange
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supported.forEach { bank ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.shadow(1.dp, RoundedCornerShape(12.dp))
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
                                    .background(Color(bank.brandColorHex))
                            )
                            Text(
                                text = bank.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (recentReceipts.isNotEmpty()) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text(text = "Lihat Semua", color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (recentReceipts.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            text = "Belum Ada Riwayat Transaksi",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Bagikan bukti dari BCA, DANA, dll. atau gunakan tombol di atas.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(recentReceipts.take(5)) { receipt ->
                ModernReceiptItem(
                    receipt = receipt,
                    onClick = { onSelectReceipt(receipt) }
                )
            }
        }
    }

    // Manual Input Dialog (Modern 2026 Custom Dialog)
    if (showManualInputDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showManualInputDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Input Teks Transaksi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tempel pesan SMS banking / bukti transfer",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = {
                            Text(
                                text = "Contoh:\nTransfer Berhasil\nNominal: Rp 50.000\nKe: Budi Santoso\nRef: 20260919123456",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = LightSurfaceSecondary,
                            unfocusedContainerColor = LightSurfaceSecondary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showManualInputDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (manualText.isNotBlank()) {
                                    onManualInputText(manualText)
                                    showManualInputDialog = false
                                    manualText = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text("Proses Nota", color = TextOnOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    bgColor: Color,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ActionPillCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ModernReceiptItem(
    receipt: TransactionReceipt,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
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
                        text = "${receipt.source.displayName} • ${receipt.transactionDate} ${receipt.transactionTime}".trim(),
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
                    fontSize = 14.sp
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
    }
}
