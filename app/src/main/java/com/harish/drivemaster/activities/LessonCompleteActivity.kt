package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.harish.drivemaster.R

class LessonCompleteActivity : AppCompatActivity() {
    private lateinit var btnContinue: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_complete)

        btnContinue = findViewById(R.id.btnContinue)

        val totalPoints = intent.getIntExtra("totalPoints", 300)
        val timeTaken = intent.getLongExtra("timeTaken", 60L)
        val percentageCorrect = intent.getDoubleExtra("percentageCorrect", 100.0)
        val currentStreak = intent.getIntExtra("currentStreak", 0)
        val streakIncreasedToday = intent.getBooleanExtra("streakIncreasedToday", false)

        // Display the data in your UI components
        // Example:
        findViewById<TextView>(R.id.tvTotalPoints).text = totalPoints.toString()
        val formattedTimeTaken = formatTimeTaken((timeTaken / 1000).toInt())
        findViewById<TextView>(R.id.tvTimeTaken).text = formattedTimeTaken
        findViewById<TextView>(R.id.tvPercentageCorrect).text =
            Math.round(percentageCorrect).toString() + "%"

        btnContinue.setOnClickListener {

            if (currentStreak > 0 && streakIncreasedToday) {
                //open streak increased activity using intent
                // Pass data to LessonCompleteActivity
                val intent = Intent(this, StreakIncreasedActivity::class.java).apply {
                    putExtra("currentStreak", currentStreak)
                }
                startActivity(intent)
                finish()
            } else
                finish()
        }
    }

    fun formatTimeTaken(seconds: Int): String {
        // Calculate minutes and seconds
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        // Format the time as MM:SS
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}