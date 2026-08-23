package com.infiniteloop.cyclefollower

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import android.view.WindowManager
import com.infiniteloop.cyclefollower.security.AppLock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infiniteloop.cyclefollower.ui.AppViewModel
import androidx.activity.compose.BackHandler
import com.infiniteloop.cyclefollower.ui.screens.CycleScreen
import com.infiniteloop.cyclefollower.ui.screens.LogScreen
import com.infiniteloop.cyclefollower.ui.screens.PlanScreen
import com.infiniteloop.cyclefollower.ui.screens.RightNowScreen
import com.infiniteloop.cyclefollower.ui.screens.LearnScreen
import com.infiniteloop.cyclefollower.ui.screens.SettingsScreen
import com.infiniteloop.cyclefollower.ui.screens.SetupScreen
import com.infiniteloop.cyclefollower.ui.screens.TodayScreen
import com.infiniteloop.cyclefollower.ui.theme.CycleFollowerTheme

class MainActivity : FragmentActivity() {
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
    PLAN("Plan", Icons.Filled.EventAvailable),
    CYCLE("Cycle", Icons.Filled.CalendarMonth),
    LEARN("Learn", Icons.Filled.MenuBook),
    SETTINGS("Settings", Icons.Filled.Settings),
}

/** Screens reached from Today rather than the bottom bar. */
private enum class Overlay { RIGHT_NOW, LOG }

@Composable
private fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val loaded = profile
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Blank the app-switcher thumbnail when asked, and stop doing so when the setting goes off.
    DisposableEffect(loaded.secureScreen, activity) {
        val window = activity?.window
        if (loaded.secureScreen) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    val lockable = activity != null && loaded.appLock && AppLock.canLock(context)
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (lockable && !unlocked) {
        LockGate(
            onPrompt = {
                AppLock.prompt(
                    activity = activity,
                    onSuccess = { unlocked = true },
                    onFailure = { },
                )
            },
        )
        return
    }

    if (!loaded.setupComplete) {
        SetupScreen(profile = loaded, viewModel = viewModel)
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }

    when (overlay) {
        Overlay.RIGHT_NOW -> {
            RightNowScreen(profile = loaded, onBack = { overlay = null })
            // Without this the system back gesture leaves the screen with no way out but the
            // on-screen button.
            BackHandler { overlay = null }
            return
        }
        Overlay.LOG -> {
            Scaffold { innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    LogScreen(profile = loaded, viewModel = viewModel)
                }
            }
            BackHandler { overlay = null }
            return
        }
        null -> Unit
    }

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
                Tab.TODAY -> TodayScreen(
                    profile = loaded,
                    viewModel = viewModel,
                    onOpenLearn = { tab = Tab.LEARN },
                    onOpenRightNow = { overlay = Overlay.RIGHT_NOW },
                    onOpenLog = { overlay = Overlay.LOG },
                )
                Tab.PLAN -> PlanScreen(profile = loaded)
                Tab.CYCLE -> CycleScreen(profile = loaded, viewModel = viewModel)
                Tab.LEARN -> LearnScreen(profile = loaded)
                Tab.SETTINGS -> SettingsScreen(profile = loaded, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LockGate(onPrompt: () -> Unit) {
    // Prompt once on arrival; the button is there for when the sheet is dismissed.
    LaunchedEffect(Unit) { onPrompt() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Cycle Follower is locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "This is her health information, on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPrompt) { Text("Unlock") }
        }
    }
}
