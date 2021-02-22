package com.example.simplereminder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminderInfo")
    fun getReminders(): List<ReminderData>

    @Query("SELECT * FROM reminderInfo WHERE uid= :id")
    fun getRemindersById(id: String): ReminderData

    @Insert
    fun insert(reminderInfo: ReminderData): Long

    @Query("DELETE FROM reminderInfo WHERE uid = :id")
    fun delete(id: Int)

    @Update
    fun updateReminder(vararg reminder: ReminderData)
}