package com.college.campusapp.security

import java.util.regex.Pattern

object SecurityUtils {

    // Standard RFC 5322 regex validation for emails
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    )

    // Standard phone validation for 10-digit formats
    private val PHONE_PATTERN = Pattern.compile(
        "^\\d{10}$"
    )

    /**
     * Checks if the email is valid and clean.
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val trimmed = email.trim()
        return EMAIL_PATTERN.matcher(trimmed).matches() && !containsInjectionPatterns(trimmed)
    }

    /**
     * Checks if the phone number consists of exactly 10 digits.
     */
    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val trimmed = phone.trim()
        return PHONE_PATTERN.matcher(trimmed).matches()
    }

    /**
     * Verifies strength of the user password.
     * Minimum 8 characters, at least 1 digit, 1 letter, and 1 special symbol.
     */
    fun isStrongPassword(password: String?): Boolean {
        if (password.isNullOrBlank()) return false
        if (password.length < 8) return false
        
        var hasDigit = false
        var hasLetter = false
        var hasSpecial = false

        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"

        for (char in password) {
            when {
                char.isDigit() -> hasDigit = true
                char.isLetter() -> hasLetter = true
                specialChars.contains(char) -> hasSpecial = true
            }
        }
        return hasDigit && hasLetter && hasSpecial
    }

    /**
     * Performs a check against common input injection threat elements
     * like script blocks, SQL symbols, or HTML elements.
     */
    fun containsInjectionPatterns(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        val cleanInput = input.lowercase()
        val threats = listOf(
            "<script", "javascript:", "onload=", "onerror=", 
            "select ", "union ", "insert ", "delete ", "drop table", 
            "\" or 1=1", "' or 1=1", "--", "src="
        )
        for (threat in threats) {
            if (cleanInput.contains(threat)) {
                return true
            }
        }
        return false
    }

    /**
     * Sanitizes inputs to eliminate potential HTML/Script tags before processing or display.
     */
    fun sanitizeInput(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.trim()
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("&", "&amp;")
    }
}
