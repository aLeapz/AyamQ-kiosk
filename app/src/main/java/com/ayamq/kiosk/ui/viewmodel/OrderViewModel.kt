package com.ayamq.kiosk.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ayamq.kiosk.data.database.AppDatabase
import com.ayamq.kiosk.data.database.entity.OrderEntity
import com.ayamq.kiosk.data.database.entity.OrderItemEntity
import com.ayamq.kiosk.data.repository.OrderRepository
import com.ayamq.kiosk.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for managing orders
 * Handles order creation, status updates, and order history
 */
class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository

    // LiveData for all orders
    val allOrders: LiveData<List<OrderEntity>>

    // LiveData for pending orders (DIPROSES)
    val pendingOrders: LiveData<List<OrderEntity>>

    // LiveData for completed orders (SELESAI)
    val completedOrders: LiveData<List<OrderEntity>>

    // Total Revenue
    val totalRevenue: LiveData<Int>

    // Status for order creation
    private val _orderCreationStatus = MutableLiveData<OrderCreationResult?>()
    val orderCreationStatus: LiveData<OrderCreationResult?> = _orderCreationStatus

    init {
        val database = AppDatabase.getDatabase(application)
        // 'repository' is initialized first
        repository = OrderRepository(database.orderDao(), database.orderItemDao())

        // 2. Now, initialize the other properties that depend on the repository
        allOrders = repository.allOrders
        pendingOrders = repository.getOrdersByStatus("DIPROSES")
        completedOrders = repository.getOrdersByStatus("SELESAI")
        totalRevenue = repository.totalRevenue
    }

    /**
     * Create a new order from cart items
     * @param cartItems List of items in the cart
     * @param totalPrice Total price of the order
     */
    fun createOrder(cartItems: List<CartItem>, totalPrice: Int) = viewModelScope.launch {
        try {
            // Create order entity (queueNumber will be set by repository)
            val order = OrderEntity(
                timestamp = System.currentTimeMillis(),
                totalPrice = totalPrice,
                status = "DIPROSES",
                queueNumber = 0  // Will be updated by repository
            )

            // Create order items from cart
            val orderItems = cartItems.map { cartItem ->
                OrderItemEntity(
                    orderId = 0, // Will be set by repository
                    menuId = cartItem.menuItem.id,
                    quantity = cartItem.quantity,
                    pricePerUnit = cartItem.menuItem.price,
                    itemName = cartItem.menuItem.name
                )
            }

            // Insert order and items
            val orderId = repository.createOrder(order, orderItems)

            // Notify success
            _orderCreationStatus.postValue(OrderCreationResult.Success(orderId.toInt()))

        } catch (e: Exception) {
            // Notify failure
            _orderCreationStatus.postValue(OrderCreationResult.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Mark order as completed
     * @param orderId The order ID
     */
    fun completeOrder(orderId: Int) = viewModelScope.launch {
        repository.updateOrderStatus(orderId, "SELESAI")
    }

    /**
     * Get order items for a specific order
     * @param orderId The order ID
     * @return LiveData list of order items
     */
    fun getOrderItems(orderId: Int): LiveData<List<OrderItemEntity>> {
        return repository.getOrderItems(orderId)
    }

    /**
     * Reset order creation status
     * Call this after handling the result
     */
    fun resetOrderCreationStatus() {
        _orderCreationStatus.value = null
    }

    /**
     * Insert a restored order into the database
     * @param order The order to be inserted
     */
    fun insertRestoredOrder(order: OrderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrder(order)
        }
    }

    /**
     * Delete all menu items (hard delete) - Admin function
     * Use with extreme caution - this permanently deletes all menu items
     */
    fun deleteAllOrders() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllOrders()
        }
    }
}

/**
 * Sealed class representing order creation result
 */
sealed class OrderCreationResult {
    data class Success(val orderId: Int) : OrderCreationResult()
    data class Error(val message: String) : OrderCreationResult()
}


