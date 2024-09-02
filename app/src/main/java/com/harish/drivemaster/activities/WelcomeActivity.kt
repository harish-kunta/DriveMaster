package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.harish.drivemaster.R

class WelcomeActivity : ComponentActivity() {

    private var userName: String? = null
    private lateinit var userNameTextView: TextView
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        userNameTextView = findViewById(R.id.tvUserName)
        btnContinue = findViewById(R.id.btnContinue)

        userName = intent.getStringExtra("name")

        userNameTextView.text = "Welcome, $userName!"

        btnContinue.setOnClickListener {
            openSignInPage()
        }
    }

    fun openSignInPage() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}