package com.thermalprinter.app.ui.screens

import android.bluetooth.BluetoothDevice
import android.net.Uri
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thermalprinter.app.domain.model.ReceiptTemplate
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.printer.PrinterConnectionStatus
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.theme.*

@Composable
fun SettingsScreen(
    settings: StoreSettings,
    printerStatus: PrinterConnectionStatus,
    subscriptionState: com.thermalprinter.app.subscription.SubscriptionManager.SubscriptionState,
    deviceId: String,
    pairedDevices: List<BluetoothDevice>,
    onSaveSettings: (StoreSettings) -> Unit,
    onPickLogo: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onOpenSubscriptionDialog: () -> Unit,
    onConnectPrinter: (String) -> Unit,
    onDisconnectPrinter: () -> Unit,
    onTestPrint: () -> Unit,
    onFeedPaper: () -> Unit,
    onCutPaper: () -> Unit
) {
    var editableSettings by remember(settings) { mutableStateOf(settings) }
    var selectedDeviceAddress by remember { mutableStateOf(settings.printerAddress) }
    var showDeviceDropdown by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickLogo(uri)
        }
    }

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
                    text = "Pengaturan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Identitas Toko, Logo & Printer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { onSaveSettings(editableSettings) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Logo Toko (NEW FEATURE)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Logo Toko / Outlet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Dicetak di bagian atas nota thermal 58mm",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Logo Preview if exists
                    if (settings.logoPath.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LightSurfaceSecondary)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(1.dp, BorderLight, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White
                            ) {
                                AsyncImage(
                                    model = settings.logoPath,
                                    contentDescription = "Preview Logo",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Logo Toko Terpasang",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Otomatis di-dither ke hitam-putih",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            IconButton(
                                onClick = onRemoveLogo,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ErrorContainer)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Logo", tint = ErrorRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { logoPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (settings.logoPath.isNotBlank()) "Ganti Logo" else "Unggah Logo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tampilkan Logo di Nota",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = editableSettings.showLogo,
                            onCheckedChange = { editableSettings = editableSettings.copy(showLogo = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange
                            )
                        )
                    }
                }
            }

            // Section 2: Bluetooth Printer
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Printer Bluetooth 58mm",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Protokol ESC/POS 32 karakter",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Connection Status
                    val (statusColor, statusBg, statusText) = when (printerStatus) {
                        is PrinterConnectionStatus.Connected -> Triple(SuccessGreen, SuccessContainer, "Terhubung: ${printerStatus.deviceName}")
                        is PrinterConnectionStatus.Connecting -> Triple(WarningAmber, WarningContainer, "Menghubungkan ke ${printerStatus.deviceName}...")
                        is PrinterConnectionStatus.Error -> Triple(ErrorRed, ErrorContainer, "Gagal: ${printerStatus.message}")
                        PrinterConnectionStatus.Disconnected -> Triple(TextSecondary, LightSurfaceSecondary, "Belum terhubung ke printer")
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Paired Device Picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showDeviceDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            val selectedName = pairedDevices.find { it.address == selectedDeviceAddress }?.name ?: selectedDeviceAddress
                            Text(
                                text = if (selectedDeviceAddress.isNotBlank()) "Printer: $selectedName" else "Pilih Printer Bluetooth",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryOrange)
                        }

                        DropdownMenu(
                            expanded = showDeviceDropdown,
                            onDismissRequest = { showDeviceDropdown = false },
                            modifier = Modifier.background(LightSurface)
                        ) {
                            if (pairedDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Tidak ada printer Bluetooth paired. Sambungkan printer di pengaturan HP terlebih dahulu.", fontSize = 12.sp, color = TextSecondary) },
                                    onClick = { showDeviceDropdown = false }
                                )
                            } else {
                                pairedDevices.forEach { device ->
                                    val devName = try { device.name ?: device.address } catch (_: Exception) { device.address }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(devName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                                Text(device.address, color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedDeviceAddress = device.address
                                            editableSettings = editableSettings.copy(
                                                printerAddress = device.address,
                                                printerName = devName
                                            )
                                            showDeviceDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (printerStatus is PrinterConnectionStatus.Connected) {
                            OutlinedButton(
                                onClick = onDisconnectPrinter,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                            ) {
                                Text("Putuskan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (selectedDeviceAddress.isNotBlank()) {
                                        onConnectPrinter(selectedDeviceAddress)
                                    }
                                },
                                enabled = selectedDeviceAddress.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Hubungkan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onTestPrint,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Uji Cetak", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onFeedPaper,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Feed Kertas", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onCutPaper,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Cut Kertas", fontSize = 11.sp)
                        }
                    }

                    // Copies Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jumlah Salinan Cetak (Copies)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (editableSettings.copies > 1) {
                                        editableSettings = editableSettings.copy(copies = editableSettings.copies - 1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = PrimaryOrange)
                            }
                            Text(
                                text = "${editableSettings.copies}x",
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            IconButton(
                                onClick = {
                                    if (editableSettings.copies < 5) {
                                        editableSettings = editableSettings.copy(copies = editableSettings.copies + 1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryOrange)
                            }
                        }
                    }
                }
            }

            // Section 3: Store Identity
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Identitas Toko / Loket",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = editableSettings.storeName,
                        onValueChange = { editableSettings = editableSettings.copy(storeName = it) },
                        label = { Text("Nama Toko / Usaha") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editableSettings.phone,
                        onValueChange = { editableSettings = editableSettings.copy(phone = it) },
                        label = { Text("Nomor WhatsApp / HP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editableSettings.address,
                        onValueChange = { editableSettings = editableSettings.copy(address = it) },
                        label = { Text("Alamat Toko (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editableSettings.ownerName,
                        onValueChange = { editableSettings = editableSettings.copy(ownerName = it) },
                        label = { Text("Nama Kasir / Pemilik") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editableSettings.footer,
                        onValueChange = { editableSettings = editableSettings.copy(footer = it) },
                        label = { Text("Pesan Footer Nota") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Section 4: Admin Fee
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Biaya Admin Toko",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = editableSettings.defaultAdminFee.toString(),
                        onValueChange = {
                            val fee = it.filter(Char::isDigit).toLongOrNull() ?: 0L
                            editableSettings = editableSettings.copy(defaultAdminFee = fee)
                        },
                        label = { Text("Biaya Admin Default (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tampilkan Biaya Admin di Nota",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = editableSettings.showAdminFee,
                            onCheckedChange = { editableSettings = editableSettings.copy(showAdminFee = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange
                            )
                        )
                    }

                    // Split vs Combined Admin Fee Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Pisahkan Admin Bank & Admin Toko",
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Jika aktif: Admin Bank & Toko dirinci terpisah. Jika mati: digabung jadi satu.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = editableSettings.splitAdminFee,
                            onCheckedChange = { editableSettings = editableSettings.copy(splitAdminFee = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange
                            )
                        )
                    }

                    // Custom Quick Fee Presets Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editableSettings.quickFeePresets,
                            onValueChange = { editableSettings = editableSettings.copy(quickFeePresets = it) },
                            label = { Text("Kustomisasi Pilihan Cepat Admin (Pisahkan Koma)") },
                            placeholder = { Text("Contoh: 0, 1000, 2000, 2500, 3000, 5000") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = modernTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Live preview of chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            editableSettings.getQuickFeeList().forEach { fee ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OrangeContainer
                                ) {
                                    Text(
                                        text = if (fee == 0L) "Gratis" else "Rp ${fee / 1000}k",
                                        color = PrimaryOrangeDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Format Nota
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Opsi Tampilan Nota",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    ToggleRowModern(
                        label = "Tampilkan Sumber / Bank",
                        checked = editableSettings.showSource,
                        onCheckedChange = { editableSettings = editableSettings.copy(showSource = it) }
                    )

                    ToggleRowModern(
                        label = "Tampilkan Nomor Referensi",
                        checked = editableSettings.showReference,
                        onCheckedChange = { editableSettings = editableSettings.copy(showReference = it) }
                    )
                }
            }

            // Section 6: Info Aplikasi & Changelog
            var showChangelogDialog by remember { mutableStateOf(false) }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Tentang ARBCN",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Versi 1.4.0 (Build 5) • Signature v1+v2+v3",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightSurfaceSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "APK Ditandatangani Konsisten (v1 + v2 + v3)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenSubscriptionDialog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (subscriptionState.isPro) "Kelola Langganan (${subscriptionState.plan})" else "Upgrade ARBCN Pro (Rp 15rb/bln)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showChangelogDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lihat Catatan Rilis / Changelog", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (showChangelogDialog) {
                com.thermalprinter.app.ui.components.ChangelogDialog(
                    onDismiss = { showChangelogDialog = false }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ToggleRowModern(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryOrange
            )
        )
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
