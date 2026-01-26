package com.ayamq.kiosk.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.ayamq.kiosk.data.database.AppDatabase
import com.ayamq.kiosk.data.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.os.HandlerThread
import com.ayamq.kiosk.broadcast.OrderStatusReceiver

/**
 * Background service that monitors order status
 * Runs continuously and sends broadcasts when order status changes
 *
 * This service demonstrates:
 * - Android Service component
 * - Background thread handling with HandlerThread
 * - BroadcastReceiver integration
 * - Database operations in background
 */
class OrderMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: OrderRepository
    private lateinit var serviceHandler: Handler
    private lateinit var handlerThread: HandlerThread
    private var isRunning = false

    // Check interval: 30 seconds
    private val CHECK_INTERVAL = 30_000L

    // Track previous pending orders count
    private var previousPendingCount = 0

    companion object {
        private const val TAG = "OrderMonitorService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize repository
        val database = AppDatabase.getDatabase(applicationContext)
        repository = OrderRepository(database.orderDao(), database.orderItemDao())

        // Initialize HandlerThread for background work
        handlerThread = HandlerThread("OrderMonitorThread").apply {
            start()
        }
        serviceHandler = Handler(handlerThread.looper)

        isRunning = true
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY // Service will be restarted if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This is a started service, not bound
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isRunning = false
        handlerThread.quitSafely()
        serviceHandler.removeCallbacks(monitorRunnable)
        serviceJob.cancel()
    }

    /**
     * Start monitoring orders periodically
     */
    private fun startMonitoring() {
        serviceHandler.post(monitorRunnable)
    }

    /**
     * Runnable that checks order status periodically
     */
    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkOrderStatus()
                // Schedule next check
                serviceHandler.postDelayed(this, CHECK_INTERVAL)
            }
        }
    }

    /**
     * Check order status and send broadcast if changed
     */
    private fun checkOrderStatus() {
        serviceScope.launch {
            try {
                // Get current pending orders count
                val currentPendingCount = repository.getPendingOrdersCount()

                Log.d(TAG, "Checking orders: $currentPendingCount pending orders")

                // If count changed, send broadcast
                if (currentPendingCount != previousPendingCount) {
                    Log.d(TAG, "Order count changed: $previousPendingCount -> $currentPendingCount")
                    sendOrderStatusBroadcast(currentPendingCount)
                    previousPendingCount = currentPendingCount
                }

                // Simulate auto-complete for demo purposes (optional)
                // In real scenario, orders would be completed by kitchen staff
                // autoCompleteOldOrders()

            } catch (e: Exception) {
                Log.e(TAG, "Error checking order status", e)
            }
        }
    }

    /**
     * Send broadcast when order status changes
     */
    private fun sendOrderStatusBroadcast(orderCount: Int) {
        val intent = Intent(applicationContext, OrderStatusReceiver::class.java)
        intent.action = "com.ayamq.kiosk.ORDER_STATUS_CHANGED"
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent: $orderCount orders")
    }
}