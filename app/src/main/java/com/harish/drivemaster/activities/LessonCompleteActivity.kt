package com.harish.drivemaster.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.ui.theme.DriveMasterTheme

class LessonCompleteActivity : AppCompatActivity() {
    private lateinit var btnContinue: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_complete)

        btnContinue = findViewById(R.id.btnContinue)
        btnContinue.setOnClickListener {
            //open main activity
            finish()
        }

        val totalPoints = intent.getIntExtra("totalPoints", 300)
        val timeTaken = intent.getLongExtra("timeTaken", 60L)
        val percentageCorrect = intent.getDoubleExtra("percentageCorrect", 100.0)

        // Display the data in your UI components
        // Example:
        findViewById<TextView>(R.id.tvTotalPoints).text = totalPoints.toString()
        val formattedTimeTaken = formatTimeTaken((timeTaken / 1000).toInt())
        findViewById<TextView>(R.id.tvTimeTaken).text = formattedTimeTaken
        findViewById<TextView>(R.id.tvPercentageCorrect).text = Math.round(percentageCorrect).toString()+"%"
    }

    fun formatTimeTaken(seconds: Int): String {
        // Calculate minutes and seconds
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        // Format the time as MM:SS
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}