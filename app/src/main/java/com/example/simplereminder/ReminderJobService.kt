package com.example.simplereminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.random.Random

// Not used anymore
class ReminderJobService: JobService() {
    var jobCancelled = false

    override fun onStartJob(params: JobParameters?): Boolean {
        val boolExtra = params?.extras!!.getBoolean("timeElapsed")

        Toast.makeText(
            applicationContext,
            "Bool value $boolExtra",
            Toast.LENGTH_SHORT
        ).show()
        if (!boolExtra) {
            return false
        }
        doBackgroundWork(params)
        return true
    }

    private fun doBackgroundWork(params: JobParameters?) {

        Thread {
            kotlin.run {
                if (jobCancelled) {
                    return@Thread
                }

                //showNotification(applicationContext, "Reminder job service scheduler")
                jobFinished(params, true)
            }
        }.start()
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobCancelled = true
        return true
    }
}