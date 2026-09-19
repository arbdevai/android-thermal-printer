package com.thermalprinter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.InvoiceItem
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.subscription.SubscriptionManager
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.components.ModernConfirmDialog
import com.thermalprinter.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomInvoiceScreen(
    settings: StoreSettings,
    subscriptionState: SubscriptionManager.SubscriptionState,
    onPrintInvoice: (CustomInvoice) -> Unit,
    onOpenSubscriptionDialog: () -> Unit
) {
    val idLocale = Locale("id", "ID")
    fun formatRp(amount: Long) = "Rp ${NumberFormat.getNumberInstance(idLocale).format(amount)}"

    var customerName by remember { mutableStateOf("") }
    var vehicleOrUnit by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var discountInput by remember { mutableStateOf("") }
    var paidInput by remember { mutableStateOf("") }
    var warrantyNotes by remember { mutableStateOf("Garansi servis 7 hari. Terima kasih atas kunjungan Anda.") }

    var itemsList by remember {
        mutableStateOf(
            listOf(
                InvoiceItem(name = "Jasa Servis Ringan", price = 35000L, quantity = 1),
                InvoiceItem(name = "Oli Mesin MPX1 0.8L", price = 55000L, quantity = 1)
            )
        )
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }

    val subtotal = itemsList.sumOf { it.subtotal() }
    val discount = discountInput.toLongOrNull() ?: 0L
    val total = (subtotal - discount).coerceAtLeast(0L)
    val paid = paidInput.toLongOrNull() ?: total
    val change = (paid - total).coerceAtLeast(0L)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nota Toko & Bengkel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Cetak Invoice Jasa & Penjualan 58mm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryOrange
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (subscriptionState.isPlus) SuccessContainer else OrangeContainer
            ) {
                Text(
                    text = if (subscriptionState.isPlus) "PLUS AKTIF" else "FITUR PLUS",
                    color = if (subscriptionState.isPlus) SuccessGreen else PrimaryOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Lock Banner if user is not in PLUS package
            if (!subscriptionState.isPlus) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OrangeContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSubscriptionDialog
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Buka Fitur Nota Toko & Bengkel", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("Upgrade ke Paket PLUS 3 Bulan (Rp 25.000) via DANA", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = PrimaryOrange)
                    }
                }
            }

            // Customer & Vehicle/Item Info Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Identitas Pelanggan / Unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nama Pelanggan") },
                        placeholder = { Text("Contoh: Pak Joko / Budi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = vehicleOrUnit,
                        onValueChange = { vehicleOrUnit = it },
                        label = { Text("Plat Kendaraan / Tipe Barang Service") },
                        placeholder = { Text("Contoh: B 1234 ABC (Vario) / iPhone 11") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Dynamic Items & Services Table Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daftar Barang & Jasa Servis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Button(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (itemsList.isEmpty()) {
                        Text("Belum ada barang/jasa ditambahkan.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        itemsList.forEachIndexed { index, item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LightSurfaceSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text("${item.quantity}x @ ${formatRp(item.price)}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(formatRp(item.subtotal()), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PrimaryOrange)
                                        IconButton(
                                            onClick = {
                                                itemsList = itemsList.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Payment & Totals Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Perhitungan Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it.filter(Char::isDigit) },
                            label = { Text("Diskon (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = paidInput,
                            onValueChange = { paidInput = it.filter(Char::isDigit) },
                            label = { Text("Uang Diterima (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Financial Summary Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OrangeSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 12.sp, color = TextSecondary)
                                Text(formatRp(subtotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            if (discount > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Diskon:", fontSize = 12.sp, color = TextSecondary)
                                    Text("-${formatRp(discount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                }
                            }
                            Divider(Modifier.padding(vertical = 4.dp), color = PrimaryOrange.copy(alpha = 0.2f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL BAYAR:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                Text(formatRp(total), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryOrange)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembalian:", fontSize = 12.sp, color = TextSecondary)
                                Text(formatRp(change), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = warrantyNotes,
                        onValueChange = { warrantyNotes = it },
                        label = { Text("Catatan / Garansi Struk") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Bottom Action Bar
        Surface(
            color = LightSurface,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (!subscriptionState.isPlus) {
                            onOpenSubscriptionDialog()
                            return@Button
                        }
                        val now = System.currentTimeMillis()
                        val sdfDate = SimpleDateFormat("dd/MM/yyyy", idLocale)
                        val sdfTime = SimpleDateFormat("HH:mm", idLocale)

                        val invoice = CustomInvoice(
                            invoiceNumber = "INV-${now.toString().takeLast(6)}",
                            invoiceDate = sdfDate.format(Date(now)),
                            invoiceTime = sdfTime.format(Date(now)),
                            customerName = customerName,
                            vehiclePlateOrItemType = vehicleOrUnit,
                            items = itemsList,
                            discountAmount = discount,
                            paidAmount = if (paidInput.isBlank()) total else paid,
                            paymentMethod = paymentMethod,
                            warrantyOrNotes = warrantyNotes
                        )
                        onPrintInvoice(invoice)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Print, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (subscriptionState.isPlus) "CETAK NOTA INVOICE 58MM" else "BUKA FITUR DENGAN PAKET PLUS (25K)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddItemDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Tambah Barang / Jasa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Nama Barang / Jasa") },
                        placeholder = { Text("Misal: Ganti Kampas Rem") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it.filter(Char::isDigit) },
                        label = { Text("Harga Satuan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it.filter(Char::isDigit) },
                        label = { Text("Jumlah (Qty)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddItemDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                val price = newItemPrice.toLongOrNull() ?: 0L
                                val qty = (newItemQty.toIntOrNull() ?: 1).coerceAtLeast(1)
                                if (newItemName.isNotBlank() && price > 0) {
                                    itemsList = itemsList + InvoiceItem(name = newItemName.trim(), price = price, quantity = qty)
                                    showAddItemDialog = false
                                    newItemName = ""
                                    newItemPrice = ""
                                    newItemQty = "1"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text("Tambah", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
