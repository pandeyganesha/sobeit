package com.pandeyganesha.kaamsutra.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import androidx.room.OnConflictStrategy
import androidx.room.Update


@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sortOrder: Int? = null,
    val completed: Boolean = false,
    val status: Status = Status.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface TodoDao {

    @Query("SELECT MAX(sortOrder) + 1 FROM todos")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(todo: Todo)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createTodo(todo: Todo) {
        insert(todo.copy(sortOrder = nextSortOrder()))
    }

    @Update
    suspend fun updateTodo(todo: Todo)

    @Update
    suspend fun updateTodos(todos: List<Todo>)

    @Query("UPDATE todos SET status = 'DELETED', sortOrder = -1 WHERE id = :todoId")
    suspend fun softDeleteTodo(todoId: String)

    @Query("SELECT * from todos where status = :status order by sortOrder DESC, createdAt DESC")
    fun getTodos(status: Status): Flow<List<Todo>>
}