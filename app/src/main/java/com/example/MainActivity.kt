package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.repository.WaterRepository
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WaterViewModel
import com.example.ui.viewmodel.WaterViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WaterRepository(database.waterDao(), database.userProfileDao())
        val factory = WaterViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainDashboard(
                        viewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                    )
                }
            }
        }
    }
}
