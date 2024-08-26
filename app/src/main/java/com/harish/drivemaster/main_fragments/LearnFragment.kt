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
import com.harish.drivemaster.models.FirebaseConstants.Companion.COMPLETED_LEVELS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.CURRENT_STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.HEARTS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.LESSONS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF

class LearnFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var lessonsDatabase: DatabaseReference
    private lateinit var userDatabase: DatabaseReference
    private lateinit var levelsContainer: RecyclerView
    private lateinit var streakValue: TextView
    private lateinit var heartsValue: TextView

    private var heartsLeft: Int = 0

    private val levelCategories = listOf(
        LEVEL_BEGINNER to 1..3,
        LEVEL_INTERMEDIATE to 4..6,
        LEVEL_ADVANCED to 7..9,
        LEVEL_EXPERT to 10..12,
        LEVEL_PRO to 13..15
    )

    companion object {
        private const val LOG_TAG = "LearnFragment"
        private const val MAX_HEARTS = 5
        private const val REGEN_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 hours in milliseconds
        private const val LEVEL_BEGINNER = "Beginner"
        private const val LEVEL_INTERMEDIATE = "Intermediate"
        private const val LEVEL_ADVANCED = "Advanced"
        private const val LEVEL_EXPERT = "Expert"
        private const val LEVEL_PRO = "Pro Level"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_learn, container, false)
        try {
            initializeUIComponents(view)
            initializeFirebaseAndLoadData()
        } catch (e: Exception) {
            logAndToastError("Error initializing fragment", e)
        }
        return view
    }

    private fun initializeUIComponents(view: View) {
        levelsContainer = view.findViewById(R.id.recyclerView)
        streakValue = view.findViewById(R.id.streakValue)
        heartsValue = view.findViewById(R.id.heartsValue)
    }

    private fun initializeFirebaseAndLoadData() {
        try {
            auth = FirebaseAuth.getInstance()
            lessonsDatabase = FirebaseDatabase.getInstance().reference.child(LESSONS_REF)
            userDatabase = FirebaseDatabase.getInstance().reference.child(USERS_REF)
            loadUserData()
            loadLevels()
        } catch (e: Exception) {
            logAndToastError("Error initializing Firebase", e)
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = userDatabase.child(userId)

        try {
            userRef.child(STREAK_REF).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val currentStreak =
                        dataSnapshot.child(CURRENT_STREAK_REF).getValue(Int::class.java) ?: 0
                    streakValue.text = currentStreak.toString()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    logAndToastError("Failed to load streak data", databaseError.toException())
                }
            })

            userRef.child(HEARTS_REF).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    heartsLeft = dataSnapshot.child("heartsLeft").getValue(Int::class.java) ?: 0
                    val lastRegenTime =
                        dataSnapshot.child("lastRegenTime").getValue(Long::class.java)
                            ?: System.currentTimeMillis()
                    regenerateHeartsIfNeeded(lastRegenTime)
                    updateHeartsDisplay()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    logAndToastError("Failed to load hearts data", databaseError.toException())
                }
            })
        } catch (e: Exception) {
            logAndToastError("Error loading user data", e)
        }
    }

    private fun regenerateHeartsIfNeeded(lastRegenTime: Long) {
        try {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastRegenTime
            val hoursElapsed = elapsedTime / REGEN_INTERVAL_MS

            if (hoursElapsed > 0 && heartsLeft < MAX_HEARTS) {
                heartsLeft = minOf(heartsLeft + hoursElapsed.toInt(), MAX_HEARTS)
                saveHeartsData(currentTime)
            }
        } catch (e: Exception) {
            logAndToastError("Error regenerating hearts", e)
        }
    }

    private fun saveHeartsData(lastRegenTime: Long) {
        val userId = auth.currentUser?.uid ?: return

        try {
            val heartsData = mapOf(
                "heartsLeft" to heartsLeft,
                "lastRegenTime" to lastRegenTime
            )

            userDatabase.child(userId).child(HEARTS_REF).setValue(heartsData)
                .addOnSuccessListener {
                    Log.d(LOG_TAG, "Hearts data saved successfully.")
                }
                .addOnFailureListener { exception ->
                    logAndToastError("Failed to save hearts data", exception)
                }
        } catch (e: Exception) {
            logAndToastError("Error saving hearts data", e)
        }
    }

    private fun loadLevels() {
        try {
            lessonsDatabase.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val levels = levelCategories.mapNotNull { (categoryName, levelRange) ->
                            val levelsInCategory =
                                levelRange.filter { snapshot.hasChild("level$it") }
                            if (levelsInCategory.isNotEmpty()) LevelCategory(
                                categoryName,
                                levelsInCategory
                            ) else null
                        }
                        listenForUserLevelUpdates(levels)
                    } catch (e: Exception) {
                        logAndToastError("Failed to parse levels", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    logAndToastError("Failed to load levels", error.toException())
                }
            })
        } catch (e: Exception) {
            logAndToastError("Error loading levels", e)
        }
    }

    private fun listenForUserLevelUpdates(levels: List<LevelCategory>) {
        val userId = auth.currentUser?.uid ?: return

        try {
            userDatabase.child(userId).child(COMPLETED_LEVELS_REF)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val completedLevels =
                                snapshot.children.mapNotNull { it.key?.toInt() }.toSet()
                            setupRecyclerView(levels, completedLevels)
                        } catch (e: Exception) {
                            logAndToastError("Failed to parse completed levels", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        logAndToastError("Failed to load user levels", error.toException())
                    }
                })
        } catch (e: Exception) {
            logAndToastError("Error listening for user level updates", e)
        }
    }

    private fun setupRecyclerView(levels: List<LevelCategory>, completedLevels: Set<Int>) {
        try {
            levelsContainer.layoutManager = LinearLayoutManager(context)
            val maxCompletedLevel = completedLevels.maxOrNull() ?: 0
            levelsContainer.adapter =
                LevelCategoryAdapter(levels, completedLevels, maxCompletedLevel) { levelId ->
                    if (levelId <= maxCompletedLevel + 1) {
                        navigateToLessonActivity(levelId)
                    } else {
                        showLockedLevelPopup()
                    }
                }
        } catch (e: Exception) {
            logAndToastError("Error setting up RecyclerView", e)
        }
    }

    private fun navigateToLessonActivity(levelId: Int) {
        try {
            if (heartsLeft > 0) {
                startActivity(Intent(context, LessonActivity::class.java).apply {
                    putExtra("levelId", levelId.toString())
                })
            } else {
                showNoHeartsPopup()
            }
        } catch (e: Exception) {
            logAndToastError("Error navigating to lesson activity", e)
        }
    }

    private fun updateHeartsDisplay() {
        heartsValue.text = heartsLeft.toString()
    }

    private fun showNoHeartsPopup() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle("No Hearts Left")
                .setMessage("You don't have any hearts left to start a new lesson. Please wait or earn more hearts.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            logAndToastError("Error showing no hearts popup", e)
        }
    }

    private fun showLockedLevelPopup() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Level Locked")
                .setMessage("You need to complete the previous level to unlock this one.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            logAndToastError("Error showing locked level popup", e)
        }
    }

    private fun logAndToastError(message: String, exception: Exception) {
        Log.e(LOG_TAG, message, exception)
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
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return LevelCategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelCategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.bind(category, completedLevels, maxCompletedLevel, onLevelSelected)
        }

        override fun getItemCount(): Int = categories.size

        class LevelCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)
            private val levelsRecyclerView: RecyclerView =
                itemView.findViewById(R.id.levelsRecyclerView)

            fun bind(
                category: LevelCategory,
                completedLevels: Set<Int>,
                maxCompletedLevel: Int,
                onLevelSelected: (Int) -> Unit
            ) {
                categoryTitle.text = category.category
                levelsRecyclerView.layoutManager =
                    LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                levelsRecyclerView.adapter = LevelsAdapter(
                    category.levels,
                    completedLevels,
                    maxCompletedLevel,
                    onLevelSelected
                )
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
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
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
                    levelId in completedLevels -> itemView.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.completedLevelColor
                        )
                    )

                    levelId == maxCompletedLevel + 1 -> itemView.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.nextLevelColor
                        )
                    )

                    else -> itemView.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.lockedLevelColor
                        )
                    )
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


