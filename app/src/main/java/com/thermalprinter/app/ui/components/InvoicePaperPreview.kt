package com.thermalprinter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.printer.InvoiceDocument
import com.thermalprinter.app.ui.theme.PaperBackground
import com.thermalprinter.app.ui.theme.PaperBorder
import com.thermalprinter.app.ui.theme.PaperText

/**
 * Jetpack Compose paper preview for 58mm thermal invoice.
 * Renders the single canonical document rows monospaced, including optional store logo.
 */
@Composable
fun InvoicePaperPreview(
    invoice: CustomInvoice,
    settings: StoreSettings,
    modifier: Modifier = Modifier
) {
    val lines = remember(invoice, settings) {
        InvoiceDocument.lines(invoice, settings)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(4.dp))
                .background(PaperBackground, RoundedCornerShape(4.dp))
                .border(1.dp, PaperBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional Coil Logo honoring showLogo
            if (settings.showLogo && settings.logoPath.isNotBlank()) {
                AsyncImage(
                    model = settings.logoPath,
                    contentDescription = "Logo Toko",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Monospaced Canonical Rows with scalable wrapping
            lines.forEach { line ->
                val isHighlight = line.contains("TOTAL") ||
                        line == settings.storeName ||
                        line.contains("NOTA INVOICE")

                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = PaperText,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
