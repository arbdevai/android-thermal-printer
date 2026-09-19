# Android Thermal Printer for E-Wallet & Bank Receipts

Aplikasi Android native berbasis Jetpack Compose untuk mengubah bukti transaksi transfer/pembayaran dari bank dan e-wallet Indonesia menjadi nota fisik yang siap dicetak di printer thermal Bluetooth 58mm (ESC/POS).

---

## Fitur Utama

- **Android Share Intent**: Cukup klik "Bagikan / Share" di aplikasi m-banking atau e-wallet (teks maupun gambar screenshot), nota langsung otomatis terdeteksi dan terisi.
- **Offline OCR & Parser**: Menggunakan Google ML Kit Text Recognition (100% offline di perangkat lokal, tanpa server eksternal).
- **Mendukung 8 Bank & E-Wallet Populer**:
  1. BCA (m-BCA, myBCA, KlikBCA)
  2. BRI (BRImo)
  3. Mandiri (Livin' by Mandiri)
  4. DANA Indonesia
  5. GoPay
  6. OVO
  7. ShopeePay
  8. SeaBank
- **Pengaturan Biaya Admin Toko**:
  - Biaya admin toko default (contoh: Rp 2.000, Rp 3.000, Rp 5.000, atau Rp 0 / Gratis).
  - Tombol pintasan biaya admin per nota.
  - Perhitungan total otomatis yang akurat.
- **2 Pilihan Template Nota**:
  - **Ringkas**: Format hemat kertas, fokus pada nominal dan total pembayaran.
  - **Detail Lengkap**: Dilengkapi rincian pengirim, penerima, nomor rekening, nomor referensi, dan waktu transaksi.
- **Printer Bluetooth 58mm (ESC/POS)**:
  - Koneksi langsung ke printer thermal Bluetooth.
  - Uji cetak 1-klik, paper feed, dan auto-cut.
  - Dukungan cetak salinan (1 - 5 rangkap).
- **Riwayat Transaksi Lokal**:
  - Penyimpanan lokal Room Database (privasi terjaga, tanpa analitik/pelacakan).
  - Pencarian dan filter berdasarkan bank.
  - Cetak ulang nota (dilengkapi tanda SALINAN).
  - Ekspor riwayat ke file CSV.
- **Desain UI Modern**:
  - Tema Solid Black dengan panel translucent glassmorphism yang elegan.

---

## Build APK Otomatis via GitHub Actions CI

Build APK dijalankan secara otomatis di cloud melalui GitHub Actions CI. Anda tidak perlu menginstall Android SDK atau Gradle secara lokal di komputer.

### Cara Mendapatkan File APK:
1. Push branch ke repository GitHub:
   ```bash
   git push origin worktree-thermal-printer-build
   ```
2. Buka tab **Actions** di repository GitHub Anda.
3. Pilih workflow **Build Android APK**.
4. Setelah build selesai, unduh artifact:
   - `thermal-printer-debug-apk`
   - `thermal-printer-release-apk`
5. Install file `.apk` ke smartphone Android Anda.

---

## Struktur Proyek

```
app/src/main/
├── AndroidManifest.xml
├── java/com/thermalprinter/app/
│   ├── ThermalPrinterApp.kt
│   ├── MainActivity.kt
│   ├── data/
│   │   └── AppData.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── BankSource.kt
│   │   │   ├── ReceiptTemplate.kt
│   │   │   ├── StoreSettings.kt
│   │   │   └── TransactionReceipt.kt
│   │   └── parser/
│   │       └── ReceiptParserEngine.kt
│   ├── ocr/
│   │   └── OcrManager.kt
│   ├── printer/
│   │   ├── BluetoothPrinterManager.kt
│   │   ├── EscPosDriver.kt
│   │   └── ReceiptFormatter.kt
│   └── ui/
│       ├── components/
│       │   ├── GlassCard.kt
│       │   └── ReceiptPaperPreview.kt
│       ├── navigation/
│       │   └── NavRoutes.kt
│       ├── screens/
│       │   ├── HistoryScreen.kt
│       │   ├── HomeScreen.kt
│       │   ├── PreviewScreen.kt
│       │   └── SettingsScreen.kt
│       └── theme/
│           ├── Color.kt
│           ├── Theme.kt
│           └── Type.kt
└── res/
    ├── drawable/
    │   ├── ic_launcher_background.xml
    │   └── ic_launcher_foreground.xml
    ├── mipmap-*/
    ├── values/
    └── xml/
```

---

## Lisensi & Privasi

Aplikasi ini 100% lokal dan offline. Tidak ada kredensial perbankan, OTP, atau nomor kartu yang dikirim ke server mana pun.
