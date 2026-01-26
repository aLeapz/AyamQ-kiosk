package com.ayamq.kiosk.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for order status updates
 * Receives broadcasts from OrderMonitorService
 * UI components register this receiver to get real-time updates
 */
class OrderStatusReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OrderStatusReceiver"
    }

    /**
     * Callback interface for handling order status changes
     */
    interface OrderStatusListener {
        fun onOrderStatusChanged(orderCount: Int)
    }

    private var listener: OrderStatusListener? = null

    /**
     * Set listener to receive callbacks
     */
    fun setListener(listener: OrderStatusListener) {
        this.listener = listener
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Broadcast received")

        if (intent?.action == "com.ayamq.kiosk.ORDER_STATUS_CHANGED") {
            val orderCount = intent.getIntExtra("order_count", 0)

            Log.d(TAG, "Order count: $orderCount")

            // Notify listener
            listener?.onOrderStatusChanged(orderCount)
        }
    }
}