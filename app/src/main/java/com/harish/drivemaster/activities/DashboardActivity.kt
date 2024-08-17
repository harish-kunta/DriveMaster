package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.harish.drivemaster.R

class DashboardActivity : AppCompatActivity() {

    private lateinit var btnStartLesson: Button
    private lateinit var btnAchievements: Button
    private lateinit var btnLeaderboard: Button
    private lateinit var btnProgress: Button
    private lateinit var btnSignOut: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        btnStartLesson = findViewById(R.id.btnStartLesson)
        btnAchievements = findViewById(R.id.btnAchievements)
        btnLeaderboard = findViewById(R.id.btnLeaderboard)
        btnProgress = findViewById(R.id.btnProgress)
        btnSignOut = findViewById(R.id.btnSignOut)

        btnStartLesson.setOnClickListener {
            val levelsIntent = Intent(this, LevelsActivity::class.java)
            startActivity(levelsIntent)
        }

        btnAchievements.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        btnLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        btnProgress.setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }
        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}
