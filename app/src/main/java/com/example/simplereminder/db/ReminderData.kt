package com.example.simplereminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminderInfo")
data class ReminderData(
    @PrimaryKey(autoGenerate = true) var uid: Int?,
    @ColumnInfo(name = "message") var message: String,
    @ColumnInfo(name = "location_x") var location_x: Float,
    @ColumnInfo(name = "location_y") var location_y: Float,
    @ColumnInfo(name= "reminder_time") var reminder_time: String,
    @ColumnInfo(name = "creation_time") val creation_time: String,
    @ColumnInfo(name= "creator_id") var creator_id: String,
    @ColumnInfo(name = "reminder_seen") var reminder_seen: Boolean
)