package com.example.dashboard_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dashboard_app.ui.screens.DashboardScreen
import com.example.dashboard_app.ui.theme.Dashboard_appTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Dashboard_appTheme {
                DashboardScreen()
            }
        }
    }
}