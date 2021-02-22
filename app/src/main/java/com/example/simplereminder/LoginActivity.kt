package com.example.simplereminder

import android.app.KeyguardManager
import android.content.Intent
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class LoginActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        // Redirect to the sign up page
        findViewById<TextView>(R.id.createAccBtn).setOnClickListener{
            startActivity(Intent(applicationContext, SignUpActivity::class.java))
        }

        val creds:SharedPreferences = applicationContext.getSharedPreferences(getString(R.string.sharedPreference),
                Context.MODE_PRIVATE)

        // Store login credentials when hitting the login button
        findViewById<Button>(R.id.logInBtn).setOnClickListener {
            val login = findViewById<EditText>(R.id.Username).text.toString().trim()
            val pwd = findViewById<EditText>(R.id.Password).text.toString().trim()
            // Fetch all credentials from the database
            val logsTable = creds.all as MutableMap<String, String>
            if (creds.contains(login)) {
                if(logsTable.getValue(login) == pwd) {
                    creds.edit().putInt("LoginStatus", 1).apply()
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.putExtra("username", login)
                    startActivity(intent)
                }
                else {
                    notifyUser("Wrong password for user $login")
                }
            }
            else {
                notifyUser("Unknown login $login")
            }
        }

        // Biometric authentication
        checkBiometric()
        findViewById<Button>(R.id.auth_btn).setOnClickListener {
            val biometricPrompt = BiometricPrompt.Builder(this)
                .setTitle("Biometric Authentication")
                .setNegativeButton("Cancel", this.mainExecutor, DialogInterface.OnClickListener { dialog, which ->
                    notifyUser("Authentication cancelled")
                }).build()
            biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
        }
    }

    // Notification method
    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Check if fingerprint is enabled
    private fun checkBiometric(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
            notifyUser("Fingerprint authentication is disabled")
            return false
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED) {
            notifyUser("Fingerprint permission is disabled")
            return false
        }
        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true
    }

    private var cancellationSignal: CancellationSignal?= null

    // Return user cancellation if attempted
    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Authentication was cancelled by the user")
        }
        return cancellationSignal as CancellationSignal
    }

    // Get authentication status
    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() =
            @RequiresApi(Build.VERSION_CODES.P)
            object : BiometricPrompt.AuthenticationCallback() {
                // Display error message if authentication failed
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    notifyUser("Authentication error: $errString")
                }

                // Display success message if authentication passed
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    notifyUser("Authentication success!")
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("username", "admin")
                    startActivity(intent)
                    // Store authentication success
                    applicationContext.getSharedPreferences(
                        getString(R.string.sharedPreference),
                        Context.MODE_PRIVATE
                    ).edit().putInt("LoginStatus", 1).apply()
                }
            }

    // If already logged in previously restore the main activity instead
    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val loginStatus = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("LoginStatus", 0)
        if (loginStatus == 1) {
            startActivity(Intent(applicationContext, MainActivity::class.java).putExtra("username", "admin"))
        }
    }
}