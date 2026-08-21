package com.infiniteloop.cyclefollower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.screens.CycleScreen
import com.infiniteloop.cyclefollower.ui.screens.LearnScreen
import com.infiniteloop.cyclefollower.ui.screens.SettingsScreen
import com.infiniteloop.cyclefollower.ui.screens.SetupScreen
import com.infiniteloop.cyclefollower.ui.screens.TodayScreen
import com.infiniteloop.cyclefollower.ui.theme.CycleFollowerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CycleFollowerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.WbSunny),
    CYCLE("Cycle", Icons.Filled.CalendarMonth),
    LEARN("Learn", Icons.Filled.MenuBook),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
private fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val loaded = profile

    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!loaded.setupComplete) {
        SetupScreen(profile = loaded, viewModel = viewModel)
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (tab) {
                Tab.TODAY -> TodayScreen(profile = loaded, viewModel = viewModel, onOpenLearn = { tab = Tab.LEARN })
                Tab.CYCLE -> CycleScreen(profile = loaded, viewModel = viewModel)
                Tab.LEARN -> LearnScreen(profile = loaded)
                Tab.SETTINGS -> SettingsScreen(profile = loaded, viewModel = viewModel)
            }
        }
    }
}
