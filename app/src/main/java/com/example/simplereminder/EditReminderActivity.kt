package com.example.simplereminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.simplereminder.db.AppDatabase
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditReminderActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
TimePickerDialog.OnTimeSetListener  {

    private lateinit var reminderCalender: Calendar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reminder)

        val username = intent.getStringExtra("username")
        val reminderId = intent.getStringExtra("reminderId")

        findViewById<EditText>(R.id.editReminderTime).inputType = InputType.TYPE_NULL
        findViewById<EditText>(R.id.editReminderTime).isClickable = true

        findViewById<EditText>(R.id.editReminderTime).setOnClickListener {
            reminderCalender = GregorianCalendar.getInstance()
            DatePickerDialog(
                this,
                this,
                reminderCalender.get(Calendar.YEAR),
                reminderCalender.get(Calendar.MONTH),
                reminderCalender.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

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
                reminderInfos.message =
                    findViewById<EditText>(R.id.editReminderData).text.toString()
                reminderInfos.location_x =
                    findViewById<EditText>(R.id.editLocation_x).text.toString().toFloat()
                reminderInfos.location_y =
                    findViewById<EditText>(R.id.editLocation_y).text.toString().toFloat()
                reminderInfos.reminder_time =
                    findViewById<EditText>(R.id.editReminderTime).text.toString()
                database.reminderDao().updateReminder(reminderInfos)
                database.close()

                val reminderCalendar = GregorianCalendar.getInstance()
                val dateFormat = "dd.MM.yyyy HH:mm"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val formatter = DateTimeFormatter.ofPattern(dateFormat)
                    val date = LocalDateTime.parse(reminderInfos.reminder_time, formatter)

                    reminderCalendar.set(Calendar.YEAR, date.year)
                    reminderCalendar.set(Calendar.MONTH, date.monthValue - 1)
                    reminderCalendar.set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
                    reminderCalendar.set(Calendar.HOUR_OF_DAY, date.hour)
                    reminderCalendar.set(Calendar.MINUTE, date.minute)

                } else {
                    if (dateFormat.contains(":")) {
                        val dateparts =
                            reminderInfos.reminder_time.split(" ").toTypedArray()[0].split(".")
                                .toTypedArray()
                        val timeparts =
                            reminderInfos.reminder_time.split(" ").toTypedArray()[1].split(":")
                                .toTypedArray()

                        reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                        reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                        reminderCalendar.set(Calendar.HOUR_OF_DAY, timeparts[0].toInt())
                        reminderCalendar.set(Calendar.MINUTE, timeparts[1].toInt())

                    } else {
                        val dateparts = reminderInfos.reminder_time.split(".").toTypedArray()
                        reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                        reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                    }
                }

                MainActivity.cancelReminder(applicationContext, reminderInfos.uid!!)
                // Set reminder
                if (reminderCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {
                    val message =
                        "Reminder from ${reminderInfos.creator_id}: ${reminderInfos.message}"

                    if (reminderId != null) {
                        MainActivity.setReminderWithWorkManager(
                            applicationContext,
                            reminderId.toInt(),
                            reminderCalender.timeInMillis,
                            message
                        )
                    }
                }
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.putExtra("username", username)
                startActivity(intent)
            }
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
        findViewById<EditText>(R.id.editReminderTime).setText(simpleDateFormat.format(reminderCalender.time))

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
        findViewById<EditText>(R.id.editReminderTime).setText(simpleDateFormat.format(reminderCalender.time))
    }
}