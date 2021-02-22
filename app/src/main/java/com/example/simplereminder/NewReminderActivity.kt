package com.example.simplereminder

import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.example.simplereminder.databinding.ActivityNewReminderBinding
import com.example.simplereminder.db.AppDatabase
import com.example.simplereminder.db.ReminderData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NewReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewReminderBinding
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_new_reminder)
        binding = ActivityNewReminderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Go back to the main activity after typing a reminder
        val username = intent.getStringExtra("username")
        /*findViewById<Button>(R.id.submitBtn).setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java)
                .putExtra("username", username))
        }*/

        binding.submitBtn.setOnClickListener {
            // Checking entry values
            if (binding.reminderData.text.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Reminder content must be filled!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (binding.reminderTime.text.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Time must be filled!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Formatting the reminder object
            val reminderInputs = ReminderData(
                null,
                message = binding.reminderData.text.toString(),
                location_x = binding.locationX.text.toString().toFloat(),
                location_y = binding.locationY.text.toString().toFloat(),
                //location_x = binding.locationX.text.toString(),
                //location_y = binding.locationY.text.toString(),
                reminder_time = binding.reminderTime.text.toString(),
                creation_time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("M/d/y H:m")),
                creator_id = username.toString(),
                reminder_seen = false
            )

            // Save values in the database
            AsyncTask.execute {
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                ).build()
                val uuid = database.reminderDao().insert(reminderInputs).toInt()
                database.close()
            }
            finish()
        }
    }
}