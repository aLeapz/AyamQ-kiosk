package com.ayamq.kiosk.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a menu item in the database
 * Stores information about food items available in the kiosk
 */
@Entity(tableName = "menu_items")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * Display name of the menu item
     * e.g., "AyamQ Dada", "Nasi Putih"
     */
    val name: String,

    /**
     * Price in Rupiah (stored as integer to avoid floating point issues)
     * e.g., 10000 for Rp 10.000
     */
    val price: Int,

    /**
     * Category of the menu item
     * Valid values: PAKET, ALA_CARTE, SAUCE, DRINK
     */
    val category: String,

    /**
     * Optional URI or path to the menu item image
     * Can be null if no image is available
     */
    val imageUri: String? = null,

    /**
     * Flag to indicate if item is currently available
     * Useful for temporarily disabling items without deleting them
     */
    val isAvailable: Boolean = true
)