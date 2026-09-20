package com.thermalprinter.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppPhoneTest {

    @Test
    fun normalize_localIndonesianNumbers_convertedTo628() {
        assertEquals("628123456789", WhatsAppPhone.normalize("08123456789"))
        assertEquals("6281234567890", WhatsAppPhone.normalize("0812-3456-7890"))
        assertEquals("6281234567890", WhatsAppPhone.normalize("0812 3456 7890"))
        assertEquals("628123456789", WhatsAppPhone.normalize("(0812) 345-6789"))
        assertEquals("6281234567890", WhatsAppPhone.normalize("0812.3456.7890"))
    }

    @Test
    fun normalize_indonesianWithCountryCode_normalizedCorrectly() {
        assertEquals("628123456789", WhatsAppPhone.normalize("+628123456789"))
        assertEquals("6281234567890", WhatsAppPhone.normalize("+62 812-3456-7890"))
        assertEquals("628123456789", WhatsAppPhone.normalize("628123456789"))
        assertEquals("628123456789", WhatsAppPhone.normalize("62812-3456-789"))
        assertEquals("628123456789", WhatsAppPhone.normalize("+62 (812) 345-6789"))
    }

    @Test
    fun normalize_otherInternationalNumbers_retainsDigitsWithoutPlus() {
        assertEquals("14155552671", WhatsAppPhone.normalize("+14155552671"))
        assertEquals("15552345678", WhatsAppPhone.normalize("+1 (555) 234-5678"))
        assertEquals("447911123456", WhatsAppPhone.normalize("+44 7911 123456"))
        assertEquals("60123456789", WhatsAppPhone.normalize("+60 12-345 6789"))
        assertEquals("819012345678", WhatsAppPhone.normalize("+81 90-1234-5678"))
        assertEquals("61412345678", WhatsAppPhone.normalize("+61 412 345 678"))
    }

    @Test
    fun normalize_illegalCharacters_rejected() {
        assertNull(WhatsAppPhone.normalize("0812abc3456"))
        assertNull(WhatsAppPhone.normalize("0812#3456"))
        assertNull(WhatsAppPhone.normalize("0812@whatsapp"))
        assertNull(WhatsAppPhone.normalize("+62-812-xyz"))
        assertNull(WhatsAppPhone.normalize("phone12345678"))
        assertNull(WhatsAppPhone.normalize("0812*12345"))
    }

    @Test
    fun normalize_misplacedOrMultiplePlusSigns_rejected() {
        assertNull(WhatsAppPhone.normalize("+62+812345678"))
        assertNull(WhatsAppPhone.normalize("62+812345678"))
        assertNull(WhatsAppPhone.normalize("0812+345678"))
        assertNull(WhatsAppPhone.normalize("++6281234567"))
    }

    @Test
    fun normalize_leadingZerosOtherThanLocal08_rejected() {
        assertNull(WhatsAppPhone.normalize("00123456789"))
        assertNull(WhatsAppPhone.normalize("0123456789"))
        assertNull(WhatsAppPhone.normalize("0215551234"))
        assertNull(WhatsAppPhone.normalize("07123456789"))
        assertNull(WhatsAppPhone.normalize("+0123456789"))
        assertNull(WhatsAppPhone.normalize("+620812345678"))
        assertNull(WhatsAppPhone.normalize("620812345678"))
    }

    @Test
    fun normalize_lengthBounds_enforcedBetween8And15Digits() {
        // Too short (< 8 digits)
        assertNull(WhatsAppPhone.normalize("08123")) // 628123 -> length 6
        assertNull(WhatsAppPhone.normalize("081234")) // 6281234 -> length 7
        assertNull(WhatsAppPhone.normalize("+123456")) // length 6

        // Valid boundary 8 digits
        assertEquals("62812345", WhatsAppPhone.normalize("0812345"))

        // Valid boundary 15 digits
        assertEquals("123456789012345", WhatsAppPhone.normalize("+123456789012345"))

        // Too long (> 15 digits)
        assertNull(WhatsAppPhone.normalize("+1234567890123456")) // length 16
        assertNull(WhatsAppPhone.normalize("08123456789012345")) // normalized 16 digits
    }

    @Test
    fun normalize_emptyAndNullInputs_returnsNull() {
        assertNull(WhatsAppPhone.normalize(null))
        assertNull(WhatsAppPhone.normalize(""))
        assertNull(WhatsAppPhone.normalize("   "))
        assertNull(WhatsAppPhone.normalize("\t\n"))
    }

    @Test
    fun normalize_internationalWithoutPlus_rejected() {
        // Non-Indonesian numbers without '+' prefix are ambiguous and rejected
        assertNull(WhatsAppPhone.normalize("14155552671"))
        assertNull(WhatsAppPhone.normalize("447911123456"))
    }

    @Test
    fun isValid_returnsTrueOnlyForValidPhone() {
        assertTrue(WhatsAppPhone.isValid("08123456789"))
        assertTrue(WhatsAppPhone.isValid("+628123456789"))
        assertTrue(WhatsAppPhone.isValid("+14155552671"))

        assertFalse(WhatsAppPhone.isValid(null))
        assertFalse(WhatsAppPhone.isValid(""))
        assertFalse(WhatsAppPhone.isValid("   "))
        assertFalse(WhatsAppPhone.isValid("0812abc"))
        assertFalse(WhatsAppPhone.isValid("0012345678"))
    }

    @Test
    fun isInvalidNonEmpty_correctlyFlagsNonEmptyInvalidPhone() {
        // Empty or null should be false (phone is optional)
        assertFalse(WhatsAppPhone.isInvalidNonEmpty(null))
        assertFalse(WhatsAppPhone.isInvalidNonEmpty(""))
        assertFalse(WhatsAppPhone.isInvalidNonEmpty("   "))

        // Valid phone should be false
        assertFalse(WhatsAppPhone.isInvalidNonEmpty("08123456789"))
        assertFalse(WhatsAppPhone.isInvalidNonEmpty("+628123456789"))

        // Non-empty and invalid should be true (blocks share, not print)
        assertTrue(WhatsAppPhone.isInvalidNonEmpty("0812abc"))
        assertTrue(WhatsAppPhone.isInvalidNonEmpty("0012345678"))
        assertTrue(WhatsAppPhone.isInvalidNonEmpty("+0123456789"))
        assertTrue(WhatsAppPhone.isInvalidNonEmpty("12345"))
    }
}
