package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.harish.drivemaster.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Layout for your splash screen

        // Delay for 3 seconds
        Handler().postDelayed({
            // Start your main activity
            startActivity(Intent(this, MainActivity::class.java))
            // Close this activity
            finish()
        }, 3000) // 3000 ms = 3 seconds
    }
}