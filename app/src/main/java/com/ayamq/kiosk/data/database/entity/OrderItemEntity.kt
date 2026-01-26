package com.ayamq.kiosk.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Junction entity representing individual items within an order
 * Links OrderEntity and MenuEntity with quantity information
 *
 * Uses foreign keys to maintain referential integrity:
 * - If an order is deleted, all its order items are deleted (CASCADE)
 * - Menu items cannot be deleted if referenced by order items (RESTRICT implied)
 */
@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MenuEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["menuId"])
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * Foreign key reference to the parent order
     */
    val orderId: Int,

    /**
     * Foreign key reference to the menu item
     */
    val menuId: Int,

    /**
     * Quantity of this menu item in the order
     * Must be >= 1
     */
    val quantity: Int,

    /**
     * Price per unit at the time of order
     * Stored to preserve historical pricing if menu prices change
     */
    val pricePerUnit: Int,

    /**
     * Cached menu item name for quick retrieval
     * Reduces need for joins when displaying order details
     */
    val itemName: String
)