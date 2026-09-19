package com.thermalprinter.app.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.domain.model.ReceiptTemplate
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.printer.PrinterConnectionStatus
import com.thermalprinter.app.ui.components.GlassCard
import com.thermalprinter.app.ui.theme.*

@Composable
fun SettingsScreen(
    settings: StoreSettings,
    printerStatus: PrinterConnectionStatus,
    pairedDevices: List<BluetoothDevice>,
    onSaveSettings: (StoreSettings) -> Unit,
    onConnectPrinter: (String) -> Unit,
    onDisconnectPrinter: () -> Unit,
    onTestPrint: () -> Unit,
    onFeedPaper: () -> Unit,
    onCutPaper: () -> Unit
) {
    var editableSettings by remember(settings) { mutableStateOf(settings) }
    var selectedDeviceAddress by remember { mutableStateOf(settings.printerAddress) }
    var showDeviceDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolidBlack)
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Identitas Toko, Printer Bluetooth & Format",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { onSaveSettings(editableSettings) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simpan", fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Bluetooth Printer
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = AccentCyan)
                        Text(
                            text = "Printer Thermal Bluetooth 58mm",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    // Connection Status info
                    val statusText = when (printerStatus) {
                        is PrinterConnectionStatus.Connected -> "Terhubung: ${printerStatus.deviceName}"
                        is PrinterConnectionStatus.Connecting -> "Menghubungkan ke ${printerStatus.deviceName}..."
                        is PrinterConnectionStatus.Error -> "Gagal: ${printerStatus.message}"
                        PrinterConnectionStatus.Disconnected -> "Belum terhubung ke printer"
                    }
                    val statusColor = when (printerStatus) {
                        is PrinterConnectionStatus.Connected -> SuccessGreen
                        is PrinterConnectionStatus.Connecting -> WarningAmber
                        is PrinterConnectionStatus.Error -> ErrorRed
                        PrinterConnectionStatus.Disconnected -> TextMuted
                    }

                    Text(text = statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    // Paired Device Picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showDeviceDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            val selectedName = pairedDevices.find { it.address == selectedDeviceAddress }?.name ?: selectedDeviceAddress
                            Text(
                                text = if (selectedDeviceAddress.isNotBlank()) "Printer: $selectedName" else "Pilih Printer Bluetooth Terpasang",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showDeviceDropdown,
                            onDismissRequest = { showDeviceDropdown = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            if (pairedDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Tidak ada perangkat Bluetooth tersambung. Pasangkan printer di pengaturan HP terlebih dahulu.", fontSize = 12.sp, color = TextMuted) },
                                    onClick = { showDeviceDropdown = false }
                                )
                            } else {
                                pairedDevices.forEach { device ->
                                    val devName = try { device.name ?: device.address } catch (_: Exception) { device.address }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(devName, color = TextPrimary, fontWeight = FontWeight.Medium)
                                                Text(device.address, color = TextMuted, fontSize = 11.sp)
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                            ) {
                                Text("Putuskan", fontSize = 12.sp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("Hubungkan", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onTestPrint,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text("Uji Cetak", fontSize = 12.sp, color = TextPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onFeedPaper,
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Feed 3 Baris", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onCutPaper,
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Cut Kertas", fontSize = 11.sp)
                        }
                    }

                    // Copies Slider / Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jumlah Salinan Cetak",
                            color = TextSecondary,
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
                                Icon(Icons.Default.Remove, contentDescription = null, tint = TextPrimary)
                            }
                            Text(
                                text = "${editableSettings.copies}x",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(
                                onClick = {
                                    if (editableSettings.copies < 5) {
                                        editableSettings = editableSettings.copy(copies = editableSettings.copies + 1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Section 2: Store Identity
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = AccentBlue)
                        Text(
                            text = "Identitas Toko / Loket",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = editableSettings.storeName,
                        onValueChange = { editableSettings = editableSettings.copy(storeName = it) },
                        label = { Text("Nama Toko / Outlet") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = editableSettings.phone,
                        onValueChange = { editableSettings = editableSettings.copy(phone = it) },
                        label = { Text("Nomor WhatsApp / HP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = editableSettings.address,
                        onValueChange = { editableSettings = editableSettings.copy(address = it) },
                        label = { Text("Alamat Toko (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = editableSettings.ownerName,
                        onValueChange = { editableSettings = editableSettings.copy(ownerName = it) },
                        label = { Text("Nama Kasir / Pemilik") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = editableSettings.footer,
                        onValueChange = { editableSettings = editableSettings.copy(footer = it) },
                        label = { Text("Pesan Footer Nota") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )
                }
            }

            // Section 3: Admin Fee Defaults
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = SuccessGreen)
                        Text(
                            text = "Biaya Admin Standar",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = editableSettings.defaultAdminFee.toString(),
                        onValueChange = {
                            val fee = it.filter(Char::isDigit).toLongOrNull() ?: 0L
                            editableSettings = editableSettings.copy(defaultAdminFee = fee)
                        },
                        label = { Text("Biaya Admin Toko Default (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tampilkan Biaya Admin di Nota",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = editableSettings.showAdminFee,
                            onCheckedChange = { editableSettings = editableSettings.copy(showAdminFee = it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue)
                        )
                    }
                }
            }

            // Section 4: Format & Toggles
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = AccentPurple)
                        Text(
                            text = "Opsi Tampilan Nota",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    ToggleRow(
                        label = "Tampilkan Sumber / Bank",
                        checked = editableSettings.showSource,
                        onCheckedChange = { editableSettings = editableSettings.copy(showSource = it) }
                    )

                    ToggleRow(
                        label = "Tampilkan Nomor Referensi",
                        checked = editableSettings.showReference,
                        onCheckedChange = { editableSettings = editableSettings.copy(showReference = it) }
                    )
                }
            }

            // Section 5: About & Local Storage Privacy
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen)
                        Text(
                            text = "100% Offline & Privasi Terjaga",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Aplikasi ini memproses OCR dan mencetak nota langsung di perangkat Anda tanpa mengirim data transaksi ke internet atau server pihak ketiga mana pun.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue)
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = GlassBorder,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkPanel,
    unfocusedContainerColor = DarkPanel
)
