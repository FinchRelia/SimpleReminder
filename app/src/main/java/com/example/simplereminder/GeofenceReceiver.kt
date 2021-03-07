package com.example.simplereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

private val TAG = "my custom log"

class GeofenceReceiver: BroadcastReceiver() {
    lateinit var text: String
    lateinit var coordinates: String
    lateinit var timing: String

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            Log.d(TAG, "context not null")
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                Log.d(TAG, "Geofence reached")

                // Retrieve data from intent
                if (intent != null) {
                    Log.d(TAG, "intent not null")
                    text = intent.getStringExtra("message")!!
                    coordinates = intent.getStringExtra("coordinates")!!
                    timing = intent.getStringExtra("timing")!!
                }

                // Compare reminder time to actual time
                val reminderCalendar = GregorianCalendar.getInstance()
                val dateFormat = "dd.MM.yyyy HH:mm"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val formatter = DateTimeFormatter.ofPattern(dateFormat)
                    val date = LocalDateTime.parse(timing, formatter)

                    reminderCalendar.set(Calendar.YEAR, date.year)
                    reminderCalendar.set(Calendar.MONTH, date.monthValue - 1)
                    reminderCalendar.set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
                    reminderCalendar.set(Calendar.HOUR_OF_DAY, date.hour)
                    reminderCalendar.set(Calendar.MINUTE, date.minute)

                } else {
                    if (dateFormat.contains(":")) {
                        val dateparts = timing.split(" ").toTypedArray()[0].split(".").toTypedArray()
                        val timeparts = timing.split(" ").toTypedArray()[1].split(":").toTypedArray()

                        reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                        reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                        reminderCalendar.set(Calendar.HOUR_OF_DAY, timeparts[0].toInt())
                        reminderCalendar.set(Calendar.MINUTE, timeparts[1].toInt())

                    } else {
                        val dateparts = timing.split(".").toTypedArray()
                        reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                        reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                    }
                }

                if (reminderCalendar.timeInMillis <= System.currentTimeMillis()) {
                    Log.d(TAG, "notif")
                    MapsActivity.showNotification(context.applicationContext, "Message: $text\n@$coordinates")
                }

                // Remove geofence
                val triggeringGeofences = geofencingEvent.triggeringGeofences
                MapsActivity.removeGeofences(context, triggeringGeofences)
            }
            else{
                Log.d(TAG, "Geofence not reached")
            }
        }
    }
}