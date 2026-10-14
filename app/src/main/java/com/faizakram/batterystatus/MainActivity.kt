package com.faizakram.batterystatus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.faizakram.batterystatus.ui.AppRoot
import com.faizakram.batterystatus.ui.theme.BatteryStatusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BatteryStatusTheme {
                AppRoot()
            }
        }
    }
}
