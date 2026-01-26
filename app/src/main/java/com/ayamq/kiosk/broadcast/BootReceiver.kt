package com.ayamq.kiosk.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ayamq.kiosk.service.OrderMonitorService

/**
 * BroadcastReceiver for device boot
 * Starts OrderMonitorService when device boots up
 * Ensures service continues after device restart
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting OrderMonitorService")

            context?.let {
                // Start OrderMonitorService after boot
                val serviceIntent = Intent(it, OrderMonitorService::class.java)

                // Use startForegroundService for API 26+ to handle background execution limits
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // In production, you'd want to make this a foreground service
                    // it.startForegroundService(serviceIntent)
                    it.startService(serviceIntent)
                } else {
                    it.startService(serviceIntent)
                }
            }
        }
    }
}