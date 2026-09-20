package com.thermalprinter.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import com.thermalprinter.app.domain.parser.ReceiptParserEngine
import com.thermalprinter.app.ocr.OcrManager
import com.thermalprinter.app.printer.ReceiptFormatter
import com.thermalprinter.app.subscription.SubscriptionManager
import com.thermalprinter.app.ui.components.FloatingNavBar
import com.thermalprinter.app.ui.components.SubscriptionDialog
import com.thermalprinter.app.ui.navigation.NavRoute
import com.thermalprinter.app.ui.screens.HistoryScreen
import com.thermalprinter.app.ui.screens.HomeScreen
import com.thermalprinter.app.ui.screens.PreviewScreen
import com.thermalprinter.app.ui.screens.SettingsScreen
import com.thermalprinter.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val appData by lazy { ThermalPrinterApp.instance.appData }
    private val printerManager by lazy { ThermalPrinterApp.instance.printerManager }
    private val ocrManager by lazy { OcrManager(this) }
    private val subscriptionManager by lazy { ThermalPrinterApp.instance.subscriptionManager }

    private val currentReceiptState = mutableStateOf(TransactionReceipt())
    private val isOcrProcessing = mutableStateOf(false)

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Izin Bluetooth diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin Bluetooth diperlukan untuk mencetak", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBluetoothPermissions()
        handleIncomingIntent(intent)

        setContent {
            AndroidThermalPrinterTheme {
                val navController = rememberNavController()
                val settings by appData.settings.collectAsStateWithLifecycle(initialValue = StoreSettings())
                val receipts by appData.receipts.collectAsStateWithLifecycle(initialValue = emptyList())
                val printerStatus by printerManager.connectionStatus.collectAsStateWithLifecycle()
                val subscriptionState by subscriptionManager.subscriptionState.collectAsStateWithLifecycle(
                    initialValue = SubscriptionManager.SubscriptionState()
                )

                var showSubscriptionDialog by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = LightBackground,
                    bottomBar = {
                        FloatingNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    if (route == NavRoute.Home.route) {
                                        popUpTo(NavRoute.Home.route) { inclusive = true }
                                    } else {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LightBackground)
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = NavRoute.Home.route
                        ) {
                            composable(NavRoute.Home.route) {
                                HomeScreen(
                                    settings = settings,
                                    recentReceipts = receipts,
                                    printerStatus = printerStatus,
                                    subscriptionState = subscriptionState,
                                    onOpenSubscriptionDialog = { showSubscriptionDialog = true },
                                    onNavigateToCustomInvoice = { navController.navigate(NavRoute.CustomInvoice.route) },
                                    onImageSelected = { uri ->
                                        processImageUri(uri) {
                                            navController.navigate(NavRoute.Preview.route)
                                        }
                                    },
                                    onManualInputText = { text ->
                                        processText(text, settings.defaultAdminFee, settings.splitAdminFee)
                                        navController.navigate(NavRoute.Preview.route)
                                    },
                                    onSelectReceipt = { receipt ->
                                        currentReceiptState.value = receipt
                                        navController.navigate(NavRoute.Preview.route)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(NavRoute.Settings.route)
                                    },
                                    onNavigateToHistory = {
                                        navController.navigate(NavRoute.History.route)
                                    },
                                    onQuickTestPrint = {
                                        scope.launch {
                                            val res = printerManager.printTestReceipt(settings.storeName)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Uji cetak terkirim ke printer")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Gagal cetak")
                                            }
                                        }
                                    }
                                )
                            }

                            composable(NavRoute.Preview.route) {
                                PreviewScreen(
                                    receipt = currentReceiptState.value,
                                    settings = settings,
                                    printerStatus = printerStatus,
                                    onUpdateReceipt = { updated ->
                                        currentReceiptState.value = updated
                                    },
                                    onPrintReceipt = { toPrint ->
                                        scope.launch {
                                            // Enforce daily quota or Pro license check
                                            if (!subscriptionManager.canGenerateReceipt()) {
                                                showSubscriptionDialog = true
                                                snackbarHostState.showSnackbar(
                                                    "Kuota cetak gratis hari ini habis (7/7). Direset besok atau upgrade ke Pro via DANA."
                                                )
                                                return@launch
                                            }

                                            val logoBitmap = if (settings.showLogo && settings.logoPath.isNotBlank()) {
                                                appData.loadLogoBitmap(settings.logoPath)
                                            } else null

                                            val bytes = ReceiptFormatter.buildEscPos(toPrint, settings, logoBitmap)
                                            val printResult = printerManager.printBytes(bytes)
                                            if (printResult.isSuccess) {
                                                subscriptionManager.consumeReceipt()
                                                appData.save(toPrint.copy(isReprint = true, printCount = toPrint.printCount + 1))
                                                snackbarHostState.showSnackbar("Nota berhasil dicetak!")
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    "Gagal mencetak: ${printResult.exceptionOrNull()?.message}"
                                                )
                                            }
                                        }
                                    },
                                    onSaveReceipt = { toSave ->
                                        scope.launch {
                                            appData.save(toSave)
                                            snackbarHostState.showSnackbar("Transaksi disimpan ke riwayat!")
                                        }
                                    },
                                    onShareText = { summary ->
                                        sharePlainText(summary)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(NavRoute.Settings.route)
                                    }
                                )
                            }

                            composable(NavRoute.CustomInvoice.route) {
                                val invoiceDraftViewModel: com.thermalprinter.app.ui.screens.InvoiceDraftViewModel =
                                    androidx.lifecycle.viewmodel.compose.viewModel()
                                var isInvoiceBusy by remember { mutableStateOf(false) }

                                com.thermalprinter.app.ui.screens.CustomInvoiceScreen(
                                    settings = settings,
                                    subscriptionState = subscriptionState,
                                    draftViewModel = invoiceDraftViewModel,
                                    isBusy = isInvoiceBusy,
                                    onPrintInvoice = { invoice ->
                                        scope.launch {
                                            isInvoiceBusy = true
                                            try {
                                                val logoBitmap = if (settings.showLogo && settings.logoPath.isNotBlank()) {
                                                    appData.loadLogoBitmap(settings.logoPath)
                                                } else null

                                                val bytes = com.thermalprinter.app.printer.InvoiceFormatter.buildEscPos(invoice, settings, logoBitmap)
                                                val printResult = printerManager.printBytes(bytes)
                                                if (printResult.isSuccess) {
                                                    snackbarHostState.showSnackbar("Nota Invoice berhasil dicetak!")
                                                } else {
                                                    snackbarHostState.showSnackbar("Gagal mencetak: ${printResult.exceptionOrNull()?.message}")
                                                }
                                            } finally {
                                                isInvoiceBusy = false
                                            }
                                        }
                                    },
                                    onShareInvoice = { invoice, asImage ->
                                        scope.launch {
                                            isInvoiceBusy = true
                                            try {
                                                shareInvoiceWhatsApp(invoice, settings, asImage)
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Gagal membagikan invoice: ${e.message}")
                                            } finally {
                                                isInvoiceBusy = false
                                            }
                                        }
                                    },
                                    onOpenSubscriptionDialog = { showSubscriptionDialog = true }
                                )
                            }

                            composable(NavRoute.History.route) {
                                HistoryScreen(
                                    receipts = receipts,
                                    onSelectReceipt = { selected ->
                                        currentReceiptState.value = selected
                                        navController.navigate(NavRoute.Preview.route)
                                    },
                                    onReprintReceipt = { toReprint ->
                                        scope.launch {
                                            val logoBitmap = if (settings.showLogo && settings.logoPath.isNotBlank()) {
                                                appData.loadLogoBitmap(settings.logoPath)
                                            } else null

                                            val bytes = ReceiptFormatter.buildEscPos(
                                                toReprint.copy(isReprint = true),
                                                settings,
                                                logoBitmap
                                            )
                                            val res = printerManager.printBytes(bytes)
                                            if (res.isSuccess) {
                                                appData.save(toReprint.copy(isReprint = true, printCount = toReprint.printCount + 1))
                                                snackbarHostState.showSnackbar("Salinan nota dicetak")
                                            } else {
                                                snackbarHostState.showSnackbar("Gagal: ${res.exceptionOrNull()?.message}")
                                            }
                                        }
                                    },
                                    onDeleteReceipt = { id ->
                                        scope.launch {
                                            appData.delete(id)
                                            snackbarHostState.showSnackbar("Nota dihapus")
                                        }
                                    },
                                    onClearAllReceipts = {
                                        scope.launch {
                                            appData.clear()
                                            snackbarHostState.showSnackbar("Semua riwayat telah dihapus")
                                        }
                                    },
                                    onExportHistory = {
                                        exportHistoryToCsv(receipts)
                                    }
                                )
                            }

                            composable(NavRoute.Settings.route) {
                                val paired = printerManager.getPairedDevices()
                                SettingsScreen(
                                    settings = settings,
                                    printerStatus = printerStatus,
                                    subscriptionState = subscriptionState,
                                    deviceId = subscriptionManager.deviceId,
                                    pairedDevices = paired,
                                    onSaveSettings = { updatedSettings ->
                                        scope.launch {
                                            appData.saveSettings(updatedSettings)
                                            snackbarHostState.showSnackbar("Pengaturan berhasil disimpan")
                                        }
                                    },
                                    onPickLogo = { logoUri ->
                                        scope.launch {
                                            val savedPath = appData.saveLogoFromUri(logoUri)
                                            if (savedPath.isNotBlank()) {
                                                snackbarHostState.showSnackbar("Logo toko berhasil diperbarui")
                                            } else {
                                                snackbarHostState.showSnackbar("Gagal menyimpan logo")
                                            }
                                        }
                                    },
                                    onRemoveLogo = {
                                        scope.launch {
                                            appData.removeLogo()
                                            snackbarHostState.showSnackbar("Logo toko dihapus")
                                        }
                                    },
                                    onOpenSubscriptionDialog = { showSubscriptionDialog = true },
                                    onConnectPrinter = { address ->
                                        scope.launch {
                                            val res = printerManager.connect(address)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Berhasil terhubung ke printer!")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Gagal konek")
                                            }
                                        }
                                    },
                                    onDisconnectPrinter = {
                                        printerManager.disconnect()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Koneksi printer diputuskan")
                                        }
                                    },
                                    onTestPrint = {
                                        scope.launch {
                                            val res = printerManager.printTestReceipt(settings.storeName)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Uji cetak terkirim")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Gagal cetak")
                                            }
                                        }
                                    },
                                    onFeedPaper = {
                                        scope.launch {
                                            printerManager.feedPaper(3)
                                        }
                                    },
                                    onCutPaper = {
                                        scope.launch {
                                            printerManager.cutPaper()
                                        }
                                    }
                                )
                            }
                        }

                        if (isOcrProcessing.value) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black.copy(alpha = 0.4f)
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    CircularProgressIndicator(color = PrimaryOrange)
                                }
                            }
                        }

                        // Subscription & Activation Dialog
                        if (showSubscriptionDialog) {
                            SubscriptionDialog(
                                subscriptionState = subscriptionState,
                                deviceId = subscriptionManager.deviceId,
                                onActivateLicense = { key ->
                                    scope.launch {
                                        val result = subscriptionManager.activateLicense(key)
                                        if (result.isSuccess) {
                                            showSubscriptionDialog = false
                                            snackbarHostState.showSnackbar(result.getOrNull() ?: "Lisensi aktif!")
                                        } else {
                                            snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Aktivasi gagal")
                                        }
                                    }
                                },
                                onApplyPromo = { promo ->
                                    scope.launch {
                                        val result = subscriptionManager.applyPromoCode(promo)
                                        if (result.isSuccess) {
                                            showSubscriptionDialog = false
                                            snackbarHostState.showSnackbar(result.getOrNull() ?: "Promo berhasil!")
                                        } else {
                                            snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Promo gagal")
                                        }
                                    }
                                },
                                onDismiss = { showSubscriptionDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        when {
            intent.type?.startsWith("text/") == true -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                lifecycleScope.launch {
                    val currentSettings = appData.settings.first()
                    processText(sharedText, currentSettings.defaultAdminFee, currentSettings.splitAdminFee)
                }
            }
            intent.type?.startsWith("image/") == true -> {
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: return
                processImageUri(imageUri)
            }
        }
    }

    private fun processText(text: String, defaultFee: Long, splitAdminFee: Boolean) {
        val parsed = ReceiptParserEngine.parse(text, defaultFee, splitAdminFee)
        currentReceiptState.value = parsed
    }

    private fun processImageUri(uri: Uri, onCompleted: (() -> Unit)? = null) {
        isOcrProcessing.value = true
        lifecycleScope.launch {
            try {
                val ocrResult = ocrManager.recognizeText(uri)
                val currentSettings = appData.settings.first()
                val extractedText = ocrResult.getOrNull().orEmpty()
                val parsed = ReceiptParserEngine.parse(
                    extractedText,
                    currentSettings.defaultAdminFee,
                    currentSettings.splitAdminFee
                )
                currentReceiptState.value = parsed
                onCompleted?.invoke()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Gagal membaca teks gambar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isOcrProcessing.value = false
            }
        }
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun sharePlainText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan Bukti Transaksi"))
    }

    private suspend fun shareInvoiceWhatsApp(
        invoice: CustomInvoice,
        settings: StoreSettings,
        asImage: Boolean
    ) {
        val normalizedPhone = com.thermalprinter.app.domain.model.WhatsAppPhone.normalize(invoice.customerPhone)
        val textBody = com.thermalprinter.app.printer.InvoiceDocument.text(invoice, settings)

        if (asImage) {
            val imageFile = com.thermalprinter.app.printer.InvoiceImageRenderer.writePng(this, invoice, settings)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Nota Invoice ${invoice.invoiceNumber.ifBlank { "Toko" }}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                // WhatsApp not installed or package dispatch failed: fallback to general chooser
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Nota Invoice ${invoice.invoiceNumber.ifBlank { "Toko" }}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(fallbackIntent, "Bagikan Gambar Nota"))
            }
        } else {
            // Text sharing
            if (normalizedPhone != null) {
                // Direct phone chat intent via wa.me URI
                val encodedText = Uri.encode(textBody)
                val directUri = Uri.parse("https://wa.me/$normalizedPhone?text=$encodedText")
                val directIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    setPackage("com.whatsapp")
                }
                try {
                    startActivity(directIntent)
                    return
                } catch (_: Exception) {
                    // Try generic ACTION_VIEW for browsers or WhatsApp Business
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, directUri))
                        return
                    } catch (_: Exception) {}
                }
            }

            // Fallback plain text share
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textBody)
                setPackage("com.whatsapp")
            }
            try {
                startActivity(sendIntent)
            } catch (_: Exception) {
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textBody)
                }
                startActivity(Intent.createChooser(chooserIntent, "Bagikan Teks Nota"))
            }
        }
    }

    private fun exportHistoryToCsv(receipts: List<TransactionReceipt>) {
        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,Waktu,Sumber,Jenis,Nominal,BiayaAdmin,Total,Penerima,Pengirim,NoReferensi,Status\n")
            for (r in receipts) {
                csvBuilder.append("${r.id},\"${r.transactionDate} ${r.transactionTime}\",\"${r.source.displayName}\",\"${r.transactionType}\",${r.transferAmount},${r.storeAdminFee},${r.calculateTotal()},\"${r.receiverName}\",\"${r.senderName}\",\"${r.referenceNumber}\",\"${r.status}\"\n")
            }

            val exportFile = File(cacheDir, "riwayat_transaksi.csv")
            exportFile.writeText(csvBuilder.toString())

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", exportFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Ekspor Riwayat Transaksi (CSV)"))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengekspor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrManager.close()
    }
}
