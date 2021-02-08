package com.example.simplereminder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast


class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        val creds: SharedPreferences = applicationContext.getSharedPreferences(getString(R.string.sharedPreference),
                Context.MODE_PRIVATE)

        findViewById<Button>(R.id.submitNewAcc).setOnClickListener {

            val username = findViewById<EditText>(R.id.newUsername).text.toString().trim()
            val pwd = findViewById<EditText>(R.id.newPassword).text.toString().trim()
            val pwdCheck = findViewById<EditText>(R.id.newPasswordConfirmation).text.toString().trim()

            if (username != "") {
                // Check is username already exists
                if (!creds.contains(username)) {
                    // Check if password is valid
                    if (pwd == pwdCheck && pwd != "" && pwdCheck != "") {
                        creds.edit().putString(username, pwd).apply()
                        creds.edit().putInt("LoginStatus", 1).apply()
                        startActivity(
                                Intent(applicationContext, MainActivity::class.java)
                                    .putExtra("username", username)
                        )
                    }
                    else {
                        notifyUser("Passwords must match!")
                    }
                }
                else {
                    notifyUser("An account with the name $username already exists!")
                }
            }
            else {
                notifyUser("Username can't be empty!")
            }
        }
    }

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}