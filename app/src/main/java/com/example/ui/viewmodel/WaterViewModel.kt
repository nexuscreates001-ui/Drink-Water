package com.example.ui.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.repository.WaterRepository
import com.example.receiver.HydrationReminderReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WaterViewModel(private val repository: WaterRepository) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    val allLogs: StateFlow<List<WaterLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Today's logs and total
    val todayLogs: StateFlow<List<WaterLog>> = allLogs.map { logs ->
        val start = getStartOfToday()
        val end = getEndOfToday()
        logs.filter { it.timestamp in start..end }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayTotalMl: StateFlow<Int> = todayLogs.map { logs ->
        logs.sumOf { it.amountMl }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Streak tracker
    val streak: StateFlow<Int> = combine(allLogs, userProfile) { logs, profile ->
        calculateStreak(logs, profile.dailyGoalMl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Weekly history stats for the last 7 days
    val weeklyStats: StateFlow<List<DayStat>> = allLogs.map { logs ->
        calculateWeeklyStats(logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Recommended goal based on profile parameters
    val recommendedGoal: StateFlow<Int> = userProfile.map { profile ->
        repository.calculateRecommendedGoal(profile.weightKg, profile.activityLevel, profile.climate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 2000
    )

    // Actions
    fun logWater(amountMl: Int, containerType: String = "Cup") {
        viewModelScope.launch {
            val log = WaterLog(amountMl = amountMl, containerType = containerType)
            repository.insertLog(log)
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    fun updateWeight(weight: Float) {
        viewModelScope.launch {
            val current = userProfile.value
            val newGoal = if (!current.customGoalEnabled) {
                repository.calculateRecommendedGoal(weight, current.activityLevel, current.climate)
            } else {
                current.dailyGoalMl
            }
            repository.saveUserProfile(
                current.copy(
                    weightKg = weight,
                    dailyGoalMl = newGoal
                )
            )
        }
    }

    fun updateActivityLevel(level: String) {
        viewModelScope.launch {
            val current = userProfile.value
            val newGoal = if (!current.customGoalEnabled) {
                repository.calculateRecommendedGoal(current.weightKg, level, current.climate)
            } else {
                current.dailyGoalMl
            }
            repository.saveUserProfile(
                current.copy(
                    activityLevel = level,
                    dailyGoalMl = newGoal
                )
            )
        }
    }

    fun updateClimate(climate: String) {
        viewModelScope.launch {
            val current = userProfile.value
            val newGoal = if (!current.customGoalEnabled) {
                repository.calculateRecommendedGoal(current.weightKg, current.activityLevel, climate)
            } else {
                current.dailyGoalMl
            }
            repository.saveUserProfile(
                current.copy(
                    climate = climate,
                    dailyGoalMl = newGoal
                )
            )
        }
    }

    fun updateCustomGoal(goalMl: Int, enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            val finalGoal = if (enabled) {
                goalMl
            } else {
                repository.calculateRecommendedGoal(current.weightKg, current.activityLevel, current.climate)
            }
            repository.saveUserProfile(
                current.copy(
                    customGoalEnabled = enabled,
                    dailyGoalMl = finalGoal
                )
            )
        }
    }

    fun toggleReminders(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(remindersEnabled = enabled))
            if (enabled) {
                scheduleSmartReminders(context, current.copy(remindersEnabled = enabled))
            } else {
                cancelSmartReminders(context)
            }
        }
    }

    fun updateReminderInterval(minutes: Int, context: Context) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(reminderIntervalMinutes = minutes)
            repository.saveUserProfile(updated)
            if (updated.remindersEnabled) {
                scheduleSmartReminders(context, updated)
            }
        }
    }

    fun updateScheduleTime(wakeupHour: Int, sleepHour: Int, context: Context) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                wakeupTimeHour = wakeupHour,
                sleepTimeHour = sleepHour
            )
            repository.saveUserProfile(updated)
            if (updated.remindersEnabled) {
                scheduleSmartReminders(context, updated)
            }
        }
    }

    // Helper functions
    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun calculateStreak(logs: List<WaterLog>, dailyGoalMl: Int): Int {
        if (logs.isEmpty()) return 0
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyTotals = logs.groupBy { sdf.format(Date(it.timestamp)) }
            .mapValues { entry -> entry.value.sumOf { it.amountMl } }

        val todayStr = sdf.format(Date())
        val cal = Calendar.getInstance()
        
        // Move back from today to find when the streak starts
        var streakCount = 0
        var isStreakActive = true
        
        // Check today's status
        val todayTotal = dailyTotals[todayStr] ?: 0
        val metToday = todayTotal >= dailyGoalMl
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        var checkDateStr = sdf.format(cal.time)
        
        if (metToday) {
            streakCount = 1
            // Walk backward for prior days
            while (isStreakActive) {
                val total = dailyTotals[checkDateStr] ?: 0
                if (total >= dailyGoalMl) {
                    streakCount++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    checkDateStr = sdf.format(cal.time)
                } else {
                    isStreakActive = false
                }
            }
        } else {
            // Check if streak was active as of yesterday
            val metYesterday = (dailyTotals[checkDateStr] ?: 0) >= dailyGoalMl
            if (metYesterday) {
                streakCount = 1
                cal.add(Calendar.DAY_OF_YEAR, -1)
                checkDateStr = sdf.format(cal.time)
                while (isStreakActive) {
                    val total = dailyTotals[checkDateStr] ?: 0
                    if (total >= dailyGoalMl) {
                        streakCount++
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        checkDateStr = sdf.format(cal.time)
                    } else {
                        isStreakActive = false
                    }
                }
            } else {
                streakCount = 0
            }
        }
        
        return streakCount
    }

    private fun calculateWeeklyStats(logs: List<WaterLog>): List<DayStat> {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfLabel = SimpleDateFormat("EEE", Locale.getDefault()) // "Mon", "Tue"
        
        // Map of date string to total logs
        val totals = logs.groupBy { sdfDate.format(Date(it.timestamp)) }
            .mapValues { entry -> entry.value.sumOf { it.amountMl } }
            
        val statsList = mutableListOf<DayStat>()
        val cal = Calendar.getInstance()
        
        // Last 7 days, including today (ends with today at index 6)
        for (i in 6 downTo 0) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdfDate.format(tempCal.time)
            val label = sdfLabel.format(tempCal.time)
            val total = totals[dateStr] ?: 0
            
            statsList.add(
                DayStat(
                    dateLabel = label,
                    amountMl = total,
                    dateString = dateStr
                )
            )
        }
        
        return statsList
    }

    // Reminders scheduling
    fun scheduleSmartReminders(context: Context, profile: UserProfile) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(context, 100, intent, flags)
        
        // Schedule first reminder 1 interval from now
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, profile.reminderIntervalMinutes)
        
        // Check if it's within wake hours, otherwise schedule for next morning
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour < profile.wakeupTimeHour || hour >= profile.sleepTimeHour) {
            calendar.set(Calendar.HOUR_OF_DAY, profile.wakeupTimeHour)
            calendar.set(Calendar.MINUTE, 0)
            if (hour >= profile.sleepTimeHour) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for newer Android standard scheduling permissions
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelSmartReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 100, intent, flags)
        alarmManager.cancel(pendingIntent)
    }
}

data class DayStat(
    val dateLabel: String,
    val amountMl: Int,
    val dateString: String
)

class WaterViewModelFactory(private val repository: WaterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
