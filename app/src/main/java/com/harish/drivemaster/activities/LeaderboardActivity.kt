package com.harish.drivemaster.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.ui.theme.DriveMasterTheme

// src/main/java/com/harish/drivemaster/LeaderboardActivity.kt
class LeaderboardActivity : AppCompatActivity() {

    private lateinit var lvLeaderboard: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        lvLeaderboard = findViewById(R.id.lvLeaderboard)
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        val leaderboardRef = FirebaseDatabase.getInstance().getReference("users").orderByChild("points").limitToLast(10)
        leaderboardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val leaderboard = mutableListOf<String>()
                for (userSnapshot in dataSnapshot.children) {
                    val userName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val points = userSnapshot.child("points").getValue(Int::class.java) ?: 0
                    leaderboard.add("$userName: $points points")
                }
                val adapter = ArrayAdapter(this@LeaderboardActivity, android.R.layout.simple_list_item_1, leaderboard)
                lvLeaderboard.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
            }
        })
    }
}
