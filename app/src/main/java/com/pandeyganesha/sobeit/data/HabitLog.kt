package com.pandeyganesha.kaamsutra.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import androidx.room.OnConflictStrategy
import androidx.room.Index


@Entity(
    tableName = "habit_log",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"])
    ]
)
data class HabitLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val habitDate: String,
    val completed: Boolean
)

@Dao
interface HabitLogDao {

    @Query("SELECT * from habit_log where habitDate = :date")
    fun getLogsFor(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_log WHERE habitId = :habitId AND habitDate = :date LIMIT 1")
    suspend fun getLogsFor(habitId: String, date: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog)

    suspend fun upsertLog(newLog: HabitLog) {
        val existing = getLogsFor(newLog.habitId, newLog.habitDate)
        val logToSave = if (existing != null) newLog.copy(id = existing.id) else newLog
        insertLog(logToSave)
    }

    @Query("SELECT * FROM habit_log WHERE habitDate = :date")
    suspend fun getLogsForDateOnce(date: String): List<HabitLog>

    @Query("SELECT * FROM habit_log WHERE habitDate IN (:dates)")
    fun getLogsForDates(dates: List<String>): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_log WHERE habitDate >= :sinceDate")
    fun getLogsSince(sinceDate: String): Flow<List<HabitLog>>

}
