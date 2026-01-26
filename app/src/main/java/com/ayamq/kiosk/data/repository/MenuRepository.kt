package com.ayamq.kiosk.data.repository

import androidx.lifecycle.LiveData
import com.ayamq.kiosk.data.database.dao.MenuDao
import com.ayamq.kiosk.data.database.entity.MenuEntity

/**
 * Repository for Menu data
 * Abstracts data source and provides clean API to ViewModels
 *
 * Repository pattern benefits:
 * - Single source of truth
 * - Abstracts data source (Room, network, cache)
 * - Easy to test and mock
 * - Centralized data operations
 */
class MenuRepository(private val menuDao: MenuDao) {


    /**
     * Get menu items filtered by category
     * @param category The category to filter (PAKET, ALA_CARTE, SAUCE, DRINK)
     * @return LiveData list of menu items
     */
    fun getMenuItemsByCategory(category: String): LiveData<List<MenuEntity>> {
        return menuDao.getMenuItemsByCategory(category)
    }

    /**
     * Insert a new menu item
     * Runs on IO thread via suspend function
     * @return The ID of the newly inserted item
     */
    suspend fun insertMenuItem(menu: MenuEntity): Long {
        return menuDao.insert(menu)
    }

    /**
     * Delete a menu item
     * Note: Will throw exception if menu item is in any order (foreign key constraint)
     * @param menu The menu item to delete
     */
    suspend fun deleteMenuItem(menu: MenuEntity) {
        // Soft delete instead of hard delete
        menuDao.softDeleteMenu(menu.id)
    }

    /**
     * Restore a soft-deleted menu item
     * @param menuId The ID of the menu item to restore
     */
    suspend fun restoreMenuItem(menuId: Int) {
        menuDao.restoreMenu(menuId)
    }

    /**
     * Get unavailable (soft-deleted) menu items by category
     * @param category The category to filter
     * @return LiveData list of unavailable menu items
     */
    fun getUnavailableMenuItemsByCategory(category: String): LiveData<List<MenuEntity>> {
        return menuDao.getUnavailableMenuItemsByCategory(category)
    }

    /**
     * Delete all menu items (hard delete)
     * Use with caution - typically only for testing or database reset
     * This is a hard delete, not soft delete
     */
    suspend fun deleteAllMenuItems() {
        menuDao.deleteAll()
    }

    /**
     * Insert all menu items
     * Used for importing Database
     */
    suspend fun insertAll(items: List<MenuEntity>) {
        menuDao.insertAll(items)
    }
}