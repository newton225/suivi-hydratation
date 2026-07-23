package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HydrationLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydrationViewModel : ViewModel() {

    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _dailyGoal = MutableStateFlow(2000) // Default 2L as requested
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    // All logs held in memory
    private val _allLogsList = MutableStateFlow<List<HydrationLog>>(emptyList())

    // Filter today's logs in-memory based on currentDate
    val todayLogs: StateFlow<List<HydrationLog>> = combine(_allLogsList, _currentDate) { logs, date ->
        logs.filter { it.dateString == date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculate sum of water for today reactively
    val todayTotal: StateFlow<Int> = todayLogs
        .map { logs -> logs.sumOf { it.amountMl } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Add water entry with capping to the authorized limit (dailyGoal)
    fun addWater(amountMl: Int) {
        val dateString = _currentDate.value
        val currentTotal = _allLogsList.value
            .filter { it.dateString == dateString }
            .sumOf { it.amountMl }
        val goal = _dailyGoal.value
        
        if (currentTotal >= goal) {
            // Goal already reached, no more additions allowed
            return
        }
        
        val allowedAmount = if (currentTotal + amountMl > goal) {
            goal - currentTotal
        } else {
            amountMl
        }
        
        if (allowedAmount <= 0) return

        val timestamp = System.currentTimeMillis()
        val log = HydrationLog(
            amountMl = allowedAmount,
            timestamp = timestamp,
            dateString = dateString
        )
        _allLogsList.value = _allLogsList.value + log
    }

    // Reset today's logs
    fun resetToday() {
        val dateString = _currentDate.value
        _allLogsList.value = _allLogsList.value.filter { it.dateString != dateString }
    }

    // Delete a specific hydration log entry
    fun deleteLog(log: HydrationLog) {
        _allLogsList.value = _allLogsList.value.filter { it.id != log.id }
    }

    // Set custom daily goal if needed
    fun setDailyGoal(goalMl: Int) {
        _dailyGoal.value = goalMl
    }

    // Move to previous day (for testing/demoing historical dates)
    fun setCustomDate(dateString: String) {
        _currentDate.value = dateString
    }

    // Refresh to today
    fun refreshToToday() {
        _currentDate.value = getTodayDateString()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
