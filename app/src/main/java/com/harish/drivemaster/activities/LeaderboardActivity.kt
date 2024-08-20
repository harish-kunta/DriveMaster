package com.harish.drivemaster.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var lvLeaderboard: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        lvLeaderboard = findViewById(R.id.lvLeaderboard)
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        val leaderboardRef =
            FirebaseDatabase.getInstance().getReference("users").orderByChild("points")
                .limitToLast(10)
        leaderboardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val leaderboard = mutableListOf<String>()
                for (userSnapshot in dataSnapshot.children) {
                    val userName =
                        userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val points = userSnapshot.child("points").getValue(Int::class.java) ?: 0
                    leaderboard.add("$userName: $points points")
                }
                val adapter = ArrayAdapter(
                    this@LeaderboardActivity,
                    android.R.layout.simple_list_item_1,
                    leaderboard
                )
                lvLeaderboard.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
            }
        })
    }
}
