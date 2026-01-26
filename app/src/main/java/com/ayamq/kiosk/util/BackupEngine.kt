package com.ayamq.kiosk.util

import android.content.Context
import android.util.Log
import com.ayamq.kiosk.data.database.AppDatabase
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BackupEngine {

    suspend fun performBackup(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)

        val menus = db.menuDao().getAllMenuItemsOnce()
        val orders = db.orderDao().getAllOrdersOnce()

        Log.d("BACKUP_DEBUG", "Menus=${menus.size}, Orders=${orders.size}")

        saveJson(context, "menu_backup.json", menus)
        saveJson(context, "orders_backup.json", orders)
    }

    private fun saveJson(context: Context, fileName: String, data: Any) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val file = File(context.filesDir, fileName)
        file.writeText(gson.toJson(data))
    }
}