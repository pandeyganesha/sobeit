package com.pandeyganesha.kaamsutra.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.WorkerParameters
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pandeyganesha.kaamsutra.MainActivity
import com.pandeyganesha.kaamsutra.Screen
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = DatabaseProvider.getDatabase(applicationContext)
        val habitDao = db.habitDao()
        val habitLogDao = db.habitLogDao()
        val today = LocalDate.now().toString()
        val activeHabits = habitDao.getActiveHabitsOnce()
        val todayLogs = habitLogDao.getLogsForDateOnce(today)

        val undoneHabits = activeHabits.filter { habit -> todayLogs.none {it.habitId == habit.id } }

        if (undoneHabits.isNotEmpty()) {
            val names = undoneHabits.joinToString(", ") { it.name }
            showNotification("Pending Habits", names)
        }
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "work_manager_channel"
        val notificationId = 1

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel if using Android 8.0 (Oreo) or higher
        val channel = NotificationChannel(
            channelId,
            "Remaining Habits",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications triggered by scheduled background jobs"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("notif_screen", Screen.HABITS)

        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.pandeyganesha.kaamsutra.R.mipmap.ic_launcher) // System icon for illustration
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)


        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build())
        }
    }
}

fun scheduleTestNotification(context: Context) {
    val data = workDataOf(
        "NOTIFICATION_TITLE" to "Test Reminder",
        "NOTIFICATION_MSG" to "This is a placeholder notification."
    )
    val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(calculateDelayUntil(19, 0), TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "Habit Reminder",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}