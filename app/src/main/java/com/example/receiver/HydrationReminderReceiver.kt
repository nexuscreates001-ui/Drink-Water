package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.UserProfile
import com.example.data.repository.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class HydrationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val db = AppDatabase.getDatabase(context)
        val repository = WaterRepository(db.waterDao(), db.userProfileDao())

        // Use a coroutine scope to query database and schedule next
        CoroutineScope(Dispatchers.IO).launch {
            val profile = repository.getUserProfileOnce()
            
            if (profile.remindersEnabled) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                
                // Only send notification if within active wake hours
                if (currentHour in profile.wakeupTimeHour until profile.sleepTimeHour) {
                    sendHydrationNotification(context)
                }
                
                // Reschedule next check
                rescheduleNextAlarm(context, profile)
            }
        }
    }

    private fun sendHydrationNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "hydration_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders to stay hydrated throughout the day."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, flags)

        val messages = listOf(
            "Time for a sip of water! Keep your hydration streak glowing. 💧",
            "A hydrated mind is a focused mind. Grab your cup! 🥛",
            "Keep that progress ring growing! Tap to log some water. 🌊",
            "Drink some water now to hit your daily goal! 🌟",
            "Your body is calling for water. Your goal is waiting! 💧"
        )
        val message = messages.random()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use standard system icon
            .setContentTitle("Stay Hydrated! 💧")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }

    private fun rescheduleNextAlarm(context: Context, profile: UserProfile) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 100, intent, flags)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, profile.reminderIntervalMinutes)

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
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
