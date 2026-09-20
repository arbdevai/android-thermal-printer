package com.thermalprinter.app.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.InvoiceItem
import com.thermalprinter.app.domain.model.WhatsAppPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Immutable UI State for Custom Invoice Draft.
 * All state is preserved across process death and configuration changes via SavedStateHandle (JSON).
 */
data class InvoiceDraftState(
    val invoiceId: Long = 0L,
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val invoiceTime: String = "",
    val createdAt: Long = 0L,
    val customerName: String = "",
    val customerPhone: String = "",
    val vehiclePlateOrItemType: String = "",
    val businessType: String = "Bengkel / Servis",
    val paymentMethod: String = "Tunai",
    val warrantyOrNotes: String = DEFAULT_WARRANTY_NOTE,
    val items: List<InvoiceItem> = emptyList(),
    val discountInput: String = "",
    val paidInput: String = "",
    val contactPickerError: String? = null,
    val isContactLoading: Boolean = false
) {
    val subtotal: Long
        get() = items.sumOf { it.price * it.quantity }

    val discountParsed: Long?
        get() = if (discountInput.isBlank()) 0L else discountInput.toLongOrNull()

    val isDiscountValid: Boolean
        get() {
            if (discountInput.isBlank()) return true
            val parsed = discountInput.toLongOrNull() ?: return false
            return parsed in 0L..MAX_AMOUNT && parsed <= subtotal
        }

    val discountError: String?
        get() {
            if (discountInput.isBlank()) return null
            val parsed = discountInput.toLongOrNull()
            return when {
                parsed == null || parsed < 0L || parsed > MAX_AMOUNT -> "Diskon tidak valid (maksimal Rp 1 Triliun)"
                parsed > subtotal -> "Diskon tidak boleh melebihi subtotal"
                else -> null
            }
        }

    val effectiveDiscount: Long
        get() = if (isDiscountValid) (discountParsed ?: 0L) else 0L

    val total: Long
        get() = (subtotal - effectiveDiscount).coerceAtLeast(0L)

    val isPaidInputBlank: Boolean
        get() = paidInput.isBlank()

    val paidParsed: Long?
        get() = if (paidInput.isBlank()) null else paidInput.toLongOrNull()

    val isPaidValid: Boolean
        get() {
            if (paidInput.isBlank()) return true
            val parsed = paidInput.toLongOrNull() ?: return false
            return parsed in 0L..MAX_AMOUNT
        }

    val paidError: String?
        get() {
            if (paidInput.isBlank()) return null
            val parsed = paidInput.toLongOrNull()
            return if (parsed == null || parsed < 0L || parsed > MAX_AMOUNT) {
                "Nominal bayar tidak valid (maksimal Rp 1 Triliun)"
            } else null
        }

    // Blank paid=0 and outstanding distinct:
    // When paid input is blank, paid is 0L, outstanding is total.
    // When paid is explicitly entered, change and outstanding are calculated against total.
    val paidAmount: Long
        get() = if (isPaidValid && paidParsed != null) paidParsed!! else 0L

    val change: Long
        get() = if (!isPaidInputBlank && isPaidValid && paidAmount >= total) {
            paidAmount - total
        } else 0L

    val outstanding: Long
        get() = if (!isPaidInputBlank && isPaidValid && paidAmount < total) {
            total - paidAmount
        } else if (isPaidInputBlank) {
            total
        } else 0L

    val isDirty: Boolean
        get() = customerName.isNotBlank() ||
                customerPhone.isNotBlank() ||
                vehiclePlateOrItemType.isNotBlank() ||
                items.isNotEmpty() ||
                discountInput.isNotBlank() ||
                paidInput.isNotBlank() ||
                warrantyOrNotes != DEFAULT_WARRANTY_NOTE

    val isPhoneValidOrEmpty: Boolean
        get() = customerPhone.isBlank() || WhatsAppPhone.isValid(customerPhone)

    val isPhoneInvalidNonEmpty: Boolean
        get() = WhatsAppPhone.isInvalidNonEmpty(customerPhone)

    val canPrint: Boolean
        get() = items.isNotEmpty() && isDiscountValid && isPaidValid

    val canShare: Boolean
        get() = canPrint && isPhoneValidOrEmpty

    fun toCustomInvoice(): CustomInvoice {
        return CustomInvoice(
            id = invoiceId,
            invoiceNumber = invoiceNumber,
            invoiceDate = invoiceDate,
            invoiceTime = invoiceTime,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            vehiclePlateOrItemType = vehiclePlateOrItemType.trim(),
            businessType = businessType,
            items = items,
            discountAmount = effectiveDiscount,
            paidAmount = if (isPaidInputBlank) 0L else paidAmount,
            paymentMethod = paymentMethod,
            warrantyOrNotes = warrantyOrNotes.trim(),
            createdAt = createdAt
        )
    }

    companion object {
        const val MAX_AMOUNT = 1_000_000_000_000L // 1e12
        const val MAX_QTY = 10_000
        const val MAX_ITEMS_COUNT = 100
        const val DEFAULT_WARRANTY_NOTE = "Garansi servis 7 hari. Terima kasih atas kunjungan Anda."
    }
}

class InvoiceDraftViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gson = Gson()
    private val _uiState = MutableStateFlow(createFreshDraftState())
    val uiState: StateFlow<InvoiceDraftState> = _uiState.asStateFlow()

    private var contactQuerySequence = 0L

    init {
        val savedJson: String? = savedStateHandle[KEY_DRAFT_STATE]
        if (savedJson != null) {
            try {
                val restored = gson.fromJson(savedJson, InvoiceDraftState::class.java)
                if (restored != null && restored.invoiceId > 0L) {
                    _uiState.value = restored
                } else {
                    val fresh = createFreshDraftState()
                    _uiState.value = fresh
                    saveToHandle(fresh)
                }
            } catch (_: Exception) {
                val fresh = createFreshDraftState()
                _uiState.value = fresh
                saveToHandle(fresh)
            }
        } else {
            val fresh = createFreshDraftState()
            _uiState.value = fresh
            saveToHandle(fresh)
        }
    }

    private fun createFreshDraftState(): InvoiceDraftState {
        val now = System.currentTimeMillis()
        val idLocale = Locale("id", "ID")
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", idLocale)
        val sdfTime = SimpleDateFormat("HH:mm", idLocale)
        return InvoiceDraftState(
            invoiceId = now,
            invoiceNumber = "INV-${now.toString().takeLast(6)}",
            invoiceDate = sdfDate.format(Date(now)),
            invoiceTime = sdfTime.format(Date(now)),
            createdAt = now,
            customerName = "",
            customerPhone = "",
            vehiclePlateOrItemType = "",
            businessType = "Bengkel / Servis",
            paymentMethod = "Tunai",
            warrantyOrNotes = InvoiceDraftState.DEFAULT_WARRANTY_NOTE,
            items = emptyList(), // Start EMPTY items
            discountInput = "",
            paidInput = "",
            contactPickerError = null,
            isContactLoading = false
        )
    }

    private fun saveToHandle(state: InvoiceDraftState) {
        try {
            savedStateHandle[KEY_DRAFT_STATE] = gson.toJson(state)
        } catch (_: Exception) {
            // Best-effort persistence
        }
    }

    private fun updateStateAndSave(transform: (InvoiceDraftState) -> InvoiceDraftState) {
        _uiState.update { current ->
            val newState = transform(current)
            saveToHandle(newState)
            newState
        }
    }

    fun setCustomerName(name: String) {
        contactQuerySequence++
        updateStateAndSave { it.copy(customerName = name) }
    }

    fun setCustomerPhone(phone: String) {
        contactQuerySequence++
        updateStateAndSave { it.copy(customerPhone = phone, contactPickerError = null) }
    }

    fun setVehiclePlateOrItemType(value: String) {
        updateStateAndSave { it.copy(vehiclePlateOrItemType = value) }
    }

    fun setBusinessType(type: String) {
        updateStateAndSave { it.copy(businessType = type) }
    }

    fun setPaymentMethod(method: String) {
        updateStateAndSave { it.copy(paymentMethod = method) }
    }

    fun setWarrantyOrNotes(notes: String) {
        updateStateAndSave { it.copy(warrantyOrNotes = notes) }
    }

    fun setDiscountInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        updateStateAndSave { it.copy(discountInput = digitsOnly) }
    }

    fun setPaidInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        updateStateAndSave { it.copy(paidInput = digitsOnly) }
    }

    fun setPaidExactTotal() {
        val total = _uiState.value.total
        setPaidInput(total.toString())
    }

    fun addItem(name: String, priceText: String, qtyText: String): String? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return "Nama barang / jasa tidak boleh kosong"

        val price = priceText.toLongOrNull()
        if (price == null || price < 0L || price > InvoiceDraftState.MAX_AMOUNT) {
            return "Harga tidak valid (maksimal Rp 1 Triliun)"
        }

        val qty = qtyText.toIntOrNull()
        if (qty == null || qty !in 1..InvoiceDraftState.MAX_QTY) {
            return "Jumlah (Qty) harus antara 1 dan 10.000"
        }

        if (_uiState.value.items.size >= InvoiceDraftState.MAX_ITEMS_COUNT) {
            return "Maksimal ${InvoiceDraftState.MAX_ITEMS_COUNT} item per nota"
        }

        val newItem = InvoiceItem(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            price = price,
            quantity = qty
        )

        updateStateAndSave { it.copy(items = it.items + newItem) }
        return null
    }

    fun updateItem(id: String, name: String, priceText: String, qtyText: String): String? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return "Nama barang / jasa tidak boleh kosong"

        val price = priceText.toLongOrNull()
        if (price == null || price < 0L || price > InvoiceDraftState.MAX_AMOUNT) {
            return "Harga tidak valid (maksimal Rp 1 Triliun)"
        }

        val qty = qtyText.toIntOrNull()
        if (qty == null || qty !in 1..InvoiceDraftState.MAX_QTY) {
            return "Jumlah (Qty) harus antara 1 dan 10.000"
        }

        updateStateAndSave { state ->
            val updated = state.items.map {
                if (it.id == id) it.copy(name = trimmedName, price = price, quantity = qty) else it
            }
            state.copy(items = updated)
        }
        return null
    }

    fun deleteItem(id: String) {
        updateStateAndSave { state ->
            state.copy(items = state.items.filter { it.id != id })
        }
    }

    fun setContactError(error: String?) {
        _uiState.update { it.copy(contactPickerError = error, isContactLoading = false) }
    }

    fun clearContactError() {
        _uiState.update { it.copy(contactPickerError = null) }
    }

    fun onContactPicked(uri: Uri, contentResolver: ContentResolver) {
        val queryToken = ++contactQuerySequence
        _uiState.update { it.copy(isContactLoading = true, contactPickerError = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scheme = uri.scheme
                val authority = uri.authority
                if (scheme != "content" || (authority != ContactsContract.AUTHORITY && authority != "com.android.contacts")) {
                    withContext(Dispatchers.Main) {
                        if (queryToken == contactQuerySequence) {
                            _uiState.update {
                                it.copy(
                                    isContactLoading = false,
                                    contactPickerError = "URI kontak tidak valid"
                                )
                            }
                        }
                    }
                    return@launch
                }

                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )

                var pickedNumber: String? = null
                var pickedName: String? = null

                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        if (numIndex >= 0) {
                            pickedNumber = cursor.getString(numIndex)
                        }
                        if (nameIndex >= 0) {
                            pickedName = cursor.getString(nameIndex)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (queryToken == contactQuerySequence) {
                        if (!pickedNumber.isNullOrBlank()) {
                            updateStateAndSave { state ->
                                state.copy(
                                    customerPhone = pickedNumber.orEmpty().trim(),
                                    customerName = if (state.customerName.isBlank() && !pickedName.isNullOrBlank()) {
                                        pickedName!!.trim()
                                    } else state.customerName,
                                    contactPickerError = null,
                                    isContactLoading = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isContactLoading = false,
                                    contactPickerError = "Nomor telepon tidak ditemukan pada kontak"
                                )
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    if (queryToken == contactQuerySequence) {
                        _uiState.update {
                            it.copy(
                                isContactLoading = false,
                                contactPickerError = "Izin kontak tidak tersedia: ${e.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (queryToken == contactQuerySequence) {
                        _uiState.update {
                            it.copy(
                                isContactLoading = false,
                                contactPickerError = "Gagal membaca kontak: ${e.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetDraft() {
        contactQuerySequence++
        val fresh = createFreshDraftState()
        _uiState.value = fresh
        saveToHandle(fresh)
    }

    companion object {
        private const val KEY_DRAFT_STATE = "key_invoice_draft_state_json"
    }
}
