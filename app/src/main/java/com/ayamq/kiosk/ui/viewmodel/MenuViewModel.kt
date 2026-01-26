package com.ayamq.kiosk.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ayamq.kiosk.data.database.AppDatabase
import com.ayamq.kiosk.data.database.entity.MenuEntity
import com.ayamq.kiosk.data.repository.MenuRepository
import com.ayamq.kiosk.model.CartItem
import com.ayamq.kiosk.model.Category
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MenuRepository by lazy {
        val database = AppDatabase.getDatabase(getApplication())
        MenuRepository(database.menuDao())
    }


    private val _selectedCategory = MutableLiveData(Category.PAKET)

    val filteredMenuItems: LiveData<List<MenuEntity>> =
        _selectedCategory.switchMap { category ->
            repository.getMenuItemsByCategory(category.dbValue)
        }

    private val _cartItems = MutableLiveData(mutableListOf<CartItem>())
    val cartItems: LiveData<MutableList<CartItem>> = _cartItems

    val totalPrice: LiveData<Int> =
        _cartItems.map { it.sumOf { item -> item.getSubtotal() } }


    /**
     * Select a category to filter menu items
     * @param category The category to display
     */
    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    /**
     * Add item to cart or increase quantity if already exists
     * @param menuItem The menu item to add
     */
    fun addToCart(menuItem: MenuEntity) {
        val currentCart = _cartItems.value?.toMutableList() ?: mutableListOf()

        // Check if item already in cart
        val existingItem = currentCart.find { it.menuItem.id == menuItem.id }

        if (existingItem != null) {
            // Increase quantity - create new CartItem to trigger DiffUtil
            val index = currentCart.indexOf(existingItem)
            currentCart[index] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            // Add new item
            currentCart.add(CartItem(menuItem, 1))
        }

        // Trigger LiveData update with new list
        _cartItems.value = currentCart
    }



    /**
     * Increase quantity of an item in cart
     * @param cartItem The cart item to increase
     */
    fun increaseQuantity(cartItem: CartItem) {
        val currentCart = _cartItems.value?.toMutableList() ?: return
        val index = currentCart.indexOf(cartItem)

        if (index != -1) {
            // Create new CartItem with increased quantity
            currentCart[index] = cartItem.copy(quantity = cartItem.quantity + 1)
            _cartItems.value = currentCart
        }
    }

    /**
     * Decrease quantity of an item in cart
     * Removes item if quantity reaches 0
     * @param cartItem The cart item to decrease
     */
    fun decreaseQuantity(cartItem: CartItem) {
        val currentCart = _cartItems.value?.toMutableList() ?: return
        val index = currentCart.indexOf(cartItem)

        if (index != -1) {
            if (cartItem.quantity > 1) {
                // Create new CartItem with decreased quantity
                currentCart[index] = cartItem.copy(quantity = cartItem.quantity - 1)
            } else {
                // Remove item completely
                currentCart.removeAt(index)
            }
            _cartItems.value = currentCart
        }
    }

    /**
     * Clear all items from cart
     */
    fun clearCart() {
        _cartItems.value = mutableListOf()
    }

    /**
     * Get menu items filtered by category
     * @param category The category database value (e.g., "PAKET", "ALA_CARTE")
     * @return LiveData list of menu items
     */
    fun getMenuItemsByCategory(category: String): LiveData<List<MenuEntity>> {
        return repository.getMenuItemsByCategory(category)
    }

    /**
     * Get current cart items as a list
     * @return List of cart items
     */
    fun getCartItemsList(): List<CartItem> {
        return _cartItems.value ?: emptyList()
    }

    /**
     * Insert a new menu item (Admin function)
     * @param menuItem The menu item to insert
     */
    fun insertMenuItem(menuItem: MenuEntity) = viewModelScope.launch {
        repository.insertMenuItem(menuItem)
    }


    /**
     * Delete a menu item (Admin function)
     * Uses soft delete (marks as unavailable)
     * @param menuItem The menu item to delete
     */
    fun deleteMenuItem(menuItem: MenuEntity) = viewModelScope.launch {
        repository.deleteMenuItem(menuItem)
    }

    /**
     * Restore a soft-deleted menu item (Admin function)
     * @param menuId The ID of the menu item to restore
     */
    fun restoreMenuItem(menuId: Int) = viewModelScope.launch {
        repository.restoreMenuItem(menuId)
    }

    /**
     * Get unavailable (soft-deleted) menu items by category
     * @param category The category database value
     * @return LiveData list of unavailable menu items
     */
    fun getUnavailableMenuItemsByCategory(category: String): LiveData<List<MenuEntity>> {
        return repository.getUnavailableMenuItemsByCategory(category)
    }

    /**
     * Delete all menu items (hard delete) - Admin function
     * Use with extreme caution - this permanently deletes all menu items
     */
    fun deleteAllMenuItems() = viewModelScope.launch {
        repository.deleteAllMenuItems()
    }

    /**
     * Insert all menu items
     * Used for importing Database
     */
    fun insertMenuItems(items: List<MenuEntity>) {
        viewModelScope.launch {
            repository.insertAll(items)
        }
    }
}