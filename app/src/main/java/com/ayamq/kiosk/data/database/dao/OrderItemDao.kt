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
     * Insert a new order item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(orderItem: OrderItemEntity): Long

    /**
     * Insert multiple order items at once
     * Used when creating a new order with multiple items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orderItems: List<OrderItemEntity>)

    /**
     * Update an existing order item
     */
    @Update
    suspend fun update(orderItem: OrderItemEntity)

    /**
     * Delete an order item
     */
    @Delete
    suspend fun delete(orderItem: OrderItemEntity)

    /**
     * Get all order items for a specific order
     * Returns LiveData for reactive UI updates
     */
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItemsByOrderId(orderId: Int): LiveData<List<OrderItemEntity>>

    /**
     * Get order items for a specific order (suspend version)
     * Used when you need the data immediately, not reactively
     */
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsByOrderIdSync(orderId: Int): List<OrderItemEntity>

    /**
     * Get all orders that contain a specific menu item
     * Useful for analyzing menu item popularity
     */
    @Query("SELECT * FROM order_items WHERE menuId = :menuId")
    fun getOrderItemsByMenuId(menuId: Int): LiveData<List<OrderItemEntity>>

    /**
     * Get total quantity sold for a specific menu item
     * Analytics function to track bestsellers
     */
    @Query("SELECT SUM(quantity) FROM order_items WHERE menuId = :menuId")
    suspend fun getTotalQuantitySold(menuId: Int): Int?

    /**
     * Delete all order items for a specific order
     * Automatically handled by foreign key CASCADE, but useful for explicit deletion
     */
    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Int)

    /**
     * Get order item count for a specific order
     * Returns the number of different items (not total quantity)
     */
    @Query("SELECT COUNT(*) FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemCount(orderId: Int): Int
}