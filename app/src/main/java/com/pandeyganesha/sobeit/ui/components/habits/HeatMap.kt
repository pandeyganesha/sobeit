package com.pandeyganesha.kaamsutra.ui.components.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandeyganesha.kaamsutra.data.Habit
import com.pandeyganesha.kaamsutra.data.HabitLog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import java.time.DayOfWeek
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max

private val DAILY_PIXEL = 14.dp
private val WEEKLY_PIXEL = 15.dp
private val MONTHLY_PIXEL = 18.dp
private val GRID_SPACING = 2.dp

private fun periodsForDaily(today: LocalDate): List<LocalDate> {
    val start = today.minusDays(90)
    return (0..90).map { start.plusDays(it.toLong()) }
}

private fun periodsForHeatMap(repeatType: RepeatType, today: LocalDate = LocalDate.now()): List<LocalDate> =
    when (repeatType) {
        RepeatType.DAILY -> periodsForDaily(today)
        RepeatType.WEEKLY -> {
            val thisMonday = today.with(DayOfWeek.MONDAY)
            val start = thisMonday.minusWeeks(51)
            generateSequence(start) { it.plusWeeks(1) }.takeWhile { !it.isAfter(thisMonday) }.toList()
        }
        RepeatType.MONTHLY -> {
            val thisMonth = today.withDayOfMonth(1)
            (0..11).map { thisMonth.minusMonths((11 - it).toLong()) }
        }
    }

@Composable
fun HeatMapGrid(
    periods: List<LocalDate>,
    completed: Set<String>,
    pixelSize: Dp,
    spacing: Dp = GRID_SPACING,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier
        .fillMaxWidth()
//        .background(color = Color(0xFF242424))
        .padding(all = 8.dp)) {
        val density = LocalDensity.current
        val pixelPx = with(density) { pixelSize.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val availablePx = constraints.maxWidth.toFloat()

        // how many pixels fit per row, given the fixed pixel size
        val columns = max(1, floor((availablePx + spacingPx) / (pixelPx + spacingPx)).toInt())
        val rows = periods.chunked(columns)

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { date ->
                        HeatMapPixel(colorIt = date.toString() in completed, size = pixelSize)
                    }
                }
            }
        }
    }
}

@Composable
fun HeatMapBox(habit: Habit, habitLogs: List<HabitLog>, modifier: Modifier = Modifier) {
    val completed = remember(habitLogs) { habitLogs.filter { it.completed }.map { it.habitDate }.toSet() }
    val periods = remember(habit.repeatType) { periodsForHeatMap(habit.repeatType) }

    when (habit.repeatType) {
        RepeatType.DAILY   -> HeatMapGrid(periods, completed, pixelSize = DAILY_PIXEL, modifier = modifier)
        RepeatType.WEEKLY  -> HeatMapGrid(periods, completed, pixelSize = WEEKLY_PIXEL, modifier = modifier)
        RepeatType.MONTHLY -> HeatMapGrid(periods, completed, pixelSize = MONTHLY_PIXEL, modifier = modifier)
    }
}

@Composable
fun HeatMapPixel(colorIt: Boolean, size: Dp = 10.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(if (colorIt) Color(0xFF4CAF50) else Color(0xFFE3EEE1))
    )
}