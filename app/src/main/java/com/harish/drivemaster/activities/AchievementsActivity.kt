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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.ui.theme.DriveMasterTheme

class AchievementsActivity : AppCompatActivity() {

    private lateinit var lvAchievements: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        lvAchievements = findViewById(R.id.lvAchievements)
        loadAchievements()
    }

    private fun loadAchievements() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(it).child("badges")
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val badges = mutableListOf<String>()
                    for (badgeSnapshot in dataSnapshot.children) {
                        badges.add(badgeSnapshot.key ?: "")
                    }
                    val adapter = ArrayAdapter(this@AchievementsActivity, android.R.layout.simple_list_item_1, badges)
                    lvAchievements.adapter = adapter
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors.
                }
            })
        }
    }
}