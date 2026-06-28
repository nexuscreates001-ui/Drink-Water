package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val weightKg: Float = 70f,
    val activityLevel: String = "Moderate", // "Sedentary", "Moderate", "Active"
    val climate: String = "Moderate", // "Cold", "Moderate", "Hot"
    val dailyGoalMl: Int = 2000,
    val customGoalEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 120, // Minutes
    val wakeupTimeHour: Int = 8,
    val wakeupTimeMinute: Int = 0,
    val sleepTimeHour: Int = 22,
    val sleepTimeMinute: Int = 0
)
