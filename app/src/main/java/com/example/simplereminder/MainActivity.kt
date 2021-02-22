package com.example.simplereminder

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.simplereminder.databinding.ActivityMainBinding
import com.example.simplereminder.databinding.TempListDataBinding
import com.example.simplereminder.db.AppDatabase
import com.example.simplereminder.db.ReminderData


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

        // Update list each time we go to the main screen
        refreshListView()
        val username = intent.getStringExtra("username")

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->

            // Retrieve selected Item
            val selectedReminder = listView.adapter.getItem(position) as ReminderData
            val message =
                    "This reminder is due on ${selectedReminder.reminder_time}, do you want to edit or delete it?"

            // Show AlertDialog to delete the reminder
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Delete reminder?")
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
                        // Refresh payments list
                        refreshListView()
                    }

                    .setNeutralButton("Edit") { _, _ ->
                        val intent = Intent(applicationContext, EditReminderActivity::class.java)
                        intent.putExtra("reminderId", selectedReminder.uid!!.toString() )
                        intent.putExtra("username", username)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        // Do nothing
                        dialog.dismiss()
                    }
                    .show()
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

    inner class LoadRemindersEntries : AsyncTask<String?, String?, List<ReminderData>>() {
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
            if (reminderInfos != null) {
                if (reminderInfos.isNotEmpty()) {
                    val adaptor = ReminderAdaptor(applicationContext, reminderInfos)
                    listView.adapter = adaptor
                } else {
                    listView.adapter = null
                    Toast.makeText(applicationContext, "No items now", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}