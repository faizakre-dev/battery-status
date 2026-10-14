package com.faizakram.batterystatus.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.faizakram.batterystatus.ui.dashboard.DashboardScreen
import com.faizakram.batterystatus.ui.history.HistoryScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Home),
    HISTORY("History", Icons.Filled.List),
}

@Composable
fun AppRoot() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = Tab.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (tabs[selected]) {
            Tab.DASHBOARD -> DashboardScreen(modifier = contentModifier)
            Tab.HISTORY -> HistoryScreen(modifier = contentModifier)
        }
    }
}
