package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.harish.drivemaster.R

class LevelsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var levelGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        levelGrid = findViewById(R.id.levelGrid)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users").child(auth.currentUser!!.uid).child("levels")

        loadLevels()
    }
    private fun loadLevels() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                levelGrid.removeAllViews()
                snapshot.children.forEach { levelSnapshot ->
                    val levelId = levelSnapshot.key ?: return
                    val levelCompleted = levelSnapshot.child("completed").getValue(Boolean::class.java) ?: false

                    val button = Button(this@LevelsActivity).apply {
                        text = levelId.capitalize()
                        isEnabled = levelCompleted || levelId == "level1" // Only Level 1 is enabled at the start
                        setOnClickListener {
                            startLessonActivity(levelId)
                        }
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = GridLayout.LayoutParams.WRAP_CONTENT
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            setMargins(8, 8, 8, 8)
                        }

                        // Apply the background based on level completion status
                        background = resources.getDrawable(
                            if (levelCompleted) R.drawable.level_unlocked else R.drawable.level_locked,
                            null
                        )
                    }
                    levelGrid.addView(button)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
            }
        })

    }

    private fun startLessonActivity(level: String) {
        val intent = Intent(this, LessonActivity::class.java)
        intent.putExtra("levelId", level)
        startActivity(intent)
    }
}