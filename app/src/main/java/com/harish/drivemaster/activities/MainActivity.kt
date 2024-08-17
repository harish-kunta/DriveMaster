package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.harish.drivemaster.R

class MainActivity : AppCompatActivity() {

    private lateinit var listViewLessons: ListView
    private val lessonTitles = ArrayList<String>()
    private val lessonIds = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listViewLessons = findViewById(R.id.listViewLessons)

        // Fetch lessons from Firebase
        val lessonsRef = FirebaseDatabase.getInstance().getReference("lessons")
        lessonsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                lessonTitles.clear()
                lessonIds.clear()
                for (lessonSnapshot in dataSnapshot.children) {
                    val lessonId = lessonSnapshot.key ?: ""
                    val title = lessonSnapshot.child("title").getValue(String::class.java) ?: ""
                    lessonTitles.add(title)
                    lessonIds.add(lessonId)
                }

                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, lessonTitles)
                listViewLessons.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // log error
                Log.e("MainActivity", "Failed to load lessons.", databaseError.toException())
                Toast.makeText(this@MainActivity, "Failed to load lessons.", Toast.LENGTH_SHORT).show()
            }
        })

        listViewLessons.setOnItemClickListener { _, _, position, _ ->
            val selectedLessonId = lessonIds[position]
            val intent = Intent(this@MainActivity, LessonActivity::class.java)
            intent.putExtra("lessonId", selectedLessonId)
            startActivity(intent)
        }
    }
}
