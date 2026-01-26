package com.ayamq.kiosk.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ayamq.kiosk.data.database.entity.MenuEntity

/**
 * Data Access Object for menu items
 * Provides database operations for MenuEntity
 */
@Dao
interface MenuDao {

    /**
     * Insert a new menu item
     * @return The row ID of the newly inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menu: MenuEntity): Long

    /**
     * Get menu items by category
     * @param category The category to filter (PAKET, ALA_CARTE, SAUCE, DRINK)
     */
    @Query("SELECT * FROM menu_items WHERE category = :category AND isAvailable = 1 ORDER BY name")
    fun getMenuItemsByCategory(category: String): LiveData<List<MenuEntity>>


    /**
     * Soft delete a menu item (mark as unavailable)
     * This allows items to be restored later
     */
    @Query("UPDATE menu_items SET isAvailable = 0 WHERE id = :menuId")
    suspend fun softDeleteMenu(menuId: Int)

    /**
     * Restore a soft-deleted menu item (mark as available)
     */
    @Query("UPDATE menu_items SET isAvailable = 1 WHERE id = :menuId")
    suspend fun restoreMenu(menuId: Int)

    /**
     * Get unavailable (soft-deleted) menu items by category
     * Used for restore functionality
     */
    @Query("SELECT * FROM menu_items WHERE category = :category AND isAvailable = 0 ORDER BY name")
    fun getUnavailableMenuItemsByCategory(category: String): LiveData<List<MenuEntity>>


    /**
     * Delete all menu items
     * Use with caution - typically only for testing or database reset
     */
    @Query("DELETE FROM menu_items")
    suspend fun deleteAll()

    /**
     * Get all menu items once
     * Used for Database Backup
     */
    @Query("SELECT * FROM menu_items ORDER BY category, name")
    suspend fun getAllMenuItemsOnce(): List<MenuEntity>

    /**
     * Insert all menu items
     * Used for importing Database
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(menuItems: List<MenuEntity>)
}