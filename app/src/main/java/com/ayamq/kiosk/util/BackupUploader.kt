package com.ayamq.kiosk.util

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/**
 * Utility class untuk upload backup files ke cPanel hosting
 * Menggunakan HTTP POST multipart/form-data
 */
object BackupUploader {

    private const val TAG = "BackupUploader"

    private const val UPLOAD_URL = "https://aleap.my.id/ayamq-backup/upload.php"

    private const val API_KEY = "DOkQzwp8b57PrhBdhLw04LXYrmp0XBId"

    /**
     * Upload file backup ke cPanel hosting
     * @param file File yang akan diupload
     * @param fileType Tipe file ("menu" atau "orders")
     * @return true jika berhasil, false jika gagal
     */
    fun uploadBackupFile(file: File, fileType: String): Boolean {
        if (!file.exists()) {
            Log.e(TAG, "File tidak ada: ${file.name}")
            return false
        }

        return try {
            val client = OkHttpClient()

            // Buat request body dengan multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("file_type", fileType)
                .addFormDataPart("timestamp", System.currentTimeMillis().toString())
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/json".toMediaType())
                )
                .build()

            // Buat request
            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            // Execute request
            Log.d(TAG, "Uploading ${file.name} ke $UPLOAD_URL...")

            val response = client.newCall(request).execute()

            val success = response.isSuccessful
            if (success) {
                Log.d(TAG, "Upload berhasil: ${file.name}")
            } else {
                Log.e(TAG, "Upload gagal: HTTP ${response.code}")
            }

            response.close()
            success



        } catch (e: IOException) {
            Log.e(TAG, "Error saat upload: ${e.message}", e)
            false
        }
    }

    /**
     * Upload semua backup files (menu dan orders)
     */
    fun uploadAllBackups(context: Context): BackupUploadResult {
        var menuSuccess = false
        var ordersSuccess = false

        // Upload menu backup
        val menuFile = File(context.filesDir, "menu_backup.json")
        if (menuFile.exists()) {
            menuSuccess = uploadBackupFile(menuFile, "menu")
        } else {
            Log.w(TAG, "File menu_backup.json tidak ada")
        }

        // Upload orders backup
        val ordersFile = File(context.filesDir, "orders_backup.json")
        if (ordersFile.exists()) {
            ordersSuccess = uploadBackupFile(ordersFile, "orders")
        } else {
            Log.w(TAG, "File orders_backup.json tidak ada")
        }

        return BackupUploadResult(menuSuccess, ordersSuccess)
    }
}

/**
 * Data class untuk hasil upload
 */
data class BackupUploadResult(
    val menuSuccess: Boolean,
    val ordersSuccess: Boolean
) {
    val allSuccess: Boolean
        get() = menuSuccess && ordersSuccess

    val anySuccess: Boolean
        get() = menuSuccess || ordersSuccess
}