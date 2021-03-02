package com.example.simplereminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.simplereminder.databinding.ActivityMainBinding
import com.example.simplereminder.databinding.TempListDataBinding
import com.example.simplereminder.db.AppDatabase
import com.example.simplereminder.db.ReminderData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var binding: ActivityMainBinding
    private lateinit var buttonData: TempListDataBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        listView = binding.listView

        val username = intent.getStringExtra("username")
        // Update list each time we go to the main screen
        refreshListView()

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->

            // Retrieve selected Item
            val selectedReminder = listView.adapter.getItem(position) as ReminderData
            val message =
                    "This reminder is due on ${selectedReminder.reminder_time}, do you want to edit or delete it?"

            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Delete or edit reminder?")
                    .setMessage(message)
                    .setPositiveButton("Delete") { _, _ ->
                        // Delete from database
                        AsyncTask.execute {
                            val db = Room
                                    .databaseBuilder(
                                            applicationContext,
                                            AppDatabase::class.java,
                                            getString(R.string.dbFileName)
                                    )
                                    .build()
                            db.reminderDao().delete(selectedReminder.uid!!)
                        }
                        // Cancel pending time based reminder
                        cancelReminder(applicationContext, selectedReminder.uid!!)
                        // Refresh payments list
                        refreshListView()
                    }
                    // Edit reminder
                    .setNeutralButton("Edit") { _, _ ->
                        val intent = Intent(applicationContext, EditReminderActivity::class.java)
                        intent.putExtra("reminderId", selectedReminder.uid!!.toString() )
                        intent.putExtra("username", username)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
        }

        // Show all reminders
        binding.showAllBtn.setOnClickListener {
            showAllReminders()
        }

        // Open NewReminderActivity when clicking the plus button
        findViewById<Button>(R.id.addBtn).setOnClickListener {
            startActivity(
                Intent(applicationContext, NewReminderActivity::class.java)
                        .putExtra("username", username)
            )
        }

        // Open ProfileActivity when clicking the profile logo
        findViewById<ImageView>(R.id.profileLogo).setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        // Set LoginStatus to 0 to log out and redirect the user to the login page
        findViewById<Button>(R.id.logOutBtn).setOnClickListener {
            applicationContext.getSharedPreferences(
                    getString(R.string.sharedPreference),
                    Context.MODE_PRIVATE
            ).edit().putInt("LoginStatus", 0).apply()
            var loginScreen = Intent(applicationContext, LoginActivity::class.java)
            startActivity(loginScreen)
        }


    }

    // When adding a new reminder update the list
    override fun onResume() {
        super.onResume()
        refreshListView()
    }

    // Load entries from the db
    private fun refreshListView() {
        var refreshTask = LoadRemindersEntries()
        refreshTask.execute()

    }

    // Load entries from the db
    private fun showAllReminders() {
        var refreshTask = LoadRemindersEntries()
        refreshTask.showAll = true
        refreshTask.execute()

    }

    inner class LoadRemindersEntries : AsyncTask<String?, String?, List<ReminderData>>() {
        var showAll: Boolean = false
        override fun doInBackground(vararg params: String?): List<ReminderData> {
            val database = Room
                .databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        getString(R.string.dbFileName)
                )
                .build()
            val reminderInfos = database.reminderDao().getReminders()
            database.close()
            return reminderInfos
        }

        override fun onPostExecute(reminderInfos: List<ReminderData>?) {
            super.onPostExecute(reminderInfos)
            var squizzedList:MutableList<ReminderData> = mutableListOf()
            if (reminderInfos != null) {
                if (reminderInfos.isNotEmpty()) {
                    if (!showAll) {
                        for (entry in reminderInfos) {
                            val reminderCalendar = GregorianCalendar.getInstance()
                            val dateFormat = "dd.MM.yyyy HH:mm"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val formatter = DateTimeFormatter.ofPattern(dateFormat)
                                val date = LocalDateTime.parse(entry.reminder_time, formatter)

                                reminderCalendar.set(Calendar.YEAR, date.year)
                                reminderCalendar.set(Calendar.MONTH, date.monthValue - 1)
                                reminderCalendar.set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
                                reminderCalendar.set(Calendar.HOUR_OF_DAY, date.hour)
                                reminderCalendar.set(Calendar.MINUTE, date.minute)

                            } else {
                                if (dateFormat.contains(":")) {
                                    val dateparts = entry.reminder_time.split(" ").toTypedArray()[0].split(".").toTypedArray()
                                    val timeparts = entry.reminder_time.split(" ").toTypedArray()[1].split(":").toTypedArray()

                                    reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                                    reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                                    reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                                    reminderCalendar.set(Calendar.HOUR_OF_DAY, timeparts[0].toInt())
                                    reminderCalendar.set(Calendar.MINUTE, timeparts[1].toInt())

                                } else {
                                    val dateparts = entry.reminder_time.split(".").toTypedArray()
                                    reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                                    reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                                    reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                                }
                            }
                            // Only add due reminders to the list
                            if (reminderCalendar.timeInMillis <= Calendar.getInstance().timeInMillis) {
                                squizzedList.add(entry)
                            }
                        }
                    }
                    else {
                        squizzedList = reminderInfos.toMutableList()
                    }
                    val adaptor = ReminderAdaptor(applicationContext, squizzedList)
                    listView.adapter = adaptor
                }
                else {
                    listView.adapter = null
                    Toast.makeText(applicationContext, "No existing items", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {

        fun showNotification(context: Context, notif: String) {

            val CHANNEL_ID = "SIMPLE_REMINDER_NOTIFICATION_CHANNEL"
            var notificationId = Random.nextInt(10, 1000) + 5
            var notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(notif)
                    .setSmallIcon(R.drawable.ic_assignment_turned_in_white_24dp)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notif))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setGroup(CHANNEL_ID)

            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Notification chancel needed since Android 8
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.app_name)
                }
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        fun setReminderWithWorkManager(
                context: Context,
                uid: Int,
                timeInMillis: Long,
                message: String
        ) {

            val reminderParameters = Data.Builder()
                    .putString("message", message)
                    .putInt("uid", uid)
                    .build()

            var minutesFromNow = 0L
            if (timeInMillis > System.currentTimeMillis())
                minutesFromNow = timeInMillis - System.currentTimeMillis()

            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(reminderParameters)
                    .setInitialDelay(minutesFromNow, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance(context).enqueue(reminderRequest)
        }

        /*fun setReminder(context: Context, uid: Int, timeInMillis: Long, message: String) {
            val intent = Intent(context, ReminderReceiver::class.java)
            intent.putExtra("uid", uid)
            intent.putExtra("message", message)

            val pendingIntent =
                    PendingIntent.getBroadcast(context, uid, intent, PendingIntent.FLAG_ONE_SHOT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC, timeInMillis, pendingIntent)
        }*/

        fun cancelReminder(context: Context, pendingIntentId: Int) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            pendingIntentId,
                            intent,
                            PendingIntent.FLAG_ONE_SHOT
                    )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}