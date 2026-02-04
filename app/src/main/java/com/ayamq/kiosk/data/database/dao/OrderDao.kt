package com.ayamq.kiosk.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ayamq.kiosk.data.database.entity.OrderEntity

/**
 * Data Access Object for orders
 * Provides database operations for OrderEntity
 */
@Dao
interface OrderDao {

    /**
     * Insert a new order
     * @return The orderId of the newly created order
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    /**
     * Update an existing order
     * Used primarily for status updates (DIPROSES -> SELESAI)
     */
    @Update
    suspend fun update(order: OrderEntity)

    /**
     * Delete an order
     * Will also delete all associated order items (CASCADE foreign key)
     */
    @Delete
    suspend fun delete(order: OrderEntity)

    /**
     * Get all orders ordered by timestamp (newest first)
     * Returns LiveData for automatic UI updates
     */
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): LiveData<List<OrderEntity>>

    /**
     * Get orders by status
     * @param status Filter by order status (DIPROSES or SELESAI)
     */
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY timestamp DESC")
    fun getOrdersByStatus(status: String): LiveData<List<OrderEntity>>

    /**
     * Update order status
     * Convenient method for status changes without loading entire entity
     */
    @Query("UPDATE orders SET status = :newStatus WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: Int, newStatus: String)

    /**
     * Get count of pending orders (DIPROSES status)
     * Used by background service to monitor workload
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'DIPROSES'")
    suspend fun getPendingOrdersCount(): Int

    /**
     * Get the highest queue number for today's pending orders
     * Used to generate next queue number
     */
    @Query("SELECT MAX(queueNumber) FROM orders WHERE status = 'DIPROSES'")
    suspend fun getMaxQueueNumber(): Int?

    /**
     * Get all orders once (snapshot)
     * Used for backup and data export
     */
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    suspend fun getAllOrdersOnce(): List<OrderEntity>

    /**
     * Get total revenue
     * Used for view orders history
     */
    @Query("SELECT IFNULL(SUM(totalPrice), 0) FROM orders WHERE status = 'SELESAI'")
    fun getTotalRevenue(): LiveData<Int>

    /**
     * Insert a new order
     * Used for restoring orders
     */
    @Insert
    suspend fun insertOrder(order: OrderEntity)

    /**
     * Delete all orders
     * Used for deleting all the orders history in the database
     */
    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
}