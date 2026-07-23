package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HydrationLog(
    val id: Long = System.nanoTime(),
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
)

