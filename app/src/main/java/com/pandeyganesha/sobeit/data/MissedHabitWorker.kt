package com.pandeyganesha.kaamsutra.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import com.pandeyganesha.kaamsutra.ui.components.habits.RepeatType
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.time.Month


class MissedHabitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = DatabaseProvider.getDatabase(applicationContext)
        val habitDao = db.habitDao()
        val habitLogDao = db.habitLogDao()
        val today = LocalDate.now()
        val activeHabits = habitDao.getActiveHabitsOnce()

        // Settle Daily habits
        settlePeriod(
            habits = activeHabits.filter { it.repeatType == RepeatType.DAILY },
            periodDate = today.minusDays(1),
            habitLogDao = habitLogDao
        )

        // Settle weekly habits
        if (today.dayOfWeek == DayOfWeek.MONDAY) {
            settlePeriod(
                habits = activeHabits.filter { it.repeatType == RepeatType.WEEKLY },
                periodDate = today.minusWeeks(1), // Monday of the week that ended
                habitLogDao = habitLogDao
            )
        }

        // Settle monthly
        if (today.dayOfMonth == 1) {
            settlePeriod(
                habits = activeHabits.filter { it.repeatType == RepeatType.MONTHLY },
                periodDate = today.minusMonths(1).withDayOfMonth(1),
                habitLogDao = habitLogDao
            )
        }

        return Result.success()
    }
}

fun scheduleMissedHabitSettlement(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<MissedHabitWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(calculateDelayUntil(0, 5), TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "missed_habit_settlement",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}

private suspend fun settlePeriod(
    habits: List<Habit>,
    periodDate: LocalDate,
    habitLogDao: HabitLogDao
){
    if (habits.isEmpty()) return

    val periodDateStr = periodDate.toString()
    val existingLogs = habitLogDao.getLogsForDateOnce(periodDateStr)

    val missedHabits = habits.filter { habit ->
        existingLogs.none { it.habitId == habit.id }
    }
    missedHabits.forEach { habit ->
        habitLogDao.insertLog(
            HabitLog(
                habitId = habit.id,
                habitDate = periodDateStr,
                completed = false
            )
        )
    }
}