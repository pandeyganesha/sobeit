package com.pandeyganesha.kaamsutra.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [Habit::class, HabitLog::class, Todo::class, Goal::class, Tag::class, TodoTag::class], version = 21)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitLogDao(): HabitLogDao
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun goalDao(): GoalDao
    abstract fun tagDao(): TagDao
    abstract fun todoTagDao(): TodoTagDao
}