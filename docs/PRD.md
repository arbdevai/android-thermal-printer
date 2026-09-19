# PRD: Android Thermal Printer for E-Wallet Transactions

## 1. Overview

Aplikasi Android native yang mengubah bukti transaksi dari bank/e-wallet menjadi nota yang siap dicetak di printer thermal 58mm. Aplikasi menerima gambar atau teks bukti transaksi via Android Share Intent, memparse data (nominal, pengirim, penerima, waktu, no. referensi), menghitung biaya admin toko, dan mencetaknya ke printer Bluetooth.

## 2. User Flow

```
[Open transaction in bank/e-wallet]
      ↓
[Tap "Share" → Select our app]
      ↓
[Receive share data (image or text)]
      ↓
[Parsing & Validation]
      ↓
[Preview Nota + Admin Fee]
      ↓
[Print to Bluetooth Printer]
      ↓
[Save to Local History]
```

## 3. Target Banks/E-Wallets

1. BCA
2. BRImo
3. Livin' Mandiri
4. DANA
5. GoPay
6. OVO
7. ShopeePay
8. SeaBank

## 4. Key Features

### 4.1 Document Parsing (OCR)

- **Image Parsing**: Extract text dari gambar bukti transaksi
- **Text Parsing**: Extract text langsung dari teks yang dibagi
- **Support Offline**: Google ML Kit Text Recognition (native Android, 100% offline)

### 4.2 Note Preview & Editing

- **Auto-Detect Source**: Identify Bank/E-Wallet from share data
- **Auto-Extract Data**: Parse nominal, sender, receiver, date, reference number
- **Manual Edit**: Allow user to:
  - Edit extracted data (nominal, amounts, dates)
  - Add/Remove admin fee
  - Set custom store name (optional, per note)
- **Data Flags**: Flag unclear/partial data for user confirmation

### 4.3 Admin Fee Management

- **Configuration**:
  - Default admin fee (configurable, example: Rp2.000)
  - Admin fee can be set to Rp0
  - Per-note override (temporary change)
- **Calculation**:
  - Separate: Transfer nominal vs Admin fee
  - Prevent double-charging for bank fees included in amount

### 4.4 Thermal Printing

- **Printer Type**: Bluetooth thermal (58mm)
- **Output Format**: ESC/POS
- **Features**:
  - Print branding (logo, store name, address)
  - Print transaction details
  - Print admin fee total
  - Print date/time
  - Print reference number
  - Print footer message
  - Multiple copies support
  - Paper auto-feed and cut support (if supported)

### 4.5 Local History

- **Storage**: Local-only (no server, privacy-first)
- **Features**:
  - Search by reference number or receiver name
  - Filter by date range and source
  - View transaction details
  - Reprint (marked as duplicate)
  - Export history (CSV/JSON)
- **Safety**: Since data is local, app deletion removes history. Backup/restore options to be added later.

## 5. App Settings

### 5.1 Store Identity

- Logo (uploaded by user, auto-converted to black & white)
- Store name
- Store address
- WhatsApp/Phone number
- Owner name (optional)
- Footer message

### 5.2 Admin Fee

- Default admin fee
- Temporary per-note admin fee
- Toggle admin fee display

### 5.3 Printer Configuration

- Bluetooth printer selection
- Connect/Disconnect printer
- Test print button
- Paper feed button
- Cut paper button (if supported)
- Default copies (1-3)

### 5.4 Note Format

- Toggle logo display
- Toggle reference number
- Toggle transaction source
- Toggle admin fee
- Custom footer message
- Print multiple copies option

### 5.5 Theme

- Main theme: Solid black background
- Panel design: Dark translucent glassmorphism
- Border: Thin translucent lines
- Text colors: White, light gray
- Primary button: Single accent color (PRINT)

## 6. UI/UX Design

### 6.1 Color Scheme

| Element | Color | Hex |
|---------|-------|-----|
| Background | Solid Black | `#000000` |
| Card/Panel Background | Translucent Dark | `rgba(30, 30, 30, 0.85)` |
| Border | Translucent Light | `rgba(255, 255, 255, 0.15)` |
| Primary Text | White | `#FFFFFF` |
| Secondary Text | Light Gray | `#B0B0B0` |
Accent Color | Blue (example) | `#3B82F6` |

### 6.2 Navigation

- **Home Screen**:
  - When opened via Share Intent → Preview screen
  - When opened normally → Homepage with:
    - Quick menu (New Nota, History, Settings)
    - Recent processed transactions

- **History Screen**: List of all printed transactions with search and filters

- **Preview Screen**:
  - Auto-detect source
  - Show parsed data with editable fields
  - Show calculated totals
  - Store name field (optional, pre-filled from settings)
  - Two action buttons: **View Settings** and **Cetak Nota**

- **Settings Screen**:
  - Tabs: Store Identity, Printer, Format, History

### 6.3 Preview Nota Design (58mm Thermal)

