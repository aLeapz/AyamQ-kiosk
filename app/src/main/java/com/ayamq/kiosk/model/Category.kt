package com.ayamq.kiosk.model

/**
 * Enum representing menu categories
 * Provides type-safe category handling
 */
enum class Category(val displayName: String, val dbValue: String) {
    PAKET("PAKET", "PAKET"),
    ALA_CARTE("ALA CARTE", "ALA_CARTE"),
    SAUCE("SAUCE", "SAUCE"),
    DRINK("DRINK", "DRINK");

}