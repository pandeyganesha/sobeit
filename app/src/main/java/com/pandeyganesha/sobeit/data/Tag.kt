package com.pandeyganesha.sobeit.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import androidx.room.OnConflictStrategy
import androidx.room.Update


@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val sortOrder: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface TagDao {

    @Query("SELECT MAX(sortOrder) + 1 FROM tags")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createTag(tag: Tag) {
        insert(tag.copy(sortOrder = nextSortOrder()))
    }

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * from tags")
    fun getTags(): Flow<List<Tag>>

    @Update
    suspend fun updateTags(tags: List<Tag>)

}