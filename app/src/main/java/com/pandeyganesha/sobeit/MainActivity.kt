package com.pandeyganesha.kaamsutra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pandeyganesha.kaamsutra.ui.theme.KaamSutraTheme
import com.pandeyganesha.kaamsutra.data.Habit
import androidx.compose.runtime.Composable
import java.time.LocalDate
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandeyganesha.kaamsutra.data.scheduleTestNotification
import com.pandeyganesha.kaamsutra.data.scheduleMissedHabitSettlement
import com.pandeyganesha.kaamsutra.ui.components.AppTopBar
import com.pandeyganesha.kaamsutra.ui.components.habits.HabitScreen
import com.pandeyganesha.kaamsutra.ui.components.goals.GoalScreen
import com.pandeyganesha.kaamsutra.ui.components.todos.TodoScreen
import com.pandeyganesha.kaamsutra.ui.components.habits.RepeatType
import java.time.DayOfWeek


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // optional: handle the user's response here, if you want
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scheduleTestNotification(applicationContext)
        scheduleMissedHabitSettlement(applicationContext)
        enableEdgeToEdge()
        val screenToOpen: Screen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("notif_screen", Screen::class.java) ?: Screen.HABITS
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("notif_screen") as? Screen ?: Screen.HABITS
        }
        setContent {
            KaamSutraTheme {
                KaamSutraApp(screenToOpen)
            }
        }
    }
}

enum class Screen(val singular: String, val plural: String) {
    //    HOME("Home"),
    HABITS("Habit", "Habits"),
    GOALS("Goal", "Goals"),
    TODO("ToDo", "Todo" )
}

fun periodStartDateFor(habit: Habit, date: LocalDate): LocalDate {
    return when (habit.repeatType) {
        RepeatType.DAILY -> date
        RepeatType.WEEKLY -> date.with(DayOfWeek.MONDAY)
        RepeatType.MONTHLY -> date.withDayOfMonth(1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KaamSutraApp(screenToOpen: Screen) {

    val pagerState = rememberPagerState(
        initialPage = Screen.HABITS.ordinal,
        pageCount = { Screen.entries.size}
    )
    val currentScreen = Screen.entries[pagerState.currentPage]
    var fabAction by remember { mutableStateOf({}) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { fabAction() }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        topBar = {
            AppTopBar(
                title = currentScreen.plural,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
        ) { page ->
            when (Screen.entries[page]) {
                Screen.HABITS -> HabitScreen(
                    isActive = page == pagerState.currentPage,
                    registerFabAction = { fabAction = it },
                    modifier = Modifier.padding(innerPadding))
                Screen.GOALS -> GoalScreen(
                    isActive = page == pagerState.currentPage,
                    registerFabAction = { fabAction = it },
                    modifier = Modifier.padding(innerPadding))
                Screen.TODO -> TodoScreen(
                    isActive = page == pagerState.currentPage,
                    registerFabAction = { fabAction = it },
                    modifier = Modifier.padding(innerPadding))
            }
        }
    }
}