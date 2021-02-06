package com.example.simplereminder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class NewReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reminder)

        // Go back to the main activity after typing a reminder
        findViewById<Button>(R.id.submitBtn).setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }
}