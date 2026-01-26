package com.ayamq.kiosk.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a customer order
 * Stores the main order information including status and total price
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val orderId: Int = 0,

    /**
     * Timestamp when the order was created
     * Stored as milliseconds since epoch (System.currentTimeMillis())
     */
    val timestamp: Long,

    /**
     * Total price of the entire order in Rupiah
     * Calculated from sum of all OrderItemEntity prices
     */
    val totalPrice: Int,

    /**
     * Current status of the order
     * Valid values:
     * - DIPROSES: Order is being prepared
     * - SELESAI: Order is completed
     */
    val status: String = "DIPROSES",

    /**
     * Queue number for display (1, 2, 3, etc.)
     * Calculated based on pending orders today
     */
    val queueNumber: Int
)