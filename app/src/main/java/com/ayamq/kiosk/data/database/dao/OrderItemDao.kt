package com.ayamq.kiosk.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ayamq.kiosk.data.database.entity.OrderItemEntity

/**
 * Data Access Object for order items
 * Manages the items within each order
 */
@Dao
interface OrderItemDao {

    /**
     * Insert multiple order items at once
     * Used when creating a new order with multiple items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orderItems: List<OrderItemEntity>)

    /**
     * Get all order items for a specific order
     * Returns LiveData for reactive UI updates
     */
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItemsByOrderId(orderId: Int): LiveData<List<OrderItemEntity>>
}