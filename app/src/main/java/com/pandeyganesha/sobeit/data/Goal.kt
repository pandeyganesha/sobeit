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


@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sortOrder: Int? = null,
    val dueDate: Long? = null,
    val completed: Boolean = false,
    val status: Status = Status.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface GoalDao {

    @Query("SELECT MAX(sortOrder) + 1 FROM goals")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createGoal(goal: Goal) {
        insert(goal.copy(sortOrder = nextSortOrder()))
    }

    @Update
    suspend fun updateGoal(goal: Goal)

    @Update
    suspend fun updateGoals(goals: List<Goal>)

    @Query("UPDATE goals SET status = 'DELETED', sortOrder = -1  WHERE id = :goalId")
    suspend fun softDeleteGoal(goalId: String)

    @Query("SELECT * from goals where status = :status order by sortOrder DESC, createdAt DESC")
    fun getGoals(status: Status): Flow<List<Goal>>
}