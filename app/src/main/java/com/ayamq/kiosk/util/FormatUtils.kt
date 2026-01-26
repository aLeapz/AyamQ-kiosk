package com.ayamq.kiosk.util
object FormatUtils {

    /**
     * Format price to Indonesian Rupiah format
     * @param price Price in integer
     * @return Formatted string (e.g., "Rp 10.000")
     */
    fun formatPrice(price: Int): String {
        val formatter = java.text.DecimalFormat("#,###")
        return "Rp ${formatter.format(price)}"
    }

    /**
     * Format timestamp to readable date and time
     * @param timestamp Milliseconds since epoch
     * @return Formatted string (e.g., "08 Jan 2026, 14:30")
     */
    fun formatDateTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
        return sdf.format(java.util.Date(timestamp))
    }
}