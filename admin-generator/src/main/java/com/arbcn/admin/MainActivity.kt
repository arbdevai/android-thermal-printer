package com.arbcn.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbcn.admin.core.LicenseSigner
import com.arbcn.admin.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(typography = Typography) {
                AdminAppScreen()
            }
        }
    }
}

@Composable
fun AdminAppScreen() {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: License, 1: Promo

    // State for License Generator
    var deviceIdInput by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("MONTHLY") }
    var generatedLicenseResult by remember { mutableStateOf("") }

    // State for Promo Generator
    var promoNameInput by remember { mutableStateOf("") }
    var promoDaysInput by remember { mutableStateOf("7") }
    var generatedPromoResult by remember { mutableStateOf("") }

    Scaffold(
        containerColor = LightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                            .background(AdminPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ARBCN Admin",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AdminPrimary
                        )
                        Text(
                            text = "License & Voucher Generator",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessContainer
                ) {
                    Text(
                        text = "ADMIN PRO",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text("Generate Lisensi", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Key, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdminPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = LightSurface,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("Generate Promo", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdminPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = LightSurface,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            if (activeTab == 0) {
                // TAB 0: LICENSE GENERATOR
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LightSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Aktivasi Pelanggan (Setelah Bayar DANA)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = deviceIdInput,
                            onValueChange = { deviceIdInput = it.uppercase() },
                            label = { Text("Device ID Pelanggan") },
                            placeholder = { Text("Contoh: A1B2C3D4E5F67890", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AdminPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        Text("Pilih Paket Langganan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                        val plans = listOf(
                            "WEEKLY" to "1 Minggu",
                            "MONTHLY" to "1 Bulan (15k)",
                            "PLUS3M" to "PLUS 3 Bulan (25k) ★",
                            "YEARLY" to "1 Tahun (99k)",
                            "LIFETIME" to "Lifetime (149k)"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            plans.forEach { (planKey, label) ->
                                val isSelected = selectedPlan == planKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPlan = planKey },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AdminContainer,
                                        selectedLabelColor = AdminPrimaryDark,
                                        containerColor = LightSurfaceSecondary,
                                        labelColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (deviceIdInput.isNotBlank()) {
                                    val days = when (selectedPlan) {
                                        "WEEKLY" -> 7
                                        "PLUS3M" -> 90
                                        "YEARLY" -> 365
                                        else -> 30
                                    }
                                    generatedLicenseResult = LicenseSigner.generateLicense(selectedPlan, deviceIdInput.trim(), days)
                                    Toast.makeText(context, "Lisensi berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Masukkan Device ID terlebih dahulu", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Kode Lisensi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                if (generatedLicenseResult.isNotBlank()) {
                    ResultBox(
                        title = "KODE LISENSI PELANGGAN",
                        code = generatedLicenseResult,
                        instructions = "Kirimkan kode ini ke pelanggan untuk ditempel di menu Pengaturan ➜ Langganan ARBCN Pro.",
                        context = context
                    )
                }
            } else {
                // TAB 1: PROMO GENERATOR
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LightSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Buat Voucher / Kode Promo Gratis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = promoNameInput,
                            onValueChange = { promoNameInput = it.uppercase() },
                            label = { Text("Nama Promo / Voucher") },
                            placeholder = { Text("Contoh: PROMO5K, MERDEKA, TRIAL", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AdminPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        OutlinedTextField(
                            value = promoDaysInput,
                            onValueChange = { promoDaysInput = it.filter(Char::isDigit) },
                            label = { Text("Durasi Gratis (Hari)") },
                            placeholder = { Text("Contoh: 3, 7, 14", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AdminPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        Button(
                            onClick = {
                                if (promoNameInput.isNotBlank()) {
                                    val days = promoDaysInput.toIntOrNull() ?: 7
                                    generatedPromoResult = LicenseSigner.generatePromo(promoNameInput.trim(), days)
                                    Toast.makeText(context, "Kode Promo dibuat!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Masukkan Nama Promo terlebih dahulu", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Kode Promo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                if (generatedPromoResult.isNotBlank()) {
                    ResultBox(
                        title = "KODE VOUCHER / PROMO",
                        code = generatedPromoResult,
                        instructions = "Bagikan kode ini di media sosial/grup. Dapat diklaim 1x per perangkat HP.",
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun ResultBox(
    title: String,
    code: String,
    instructions: String,
    context: Context
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AdminSubtle,
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AdminPrimaryDark)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = code,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Text(instructions, fontSize = 11.sp, color = TextSecondary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("ARBCN Code", code))
                        Toast.makeText(context, "Kode disalin!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Kode", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Halo! Berikut kode aktivasi ARBCN Pro Anda:\n\n$code\n\nBuka aplikasi ARBCN ➜ Pengaturan ➜ Langganan ➜ Masukkan kode di atas.")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Kirim Kode ke Pelanggan"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kirim WA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
