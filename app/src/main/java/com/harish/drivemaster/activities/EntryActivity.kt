package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
            val notificationIntent = Intent(this, NotificationsActivity::class.java)
            startActivity(notificationIntent)
        }
    }
}