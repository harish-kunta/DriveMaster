package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.LessonActivity
import com.harish.drivemaster.activities.StreakIncreasedActivity
import com.harish.drivemaster.helpers.HapticFeedbackUtil
import com.harish.drivemaster.helpers.UserViewModel

class LearnFragment : Fragment() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var levelsContainer: RecyclerView
    private lateinit var streakValue: TextView
    private lateinit var heartsValue: TextView

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
        initializeUIComponents(view)
        setupViewModel()
        return view
    }

    private fun initializeUIComponents(view: View) {
        levelsContainer = view.findViewById(R.id.recyclerView)
        streakValue = view.findViewById(R.id.streakValue)
        heartsValue = view.findViewById(R.id.heartsValue)
    }

    private fun setupViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        userViewModel.streak.observe(viewLifecycleOwner, Observer { streak ->
            streakValue.text = streak.toString()
        })

        userViewModel.heartsLeft.observe(viewLifecycleOwner, Observer { heartsLeft ->
            heartsValue.text = heartsLeft.toString()
        })

        userViewModel.completedLevels.observe(viewLifecycleOwner, Observer { completedLevels ->
            setupRecyclerView(completedLevels)
        })

        userViewModel.lastRegenTime.observe(viewLifecycleOwner, Observer { lastRegenTime ->
            regenerateHeartsIfNeeded(lastRegenTime)
        })
    }

    private fun setupRecyclerView(completedLevels: Set<Int>) {
        levelsContainer.layoutManager = LinearLayoutManager(context)
        val levels = levelCategories.mapNotNull { (categoryName, levelRange) ->
            val levelsInCategory = levelRange.toList()
            if (levelsInCategory.isNotEmpty()) LevelCategory(categoryName, levelsInCategory) else null
        }
        val maxCompletedLevel = completedLevels.maxOrNull() ?: 0
        levelsContainer.adapter = LevelCategoryAdapter(levels, completedLevels, maxCompletedLevel) { levelId ->
            if (levelId <= maxCompletedLevel + 1) {
                navigateToLessonActivity(levelId)
            } else {
                showLockedLevelPopup()
            }
        }
    }

    private fun regenerateHeartsIfNeeded(lastRegenTime: Long) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastRegenTime
        val hoursElapsed = elapsedTime / REGEN_INTERVAL_MS

        if (hoursElapsed > 0 && (userViewModel.heartsLeft.value ?: 0) < MAX_HEARTS) {
            val newHearts = minOf((userViewModel.heartsLeft.value ?: 0) + hoursElapsed.toInt(), MAX_HEARTS)
            userViewModel.saveHeartsData(newHearts, currentTime)
        }
    }

    private fun navigateToLessonActivity(levelId: Int) {
        if (userViewModel.heartsLeft.value ?: 0 > 0) {
            startActivity(Intent(context, LessonActivity::class.java).apply {
                putExtra("levelId", levelId.toString())
            })
        } else {
            showNoHeartsPopup()
        }
//        startActivity(Intent(context, StreakIncreasedActivity::class.java).apply {
//            putExtra("levelId", levelId.toString())
//        })
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
            holder.bind(categories[position], completedLevels, maxCompletedLevel, onLevelSelected)
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
            holder.bind(levels[position], completedLevels, maxCompletedLevel, onLevelSelected)
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
                levelButton.text = "Level"
                levelNumber.text = levelId.toString()

                val context = itemView.context

                val backgroundColorRes = when {
                    levelId in completedLevels -> R.color.completedLevelColor
                    levelId == maxCompletedLevel + 1 -> R.color.nextLevelColor
                    else -> R.color.lockedLevelColor
                }

                itemView.setBackgroundColor(ContextCompat.getColor(context, backgroundColorRes))

                itemView.setOnClickListener {
                    HapticFeedbackUtil.performHapticFeedback(context)
                    onLevelSelected(levelId)
                }
            }
        }
    }
}


