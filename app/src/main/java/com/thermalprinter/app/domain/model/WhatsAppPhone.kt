package com.thermalprinter.app.domain.model

/**
 * Utility for parsing and normalizing WhatsApp phone numbers.
 * Supports:
 * - Local Indonesian: "08..." -> "628..."
 * - Indonesian international: "+62..." or "62..." -> "62..."
 * - Other international with '+': "+1...", "+44..." -> "1...", "44..."
 *
 * Validation rules:
 * - Strips formatting characters (spaces, dashes, dots, parentheses).
 * - Rejects illegal non-phone characters (letters, symbols).
 * - Rejects misplaced or multiple '+' signs (only single leading '+' allowed).
 * - Rejects leading zeros other than local 08 (e.g. 00..., 01..., 02..., +0..., +620...).
 * - Enforces standard ITU-T E.164 length of 8..15 digits.
 * - Returns null for null, blank, or invalid inputs.
 */
object WhatsAppPhone {

    fun normalize(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Check for illegal characters and validate '+' position
        for (i in trimmed.indices) {
            val c = trimmed[i]
            if (!c.isDigit() && c != '+' && c != ' ' && c != '-' && c != '.' && c != '(' && c != ')') {
                return null
            }
            if (c == '+' && i != 0) {
                return null
            }
        }

        val resultDigits = if (trimmed.startsWith("+")) {
            val digits = trimmed.substring(1).filter { it.isDigit() }
            if (digits.isEmpty() || digits.startsWith("0") || digits.startsWith("620")) {
                return null
            }
            digits
        } else {
            val digits = trimmed.filter { it.isDigit() }
            if (digits.isEmpty()) {
                return null
            }
            if (digits.startsWith("0")) {
                if (digits.startsWith("08")) {
                    "62" + digits.substring(1)
                } else {
                    // Leading zero other than local Indonesian 08 is invalid
                    return null
                }
            } else if (digits.startsWith("62")) {
                if (digits.startsWith("620")) {
                    return null
                }
                digits
            } else {
                // Non-Indonesian numbers without '+' prefix are rejected
                return null
            }
        }

        if (resultDigits.length !in 8..15) {
            return null
        }

        return resultDigits
    }

    fun isValid(raw: String?): Boolean = normalize(raw) != null

    fun isInvalidNonEmpty(raw: String?): Boolean = !raw.isNullOrBlank() && normalize(raw) == null
}