```
[LOGO - Grayscale, Centered]
       [STORE NAME]
   [ADDRESS/WA/CALL]
------------------------------
[TRANSACTION SOURCE]
[NOMINAL TRANSFER        XXXX]
[NOMINAL ADMIN FEE    XXXX]
[NOMINAL TOTAL        XXXX]
------------------------------
[REF NO: XXXXXXX]
[DATE: DD/MM/YYYY]
[TIME: HH:MM]
------------------------------
[FOOTER MESSAGE]
[COPIES: X]
[SCANNED BY APP NAME]
```

**Note Area**: White background, black text (business card style), no dark background on paper.

## 7. Technical Stack

- **Platform**: Android Native
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **OCR Library**: Google ML Kit Text Recognition
- **Bluetooth Printer**: ESC/POS Android Library (e.g., `thermalprinter` or similar)
- **Image Processing**: Android Bitmap API + Dithering for logo
- **Local Storage**: SharedPreferences + Room Database
- **File Format**: JSON for history export

## 8. Architecture

### 8.1 Components

```
├── Data Layer
│   ├── DataStore (settings + printer config)
│   ├── Room Database (transaction history)
│   └── ML Kit (OCR)
│
├── Domain Layer
│   ├── Document Parser (image/text)
│   ├── Calculator (amounts, totals)
│   └── NotaPrinter (ESC/POS commands)
│
├── Presentation Layer
│   ├── Screens (Home, History, Settings, Preview)
│   ├── Composables (PreviewNota, AdminFeeInput, PrinterSettings)
│   └── ViewModel (PreviewViewModel, HistoryViewModel)
│
└── Infrastructure
    ├── Android Share Intent Handler
    ├── Bluetooth Printer Controller
    └── History Export Manager
```

### 8.2 GitHub CI Setup

```yaml
# .github/workflows/build-apk.yml
- Trigger: push/PR/metadata_change
- Stages:
  1. Checkout code
  2. Setup JDK 17
  3. Dependencies (gradle cache)
  4. Build Debug APK
  5. Generate App Icon (e.g., use gradle icon generator)
  6. Create signed Release APK (if needed)
  7. Upload APK artifact
```

## 9. Prerequisites & Dependencies

### 9.1 Libraries

| Library | Purpose | Source |
|---------|---------|--------|
| Compose BOM | UI components | Google |
| ML Kit Text Recognition | OCR | Google Play Services |
| Bluetooth ESC/POS | Printer commands | Various MIT/Apache |
| Room Database | Local storage | Google |
| DataStore | Settings storage | Google |
| ViewModel / Navigation | Architecture | Google |
| Material3 | Design components | Google |
| Bitmap Dithering | Logo conversion | Custom implementation |
| Gson/Moshi | JSON handling | Google |

### 9.2 Printer Recommendations

- TP-Link FP-150B
- Bixolon SRP-270III
- Star Micronics TSP650II
- Custom generic ESC/POS Bluetooth printer
- (Will validate compatibility during testing)

## 10. Security & Privacy

- **Zero Bank Data**: App never receives bank credentials or OTP
- **Local-Only**: Transaction data stored locally (optionally exported via CSV/JSON)
- **No Analytics**: No tracking, no telemetry
- **No Permissions Needed**:
  - No camera (not required after 2025-03-01 shadow ban)
  - No contacts
  - No SMS
  - No location
  - Only Bluetooth (for printing)

## 11. Development Timeline Estimate

| Phase | Tasks | Est. Time |
|-------|-------|-----------|
| 1 | Repo setup + GitHub CI + Icon generator | 2-3 days |
| 2 | Core architecture + Data layer | 3-4 days |
| 3 | OCR integration + Document parser | 4-5 days |
| 4 | Nota preview + Settings screen | 3-4 days |
| 5 | Bluetooth printer + ESC/POS | 3-4 days |
| 6 | History + Export | 2-3 days |
| 7 | Testing + Polish | 2-3 days |
| **Total** | | **19-26 days** |

## 12. Risks & Mitigations

### 12.1 Risks

1. **OCR Accuracy Limited**: Different banks format differ, OCR may struggle
   - *Mitigation*: Manual editing, flag unclear parts, maintain a document template system

2. **Printer Compatibility**: Generic printer may have limitations
   - *Mitigation*: Support ESC/POS standard, provide test prints, document printer requirements

3. **Local History Loss**: Removal/deletion cases
   - *Mitigation*: Export feature, warn users, future backup/restore option

4. **New Bank Formats**: Share format may change after app updates
   - *Mitigation*: User manual correction (flag/edit), community feedback channel

### 12.2 Out-of-Scope (for v1.0)

- Cloud backup/restore
- Multi-tenant support
- Cloud sync
- Advanced analytics
- Integration with other loyalty systems
- Scan barcode/QR code (future)

## 13. Resolution History

YYYY-MM-DD | Summary | Status
------------|---------|--------
2026-09-19 | Initial PRD creation - agreed on 8 banks, admin fee, local history, Android native, black solid theme with glass panels | Draft