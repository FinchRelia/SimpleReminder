package com.example.simplereminder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Get logged user's name
        val username = intent.getStringExtra("username")
        findViewById<TextView>(R.id.usernameField).setText(username)

        // Go back to the home activity when hitting the button
        findViewById<Button>(R.id.homeBtn).setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java)
                .putExtra("username", username))
        }
    }
}