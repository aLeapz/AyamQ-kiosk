package com.ayamq.kiosk.data.repository

import androidx.lifecycle.LiveData
import com.ayamq.kiosk.data.database.dao.OrderDao
import com.ayamq.kiosk.data.database.dao.OrderItemDao
import com.ayamq.kiosk.data.database.entity.OrderEntity
import com.ayamq.kiosk.data.database.entity.OrderItemEntity

/**
 * Repository for Order data
 * Handles both orders and order items
 * Coordinates multiple DAOs for complex operations
 */
class OrderRepository(
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao
) {

    /**
     * Get all orders as LiveData
     * Orders are sorted by timestamp (newest first)
     */
    val allOrders: LiveData<List<OrderEntity>> = orderDao.getAllOrders()

    /**
     * Get orders filtered by status
     * @param status Order status (DIPROSES or SELESAI)
     */
    fun getOrdersByStatus(status: String): LiveData<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(status)
    }

    /**
     * Get total revenue
     * Used for view orders history
     */
    val totalRevenue: LiveData<Int> = orderDao.getTotalRevenue()

    /**
     * Create a new order with items
     * This is a transaction - all or nothing
     * @param order The order entity
     * @param orderItems List of order items
     * @return The orderId of the created order
     */
    suspend fun createOrder(order: OrderEntity, orderItems: List<OrderItemEntity>): Long {
        // Get next queue number
        val maxQueue = orderDao.getMaxQueueNumber() ?: 0
        val nextQueueNumber = maxQueue + 1

        // Create order with queue number
        val orderWithQueue = order.copy(queueNumber = nextQueueNumber)

        // Insert order first to get orderId
        val orderId = orderDao.insert(orderWithQueue)

        // Update order items with the correct orderId
        val itemsWithOrderId = orderItems.map { it.copy(orderId = orderId.toInt()) }

        // Insert all order items
        orderItemDao.insertAll(itemsWithOrderId)

        return orderId
    }

    /**
     * Update order status
     * @param orderId The ID of the order to update
     * @param newStatus The new status (SELESAI, etc.)
     */
    suspend fun updateOrderStatus(orderId: Int, newStatus: String) {
        orderDao.updateOrderStatus(orderId, newStatus)
    }

    /**
     * Get order items for a specific order
     * @param orderId The order ID
     * @return LiveData list of order items
     */
    fun getOrderItems(orderId: Int): LiveData<List<OrderItemEntity>> {
        return orderItemDao.getOrderItemsByOrderId(orderId)
    }

    /**
     * Inserts a single pre-existing order entity into the database.
     * Used for restoring from a backup.
     * @param order The complete order entity to insert.
     */
    suspend fun insertOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
    }

    /**
     * Get count of pending orders
     * Used by background service to monitor workload
     * @return Number of orders with DIPROSES status
     */
    suspend fun getPendingOrdersCount(): Int {
        return orderDao.getPendingOrdersCount()
    }

    /**
     * Delete all orders (hard delete) - Admin function
     * Use with caution - typically only for testing or database reset
     * This is a hard delete, not soft delete
     */
    suspend fun deleteAllOrders() {
        orderDao.deleteAllOrders()
        orderDao.resetOrderIdSequence()
    }
}