package com.ayamq.kiosk.model

import com.ayamq.kiosk.data.database.entity.MenuEntity

/**
 * Model representing an item in the shopping cart
 * Combines menu information with quantity
 */
data class CartItem(
    val menuItem: MenuEntity,
    var quantity: Int = 1
) {
    /**
     * Calculate subtotal for this cart item
     * @return price * quantity
     */
    fun getSubtotal(): Int {
        return menuItem.price * quantity
    }
}
