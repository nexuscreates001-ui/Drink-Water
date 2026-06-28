package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.receiver.HydrationReminderReceiver
import com.example.ui.viewmodel.WaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Collect states from ViewModel safely
    val todayTotalMl by viewModel.todayTotalMl.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val recommendedGoal by viewModel.recommendedGoal.collectAsStateWithLifecycle()

    // Notification Permission Request flow for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted! 💧", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. Enable notifications in System Settings to get water reminders.", Toast.LENGTH_LONG).show()
        }
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Header Title depending on chosen view
    val title = when (selectedTab) {
        0 -> "Droplet"
        1 -> "Insights"
        else -> "Settings"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Hydrate") },
                    modifier = Modifier.testTag("tab_home")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Insights"
                        )
                    },
                    label = { Text("Insights") },
                    modifier = Modifier.testTag("tab_insights")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    todayTotalMl = todayTotalMl,
                    profile = userProfile,
                    streak = streak,
                    onLogWater = { amount, type ->
                        viewModel.logWater(amount, type)
                    },
                    onTestNotification = {
                        requestNotificationPermission()
                        // Schedule single-shot immediate notification or trigger directly
                        val receiver = HydrationReminderReceiver()
                        val triggerIntent = android.content.Intent(context, HydrationReminderReceiver::class.java)
                        context.sendBroadcast(triggerIntent)
                    }
                )

                1 -> HistoryScreen(
                    weeklyStats = weeklyStats,
                    todayLogs = todayLogs,
                    dailyGoalMl = userProfile.dailyGoalMl,
                    onDeleteLog = { id ->
                        viewModel.deleteLog(id)
                    }
                )

                2 -> SettingsScreen(
                    profile = userProfile,
                    recommendedGoal = recommendedGoal,
                    onWeightChanged = { weight ->
                        viewModel.updateWeight(weight)
                    },
                    onActivityLevelChanged = { level ->
                        viewModel.updateActivityLevel(level)
                    },
                    onClimateChanged = { climate ->
                        viewModel.updateClimate(climate)
                    },
                    onCustomGoalToggled = { goal, enabled ->
                        viewModel.updateCustomGoal(goal, enabled)
                    },
                    onToggleReminders = { enabled ->
                        requestNotificationPermission()
                        viewModel.toggleReminders(enabled, context)
                    },
                    onReminderIntervalChanged = { minutes ->
                        viewModel.updateReminderInterval(minutes, context)
                    },
                    onScheduleHoursChanged = { wakeup, sleep ->
                        viewModel.updateScheduleTime(wakeup, sleep, context)
                    },
                    onClearHistory = {
                        viewModel.clearHistory()
                    }
                )
            }
        }
    }
}
