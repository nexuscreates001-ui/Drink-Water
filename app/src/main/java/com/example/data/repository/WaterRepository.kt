package com.example.data.repository

import com.example.data.db.WaterDao
import com.example.data.db.UserProfileDao
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WaterRepository(
    private val waterDao: WaterDao,
    private val userProfileDao: UserProfileDao
) {
    val allLogs: Flow<List<WaterLog>> = waterDao.getAllLogs()

    fun getLogsBetween(start: Long, end: Long): Flow<List<WaterLog>> {
        return waterDao.getLogsBetween(start, end)
    }

    suspend fun insertLog(log: WaterLog) {
        waterDao.insertLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        waterDao.deleteLogById(id)
    }

    suspend fun clearAllLogs() {
        waterDao.clearAllLogs()
    }

    // User Profile
    val userProfile: Flow<UserProfile> = userProfileDao.getUserProfile().map { profile ->
        profile ?: UserProfile() // Return default profile if null
    }

    suspend fun getUserProfileOnce(): UserProfile {
        return userProfileDao.getUserProfileOnce() ?: UserProfile()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    fun calculateRecommendedGoal(weightKg: Float, activityLevel: String, climate: String): Int {
        val baseWater = weightKg * 35f // 35 ml per kg
        val activityBonus = when (activityLevel) {
            "Sedentary" -> 0f
            "Moderate" -> 450f
            "Active" -> 900f
            else -> 450f
        }
        val climateBonus = when (climate) {
            "Cold" -> -200f
            "Moderate" -> 0f
            "Hot" -> 450f
            else -> 0f
        }
        val finalGoal = baseWater + activityBonus + climateBonus
        // Round to nearest 100ml, min 1000ml
        val rounded = (finalGoal / 100f).coerceAtLeast(10f).toInt() * 100
        return rounded
    }
}
