package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.LessonActivity

class LearnFragment : Fragment() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private lateinit var lessonsDatabase: DatabaseReference
    private lateinit var userDatabase: DatabaseReference

    // UI Components
    private lateinit var pathView: PathView

    // Level categories
    private val levelCategories = listOf(
        "Beginner" to 1..3,
        "Intermediate" to 4..6,
        "Advanced" to 7..9,
        "Expert" to 10..12,
        "Pro Level" to 13..15
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_learn, container, false)
        initializeUIComponents(view)
        initializeFirebase()
        loadLevels()
        return view
    }

    // Initialize UI Components
    private fun initializeUIComponents(view: View) {
        pathView = view.findViewById(R.id.pathView)
    }

    // Initialize Firebase
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        lessonsDatabase = FirebaseDatabase.getInstance().reference.child("lessons")
        userDatabase = FirebaseDatabase.getInstance().reference.child("users")
    }

    // Load levels from Firebase
    private fun loadLevels() {
        lessonsDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val levels = parseLevels(snapshot)
                    setupPathView(levels)
                } catch (e: Exception) {
                    logAndToastError("Failed to load levels", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                logAndToastError("Failed to load levels", error.toException())
            }
        })
    }

    // Parse levels from snapshot
    private fun parseLevels(snapshot: DataSnapshot): List<LevelCategory> {
        return levelCategories.mapNotNull { (categoryName, levelRange) ->
            val levelsInCategory = levelRange.mapNotNull { levelIndex ->
                val levelId = "level$levelIndex"
                if (snapshot.hasChild(levelId)) levelId else null
            }
            if (levelsInCategory.isNotEmpty()) LevelCategory(categoryName, levelsInCategory) else null
        }
    }

    // Setup PathView
    private fun setupPathView(levels: List<LevelCategory>) {
        // Get user's progress to determine completed levels
        val currentUser = auth.currentUser
        userDatabase.child(currentUser?.uid ?: "")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completedLevels = snapshot.child("completedLevels").children.mapNotNull {
                        it.key?.toIntOrNull()
                    }

                    val currentLevel = snapshot.child("currentLevel").getValue(Int::class.java) ?: 1

                    // Prepare marker data for PathView
                    val markers = mutableListOf<Marker>()

                    // Iterate through each level category
                    levels.forEach { levelCategory ->
                        levelCategory.levels.forEachIndexed { index, levelId ->
                            val levelNumber = index + 1
                            val status = when {
                                completedLevels.contains(levelNumber) -> LevelStatus.COMPLETED
                                levelNumber == currentLevel -> LevelStatus.CURRENT
                                else -> LevelStatus.LOCKED
                            }
                            markers.add(Marker(levelNumber, status))
                        }
                    }

                    // Update PathView with markers
                    pathView.setMarkers(markers)
                }

                override fun onCancelled(error: DatabaseError) {
                    logAndToastError("Failed to load user progress", error.toException())
                }
            })
    }

    // Log error and show a toast message
    private fun logAndToastError(message: String, exception: Exception) {
        Log.e("LearnFragment", message, exception)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Data class for LevelCategory
    data class LevelCategory(val category: String, val levels: List<String>)

    // Adapter for LevelCategory
    class LevelCategoryAdapter(
        private val categories: List<LevelCategory>,
        private val onLevelSelected: (String) -> Unit
    ) : RecyclerView.Adapter<LevelCategoryAdapter.LevelCategoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelCategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return LevelCategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelCategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.bind(category, onLevelSelected)
        }

        override fun getItemCount(): Int = categories.size

        class LevelCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)
            private val pathView: PathView = itemView.findViewById(R.id.pathView)

            fun bind(category: LevelCategory, onLevelSelected: (String) -> Unit) {
                categoryTitle.text = category.category

                // Prepare markers for PathView based on the status of each level
                // Fetch completed levels and current level from Firebase
                (itemView.context as? LearnFragment)?.let { fragment ->
                    fragment.getCompletedLevels { completedLevels ->
                        fragment.getCurrentLevel { currentLevel ->
                            val markers = category.levels.mapIndexed { index, levelId ->
                                val levelNumber = index + 1
                                val status = when {
                                    completedLevels.contains(levelNumber) -> LevelStatus.COMPLETED
                                    levelNumber == currentLevel -> LevelStatus.CURRENT
                                    else -> LevelStatus.LOCKED
                                }
                                Marker(levelNumber, status)
                            }

                            // Set the markers to the PathView
                            pathView.setMarkers(markers)

                            // Handle level selection
                            pathView.setOnMarkerClickListener { marker ->
                                if (marker.status == LevelStatus.CURRENT || marker.status == LevelStatus.COMPLETED) {
                                    onLevelSelected("level${marker.levelNumber}")
                                } else {
                                    showLockedLevelPopup()
                                }
                            }
                        }
                    }
                }
            }

            // Show popup for locked level
            private fun showLockedLevelPopup() {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Level Locked")
                    .setMessage("You need to complete the previous level to unlock this one.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    // Adapter for Levels
    class LevelsAdapter(
        private val levels: List<String>,
        private val onLevelSelected: (String) -> Unit
    ) : RecyclerView.Adapter<LevelsAdapter.LevelViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
            return LevelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
            val level = levels[position]
            holder.bind(level, onLevelSelected)
        }

        override fun getItemCount(): Int = levels.size

        class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val levelButton: TextView = itemView.findViewById(R.id.levelButton)

            fun bind(level: String, onLevelSelected: (String) -> Unit) {
                levelButton.text = level.capitalize()
                itemView.setOnClickListener {
                    onLevelSelected(level)
                }
            }
        }
    }

    // Extension function to get completed levels
    private fun getCompletedLevels(callback: (List<Int>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(emptyList())
        val userLevelsRef = userDatabase.child(userId).child("levels")

        userLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val completedLevels = mutableListOf<Int>()
                snapshot.children.forEach { levelSnapshot ->
                    val levelNumber = levelSnapshot.key?.removePrefix("level")?.toIntOrNull()
                    val isCompleted = levelSnapshot.child("completed").getValue(Boolean::class.java) ?: false
                    if (isCompleted && levelNumber != null) {
                        completedLevels.add(levelNumber)
                    }
                }
                callback(completedLevels)
            }

            override fun onCancelled(error: DatabaseError) {
                logAndToastError("Failed to retrieve completed levels", error.toException())
                callback(emptyList())
            }
        })
    }

    // Extension function to get current level
    private fun getCurrentLevel(callback: (Int) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(1)
        val userLevelsRef = userDatabase.child(userId).child("currentLevel")

        userLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentLevel = snapshot.getValue(Int::class.java) ?: 1
                callback(currentLevel)
            }

            override fun onCancelled(error: DatabaseError) {
                logAndToastError("Failed to retrieve current level", error.toException())
                callback(1)
            }
        })
    }
}


