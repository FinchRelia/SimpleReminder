package com.example.simplereminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.example.simplereminder.databinding.ActivityNewReminderBinding
import com.example.simplereminder.db.AppDatabase
import com.example.simplereminder.db.ReminderData
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class NewReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {
    private lateinit var binding: ActivityNewReminderBinding
    private lateinit var reminderCalender: Calendar
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_new_reminder)
        binding = ActivityNewReminderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val username = intent.getStringExtra("username")

        findViewById<Button>(R.id.mapsBtn).setOnClickListener {
            // Safe checks
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
            startActivity(Intent(applicationContext, MapsActivity::class.java)
                .putExtra("username", username)
                .putExtra("messageContent", binding.reminderData.text.toString())
                .putExtra("timeSelected", binding.reminderTime.text.toString()))
        }

        // Hide keyboard when the dateTextBox is clicked
        binding.reminderTime.inputType = InputType.TYPE_NULL
        binding.reminderTime.isClickable=true

        binding.reminderTime.setOnClickListener {
            reminderCalender = GregorianCalendar.getInstance()
            DatePickerDialog(
                this,
                this,
                reminderCalender.get(Calendar.YEAR),
                reminderCalender.get(Calendar.MONTH),
                reminderCalender.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

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
                reminder_time = binding.reminderTime.text.toString(),
                creation_time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                creator_id = username.toString(),
                reminder_seen = false
            )

            val reminderCalendar = GregorianCalendar.getInstance()
            val dateFormat = "dd.MM.yyyy HH:mm"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ofPattern(dateFormat)
                val date = LocalDateTime.parse(reminderInputs.reminder_time, formatter)

                reminderCalendar.set(Calendar.YEAR,date.year)
                reminderCalendar.set(Calendar.MONTH,date.monthValue-1)
                reminderCalendar.set(Calendar.DAY_OF_MONTH,date.dayOfMonth)
                reminderCalendar.set(Calendar.HOUR_OF_DAY,date.hour)
                reminderCalendar.set(Calendar.MINUTE,date.minute)

            } else {
                if(dateFormat.contains(":")){
                    val dateparts = reminderInputs.reminder_time.split(" ").toTypedArray()[0].split(".").toTypedArray()
                    val timeparts = reminderInputs.reminder_time.split(" ").toTypedArray()[1].split(":").toTypedArray()

                    reminderCalendar.set(Calendar.YEAR,dateparts[2].toInt())
                    reminderCalendar.set(Calendar.MONTH,dateparts[1].toInt()-1)
                    reminderCalendar.set(Calendar.DAY_OF_MONTH,dateparts[0].toInt())
                    reminderCalendar.set(Calendar.HOUR_OF_DAY, timeparts[0].toInt())
                    reminderCalendar.set(Calendar.MINUTE, timeparts[1].toInt())

                } else{
                    val dateparts = reminderInputs.reminder_time.split(".").toTypedArray()
                    reminderCalendar.set(Calendar.YEAR,dateparts[2].toInt())
                    reminderCalendar.set(Calendar.MONTH,dateparts[1].toInt()-1)
                    reminderCalendar.set(Calendar.DAY_OF_MONTH,dateparts[0].toInt())
                }
            }

            // Save values in the database
            AsyncTask.execute {
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                ).build()
                val uuid = database.reminderDao().insert(reminderInputs).toInt()
                database.close()

                // Set reminder
                if (reminderCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {
                    val message =
                        "Reminder from ${reminderInputs.creator_id}: ${reminderInputs.message}"

                    MainActivity.setReminderWithWorkManager(
                        applicationContext,
                        uuid,
                        reminderCalender.timeInMillis,
                        message
                    )
                }
            }
            finish()
        }
    }
    override fun onDateSet(
        dailogView: DatePicker?,
        selectedYear: Int,
        selectedMonth: Int,
        selectedDayOfMonth: Int
    ) {
        reminderCalender.set(Calendar.YEAR, selectedYear)
        reminderCalender.set(Calendar.MONTH, selectedMonth)
        reminderCalender.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy")
        binding.reminderTime.setText(simpleDateFormat.format(reminderCalender.time))

        TimePickerDialog(
            this,
            this,
            reminderCalender.get(Calendar.HOUR_OF_DAY),
            reminderCalender.get(Calendar.MINUTE),
            true
        ).show()
    }

    override fun onTimeSet(view: TimePicker?, selectedhourOfDay: Int, selectedMinute: Int) {
        reminderCalender.set(Calendar.HOUR_OF_DAY, selectedhourOfDay)
        reminderCalender.set(Calendar.MINUTE, selectedMinute)
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
        binding.reminderTime.setText(simpleDateFormat.format(reminderCalender.time))
    }
}