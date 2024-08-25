package com.harish.drivemaster.main_fragments

import android.content.Context
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

class LearnFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var lessonsDatabase: DatabaseReference
    private lateinit var userDatabase: DatabaseReference
    private lateinit var levelsContainer: RecyclerView

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

    private fun initializeUIComponents(view: View) {
        levelsContainer = view.findViewById(R.id.recyclerView)
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        lessonsDatabase = FirebaseDatabase.getInstance().reference.child("lessons")
        userDatabase = FirebaseDatabase.getInstance().reference.child("users")
    }

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

    private fun parseLevels(snapshot: DataSnapshot): List<LevelCategory> {
        return levelCategories.mapNotNull { (categoryName, levelRange) ->
            val levelsInCategory = levelRange.mapNotNull { levelId ->
                if (snapshot.hasChild("level$levelId")) levelId else null
            }
            if (levelsInCategory.isNotEmpty()) LevelCategory(categoryName, levelsInCategory) else null
        }
    }

    private fun setupRecyclerView(levels: List<LevelCategory>) {
        levelsContainer.layoutManager = LinearLayoutManager(context)
        levelsContainer.adapter = LevelCategoryAdapter(levels) { levelId ->
            checkLevelUnlocked(levelId) { isUnlocked, isNextLevel ->
                if (isUnlocked || isNextLevel) {
                    navigateToLessonActivity(levelId)
                } else {
                    showLockedLevelPopup()
                }
            }
        }
    }

    private fun checkLevelUnlocked(levelId: Int, callback: (Boolean, Boolean) -> Unit) {
        auth.currentUser?.uid?.let { userId ->
            val userLevelsRef = userDatabase.child(userId).child("completed_levels")
            userLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completedLevels = snapshot.children.mapNotNull { it.key?.toInt() }
                    val isUnlocked = levelId in completedLevels
                    val isNextLevel = levelId == (completedLevels.maxOrNull()?.plus(1) ?: 1)

                    callback(isUnlocked, isNextLevel)
                }

                override fun onCancelled(error: DatabaseError) {
                    logAndToastError("Failed to check level status", error.toException())
                    callback(false, false)
                }
            })
        } ?: callback(false, false)
    }

    private fun navigateToLessonActivity(levelId: Int) {
        val intent = Intent(context, LessonActivity::class.java).apply {
            putExtra("levelId", levelId.toString())
        }
        startActivity(intent)
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
        private val onLevelSelected: (Int) -> Unit
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

            fun bind(category: LevelCategory, onLevelSelected: (Int) -> Unit) {
                categoryTitle.text = category.category
                levelsRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                levelsRecyclerView.adapter = LevelsAdapter(category.levels, onLevelSelected)
            }
        }
    }

    class LevelsAdapter(
        private val levels: List<Int>,
        private val onLevelSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<LevelsAdapter.LevelViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
            return LevelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
            val levelId = levels[position]
            holder.bind(levelId, onLevelSelected)
        }

        override fun getItemCount(): Int = levels.size

        class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val levelButton: TextView = itemView.findViewById(R.id.levelButton)
            private val levelNumber: TextView = itemView.findViewById(R.id.levelNumber)

            fun bind(levelId: Int, onLevelSelected: (Int) -> Unit) {
                levelButton.text = "Level $levelId"
                levelNumber.text = levelId.toString()
                itemView.setOnClickListener {
                    onLevelSelected(levelId)
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val userDatabase = FirebaseDatabase.getInstance().reference.child("users")
                userId?.let {
                    val userLevelRef = userDatabase.child(it).child("completed_levels")
                    userLevelRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val completedLevels = snapshot.children.mapNotNull { it.key?.toInt() }
                            val isCompleted = levelId in completedLevels
                            val isNextLevel = levelId == (completedLevels.maxOrNull()?.plus(1) ?: 1)

                            when {
                                isCompleted -> itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.completedLevelColor))
                                isNextLevel -> itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.completedLevelColor))
                                else -> itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.lockedLevelColor))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("LevelViewHolder", "Failed to check level completion", error.toException())
                        }
                    })
                }
            }
        }
    }
}




