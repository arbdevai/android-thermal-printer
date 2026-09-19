package com.thermalprinter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermalprinter.app.ui.theme.*

data class ReleaseLog(
    val version: String,
    val date: String,
    val isLatest: Boolean,
    val changes: List<String>
)

val APP_CHANGELOG = listOf(
    ReleaseLog(
        version = "v1.1.0",
        date = "September 2026",
        isLatest = true,
        changes = listOf(
            "Rebranding resmi aplikasi menjadi ARBCN",
            "Tema Baru Orange Light 2026 dengan teks High-Contrast Deep Slate yang tajam & jelas",
            "Floating Pill Dock Navbar: navigasi modern melayang menggantikan navbar kotak lama",
            "Fitur Upload & Cetak Logo Toko dari galeri dengan dithering monokrom presisi 58mm",
            "Hero Card & Carousel Banners interaktif pada layar Beranda",
            "Penandatanganan Konsisten APK (Signature Scheme v1 + v2 + v3)",
            "Menu Catatan Rilis & Changelog terintegrasi di aplikasi"
        )
    ),
    ReleaseLog(
        version = "v1.0.0",
        date = "September 2026",
        isLatest = false,
        changes = listOf(
            "Rilis perdana aplikasi pencetak struk thermal Bluetooth 58mm",
            "Offline OCR Google ML Kit & Parser 8 Bank/E-Wallet (BCA, BRImo, Livin, DANA, GoPay, OVO, ShopeePay, SeaBank)",
            "Driver ESC/POS dengan template Ringkas dan Detail Lengkap",
            "Penyimpanan riwayat transaksi lokal Room Database & ekspor CSV"
        )
    )
)

@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit
) {
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
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Catatan Rilis ARBCN",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Riwayat pembaruan & fitur baru",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                APP_CHANGELOG.forEach { release ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (release.isLatest) OrangeSubtle else LightSurfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (release.isLatest) PrimaryOrange.copy(alpha = 0.3f) else BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = release.version,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (release.isLatest) PrimaryOrangeDark else TextPrimary
                                    )
                                    if (release.isLatest) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PrimaryOrange
                                        ) {
                                            Text(
                                                text = "TERBARU",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = release.date,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                release.changes.forEach { change ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (release.isLatest) PrimaryOrange else TextSecondary,
                                            modifier = Modifier
                                                .size(15.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Text(
                                            text = change,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
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
