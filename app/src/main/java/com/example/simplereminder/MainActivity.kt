package com.example.simplereminder

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Message
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val username = intent.getStringExtra("username")

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
}