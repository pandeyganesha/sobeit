package com.pandeyganesha.kaamsutra.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.pandeyganesha.kaamsutra.ui.components.habits.RepeatType


@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sortOrder: Int? = null,
    val repeatType: RepeatType,
    val repeatDays: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: Status = Status.ACTIVE
)

@Dao
interface HabitDao {
    @Insert
    suspend fun insert(habit: Habit)

    @Query("SELECT MAX(sortOrder) + 1 FROM todos")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createHabit(habit: Habit) {
        insert(habit.copy(sortOrder = nextSortOrder()))
    }

    @Update
    suspend fun updateHabit(habit: Habit)


    @Update
    suspend fun updateHabits(habits: List<Habit>)

    @Query("UPDATE habits SET status = 'DELETED', sortOrder = -1 WHERE id = :habitId")
    suspend fun softDeleteHabit(habitId: String)

    @Query("SELECT * from habits where status = :status order by sortOrder DESC, createdAt DESC")
    fun getHabits(status: Status): Flow<List<Habit>>

    @Query("SELECT * from habits where status = 'ACTIVE' order by createdAt")
    fun getActiveHabitsOnce(): List<Habit>
}