package com.example.simplereminder

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.simplereminder.db.AppDatabase

class EditReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reminder)

        val username = intent.getStringExtra("username")
        val reminderId = intent.getStringExtra("reminderId")

        // Get reminder attributes
        AsyncTask.execute {
            val database = Room
                    .databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            getString(R.string.dbFileName)
                    )
                    .build()
            val reminderInfos = database.reminderDao().getRemindersById(reminderId.toString())
            database.close()

            // Display attributes in the placeholders
            findViewById<EditText>(R.id.editReminderData).setText(reminderInfos.message)
            findViewById<EditText>(R.id.editLocation_x).setText(reminderInfos.location_x.toString())
            findViewById<EditText>(R.id.editLocation_y).setText(reminderInfos.location_y.toString())
            findViewById<EditText>(R.id.editReminderTime).setText(reminderInfos.reminder_time)
        }

        findViewById<Button>(R.id.updateReminderBtn).setOnClickListener {
            AsyncTask.execute {
                val database = Room
                    .databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        getString(R.string.dbFileName)
                    )
                    .build()
                val reminderInfos = database.reminderDao().getRemindersById(reminderId.toString())

                // Update reminder attributes
                reminderInfos.message = findViewById<EditText>(R.id.editReminderData).text.toString()
                reminderInfos.location_x = findViewById<EditText>(R.id.editLocation_x).text.toString().toFloat()
                reminderInfos.location_y = findViewById<EditText>(R.id.editLocation_y).text.toString().toFloat()
                reminderInfos.reminder_time = findViewById<EditText>(R.id.editReminderTime).text.toString()
                database.reminderDao().updateReminder(reminderInfos)
                database.close()
            }
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

    }
}