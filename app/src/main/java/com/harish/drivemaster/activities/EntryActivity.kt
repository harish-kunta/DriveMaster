package com.harish.drivemaster.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.harish.drivemaster.R

class EntryActivity : AppCompatActivity() {

    private lateinit var btnGetStarted: Button
    private lateinit var btnSignIn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entry)

        btnGetStarted = findViewById(R.id.btnGetStarted)
        btnSignIn = findViewById(R.id.btnSignIn)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnGetStarted.setOnClickListener {
            if (areNotificationsGranted()) {
                // If notifications are already granted, proceed directly to SignInActivity
                val signInIntent = Intent(this, SignInActivity::class.java)
                startActivity(signInIntent)
            } else {
                // Otherwise, open the NotificationsActivity
                val notificationIntent = Intent(this, NotificationsActivity::class.java)
                startActivity(notificationIntent)
            }
        }

        btnSignIn.setOnClickListener {
            val signInIntent = Intent(this, SignInActivity::class.java)
            startActivity(signInIntent)
        }

    }

    private fun areNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }
}