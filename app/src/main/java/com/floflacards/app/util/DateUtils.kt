package com.floflacards.app.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    /**
     * Formats a timestamp (Long) to a readable date and time string
     * Example: "Jan 15, 2024 14:30"
     */
    fun formatDateTime(timestamp: Long): String {
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateTimeFormat.format(Date(timestamp))
    }
}
