package com.pandeyganesha.kaamsutra.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "todo_tag",
    primaryKeys = ["todoId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Todo::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("todoId"), Index("tagId")]
)
data class TodoTag(
    val todoId: String,
    val tagId: String
)

data class TodoTagRow(
    val todoId: String,
    @Embedded val tag: Tag
)

@Dao
interface TodoTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(todoTag: TodoTag)

    @Delete
    suspend fun delete(todoTag: TodoTag)

    @Query("DELETE FROM todo_tag WHERE todoId = :todoId")
    suspend fun deleteAllForTodo(todoId: String)

    @Query("SELECT * FROM tags INNER JOIN todo_tag ON tags.id = todo_tag.tagId WHERE todo_tag.todoId = :todoId")
    suspend fun getTagsForTodo(todoId: String): List<Tag>

    @Query("SELECT todo_tag.todoId as todoId, tags.* FROM tags INNER JOIN todo_tag ON tags.id = todo_tag.tagId")
    fun getAllTodoTags(): Flow<List<TodoTagRow>>
}