package com.example.simplereminder.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(ReminderData::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
