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
    private lateinit var levelsContainer: RecyclerView

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
        levelsContainer = view.findViewById(R.id.recyclerView)
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
                    setupRecyclerView(levels)
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

    // Setup RecyclerView
    private fun setupRecyclerView(levels: List<LevelCategory>) {
        levelsContainer.layoutManager = LinearLayoutManager(context)
        levelsContainer.adapter = LevelCategoryAdapter(levels) { level ->
            checkLevelUnlocked(level) { isUnlocked ->
                if (isUnlocked) {
                    navigateToLessonActivity(level)
                } else {
                    showLockedLevelPopup()
                }
            }
        }
    }

    // Check if the level is unlocked
    private fun checkLevelUnlocked(level: String, callback: (Boolean) -> Unit) {
        if (level == "level1") {
            callback(true)
            return
        }

        auth.currentUser?.uid?.let { userId ->
            val userLevelsRef = userDatabase.child(userId).child("levels").child(level)
            userLevelsRef.child("unlocked").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isUnlocked = snapshot.getValue(Boolean::class.java) ?: false
                    callback(isUnlocked)
                }

                override fun onCancelled(error: DatabaseError) {
                    logAndToastError("Failed to check level status", error.toException())
                    callback(false)
                }
            })
        } ?: callback(false)
    }

    // Navigate to LessonActivity
    private fun navigateToLessonActivity(level: String) {
        val intent = Intent(context, LessonActivity::class.java).apply {
            putExtra("levelId", level)
        }
        startActivity(intent)
    }

    // Show popup for locked level
    private fun showLockedLevelPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Level Locked")
            .setMessage("You need to complete the previous level to unlock this one.")
            .setPositiveButton("OK", null)
            .show()
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
            private val levelsRecyclerView: RecyclerView = itemView.findViewById(R.id.levelsRecyclerView)

            fun bind(category: LevelCategory, onLevelSelected: (String) -> Unit) {
                categoryTitle.text = category.category
                levelsRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                levelsRecyclerView.adapter = LevelsAdapter(category.levels, onLevelSelected)
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
}

