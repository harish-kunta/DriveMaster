package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

// LearnFragment.kt
class LearnFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var levelsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_learn, container, false)

        val levels = listOf(
            LevelCategory("Beginner", listOf("Level 1", "Level 2", "Level 3")),
            LevelCategory("Intermediate", listOf("Level 4", "Level 5", "Level 6")),
            LevelCategory("Advanced", listOf("Level 7", "Level 8", "Level 9")),
            LevelCategory("Expert", listOf("Level 10", "Level 11", "Level 12")),
            LevelCategory("Pro Level", listOf("Level 13", "Level 14", "Level 15"))
        )

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = LevelCategoryAdapter(levels) { level ->
            Toast.makeText(context, "Selected $level", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    data class LevelCategory(val category: String, val levels: List<String>)


    private fun loadLevels() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                levelsContainer.removeAllViews()
                val levelGroups = listOf(
                    "Beginner" to 1..3,
                    "Intermediate" to 4..6,
                    "Advanced" to 7..9,
                    "Expert" to 10..12,
                    "Pro Level" to 13..15
                )

                levelGroups.forEach { (groupName, levelRange) ->
                    // Create group header
                    val groupHeader = TextView(requireContext()).apply {
                        text = groupName
                        textSize = 22f
                        setPadding(16, 16, 16, 8)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    levelsContainer.addView(groupHeader)

                    // Create grid layout for levels
                    val gridLayout = GridLayout(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        columnCount = 3
                        rowCount =
                            (levelRange.count() + 2) / 3 // Adjust row count based on number of levels
                    }

                    levelRange.forEach { levelIndex ->
                        val levelId = "level$levelIndex"
                        val levelSnapshot = snapshot.child(levelId)
                        val levelCompleted =
                            levelSnapshot.child("completed").getValue(Boolean::class.java) ?: false

                        val levelTileView =
                            layoutInflater.inflate(R.layout.level_tile, gridLayout, false)
                        val tileText = levelTileView.findViewById<TextView>(R.id.tileText)
                        tileText.text = levelId.capitalize()

                        val isLevelUnlocked =
                            levelId == "level1" || isPreviousLevelCompleted(levelId)
                        levelTileView.isEnabled = isLevelUnlocked
                        levelTileView.setOnClickListener {
                            if (isLevelUnlocked) {
                                startLessonActivity(levelId)
                            }
                        }

                        val tileBackground = if (isLevelUnlocked) {
                            R.drawable.tile_unlocked
                        } else {
                            R.drawable.tile_locked
                        }
                        levelTileView.background = resources.getDrawable(tileBackground, null)

                        gridLayout.addView(levelTileView)
                    }

                    levelsContainer.addView(gridLayout)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
            }
        })
    }

    private fun isPreviousLevelCompleted(levelId: String): Boolean {
        val levelIndex = levelId.replace("level", "").toIntOrNull() ?: return false
        val previousLevelId = "level${levelIndex - 1}"
        var previousLevelCompleted = false

        database.child(previousLevelId).child("completed").get().addOnSuccessListener {
            previousLevelCompleted = it.getValue(Boolean::class.java) ?: false
        }
        return previousLevelCompleted
    }

    private fun startLessonActivity(level: String) {
        val intent = Intent(activity, LessonActivity::class.java)
        intent.putExtra("levelId", level)
        startActivity(intent)
    }

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
                levelButton.text = level
                levelButton.setOnClickListener {
                    onLevelSelected(level)
                }
            }
        }
    }
}

