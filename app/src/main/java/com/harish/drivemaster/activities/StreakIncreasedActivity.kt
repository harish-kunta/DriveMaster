package com.harish.drivemaster.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.harish.drivemaster.R
import com.harish.drivemaster.models.FirebaseConstants.Companion.CURRENT_STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.LAST_ACTIVITY_DATE_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF
import java.time.LocalDate

class StreakIncreasedActivity : AppCompatActivity() {

    private lateinit var tvCurrentStreak: TextView
    private lateinit var btnContinue: Button
    private var currentStreak = 0
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var lastActivityDate: LocalDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streak_increased)

        tvCurrentStreak = findViewById(R.id.tvCurrentStreak)

        btnContinue = findViewById(R.id.btnContinue)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        currentStreak = intent.getIntExtra("currentStreak", 0)
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        lastActivityDate = today

        tvCurrentStreak.text = currentStreak.toString()

        saveStreakData()

        btnContinue.setOnClickListener {
            finish()
        }
    }

    private fun saveStreakData() {
        val userId = auth.currentUser?.uid ?: return
        val streakRef = database.child(USERS_REF).child(userId).child(STREAK_REF)

        val streakData = mapOf(
            CURRENT_STREAK_REF to currentStreak,
            LAST_ACTIVITY_DATE_REF to lastActivityDate.toString()
        )

        streakRef.setValue(streakData)
            .addOnSuccessListener {
                Log.d("StreakIncreasedActivity", "Streak data saved successfully.")
            }
            .addOnFailureListener { exception ->
                Log.e("StreakIncreasedActivity", "Failed to save streak data", exception)
            }
    }
}
