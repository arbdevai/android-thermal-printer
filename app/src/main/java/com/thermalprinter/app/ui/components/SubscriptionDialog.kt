package com.thermalprinter.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.subscription.SubscriptionManager
import com.thermalprinter.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionDialog(
    subscriptionState: SubscriptionManager.SubscriptionState,
    deviceId: String,
    onActivateLicense: (String) -> Unit,
    onApplyPromo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var licenseInput by remember { mutableStateOf("") }
    var promoInput by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Info & Paket, 1: Aktivasi Lisensi, 2: Kode Promo

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LightSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
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
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Langganan ARBCN Pro",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (subscriptionState.isPro) "Status: PRO AKTIF (${subscriptionState.plan})" else "Status: Gratis (Kuota 7x/hari)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (subscriptionState.isPro) SuccessGreen else PrimaryOrange
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device ID Box (One-click copy)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LightSurfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Device ID Anda:", fontSize = 10.sp, color = TextSecondary)
                            Text(deviceId, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                                Toast.makeText(context, "Device ID disalin!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = PrimaryOrange)
                            Spacer(Modifier.width(4.dp))
                            Text("Salin", fontSize = 11.sp, color = PrimaryOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text("Paket Pro", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White,
                            containerColor = LightSurfaceSecondary,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text("Aktivasi", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White,
                            containerColor = LightSurfaceSecondary,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        label = { Text("Kode Promo", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White,
                            containerColor = LightSurfaceSecondary,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                when (activeTab) {
                    0 -> {
                        // Paket List & Cara Bayar DANA
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OrangeSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Pilihan Paket Langganan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryOrangeDark)
                                PlanRow("1 Bulan (Standard Pro)", "Rp 15.000", "Bebas cetak struk bank sepuasnya")
                                PlanRow("3 Bulan (PLUS Custom Nota) ★", "Rp 25.000", "Semua fitur Pro + Bebas buat Nota Toko/Bengkel/Jasa")
                                PlanRow("1 Tahun (Hemat)", "Rp 99.000", "Semua fitur Pro + Custom Nota 1 tahun")
                                PlanRow("Lifetime / Permanen", "Rp 149.000", "Akses penuh semua fitur selamanya")
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = InfoContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, InfoBlue.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Payment, null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                                    Text("Cara Pembayaran via DANA:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InfoBlue)
                                }
                                Text("1. Salin Device ID Anda di atas.", fontSize = 11.sp, color = TextPrimary)
                                Text("2. Transfer ke DANA Admin & kirim Device ID.", fontSize = 11.sp, color = TextPrimary)
                                Text("3. Admin akan mengirimkan Kode Lisensi aktivasi.", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                    1 -> {
                        // Input Kode Lisensi
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Masukkan Kode Lisensi dari Admin:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            OutlinedTextField(
                                value = licenseInput,
                                onValueChange = { licenseInput = it },
                                placeholder = { Text("ARBCN-MONTHLY-...", fontSize = 12.sp, color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                            Button(
                                onClick = {
                                    if (licenseInput.isNotBlank()) {
                                        onActivateLicense(licenseInput.trim())
                                        licenseInput = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Aktifkan Lisensi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        // Input Kode Promo
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Punya Kode Promo / Voucher?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            OutlinedTextField(
                                value = promoInput,
                                onValueChange = { promoInput = it },
                                placeholder = { Text("PROMO-ARBCN-...", fontSize = 12.sp, color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                            Button(
                                onClick = {
                                    if (promoInput.isNotBlank()) {
                                        onApplyPromo(promoInput.trim())
                                        promoInput = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Klaim Promo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
private fun PlanRow(title: String, price: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(price, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryOrange)
        }
        Text(desc, fontSize = 10.sp, color = TextSecondary)
    }
}
