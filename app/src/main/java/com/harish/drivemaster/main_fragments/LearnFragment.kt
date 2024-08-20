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
        levelsContainer = view.findViewById(R.id.recyclerView)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("lessons")

        loadLevels() // Load levels from Firebase

        return view
    }

    private fun loadLevels() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val levels = mutableListOf<LevelCategory>()

                levelCategories.forEach { (categoryName, levelRange) ->
                    val levelsInCategory = mutableListOf<String>()
                    levelRange.forEach { levelIndex ->
                        val levelId = "level$levelIndex"
                        if (snapshot.hasChild(levelId)) {
                            levelsInCategory.add(levelId)
                        }
                    }
                    if (levelsInCategory.isNotEmpty()) {
                        levels.add(LevelCategory(categoryName, levelsInCategory))
                    }
                }

                setupRecyclerView(levels)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors here
            }
        })
    }

    private fun setupRecyclerView(levels: List<LevelCategory>) {
        val recyclerView: RecyclerView = levelsContainer.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = LevelCategoryAdapter(levels) { level ->
            Toast.makeText(context, "Selected $level", Toast.LENGTH_SHORT).show()
        }
    }

    data class LevelCategory(val category: String, val levels: List<String>)

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
                levelButton.text = level.capitalize()
                itemView.setOnClickListener {
                    onLevelSelected(level)
                }
            }
        }
    }
}

