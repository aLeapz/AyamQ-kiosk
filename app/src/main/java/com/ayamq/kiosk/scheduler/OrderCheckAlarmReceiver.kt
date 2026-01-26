package com.ayamq.kiosk.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ayamq.kiosk.data.database.AppDatabase
import com.ayamq.kiosk.data.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


/**
 * AlarmManager-based periodic order checker
 * Checks for pending orders at regular intervals
 * More battery-efficient than continuous Service for periodic tasks
 */
class OrderCheckAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OrderCheckAlarm"
        private const val ALARM_INTERVAL = 15 * 60 * 1000L // 15 minutes
        private const val REQUEST_CODE = 100

        /**
         * Schedule alarm to check orders periodically
         */
        fun scheduleAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, OrderCheckAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule repeating alarm
            val triggerTime = System.currentTimeMillis() + ALARM_INTERVAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle for API 23+
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Alarm scheduled for ${ALARM_INTERVAL / 1000 / 60} minutes")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Alarm triggered")

        context?.let {
            // Check orders in background
            checkPendingOrders(it)

            // Reschedule next alarm
            scheduleAlarm(it)
        }
    }

    /**
     * Check pending orders and log status
     */
    private fun checkPendingOrders(context: Context) {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)

        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = OrderRepository(database.orderDao(), database.orderItemDao())

                val pendingCount = repository.getPendingOrdersCount()
                Log.d(TAG, "Pending orders: $pendingCount")

                // Optional: Send notification if many pending orders
                if (pendingCount > 10) {
                    Log.w(TAG, "Warning: High number of pending orders ($pendingCount)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking orders", e)
            }
        }
    }
}