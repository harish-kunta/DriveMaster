package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.LessonActivity
import com.harish.drivemaster.adapters.LevelsAdapter
import com.harish.drivemaster.models.Level

// LearnFragment.kt
class LearnFragment : Fragment() {

    private lateinit var levelsAdapter: LevelsAdapter
    private lateinit var levelsList: MutableList<Level>
    private lateinit var rvLevels: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_learn, container, false)
        rvLevels = view.findViewById(R.id.rvLevels)
        rvLevels.layoutManager = LinearLayoutManager(context)

        levelsList = mutableListOf()
        levelsAdapter = LevelsAdapter(levelsList) { levelId ->
            openLessonActivity(levelId)
        }
        rvLevels.adapter = levelsAdapter

        fetchAndDisplayLevels()

        return view
    }

    private fun fetchAndDisplayLevels() {
        val levelsRef = FirebaseDatabase.getInstance().getReference("lessons")
        levelsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                levelsList.clear()
                for (snapshot in dataSnapshot.children) {
                    val level = snapshot.getValue(Level::class.java)
                    level?.let { levelsList.add(it) }
                }
                levelsList.sortBy { it.id }
                levelsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load levels.", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun openLessonActivity(levelId: String) {
        val intent = Intent(activity, LessonActivity::class.java)
        intent.putExtra("levelId", levelId)
        startActivity(intent)
    }
}
