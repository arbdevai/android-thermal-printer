package com.thermalprinter.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.InvoiceItem
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.WhatsAppPhone
import com.thermalprinter.app.subscription.SubscriptionManager
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.components.InvoicePaperPreview
import com.thermalprinter.app.ui.components.ModernConfirmDialog
import com.thermalprinter.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CustomInvoiceScreen(
    settings: StoreSettings,
    subscriptionState: SubscriptionManager.SubscriptionState,
    onPrintInvoice: (CustomInvoice) -> Unit,
    onShareInvoice: (CustomInvoice, Boolean) -> Unit = { _, _ -> },
    onOpenSubscriptionDialog: () -> Unit,
    draftViewModel: InvoiceDraftViewModel = viewModel(),
    isBusy: Boolean = false
) {
    val state by draftViewModel.uiState.collectAsStateWithLifecycle()
    val invoiceSnapshot = remember(state) { state.toCustomInvoice() }

    val idLocale = remember { Locale("id", "ID") }
    fun formatRp(amount: Long): String {
        return "Rp ${NumberFormat.getNumberInstance(idLocale).format(amount)}"
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Form Input, 1: Pratinjau
    var showShareDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Dialog state for adding/editing items
    var itemBeingEdited by remember { mutableStateOf<InvoiceItem?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }
    var itemNameInput by remember { mutableStateOf("") }
    var itemPriceInput by remember { mutableStateOf("") }
    var itemQtyInput by remember { mutableStateOf("1") }
    var itemDialogError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                draftViewModel.onContactPicked(contactUri, context.contentResolver)
            }
        }
    }

    val formScrollState = rememberScrollState()
    val previewScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .imePadding()
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nota Toko & Bengkel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "${state.invoiceNumber} • ${state.invoiceDate}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryOrange
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // New / Reset Invoice button
                IconButton(
                    onClick = {
                        if (state.isDirty) {
                            showResetDialog = true
                        } else {
                            draftViewModel.resetDraft()
                        }
                    },
                    enabled = !isBusy
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Nota Baru",
                        tint = if (state.isDirty) PrimaryOrange else TextMuted
                    )
                }

                // Plus badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (subscriptionState.isPlus) SuccessContainer else OrangeContainer,
                    onClick = { if (!subscriptionState.isPlus) onOpenSubscriptionDialog() }
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
        }

        // Tabs: Form Input vs Pratinjau
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Form Input", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp)) },
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
                label = { Text("Pratinjau Nota", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(18.dp)) },
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

        // Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab == 0) {
                // Form Input Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(formScrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fitur Nota Toko & Bengkel (Plus)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Upgrade Paket PLUS untuk cetak struk custom dan kirim WhatsApp", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = PrimaryOrange)
                            }
                        }
                    }

                    // Customer & WhatsApp Info Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Identitas Pelanggan & WhatsApp",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            OutlinedTextField(
                                value = state.customerName,
                                onValueChange = { draftViewModel.setCustomerName(it) },
                                label = { Text("Nama Pelanggan") },
                                placeholder = { Text("Contoh: Pak Joko / Budi") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !isBusy
                            )

                            // WhatsApp Phone Field with Contact Picker
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = state.customerPhone,
                                    onValueChange = { draftViewModel.setCustomerPhone(it) },
                                    label = { Text("Nomor WhatsApp (Opsional)") },
                                    placeholder = { Text("Contoh: 08123456789 / +62812...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    enabled = !isBusy,
                                    isError = state.isPhoneInvalidNonEmpty,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(
                                                        Intent.ACTION_PICK,
                                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                                    )
                                                    contactPickerLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    draftViewModel.setContactError("Tidak dapat membuka kontak: ${e.message}")
                                                }
                                            },
                                            enabled = !isBusy && !state.isContactLoading
                                        ) {
                                            if (state.isContactLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = PrimaryOrange
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Contacts,
                                                    contentDescription = "Pilih Kontak",
                                                    tint = PrimaryOrange
                                                )
                                            }
                                        }
                                    }
                                )

                                // Inline Phone Validation & Error Feedback
                                if (state.contactPickerError != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = state.contactPickerError.orEmpty(),
                                            color = ErrorRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else if (state.customerPhone.isBlank()) {
                                    Text(
                                        text = "Opsional. Digunakan untuk kirim nota via WhatsApp.",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                } else if (state.isPhoneInvalidNonEmpty) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "Nomor WhatsApp tidak valid (format: 08xx / +62xx / 8-15 digit)",
                                            color = ErrorRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    val normalized = WhatsAppPhone.normalize(state.customerPhone)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "Valid WhatsApp: +$normalized",
                                            color = SuccessGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = state.vehiclePlateOrItemType,
                                onValueChange = { draftViewModel.setVehiclePlateOrItemType(it) },
                                label = { Text("Plat Kendaraan / Tipe Barang Servis") },
                                placeholder = { Text("Contoh: B 1234 ABC (Vario) / iPhone 13 LCD") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !isBusy
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
                                Column {
                                    Text(
                                        "Daftar Barang & Jasa Servis",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "${state.items.size}/${InvoiceDraftState.MAX_ITEMS_COUNT} item",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Button(
                                    onClick = {
                                        itemBeingEdited = null
                                        itemNameInput = ""
                                        itemPriceInput = ""
                                        itemQtyInput = "1"
                                        itemDialogError = null
                                        showItemDialog = true
                                    },
                                    enabled = !isBusy && state.items.size < InvoiceDraftState.MAX_ITEMS_COUNT,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tambah Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Start EMPTY items state
                            if (state.items.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LightSurfaceSecondary,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, null, tint = TextMuted, modifier = Modifier.size(28.dp))
                                        Text(
                                            "Belum ada barang/jasa ditambahkan",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary
                                        )
                                        Text(
                                            "Tekan '+ Tambah Item' untuk menambahkan rincian nota.",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                state.items.forEach { item ->
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
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    formatRp(item.subtotal()),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = PrimaryOrange
                                                )

                                                // Edit button
                                                IconButton(
                                                    onClick = {
                                                        itemBeingEdited = item
                                                        itemNameInput = item.name
                                                        itemPriceInput = item.price.toString()
                                                        itemQtyInput = item.quantity.toString()
                                                        itemDialogError = null
                                                        showItemDialog = true
                                                    },
                                                    enabled = !isBusy,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                                }

                                                // Delete button
                                                IconButton(
                                                    onClick = { draftViewModel.deleteItem(item.id) },
                                                    enabled = !isBusy,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Diskon input
                                OutlinedTextField(
                                    value = state.discountInput,
                                    onValueChange = { draftViewModel.setDiscountInput(it) },
                                    label = { Text("Diskon (Rp)") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = state.discountError != null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    enabled = !isBusy
                                )

                                // Uang Diterima input
                                OutlinedTextField(
                                    value = state.paidInput,
                                    onValueChange = { draftViewModel.setPaidInput(it) },
                                    label = { Text("Uang Bayar (Rp)") },
                                    placeholder = { Text("Uang Pas / 0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = state.paidError != null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    enabled = !isBusy
                                )
                            }

                            // Error messages for discount or paid
                            if (state.discountError != null) {
                                Text(
                                    text = state.discountError.orEmpty(),
                                    color = ErrorRed,
                                    fontSize = 11.sp
                                )
                            }
                            if (state.paidError != null) {
                                Text(
                                    text = state.paidError.orEmpty(),
                                    color = ErrorRed,
                                    fontSize = 11.sp
                                )
                            }

                            // Quick "Uang Pas" chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pintasan Nominal:", fontSize = 11.sp, color = TextMuted)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OrangeSubtle,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
                                    onClick = { draftViewModel.setPaidExactTotal() }
                                ) {
                                    Text(
                                        text = "Uang Pas (${formatRp(state.total)})",
                                        color = PrimaryOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Financial Summary Box (Distinct blank paid vs explicit 0 vs paid)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OrangeSubtle,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subtotal:", fontSize = 12.sp, color = TextSecondary)
                                        Text(formatRp(state.subtotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }

                                    if (state.effectiveDiscount > 0) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Diskon:", fontSize = 12.sp, color = TextSecondary)
                                            Text("-${formatRp(state.effectiveDiscount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                        }
                                    }

                                    Divider(Modifier.padding(vertical = 4.dp), color = PrimaryOrange.copy(alpha = 0.2f))

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("TOTAL BAYAR:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                        Text(formatRp(state.total), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryOrange)
                                    }

                                    // Blank paid=0 and outstanding distinct
                                    if (state.isPaidInputBlank) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Status Bayar:", fontSize = 12.sp, color = TextSecondary)
                                            Text("Belum Diisi (Sisa: ${formatRp(state.outstanding)})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WarningAmber)
                                        }
                                    } else {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Uang Diterima:", fontSize = 12.sp, color = TextSecondary)
                                            Text(formatRp(state.paidAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }

                                        if (state.change > 0) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Kembalian:", fontSize = 12.sp, color = TextSecondary)
                                                Text(formatRp(state.change), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                            }
                                        } else if (state.outstanding > 0) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Kurang / Sisa:", fontSize = 12.sp, color = TextSecondary)
                                                Text(formatRp(state.outstanding), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                            }
                                        }
                                    }
                                }
                            }

                            // Payment Method Row
                            Text("Metode Pembayaran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Tunai", "Transfer Bank", "QRIS").forEach { method ->
                                    val isSelected = state.paymentMethod == method
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) PrimaryOrange else LightSurfaceSecondary,
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        onClick = { draftViewModel.setPaymentMethod(method) }
                                    ) {
                                        Text(
                                            text = method,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Warranty / Notes
                            OutlinedTextField(
                                value = state.warrantyOrNotes,
                                onValueChange = { draftViewModel.setWarrantyOrNotes(it) },
                                label = { Text("Catatan / Garansi Struk") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isBusy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // Pratinjau Tab (Live Preview)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(previewScrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InvoicePaperPreview(
                        invoice = invoiceSnapshot,
                        settings = settings,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Bottom Action Bar: Bagikan & Cetak Nota
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
                // Share Button: blocked if phone is invalid non-empty, disabled while busy
                OutlinedButton(
                    onClick = {
                        if (!subscriptionState.isPlus) {
                            onOpenSubscriptionDialog()
                            return@OutlinedButton
                        }
                        if (state.isPhoneInvalidNonEmpty) {
                            // Non-empty invalid phone blocks share
                            return@OutlinedButton
                        }
                        showShareDialog = true
                    },
                    enabled = !isBusy && state.canShare,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.canShare) PrimaryOrange else BorderLight
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryOrange,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier.weight(0.4f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Bagikan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Print Button: invalid non-empty phone does NOT block print!
                Button(
                    onClick = {
                        if (!subscriptionState.isPlus) {
                            onOpenSubscriptionDialog()
                            return@Button
                        }
                        onPrintInvoice(invoiceSnapshot)
                    },
                    enabled = !isBusy && state.canPrint,
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (subscriptionState.isPlus) "CETAK NOTA" else "BUKA PLUS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Custom Share Dialog (Text vs HD PNG)
    if (showShareDialog) {
        Dialog(onDismissRequest = { showShareDialog = false }) {
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Bagikan Nota Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            if (WhatsAppPhone.isValid(state.customerPhone)) {
                                Text(
                                    "Penerima: +${WhatsAppPhone.normalize(state.customerPhone)}",
                                    fontSize = 12.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text("Penerima: Pilih di WhatsApp / Pemilih Aplikasi", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }

                    Divider(color = BorderSubtle)

                    // Option 1: WhatsApp Text Format
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LightSurfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showShareDialog = false
                            onShareInvoice(invoiceSnapshot, false) // false = text
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Chat, null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Teks Ringkasan (WhatsApp)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    if (WhatsAppPhone.isValid(state.customerPhone)) {
                                        "Kirim langsung ke nomor WhatsApp pelanggan."
                                    } else {
                                        "Pilih kontak di WhatsApp via pemilih aplikasi."
                                    },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
                        }
                    }

                    // Option 2: HD PNG Image Format
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LightSurfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showShareDialog = false
                            onShareInvoice(invoiceSnapshot, true) // true = PNG
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Image, null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gambar Struk HD (PNG)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    "Format gambar struk 58mm. Penerima dipilih di menu berbagi (chooser).",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
                        }
                    }

                    OutlinedButton(
                        onClick = { showShareDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Text("Batal", color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Add / Edit Item Dialog
    if (showItemDialog) {
        Dialog(onDismissRequest = { showItemDialog = false }) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (itemBeingEdited != null) "Edit Barang / Jasa" else "Tambah Barang / Jasa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = itemNameInput,
                        onValueChange = {
                            itemNameInput = it
                            itemDialogError = null
                        },
                        label = { Text("Nama Barang / Jasa") },
                        placeholder = { Text("Misal: Servis Rutin / Oli MPX") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = itemPriceInput,
                        onValueChange = {
                            itemPriceInput = it.filter(Char::isDigit)
                            itemDialogError = null
                        },
                        label = { Text("Harga Satuan (Rp)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = itemQtyInput,
                        onValueChange = {
                            itemQtyInput = it.filter(Char::isDigit)
                            itemDialogError = null
                        },
                        label = { Text("Jumlah (Qty: 1..10.000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Error in dialog
                    if (itemDialogError != null) {
                        Text(
                            text = itemDialogError.orEmpty(),
                            color = ErrorRed,
                            fontSize = 11.sp
                        )
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showItemDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                        ) {
                            Text("Batal", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val err = if (itemBeingEdited != null) {
                                    draftViewModel.updateItem(
                                        id = itemBeingEdited!!.id,
                                        name = itemNameInput,
                                        priceText = itemPriceInput,
                                        qtyText = itemQtyInput
                                    )
                                } else {
                                    draftViewModel.addItem(
                                        name = itemNameInput,
                                        priceText = itemPriceInput,
                                        qtyText = itemQtyInput
                                    )
                                }

                                if (err == null) {
                                    showItemDialog = false
                                } else {
                                    itemDialogError = err
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text(
                                text = if (itemBeingEdited != null) "Simpan" else "Tambah",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog (if dirty)
    if (showResetDialog) {
        ModernConfirmDialog(
            title = "Buat Nota Baru?",
            message = "Formulir saat ini memiliki data yang belum disimpan. Yakin ingin mengosongkan dan membuat nota baru?",
            confirmText = "Buat Baru",
            cancelText = "Batal",
            onConfirm = {
                showResetDialog = false
                draftViewModel.resetDraft()
            },
            onDismiss = { showResetDialog = false }
        )
    }
}
