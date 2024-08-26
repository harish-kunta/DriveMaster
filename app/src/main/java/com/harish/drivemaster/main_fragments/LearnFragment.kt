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
import androidx.core.content.ContextCompat
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
import com.harish.drivemaster.helpers.HapticFeedbackUtil

class LearnFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var lessonsDatabase: DatabaseReference
    private lateinit var userDatabase: DatabaseReference
    private lateinit var levelsContainer: RecyclerView
    private lateinit var streakValue: TextView
    private lateinit var heartsValue: TextView

    private var heartsLeft: Int = 0

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
        initializeFirebaseAndLoadData()
        return view
    }

    private fun initializeUIComponents(view: View) {
        levelsContainer = view.findViewById(R.id.recyclerView)
        streakValue = view.findViewById(R.id.streakValue)
        heartsValue = view.findViewById(R.id.heartsValue)
    }

    private fun initializeFirebaseAndLoadData() {
        auth = FirebaseAuth.getInstance()
        lessonsDatabase = FirebaseDatabase.getInstance().reference.child("lessons")
        userDatabase = FirebaseDatabase.getInstance().reference.child("users")

        loadUserData()
        loadLevels()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = userDatabase.child(userId)

        userRef.child("streak").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                streakValue.text = (dataSnapshot.child("currentStreak").getValue(Int::class.java) ?: 0).toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to load streak data", databaseError.toException())
            }
        })

        userRef.child("hearts").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                heartsLeft = dataSnapshot.child("heartsLeft").getValue(Int::class.java) ?: 0
                val lastRegenTime = dataSnapshot.child("lastRegenTime").getValue(Long::class.java) ?: System.currentTimeMillis()
                regenerateHeartsIfNeeded(lastRegenTime)
                updateHeartsDisplay()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to load hearts data", databaseError.toException())
            }
        })
    }

    private fun regenerateHeartsIfNeeded(lastRegenTime: Long) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastRegenTime
        val hoursElapsed = elapsedTime / (2 * 60 * 60 * 1000) // 2 hours in milliseconds

        if (hoursElapsed > 0 && heartsLeft < 5) {
            heartsLeft = minOf(heartsLeft + hoursElapsed.toInt(), 5)
            saveHeartsData(currentTime)
        }
    }

    private fun saveHeartsData(lastRegenTime: Long) {
        auth.currentUser?.uid?.let { userId ->
            userDatabase.child(userId).child("hearts").setValue(mapOf(
                "heartsLeft" to heartsLeft,
                "lastRegenTime" to lastRegenTime
            )).addOnFailureListener { exception ->
                logAndToastError("Failed to save hearts data", exception)
            }
        }
    }

    private fun loadLevels() {
        lessonsDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val levels = levelCategories.mapNotNull { (categoryName, levelRange) ->
                    val levelsInCategory = levelRange.filter { snapshot.hasChild("level$it") }
                    if (levelsInCategory.isNotEmpty()) LevelCategory(categoryName, levelsInCategory) else null
                }
                listenForUserLevelUpdates(levels)
            }

            override fun onCancelled(error: DatabaseError) {
                logAndToastError("Failed to load levels", error.toException())
            }
        })
    }

    private fun listenForUserLevelUpdates(levels: List<LevelCategory>) {
        auth.currentUser?.uid?.let { userId ->
            userDatabase.child(userId).child("completed_levels")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val completedLevels = snapshot.children.mapNotNull { it.key?.toInt() }.toSet()
                        setupRecyclerView(levels, completedLevels)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        logAndToastError("Failed to load user levels", error.toException())
                    }
                })
        }
    }

    private fun setupRecyclerView(levels: List<LevelCategory>, completedLevels: Set<Int>) {
        levelsContainer.layoutManager = LinearLayoutManager(context)
        levelsContainer.adapter = LevelCategoryAdapter(levels, completedLevels, completedLevels.maxOrNull() ?: 0) { levelId ->
            if (levelId <= (completedLevels.maxOrNull() ?: 0) + 1) {
                navigateToLessonActivity(levelId)
            } else {
                showLockedLevelPopup()
            }
        }
    }

    private fun navigateToLessonActivity(levelId: Int) {
        if (heartsLeft > 0) {
            startActivity(Intent(context, LessonActivity::class.java).apply {
                putExtra("levelId", levelId.toString())
            })
        } else {
            showNoHeartsPopup()
        }
    }

    private fun updateHeartsDisplay() {
        heartsValue.text = heartsLeft.toString()
    }

    private fun showNoHeartsPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("No Hearts Left")
            .setMessage("You don't have any hearts left to start a new lesson. Please wait or earn more hearts.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLockedLevelPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Level Locked")
            .setMessage("You need to complete the previous level to unlock this one.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logAndToastError(message: String, exception: Exception) {
        Log.e("LearnFragment", message, exception)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    data class LevelCategory(val category: String, val levels: List<Int>)

    class LevelCategoryAdapter(
        private val categories: List<LevelCategory>,
        private val completedLevels: Set<Int>,
        private val maxCompletedLevel: Int,
        private val onLevelSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<LevelCategoryAdapter.LevelCategoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelCategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return LevelCategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelCategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.bind(category, completedLevels, maxCompletedLevel, onLevelSelected)
        }

        override fun getItemCount(): Int = categories.size

        class LevelCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)
            private val levelsRecyclerView: RecyclerView = itemView.findViewById(R.id.levelsRecyclerView)

            fun bind(
                category: LevelCategory,
                completedLevels: Set<Int>,
                maxCompletedLevel: Int,
                onLevelSelected: (Int) -> Unit
            ) {
                categoryTitle.text = category.category
                levelsRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                levelsRecyclerView.adapter = LevelsAdapter(category.levels, completedLevels, maxCompletedLevel, onLevelSelected)
            }
        }
    }

    class LevelsAdapter(
        private val levels: List<Int>,
        private val completedLevels: Set<Int>,
        private val maxCompletedLevel: Int,
        private val onLevelSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<LevelsAdapter.LevelViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
            return LevelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
            val levelId = levels[position]
            holder.bind(levelId, completedLevels, maxCompletedLevel, onLevelSelected)
        }

        override fun getItemCount(): Int = levels.size

        class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val levelButton: TextView = itemView.findViewById(R.id.levelButton)
            private val levelNumber: TextView = itemView.findViewById(R.id.levelNumber)

            fun bind(
                levelId: Int,
                completedLevels: Set<Int>,
                maxCompletedLevel: Int,
                onLevelSelected: (Int) -> Unit
            ) {
                levelButton.text = "Level $levelId"
                levelNumber.text = levelId.toString()

                val context = itemView.context

                when {
                    levelId in completedLevels -> itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.completedLevelColor))
                    levelId == maxCompletedLevel + 1 -> itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.nextLevelColor))
                    else -> itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.lockedLevelColor))
                }

                itemView.setOnClickListener {
                    // Perform haptic feedback
                    HapticFeedbackUtil.performHapticFeedback(context)
                    onLevelSelected(levelId)
                }
            }
        }
    }
}

